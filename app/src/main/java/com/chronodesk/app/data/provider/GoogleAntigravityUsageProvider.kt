package com.chronodesk.app.data.provider

import android.util.Log
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.network.NetworkClient
import com.chronodesk.app.data.security.JwtUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Google Antigravity (agy) 官方 OAuth 2.0 PKCE 授权与多桶额度查询适配器。
 *
 * 严格对齐 farion1231/cc-switch (`agy_account/quota.rs`、`agy_account/oauth.rs`、`subscription_agy.rs`)：
 * 1. 使用 agy 官方客户端凭据与作用域完成 OAuth 2.0 PKCE 授权及 Refresh Token 续期。
 * 2. 两步式向 `daily-cloudcode-pa.googleapis.com/v1internal` 查询：
 *    - 先调用 `loadCodeAssist` 获取套餐名称、托管项目 `cloudaicompanionProject` 与使用资格；
 *    - 再携带 `project` 调用 `retrieveUserQuotaSummary` 获取 Gemini / Claude&GPT 等额度桶；
 *    - `User-Agent` 必须包含 `antigravity/cli/<version>`（>= 1.2.0），否则服务端拒绝或遗漏 Gemini 桶。
 */
open class GoogleAntigravityUsageProvider(
    private val client: OkHttpClient = NetworkClient.antigravityHttpClient,
    private val quotaApiBase: String = DEFAULT_QUOTA_API_BASE,
    private val tokenEndpoint: String = DEFAULT_TOKEN_ENDPOINT,
    private val userInfoEndpoint: String = DEFAULT_USERINFO_ENDPOINT,
    private val agyVersion: String = FALLBACK_AGY_VERSION
) {

    data class PkcePair(
        val state: String,
        val verifier: String,
        val challenge: String
    )

    data class ParsedCredential(
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val idToken: String? = null,
        val email: String? = null,
        val expiresInSeconds: Long = 3599L
    )

    data class AccountQuotaPayload(
        val tiers: List<CodexUsageWindow>,
        val plan: String?
    )

    private sealed class QuotaCallResult {
        data class Success(val payload: AccountQuotaPayload) : QuotaCallResult()
        data class AuthExpired(val message: String) : QuotaCallResult()
        data class Ineligible(val reason: String) : QuotaCallResult()
        data class AccessDenied(val message: String) : QuotaCallResult()
        data class RateLimited(val message: String, val retryAfterSeconds: Long?) : QuotaCallResult()
        data class ServerError(val code: Int, val message: String) : QuotaCallResult()
        data class DataFormatError(val message: String) : QuotaCallResult()
        data class NetworkError(val message: String) : QuotaCallResult()
    }

    private data class HttpCallResponse(
        val statusCode: Int,
        val body: String,
        val retryAfterSeconds: Long? = null
    )

    fun buildUserAgent(ver: String = agyVersion): String {
        val cleanVer = ver.trim().ifEmpty { FALLBACK_AGY_VERSION }
        return "ChronoDesk/1.0.0 (antigravity/cli/$cleanVer)"
    }

    fun generatePkcePair(): PkcePair {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val verifier = encoder.encodeToString(randomBytes)
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = encoder.encodeToString(digest)
        val state = UUID.randomUUID().toString().replace("-", "")
        return PkcePair(state = state, verifier = verifier, challenge = challenge)
    }

    fun redirectUri(port: Int): String = "http://127.0.0.1:$port$CALLBACK_PATH"

    fun authorizationUrl(redirectUri: String, pkce: PkcePair): String {
        val params = listOf(
            "client_id" to CLIENT_ID,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to SCOPES.joinToString(" "),
            "state" to pkce.state,
            "code_challenge" to pkce.challenge,
            "code_challenge_method" to "S256",
            "access_type" to "offline",
            "prompt" to "select_account consent"
        )
        val query = params.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        return "$AUTH_ENDPOINT?$query"
    }

    /**
     * 解析浏览器回调 URL（如 `http://127.0.0.1:51234/oauth-callback?code=...&state=...`）或纯授权码。
     */
    fun extractCodeFromCallbackInput(input: String, expectedState: String? = null): Result<String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("回调内容不能为空"))
        }
        if (!trimmed.contains("://") && !trimmed.contains("?") && !trimmed.contains("code=")) {
            return Result.success(trimmed)
        }
        val query = try {
            if (trimmed.contains("://")) {
                URI(trimmed).rawQuery ?: ""
            } else {
                trimmed.substringAfter("?", trimmed)
            }
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("无法解析回调链接: ${e.message}"))
        }
        val params = parseFormQuery(query)
        val state = params["state"]
        if (expectedState != null && !state.isNullOrEmpty() && state != expectedState) {
            return Result.failure(IllegalArgumentException("回调 state 校验失败，请重新发起登录"))
        }
        val err = params["error"]
        if (!err.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Google 登录取消或被拒绝: ${sanitizeCode(err)}"))
        }
        val code = params["code"]?.trim()
        if (code.isNullOrEmpty()) {
            return Result.failure(IllegalArgumentException("回调链接中缺少授权码 code"))
        }
        return Result.success(code)
    }

    suspend fun exchangeCode(
        code: String,
        redirectUri: String,
        codeVerifier: String
    ): TokenResult = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code.trim())
            .add("redirect_uri", redirectUri)
            .add("client_id", CLIENT_ID)
            .add("client_secret", clientSecret())
            .add("code_verifier", codeVerifier)
            .build()
        postToken(form, fallbackRefreshToken = null)
    }

    open suspend fun refreshToken(refreshToken: String): TokenResult = withContext(Dispatchers.IO) {
        val cleanRefresh = refreshToken.trim()
        if (cleanRefresh.isEmpty()) {
            return@withContext TokenResult.ExpiredOrInvalid("缺少 Refresh Token，请重新登录")
        }
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", cleanRefresh)
            .add("client_id", CLIENT_ID)
            .add("client_secret", clientSecret())
            .build()
        postToken(form, fallbackRefreshToken = cleanRefresh)
    }

    private fun postToken(form: FormBody, fallbackRefreshToken: String?): TokenResult {
        return try {
            val req = Request.Builder()
                .url(tokenEndpoint)
                .post(form)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                val status = resp.code
                val bytes = resp.body?.bytes() ?: ByteArray(0)
                if (bytes.size > MAX_TOKEN_RESPONSE_BYTES) {
                    return TokenResult.TransientError("Google OAuth 响应过大")
                }
                val text = String(bytes, Charsets.UTF_8)
                val json = try {
                    JSONObject(text)
                } catch (_: Exception) {
                    return TokenResult.TransientError("换取令牌失败: HTTP $status，响应非标准 JSON")
                }
                if (status !in 200..299) {
                    val errCode = sanitizeCode(json.optCleanString("error") ?: "")
                    if (errCode.equals("invalid_grant", ignoreCase = true) || status == 401) {
                        return TokenResult.ExpiredOrInvalid("Antigravity 登录已失效，需要重新登录")
                    }
                    return TokenResult.TransientError("换取令牌失败: HTTP $status $errCode".trim())
                }
                val accessToken = json.optCleanString("access_token")
                    ?: return TokenResult.TransientError("OAuth 响应缺少 access_token")
                val refresh = json.optCleanString("refresh_token") ?: fallbackRefreshToken
                val idToken = json.optCleanString("id_token")
                val expiresIn = if (json.has("expires_in") && !json.isNull("expires_in")) {
                    json.optLong("expires_in", 3599L).coerceAtLeast(60L)
                } else {
                    3599L
                }
                TokenResult.Success(
                    accessToken = accessToken,
                    refreshToken = refresh,
                    idToken = idToken,
                    expiresInSeconds = expiresIn
                )
            }
        } catch (e: IOException) {
            Log.w(TAG, "postToken network error: ${e.message}")
            TokenResult.TransientError("请求 Google OAuth 失败，请检查代理或网络: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "postToken unexpected error", e)
            TokenResult.TransientError("令牌处理异常: ${e.message}")
        }
    }

    suspend fun fetchIdentityEmail(accessToken: String, idToken: String? = null): String? {
        JwtUtils.extractEmail(idToken)?.let { return it }
        JwtUtils.extractEmail(accessToken)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(userInfoEndpoint)
                    .get()
                    .header("Authorization", "Bearer ${accessToken.trim()}")
                    .header("Accept", "application/json")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val bytes = resp.body?.bytes() ?: return@withContext null
                    if (bytes.size > MAX_TOKEN_RESPONSE_BYTES) return@withContext null
                    val json = JSONObject(String(bytes, Charsets.UTF_8))
                    json.optCleanString("email")?.takeIf { it.contains("@") }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    open suspend fun fetchUsage(
        accountId: String,
        accountAlias: String,
        accessToken: String?,
        emailHint: String? = null
    ): UsageFetchResult = withContext(Dispatchers.IO) {
        val cleanToken = accessToken?.trim() ?: ""
        if (cleanToken.isEmpty() || cleanToken == "demo") {
            return@withContext UsageFetchResult.Success(
                CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "Antigravity" },
                    email = emailHint,
                    credentialStatus = CredentialStatus.NOT_LOGGED_IN,
                    isAvailable = false,
                    queriedAt = null,
                    errorMessage = "未授权 Google Antigravity 账号",
                    modelProvider = "ANTIGRAVITY"
                )
            )
        }
        if (cleanToken.startsWith("AIza")) {
            return@withContext UsageFetchResult.AuthExpired(
                "旧版 Gemini API Key 不支持查询 Antigravity 额度，请改用 Google 账号授权",
                401
            )
        }

        when (val result = fetchQuotaInternal(cleanToken)) {
            is QuotaCallResult.Success -> {
                val tiers = result.payload.tiers
                val geminiBottleneck = selectBottleneckWindow(
                    tiers = tiers,
                    groupLabel = "Gemini",
                    names = setOf(
                        CodexUsageWindow.TIER_AGY_GEMINI_FIVE_HOUR,
                        CodexUsageWindow.TIER_AGY_GEMINI_WEEKLY,
                        CodexUsageWindow.TIER_AGY_GEMINI_ARGON_WEEKLY
                    )
                )
                val thirdPartyBottleneck = selectBottleneckWindow(
                    tiers = tiers,
                    groupLabel = "Claude/GPT",
                    names = setOf(
                        CodexUsageWindow.TIER_AGY_THIRD_PARTY_FIVE_HOUR,
                        CodexUsageWindow.TIER_AGY_THIRD_PARTY_WEEKLY
                    )
                )
                val nowMs = System.currentTimeMillis()
                val snapshot = CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "Antigravity" },
                    email = emailHint,
                    credentialStatus = CredentialStatus.VALID,
                    primaryWindow = geminiBottleneck ?: tiers.firstOrNull(),
                    secondaryWindow = thirdPartyBottleneck ?: tiers.getOrNull(1),
                    quotaWindows = tiers,
                    isAvailable = true,
                    planName = result.payload.plan,
                    queriedAt = nowMs,
                    lastSuccessTime = nowMs,
                    isLastGood = false,
                    errorMessage = null,
                    limitReached = tiers.any { (it.remainingPercent ?: 100.0) <= 0.0 },
                    modelProvider = "ANTIGRAVITY"
                )
                UsageFetchResult.Success(snapshot)
            }
            is QuotaCallResult.AuthExpired -> UsageFetchResult.AuthExpired(result.message, 401)
            is QuotaCallResult.Ineligible -> UsageFetchResult.AccessDenied(result.reason, 403)
            is QuotaCallResult.AccessDenied -> UsageFetchResult.AccessDenied(result.message, 403)
            is QuotaCallResult.RateLimited -> UsageFetchResult.RateLimited(result.message, result.retryAfterSeconds)
            is QuotaCallResult.ServerError -> UsageFetchResult.ServerError(result.message, result.code)
            is QuotaCallResult.DataFormatError -> UsageFetchResult.DataFormatError(result.message)
            is QuotaCallResult.NetworkError -> UsageFetchResult.TransientError(result.message)
        }
    }

    private fun selectBottleneckWindow(
        tiers: List<CodexUsageWindow>,
        groupLabel: String,
        names: Set<String>
    ): CodexUsageWindow? {
        val groupTiers = tiers.filter { it.name in names }
        if (groupTiers.isEmpty()) return null
        val bottleneck = groupTiers.maxByOrNull { it.usedPercent ?: 0.0 } ?: return null
        return bottleneck.copy(label = groupLabel)
    }

    private fun fetchQuotaInternal(accessToken: String): QuotaCallResult {
        val loadResp = try {
            postInternal(
                accessToken = accessToken,
                method = "loadCodeAssist",
                bodyJson = JSONObject().put("metadata", JSONObject().put("ideType", "ANTIGRAVITY")).toString()
            )
        } catch (e: IOException) {
            Log.w(TAG, "loadCodeAssist failed, degrading to direct retrieveUserQuotaSummary: ${e.message}")
            null
        } catch (e: Exception) {
            Log.w(TAG, "loadCodeAssist parse/unexpected failure: ${e.message}")
            null
        }

        if (loadResp != null && loadResp.statusCode == 401) {
            return QuotaCallResult.AuthExpired("Google 拒绝了登录凭据 (HTTP 401)")
        }

        if (loadResp != null && loadResp.statusCode in 200..299) {
            val ineligible = parseIneligibleReason(loadResp.body)
            if (ineligible != null) {
                return QuotaCallResult.Ineligible(ineligible)
            }
            val plan = codeAssistPlan(loadResp.body)
            val project = parseProject(loadResp.body)
            return when (val tiersRes = fetchTiers(accessToken, project)) {
                is QuotaCallResult.Success -> QuotaCallResult.Success(
                    tiersRes.payload.copy(plan = plan)
                )
                else -> tiersRes
            }
        }

        // loadCodeAssist 因非 401 失败时，降级为不带项目直查额度
        val directRes = fetchTiers(accessToken, project = null)
        if (directRes is QuotaCallResult.AccessDenied) {
            val retryLoad = try {
                postInternal(
                    accessToken = accessToken,
                    method = "loadCodeAssist",
                    bodyJson = JSONObject().put("metadata", JSONObject().put("ideType", "ANTIGRAVITY")).toString()
                )
            } catch (_: Exception) {
                null
            }
            if (retryLoad != null && retryLoad.statusCode in 200..299) {
                parseIneligibleReason(retryLoad.body)?.let { reason ->
                    return QuotaCallResult.Ineligible(reason)
                }
            }
        }
        return directRes
    }

    private fun fetchTiers(accessToken: String, project: String?): QuotaCallResult {
        val reqBody = JSONObject()
        if (!project.isNullOrBlank()) {
            reqBody.put("project", project)
        }
        val resp = try {
            postInternal(accessToken, "retrieveUserQuotaSummary", reqBody.toString())
        } catch (e: IOException) {
            return QuotaCallResult.NetworkError("网络连接失败，请检查代理或网络: ${e.message}")
        } catch (e: Exception) {
            return QuotaCallResult.DataFormatError("额度响应异常: ${e.message}")
        }

        return when (resp.statusCode) {
            401 -> QuotaCallResult.AuthExpired("Google 拒绝了登录凭据 (HTTP 401)")
            403 -> {
                val msg = errorMessage(resp.body).ifBlank { "无有效 Antigravity 许可 (HTTP 403)" }
                QuotaCallResult.AccessDenied(msg)
            }
            429 -> QuotaCallResult.RateLimited("Antigravity 接口请求过于频繁 (HTTP 429)", resp.retryAfterSeconds)
            in 500..599 -> QuotaCallResult.ServerError(
                resp.statusCode,
                "Google 服务暂时不可用 (HTTP ${resp.statusCode})"
            )
            in 200..299 -> {
                val tiers = parseSummaryTiers(resp.body)
                if (tiers.isNullOrEmpty()) {
                    QuotaCallResult.DataFormatError("未解析到有效 Antigravity 额度桶")
                } else {
                    QuotaCallResult.Success(AccountQuotaPayload(tiers = tiers, plan = null))
                }
            }
            else -> {
                val msg = errorMessage(resp.body)
                QuotaCallResult.NetworkError("Google 返回 HTTP ${resp.statusCode}${if (msg.isNotBlank()) ": $msg" else ""}")
            }
        }
    }

    private fun postInternal(
        accessToken: String,
        method: String,
        bodyJson: String
    ): HttpCallResponse {
        val base = quotaApiBase.trimEnd('/')
        val url = "$base:$method"
        val req = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", buildUserAgent())
            .header("Accept", "application/json")
            .build()

        client.newCall(req).execute().use { resp ->
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            if (bytes.size > MAX_BODY_BYTES) {
                throw IllegalStateException("响应体超过 256KB 上限")
            }
            val retrySec = resp.header("Retry-After")?.toLongOrNull()
            return HttpCallResponse(
                statusCode = resp.code,
                body = String(bytes, Charsets.UTF_8),
                retryAfterSeconds = retrySec
            )
        }
    }

    companion object {
        private const val TAG = "AntigravityProvider"
        const val DEFAULT_QUOTA_API_BASE = "https://daily-cloudcode-pa.googleapis.com/v1internal"
        const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val DEFAULT_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        const val DEFAULT_USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo"
        const val CALLBACK_PATH = "/oauth-callback"
        const val FALLBACK_AGY_VERSION = "1.5.14"

        const val CLIENT_ID = "1071006060591-tmhssin2h21lcre235vtolojh4g403ep.apps.googleusercontent.com"
        private val CLIENT_SECRET_PARTS = arrayOf("GOCSPX-", "K58FWR486LdLJ1mLB8sXC4z6qDAf")
        val SCOPES = listOf(
            "openid",
            "https://www.googleapis.com/auth/cloud-platform",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
            "https://www.googleapis.com/auth/cclog",
            "https://www.googleapis.com/auth/experimentsandconfigs",
            "https://www.googleapis.com/auth/aicode"
        )

        private const val MAX_BODY_BYTES = 256 * 1024
        private const val MAX_TOKEN_RESPONSE_BYTES = 64 * 1024
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun clientSecret(): String = CLIENT_SECRET_PARTS.joinToString("")

        fun sanitizeCode(value: String): String =
            value.filter { it.isLetterOrDigit() || it == '_' }.take(64)

        /**
         * 解析用户输入的多形态凭据：
         * - `agy` 钥匙串结构：`{"token": {...}, "id_token": "..."}`
         * - `cc-switch` 导出结构：`{"credential": {"token": {...}}, "email": "..."}`
         * - 扁平 OAuth JSON：`{"access_token": "...", "refresh_token": "..."}`
         * - 裸 Refresh Token (`1//...`) 或裸 Access Token (`ya29...`)
         */
        fun parseCredentialInput(rawInput: String): ParsedCredential? {
            val trimmed = rawInput.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.startsWith("{")) {
                return try {
                    val root = JSONObject(trimmed)
                    val email = root.optCleanString("email")
                    val credObj = root.optJSONObject("credential") ?: root
                    val tokenObj = credObj.optJSONObject("token") ?: credObj
                    val access = tokenObj.optCleanString("access_token")
                        ?: tokenObj.optCleanString("accessToken")
                    val refresh = tokenObj.optCleanString("refresh_token")
                        ?: tokenObj.optCleanString("refreshToken")
                    val idToken = credObj.optCleanString("id_token")
                        ?: credObj.optCleanString("idToken")
                        ?: tokenObj.optCleanString("id_token")
                    val expiresIn = when {
                        tokenObj.has("expires_in") && !tokenObj.isNull("expires_in") ->
                            tokenObj.optLong("expires_in", 3599L).coerceAtLeast(60L)
                        else -> 3599L
                    }
                    if (access.isNullOrBlank() && refresh.isNullOrBlank()) {
                        null
                    } else {
                        ParsedCredential(
                            accessToken = access,
                            refreshToken = refresh,
                            idToken = idToken,
                            email = email ?: JwtUtils.extractEmail(idToken) ?: JwtUtils.extractEmail(access),
                            expiresInSeconds = expiresIn
                        )
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (trimmed.startsWith("AIza")) {
                return null
            }
            return if (trimmed.startsWith("1//")) {
                ParsedCredential(refreshToken = trimmed)
            } else {
                ParsedCredential(accessToken = trimmed)
            }
        }

        fun parseSummaryTiers(body: String): List<CodexUsageWindow>? {
            val root = try {
                JSONObject(body)
            } catch (_: Exception) {
                return null
            }
            val groups = root.optJSONArray("groups") ?: return null
            val tiers = mutableListOf<CodexUsageWindow>()
            for (gIdx in 0 until groups.length()) {
                val group = groups.optJSONObject(gIdx) ?: continue
                val buckets = group.optJSONArray("buckets") ?: continue
                for (bIdx in 0 until buckets.length()) {
                    val bucket = buckets.optJSONObject(bIdx) ?: continue
                    val bucketId = bucket.optCleanString("bucketId") ?: bucket.optCleanString("id")
                    val remainingFraction = if (bucket.has("remainingFraction") && !bucket.isNull("remainingFraction")) {
                        bucket.optDouble("remainingFraction")
                    } else if (bucket.has("remaining_fraction") && !bucket.isNull("remaining_fraction")) {
                        bucket.optDouble("remaining_fraction")
                    } else {
                        null
                    }
                    val resetTime = bucket.optCleanString("resetTime") ?: bucket.optCleanString("reset_time")
                    val tier = CodexUsageWindow.fromAntigravityBucket(
                        bucketId = bucketId,
                        remainingFraction = remainingFraction,
                        resetTimeIso = resetTime
                    )
                    if (tier != null) {
                        tiers.add(tier)
                    }
                }
            }
            if (tiers.isEmpty()) return null
            return tiers.sortedBy { CodexUsageWindow.antigravitySortOrder(it.name) }
        }

        fun codeAssistPlan(body: String): String? {
            val root = try {
                JSONObject(body)
            } catch (_: Exception) {
                return null
            }
            val paidTier = root.optJSONObject("paidTier")
            val paidName = paidTier?.optCleanString("name")
            if (paidName != null) {
                val paidId = paidTier.optCleanString("id")
                return formatPlanName(paidName, paidId)
            }
            val currentTier = root.optJSONObject("currentTier") ?: return null
            val currentId = currentTier.optCleanString("id") ?: return null
            return when (currentId) {
                "free-tier" -> "Free"
                "standard-tier" -> "Paid"
                else -> currentTier.optCleanString("name")?.let { formatPlanName(it, currentId) }
            }
        }

        private fun formatPlanName(name: String, tierId: String?): String {
            val short = stripGoogleAiPrefix(name)
            return withUltraVariant(short, tierId)
        }

        private fun stripGoogleAiPrefix(name: String): String {
            if (name.length < 7) return name
            val prefix = name.substring(0, 7)
            if (!prefix.equals("google ", ignoreCase = true)) return name
            val rest = name.substring(7).trimStart()
            if (rest.length >= 3 && rest.substring(0, 3).equals("ai ", ignoreCase = true) && rest.substring(3).trim().isNotEmpty()) {
                return rest
            }
            return name
        }

        private fun withUltraVariant(name: String, tierId: String?): String {
            val variant = when (tierId) {
                "g1-ultra-lite-tier" -> "5x"
                "g1-ultra-tier" -> "20x"
                else -> return name
            }
            val lower = name.lowercase()
            if (!lower.contains("ultra") || lower.contains("5x") || lower.contains("20x")) {
                return name
            }
            return "$name $variant"
        }

        fun parseProject(body: String): String? {
            val root = try {
                JSONObject(body)
            } catch (_: Exception) {
                return null
            }
            if (!root.has("cloudaicompanionProject") || root.isNull("cloudaicompanionProject")) {
                return null
            }
            return when (val raw = root.opt("cloudaicompanionProject")) {
                is String -> raw.trim().takeIf { it.isNotEmpty() }
                is JSONObject -> raw.optCleanString("id") ?: raw.optCleanString("projectId")
                else -> null
            }
        }

        fun parseIneligibleReason(body: String): String? {
            val root = try {
                JSONObject(body)
            } catch (_: Exception) {
                return null
            }
            val allowedTiers = root.optJSONArray("allowedTiers") ?: JSONArray()
            val ineligibleTiers = root.optJSONArray("ineligibleTiers") ?: JSONArray()
            val currentTier = root.optJSONObject("currentTier")
            val currentId = currentTier?.optCleanString("id")
            val gcpManaged = root.has("gcpManaged") && !root.isNull("gcpManaged") && root.optBoolean("gcpManaged", false)

            var hasFreeTier = currentId == "free-tier"
            if (!hasFreeTier) {
                for (i in 0 until allowedTiers.length()) {
                    val item = allowedTiers.optJSONObject(i) ?: continue
                    if (item.optCleanString("id") == "free-tier") {
                        hasFreeTier = true
                        break
                    }
                }
            }

            if (!gcpManaged && !hasFreeTier) {
                for (i in 0 until ineligibleTiers.length()) {
                    val item = ineligibleTiers.optJSONObject(i) ?: continue
                    if (item.optCleanString("tierId") == "free-tier") {
                        val reason = item.optCleanString("reasonMessage")
                        if (reason != null) return reason
                    }
                }
            }

            if (currentTier != null) return null

            for (i in 0 until ineligibleTiers.length()) {
                val item = ineligibleTiers.optJSONObject(i) ?: continue
                val reason = item.optCleanString("reasonMessage")
                if (reason != null) return reason
            }
            return null
        }

        fun errorMessage(body: String): String {
            return try {
                val root = JSONObject(body)
                root.optJSONObject("error")?.optCleanString("message")?.take(200) ?: ""
            } catch (_: Exception) {
                ""
            }
        }

        private fun parseFormQuery(query: String): Map<String, String> {
            val map = mutableMapOf<String, String>()
            if (query.isBlank()) return map
            query.split("&").forEach { pair ->
                if (pair.isBlank()) return@forEach
                val idx = pair.indexOf('=')
                val rawKey = if (idx >= 0) pair.substring(0, idx) else pair
                val rawVal = if (idx >= 0) pair.substring(idx + 1) else ""
                val key = URLDecoder.decode(rawKey, "UTF-8")
                val value = URLDecoder.decode(rawVal, "UTF-8")
                map[key] = value
            }
            return map
        }

        private fun JSONObject.optCleanString(key: String): String? {
            if (!has(key) || isNull(key)) return null
            val raw = opt(key) ?: return null
            if (raw == JSONObject.NULL || raw !is String) return null
            return raw.trim().takeIf { it.isNotEmpty() }
        }
    }
}
