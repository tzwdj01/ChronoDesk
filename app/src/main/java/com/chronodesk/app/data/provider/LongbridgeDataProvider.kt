package com.chronodesk.app.data.provider

import com.chronodesk.app.data.model.LongbridgeCashItem
import com.chronodesk.app.data.model.LongbridgeConnectionState
import com.chronodesk.app.data.model.LongbridgeFullSnapshot
import com.chronodesk.app.data.model.LongbridgeIndexQuote
import com.chronodesk.app.data.model.LongbridgeMarketTemp
import com.chronodesk.app.data.model.LongbridgePositionItem
import com.chronodesk.app.data.model.LongbridgeRatePair
import com.chronodesk.app.data.model.LongbridgeSettings
import com.chronodesk.app.data.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.Locale
import kotlin.math.abs

data class LongbridgePkcePair(
    val codeVerifier: String,
    val codeChallenge: String,
    val state: String
)

sealed class LongbridgeAuthResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String? = null,
        val expiresAtMs: Long = Long.MAX_VALUE,
        val clientId: String? = null
    ) : LongbridgeAuthResult()

    data class AuthError(val message: String) : LongbridgeAuthResult()
    data class NetworkError(val message: String) : LongbridgeAuthResult()
}

sealed class LongbridgeFetchResult {
    data class Success(val snapshot: LongbridgeFullSnapshot) : LongbridgeFetchResult()
    data class AuthExpired(val message: String) : LongbridgeFetchResult()
    data class TransientError(val message: String) : LongbridgeFetchResult()
}

data class LongbridgeParsedCredential(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtMs: Long = Long.MAX_VALUE,
    val clientId: String? = null
)

private sealed class McpToolOutcome {
    data class Ok(val rawText: String) : McpToolOutcome()
    data class AuthExpired(val message: String) : McpToolOutcome()
    data class ScopeMissing(val message: String) : McpToolOutcome()
    data class Failed(val message: String) : McpToolOutcome()
}

private data class QuoteMetric(
    val symbol: String,
    val lastDone: Double,
    val prevClose: Double?,
    val effectivePrevClose: Double?,
    val sessionTag: String?
)

private data class ExecutionStats(
    val buyQty: Double = 0.0,
    val buyCost: Double = 0.0,
    val sellQty: Double = 0.0,
    val sellProceeds: Double = 0.0
)

private data class BenchmarkIndexSpec(
    val symbol: String,
    val shortName: String,
    val market: String
)

/**
 * 长桥 (Longbridge) 官方托管 MCP + OAuth 2.1 只读数据提供者：
 * - 支持中国大陆 `.cn` 与国际 `.com` 端点；
 * - 认证支持：一次性 `/agent` 短授权码兑换（严格只读）、OAuth 2.1 PKCE 浏览器授权、及本地凭据导入；
 * - 数据采集完全走无状态 HTTPS JSON-RPC，不占用账号唯一的 WebSocket 行情长连接。
 */
open class LongbridgeDataProvider @JvmOverloads constructor(
    private val httpClient: OkHttpClient = NetworkClient.longbridgeHttpClient,
    private val mcpBaseUrlOverride: String? = null,
    private val oauthBaseUrlOverride: String? = null
) {

    fun resolveMcpBaseUrl(region: String): String {
        mcpBaseUrlOverride?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }?.let { return it }
        return if (region.equals("GLOBAL", ignoreCase = true)) {
            "https://mcp.longbridge.com"
        } else {
            "https://mcp.longbridge.cn"
        }
    }

    fun resolveOAuthBaseUrl(region: String): String {
        oauthBaseUrlOverride?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }?.let { return it }
        return if (region.equals("GLOBAL", ignoreCase = true)) {
            "https://openapi.longbridge.com/oauth2"
        } else {
            "https://openapi.longbridge.cn/oauth2"
        }
    }

    fun resolveConnectUrl(region: String): String {
        return if (region.equals("GLOBAL", ignoreCase = true)) {
            "https://open.longbridge.com/connect"
        } else {
            "https://open.longbridge.cn/connect"
        }
    }

    fun generatePkcePair(): LongbridgePkcePair {
        val random = SecureRandom()
        val verifierBytes = ByteArray(32)
        random.nextBytes(verifierBytes)
        val codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
        val codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        val stateBytes = ByteArray(16)
        random.nextBytes(stateBytes)
        val state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes)
        return LongbridgePkcePair(codeVerifier, codeChallenge, state)
    }

    /**
     * 为 Android 桌面看板注册公开 OAuth 2.1 客户端 (token_endpoint_auth_method=none + PKCE)
     */
    open suspend fun registerOAuthClient(
        region: String,
        redirectUris: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val oauthBase = resolveOAuthBaseUrl(region)
            val payload = JSONObject().apply {
                put("redirect_uris", JSONArray(redirectUris))
                put("token_endpoint_auth_method", "none")
                put("grant_types", JSONArray(listOf("authorization_code", "refresh_token")))
                put("response_types", JSONArray(listOf("code")))
                put("client_name", "ChronoDesk ReadOnly")
            }
            val request = Request.Builder()
                .url("$oauthBase/register")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("注册长桥 OAuth 客户端失败 (HTTP ${resp.code}): ${extractHttpErrorMessage(raw, resp.code)}")
                    )
                }
                val json = JSONObject(raw)
                val clientId = optCleanString(json, "client_id")
                if (clientId.isNullOrBlank()) {
                    return@withContext Result.failure(IllegalStateException("注册响应缺少 client_id"))
                }
                Result.success(clientId)
            }
        } catch (e: IOException) {
            Result.failure(IllegalStateException("网络异常：${e.message}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildAuthorizationUrl(
        region: String,
        clientId: String,
        redirectUri: String,
        pkce: LongbridgePkcePair
    ): String {
        val oauthBase = resolveOAuthBaseUrl(region)
        val enc = { s: String -> URLEncoder.encode(s, StandardCharsets.UTF_8.name()) }
        return "$oauthBase/authorize" +
            "?response_type=code" +
            "&client_id=${enc(clientId)}" +
            "&redirect_uri=${enc(redirectUri)}" +
            "&state=${enc(pkce.state)}" +
            "&code_challenge=${enc(pkce.codeChallenge)}" +
            "&code_challenge_method=S256"
    }

    fun extractCodeFromCallbackInput(
        rawInput: String,
        expectedState: String? = null
    ): Result<String> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("回调地址不能为空"))

        if (!trimmed.contains("://") && !trimmed.contains("?")) {
            return Result.success(trimmed)
        }
        return try {
            val normalizedUrl = if (trimmed.startsWith("/")) "http://localhost$trimmed" else trimmed
            val query = URI(normalizedUrl).rawQuery.orEmpty()
            val params = parseQueryParams(query)
            val err = params["error"]
            if (!err.isNullOrBlank()) {
                val desc = params["error_description"] ?: err
                return Result.failure(IllegalStateException("授权被拒绝：$desc"))
            }
            val actualState = params["state"]
            if (!expectedState.isNullOrBlank() && actualState != null && actualState != expectedState) {
                return Result.failure(IllegalStateException("OAuth state 校验不匹配，请重新发起授权"))
            }
            val code = params["code"]
            if (code.isNullOrBlank()) {
                Result.failure(IllegalArgumentException("回调 URL 中未包含授权 code"))
            } else {
                Result.success(code)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("无法解析回调地址：${e.message}"))
        }
    }

    open suspend fun exchangeCodeForToken(
        region: String,
        clientId: String,
        code: String,
        redirectUri: String,
        codeVerifier: String
    ): LongbridgeAuthResult = withContext(Dispatchers.IO) {
        try {
            val oauthBase = resolveOAuthBaseUrl(region)
            val formBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", clientId)
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .add("code_verifier", codeVerifier)
                .build()
            val request = Request.Builder()
                .url("$oauthBase/token")
                .post(formBody)
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                parseOAuthTokenHttpResponse(resp.code, raw, fallbackClientId = clientId, fallbackRefresh = null)
            }
        } catch (e: IOException) {
            LongbridgeAuthResult.NetworkError("换取令牌网络异常：${e.message}")
        } catch (e: Exception) {
            LongbridgeAuthResult.AuthError("换取令牌失败：${e.message}")
        }
    }

    open suspend fun refreshAccessToken(
        region: String,
        clientId: String,
        refreshToken: String
    ): LongbridgeAuthResult = withContext(Dispatchers.IO) {
        val cleanRefresh = refreshToken.trim()
        if (cleanRefresh.isEmpty() || clientId.isBlank()) {
            return@withContext LongbridgeAuthResult.AuthError("缺少 client_id 或 refresh_token，请重新授权")
        }
        try {
            val oauthBase = resolveOAuthBaseUrl(region)
            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("refresh_token", cleanRefresh)
                .build()
            val request = Request.Builder()
                .url("$oauthBase/token")
                .post(formBody)
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                parseOAuthTokenHttpResponse(resp.code, raw, fallbackClientId = clientId, fallbackRefresh = cleanRefresh)
            }
        } catch (e: IOException) {
            LongbridgeAuthResult.NetworkError("刷新令牌网络异常：${e.message}")
        } catch (e: Exception) {
            LongbridgeAuthResult.AuthError("刷新令牌异常：${e.message}")
        }
    }

    /**
     * 通过 `https://mcp.longbridge.cn/agent` 的 `authenticate` 工具兑换一次性短授权码
     */
    open suspend fun exchangeOneTimeAuthCode(
        region: String,
        authCode: String
    ): LongbridgeAuthResult = withContext(Dispatchers.IO) {
        val cleanCode = authCode.trim()
        if (cleanCode.isEmpty()) {
            return@withContext LongbridgeAuthResult.AuthError("请输入一次性授权码")
        }
        val agentUrl = "${resolveMcpBaseUrl(region)}/agent"
        val args = JSONObject().put("auth_code", cleanCode)
        when (val outcome = callMcpTool(endpointUrl = agentUrl, accessToken = null, toolName = "authenticate", args = args)) {
            is McpToolOutcome.Ok -> {
                val parsed = extractTokenFromAuthenticateText(outcome.rawText)
                    ?: return@withContext LongbridgeAuthResult.AuthError("授权码兑换成功但未解析到 Bearer Token")
                LongbridgeAuthResult.Success(
                    accessToken = parsed.accessToken,
                    refreshToken = parsed.refreshToken,
                    expiresAtMs = parsed.expiresAtMs,
                    clientId = parsed.clientId
                )
            }
            is McpToolOutcome.AuthExpired -> LongbridgeAuthResult.AuthError("授权码无效、已过期或已被使用：${outcome.message}")
            is McpToolOutcome.ScopeMissing -> LongbridgeAuthResult.AuthError(outcome.message)
            is McpToolOutcome.Failed -> {
                if (outcome.message.contains("网络") || outcome.message.contains("HTTP 5")) {
                    LongbridgeAuthResult.NetworkError(outcome.message)
                } else {
                    LongbridgeAuthResult.AuthError(outcome.message)
                }
            }
        }
    }

    /**
     * 拉取账户资产、多币种现金、持仓及当日实时盈亏
     */
    open suspend fun fetchSnapshot(
        accessToken: String,
        settings: LongbridgeSettings
    ): LongbridgeFetchResult = withContext(Dispatchers.IO) {
        val mcpUrl = resolveMcpBaseUrl(settings.normalizedRegion)

        // 1. 账户资产汇总 (必须成功)
        val balanceOutcome = callMcpTool(mcpUrl, accessToken, "account_balance", JSONObject())
        val balanceText = when (balanceOutcome) {
            is McpToolOutcome.Ok -> balanceOutcome.rawText
            is McpToolOutcome.AuthExpired -> return@withContext LongbridgeFetchResult.AuthExpired(balanceOutcome.message)
            is McpToolOutcome.ScopeMissing -> return@withContext LongbridgeFetchResult.AuthExpired("缺少账户资产只读权限 (account.read)：${balanceOutcome.message}")
            is McpToolOutcome.Failed -> return@withContext LongbridgeFetchResult.TransientError(balanceOutcome.message)
        }

        // 2. 股票持仓 (必须成功，兼容空持仓)
        val posOutcome = callMcpTool(mcpUrl, accessToken, "stock_positions", JSONObject())
        val posText = when (posOutcome) {
            is McpToolOutcome.Ok -> posOutcome.rawText
            is McpToolOutcome.AuthExpired -> return@withContext LongbridgeFetchResult.AuthExpired(posOutcome.message)
            is McpToolOutcome.ScopeMissing -> return@withContext LongbridgeFetchResult.AuthExpired("缺少股票持仓只读权限 (account.read)：${posOutcome.message}")
            is McpToolOutcome.Failed -> return@withContext LongbridgeFetchResult.TransientError(posOutcome.message)
        }

        // 3. 账户汇率 (可选，失败使用交叉或参考汇率兜底)
        val exchangeRates = when (val rateOutcome = callMcpTool(mcpUrl, accessToken, "exchange_rate", JSONObject())) {
            is McpToolOutcome.Ok -> parseExchangeRates(rateOutcome.rawText)
            else -> emptyList()
        }

        // 4. 当前账户身份 (可选)
        var accountId: String? = null
        var accountName: String? = null
        var dcRegion: String? = null
        when (val meOut = callMcpTool(mcpUrl, accessToken, "get_me", JSONObject())) {
            is McpToolOutcome.Ok -> {
                val meObj = parseJsonObjectSafe(meOut.rawText)
                if (meObj != null) {
                    accountId = optCleanString(meObj, "account_no")
                        ?: optCleanString(meObj, "account_id")
                        ?: optCleanString(meObj, "id")
                    accountName = optCleanString(meObj, "nickname")
                        ?: optCleanString(meObj, "account_name")
                    dcRegion = optCleanString(meObj, "dc_region")
                        ?: optCleanString(meObj, "dc")
                }
            }
            else -> {}
        }

        val balanceParsed = parseAccountBalance(balanceText)
            ?: return@withContext LongbridgeFetchResult.TransientError("账户资产响应格式异常")

        val rawPositions = parseStockPositions(posText)
        val symbols = rawPositions.map { it.symbol }.filter { it.isNotBlank() }.distinct().take(500)

        // 5. 若有持仓，批量获取行情 (last_done / prev_close / 美股盘前盘后夜盘) 与当日成交 (如有权限)
        val quotesMap: Map<String, QuoteMetric> = if (symbols.isNotEmpty()) {
            val quoteArgs = JSONObject().put("symbols", JSONArray(symbols))
            when (val qOut = callMcpTool(mcpUrl, accessToken, "quote", quoteArgs)) {
                is McpToolOutcome.Ok -> parseQuotes(qOut.rawText, settings.includeExtendedHours)
                else -> emptyMap()
            }
        } else {
            emptyMap()
        }

        var executionsMap: Map<String, ExecutionStats>? = null
        if (symbols.isNotEmpty()) {
            when (val exOut = callMcpTool(mcpUrl, accessToken, "today_executions", JSONObject())) {
                is McpToolOutcome.Ok -> executionsMap = parseTodayExecutions(exOut.rawText)
                else -> {} // 未勾选 trade.read 时静默降级
            }
        }

        // 6. 基金持仓与累计盈亏汇总 (均属可选只读接口，失败不阻断核心看板)
        var fundRecentProfitInBase = 0.0
        var hasFundRecentProfit = false
        when (val fundOut = callMcpTool(mcpUrl, accessToken, "fund_position_overview", JSONObject())) {
            is McpToolOutcome.Ok -> {
                val fundItems = parseFundOverview(fundOut.rawText)
                fundItems.forEach { (curr, recentPl) ->
                    if (recentPl != null) {
                        fundRecentProfitInBase += LongbridgeFullSnapshot.convertAmount(
                            recentPl,
                            curr,
                            balanceParsed.baseCurrency,
                            exchangeRates
                        )
                        hasFundRecentProfit = true
                    }
                }
            }
            else -> {}
        }

        var cumulativePl: Double? = null
        var cumulativeCurrency: String? = null
        when (val paOut = callMcpTool(mcpUrl, accessToken, "profit_analysis", JSONObject())) {
            is McpToolOutcome.Ok -> {
                val (paVal, paCurr) = parseProfitAnalysis(paOut.rawText)
                cumulativePl = paVal
                cumulativeCurrency = paCurr
            }
            else -> {}
        }

        // 7. 计算逐标的与组合整体当日盈亏
        var totalStockTodayPlInBase = 0.0
        var hasAnyStockTodayPl = false
        var totalStockMvInBase = 0.0
        var hasAnyStockMv = false

        val enrichedPositions = rawPositions.map { pos ->
            val q = quotesMap[pos.symbol]
            val effectiveLast = q?.lastDone ?: pos.presetLastDone
            val effectivePrev = when {
                q != null && q.effectivePrevClose != null -> q.effectivePrevClose
                q != null && q.prevClose != null -> q.prevClose
                else -> pos.presetPrevClose
            }
            val marketValue = when {
                effectiveLast != null -> pos.quantity * effectiveLast
                else -> pos.presetMarketValue
            }
            val exec = executionsMap?.get(pos.symbol)

            val todayPl: Double? = when {
                // 若美股子账户已直接给出 today_pl，且未处于更新的盘前/盘后/夜盘扩展价格阶段
                pos.presetTodayPl != null && (q == null || q.sessionTag == null || !settings.includeExtendedHours) -> {
                    pos.presetTodayPl
                }
                effectiveLast != null && effectivePrev != null -> {
                    if (executionsMap != null && pos.initQuantity != null) {
                        val stats = exec ?: ExecutionStats()
                        pos.quantity * effectiveLast -
                            pos.initQuantity * effectivePrev +
                            stats.sellProceeds - stats.buyCost
                    } else {
                        pos.quantity * (effectiveLast - effectivePrev)
                    }
                }
                else -> pos.presetTodayPl
            }

            val ratePercent: Double? = when {
                todayPl != null && effectivePrev != null && effectivePrev > 0.0 -> {
                    val stats = exec ?: ExecutionStats()
                    val baseCapital = abs(pos.initQuantity ?: pos.quantity) * effectivePrev + stats.buyCost
                    if (baseCapital > 1e-6) {
                        (todayPl / baseCapital) * 100.0
                    } else if (effectiveLast != null) {
                        ((effectiveLast - effectivePrev) / effectivePrev) * 100.0
                    } else null
                }
                effectiveLast != null && effectivePrev != null && effectivePrev > 0.0 -> {
                    ((effectiveLast - effectivePrev) / effectivePrev) * 100.0
                }
                else -> null
            }

            val totalPl: Double? = if (effectiveLast != null && pos.costPrice != null && pos.costPrice > 0.0) {
                pos.quantity * (effectiveLast - pos.costPrice)
            } else {
                null
            }

            if (todayPl != null) {
                totalStockTodayPlInBase += LongbridgeFullSnapshot.convertAmount(
                    todayPl,
                    pos.currency,
                    balanceParsed.baseCurrency,
                    exchangeRates
                )
                hasAnyStockTodayPl = true
            }
            if (marketValue != null) {
                totalStockMvInBase += LongbridgeFullSnapshot.convertAmount(
                    marketValue,
                    pos.currency,
                    balanceParsed.baseCurrency,
                    exchangeRates
                )
                hasAnyStockMv = true
            }

            LongbridgePositionItem(
                symbol = pos.symbol,
                symbolName = pos.symbolName,
                market = pos.market,
                currency = pos.currency,
                quantity = pos.quantity,
                initQuantity = pos.initQuantity,
                costPrice = pos.costPrice,
                lastDone = effectiveLast,
                prevClose = effectivePrev,
                marketValue = marketValue,
                todayPl = todayPl,
                todayPlRatePercent = ratePercent,
                totalPl = totalPl,
                sessionTag = q?.sessionTag
            )
        }.sortedByDescending { abs(it.todayPl ?: 0.0) }

        val rawTodayPlInBase: Double? = when {
            hasAnyStockTodayPl || hasFundRecentProfit -> totalStockTodayPlInBase + fundRecentProfitInBase
            rawPositions.isEmpty() -> 0.0
            else -> null
        }

        val finalMvInBase: Double? = when {
            hasAnyStockMv -> totalStockMvInBase
            balanceParsed.netAssets != null && balanceParsed.totalCash != null ->
                (balanceParsed.netAssets - balanceParsed.totalCash).coerceAtLeast(0.0)
            else -> null
        }

        val todayPlRatePercent: Double? = if (rawTodayPlInBase != null && balanceParsed.netAssets != null) {
            val prevNet = balanceParsed.netAssets - rawTodayPlInBase
            if (prevNet > 1e-6) (rawTodayPlInBase / prevNet) * 100.0 else null
        } else {
            null
        }

        val now = System.currentTimeMillis()
        // 8. 可选市场宏观与风向标（美股 / 港股 / A 股核心指数与市场温度，失败不阻塞账户资产展示）
        val marketIndices = fetchBenchmarkIndices(mcpUrl, accessToken)
        val marketTemperatures = fetchMarketTemperatures(mcpUrl, accessToken)

        val rawSnapshot = LongbridgeFullSnapshot(
            accountId = accountId,
            accountName = accountName,
            dcRegion = dcRegion,
            baseCurrency = balanceParsed.baseCurrency,
            displayCurrency = balanceParsed.baseCurrency,
            netAssets = balanceParsed.netAssets,
            totalCash = balanceParsed.totalCash,
            buyPower = balanceParsed.buyPower,
            marketValueTotal = finalMvInBase,
            todayPl = rawTodayPlInBase,
            todayPlRatePercent = todayPlRatePercent,
            cumulativePl = cumulativePl,
            riskLevel = balanceParsed.riskLevel,
            marginCall = balanceParsed.marginCall,
            cashDetails = balanceParsed.cashDetails,
            positions = enrichedPositions,
            marketIndices = marketIndices,
            marketTemperatures = marketTemperatures,
            rawNetAssetsInBase = balanceParsed.netAssets,
            rawTotalCashInBase = balanceParsed.totalCash,
            rawBuyPowerInBase = balanceParsed.buyPower,
            rawMarketValueInBase = finalMvInBase,
            rawTodayPlInBase = rawTodayPlInBase,
            rawCumulativePlInBase = cumulativePl,
            rawCumulativeCurrency = cumulativeCurrency ?: balanceParsed.baseCurrency,
            exchangeRates = exchangeRates,
            isMock = false,
            connectionState = LongbridgeConnectionState(
                isConfigured = true,
                isLoading = false,
                isAuthExpired = false,
                lastSuccessTime = now,
                errorMessage = null
            ),
            queriedAt = now
        )

        LongbridgeFetchResult.Success(rawSnapshot.reprojectToCurrency(settings.normalizedDisplayCurrency))
    }

    private fun callMcpTool(
        endpointUrl: String,
        accessToken: String?,
        toolName: String,
        args: JSONObject
    ): McpToolOutcome {
        return try {
            val rpcBody = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "tools/call")
                put("params", JSONObject().apply {
                    put("name", toolName)
                    put("arguments", args)
                })
            }
            val builder = Request.Builder()
                .url(endpointUrl)
                .post(rpcBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
            if (!accessToken.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $accessToken")
            }

            httpClient.newCall(builder.build()).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    return McpToolOutcome.AuthExpired("长桥授权已失效或过期 (HTTP 401)")
                }
                if (resp.code == 403) {
                    val lower = rawBody.lowercase(Locale.US)
                    return if (lower.contains("403308") || lower.contains("scope")) {
                        McpToolOutcome.ScopeMissing(extractHttpErrorMessage(rawBody, 403))
                    } else {
                        McpToolOutcome.AuthExpired("长桥拒绝访问 (HTTP 403)：${extractHttpErrorMessage(rawBody, 403)}")
                    }
                }
                if (resp.code == 429) {
                    return McpToolOutcome.Failed("请求过于频繁 (HTTP 429)，请稍后再试")
                }
                if (!resp.isSuccessful) {
                    return McpToolOutcome.Failed("长桥接口错误 (HTTP ${resp.code}): ${extractHttpErrorMessage(rawBody, resp.code)}")
                }

                val envelope = parseMcpEnvelope(rawBody)
                    ?: return McpToolOutcome.Failed("MCP 响应无法解析")

                envelope.optJSONObject("error")?.let { errObj ->
                    val code = errObj.optInt("code", 0)
                    val msg = optCleanString(errObj, "message") ?: "MCP error $code"
                    val lower = msg.lowercase(Locale.US)
                    return when {
                        lower.contains("403308") || lower.contains("scope") -> McpToolOutcome.ScopeMissing(msg)
                        code == -32001 || lower.contains("unauthorized") || lower.contains("invalid token") ||
                            lower.contains("expired") -> McpToolOutcome.AuthExpired(msg)
                        else -> McpToolOutcome.Failed(msg)
                    }
                }

                val result = envelope.optJSONObject("result")
                    ?: return McpToolOutcome.Ok(envelope.toString())

                val isToolError = result.optBoolean("isError", false)
                val contentText = extractMcpContentText(result)

                if (isToolError) {
                    val lower = contentText.lowercase(Locale.US)
                    return when {
                        lower.contains("403308") || lower.contains("scope") -> McpToolOutcome.ScopeMissing(contentText)
                        lower.contains("401102") || lower.contains("\"recoverable\":\"reauth\"") ||
                            lower.contains("unauthorized") || lower.contains("401") || lower.contains("invalid token") ||
                            lower.contains("token expired") -> McpToolOutcome.AuthExpired(contentText)
                        else -> McpToolOutcome.Failed(contentText.ifBlank { "MCP 工具返回错误" })
                    }
                }
                McpToolOutcome.Ok(contentText)
            }
        } catch (e: IOException) {
            McpToolOutcome.Failed("网络连接异常：${e.message}")
        } catch (e: Exception) {
            McpToolOutcome.Failed("调用异常：${e.message}")
        }
    }

    private fun parseMcpEnvelope(rawBody: String): JSONObject? {
        val trimmed = rawBody.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("{")) {
            return try {
                JSONObject(trimmed)
            } catch (_: Exception) {
                null
            }
        }
        // 解析 SSE event-stream：收集每个 event 的 data: 行
        val blocks = trimmed.split(Regex("\\r?\\n\\r?\\n"))
        var lastJson: JSONObject? = null
        for (block in blocks.reversed()) {
            val dataLines = block.lines()
                .filter { it.startsWith("data:") }
                .joinToString("\n") { it.removePrefix("data:").trimStart() }
                .trim()
            if (dataLines.isNotEmpty() && dataLines != "[DONE]") {
                val parsed = try {
                    JSONObject(dataLines)
                } catch (_: Exception) {
                    null
                }
                if (parsed != null) {
                    if (parsed.has("result") || parsed.has("error")) return parsed
                    if (lastJson == null) lastJson = parsed
                }
            }
        }
        if (lastJson != null) return lastJson

        // 兜底：逐行扫描最后一个 data: JSON
        val singleLines = trimmed.lines().reversed()
        for (line in singleLines) {
            if (line.startsWith("data:")) {
                val payload = line.removePrefix("data:").trim()
                val parsed = try { JSONObject(payload) } catch (_: Exception) { null }
                if (parsed != null) return parsed
            }
        }
        return null
    }

    private fun extractMcpContentText(result: JSONObject): String {
        val contentArr = result.optJSONArray("content")
        if (contentArr != null && contentArr.length() > 0) {
            val parts = mutableListOf<String>()
            for (i in 0 until contentArr.length()) {
                val item = contentArr.optJSONObject(i) ?: continue
                val txt = optCleanString(item, "text")
                if (!txt.isNullOrBlank()) {
                    parts.add(txt)
                }
            }
            if (parts.isNotEmpty()) return parts.joinToString("\n")
        }
        result.opt("structuredContent")?.let { return it.toString() }
        return result.toString()
    }

    private fun parseOAuthTokenHttpResponse(
        httpCode: Int,
        raw: String,
        fallbackClientId: String?,
        fallbackRefresh: String?
    ): LongbridgeAuthResult {
        if (httpCode in 200..299) {
            return try {
                val json = JSONObject(raw)
                val at = optCleanString(json, "access_token")
                    ?: return LongbridgeAuthResult.AuthError("OAuth 响应缺少 access_token")
                val rt = optCleanString(json, "refresh_token") ?: fallbackRefresh
                val expSec = optFiniteDouble(json, "expires_in")?.toLong()
                val expiresAtMs = if (expSec != null && expSec > 0L) {
                    System.currentTimeMillis() + expSec * 1000L
                } else {
                    Long.MAX_VALUE
                }
                LongbridgeAuthResult.Success(
                    accessToken = at,
                    refreshToken = rt,
                    expiresAtMs = expiresAtMs,
                    clientId = optCleanString(json, "client_id") ?: fallbackClientId
                )
            } catch (e: Exception) {
                LongbridgeAuthResult.AuthError("OAuth 响应解析失败：${e.message}")
            }
        }
        val msg = extractHttpErrorMessage(raw, httpCode)
        return if (httpCode in listOf(400, 401, 403)) {
            LongbridgeAuthResult.AuthError(msg)
        } else {
            LongbridgeAuthResult.NetworkError(msg)
        }
    }

    private data class ParsedBalance(
        val baseCurrency: String,
        val netAssets: Double?,
        val totalCash: Double?,
        val buyPower: Double?,
        val riskLevel: Int?,
        val marginCall: Double?,
        val cashDetails: List<LongbridgeCashItem>
    )

    private fun parseAccountBalance(rawText: String): ParsedBalance? {
        val list = unwrapToArray(rawText, arrayKeys = listOf("list", "balances", "accounts"))
            ?: return null
        if (list.isEmpty()) return null

        val primary = list.firstOrNull { optFiniteDouble(it, "net_assets") != null }
            ?: list.first()
        val baseCurrency = LongbridgeFullSnapshot.normalizeCurrency(optCleanString(primary, "currency") ?: "HKD")
        val netAssets = optFiniteDouble(primary, "net_assets")
        val totalCash = optFiniteDouble(primary, "total_cash")
        val buyPower = optFiniteDouble(primary, "buy_power")
            ?: primary.optJSONObject("us")?.optJSONObject("buy_limit_info")?.let { optFiniteDouble(it, "cash_buy_power") }
        val riskLevel = optCleanString(primary, "risk_level")?.toIntOrNull()
            ?: if (primary.has("risk_level") && !primary.isNull("risk_level")) primary.optInt("risk_level") else null
        val marginCall = optFiniteDouble(primary, "margin_call")

        val cashDetails = mutableListOf<LongbridgeCashItem>()
        val rawCashArr = primary.optJSONArray("cash_infos")
            ?: primary.optJSONObject("us")?.optJSONArray("cash_list")
        if (rawCashArr != null) {
            for (i in 0 until rawCashArr.length()) {
                val cObj = rawCashArr.optJSONObject(i) ?: continue
                val curr = optCleanString(cObj, "currency") ?: baseCurrency
                cashDetails.add(
                    LongbridgeCashItem(
                        currency = curr,
                        availableCash = optFiniteDouble(cObj, "available_cash")
                            ?: optFiniteDouble(cObj, "total_cash")
                            ?: optFiniteDouble(cObj, "cash"),
                        withdrawCash = optFiniteDouble(cObj, "withdraw_cash"),
                        frozenCash = optFiniteDouble(cObj, "frozen_cash"),
                        settlingCash = optFiniteDouble(cObj, "settling_cash") ?: optFiniteDouble(cObj, "settled_cash")
                    )
                )
            }
        }
        if (cashDetails.isEmpty() && totalCash != null) {
            cashDetails.add(LongbridgeCashItem(currency = baseCurrency, availableCash = totalCash))
        }

        return ParsedBalance(
            baseCurrency = baseCurrency,
            netAssets = netAssets,
            totalCash = totalCash,
            buyPower = buyPower,
            riskLevel = riskLevel,
            marginCall = marginCall,
            cashDetails = cashDetails
        )
    }

    private data class RawPositionRow(
        val symbol: String,
        val symbolName: String,
        val market: String,
        val currency: String,
        val quantity: Double,
        val initQuantity: Double?,
        val costPrice: Double?,
        val presetLastDone: Double?,
        val presetPrevClose: Double?,
        val presetMarketValue: Double?,
        val presetTodayPl: Double?
    )

    private fun parseStockPositions(rawText: String): List<RawPositionRow> {
        val tokener = parseJsonTokener(rawText) ?: return emptyList()
        val rootArray: JSONArray? = when (tokener) {
            is JSONArray -> tokener
            is JSONObject -> {
                val data = tokener.opt("data")
                when {
                    tokener.optJSONArray("list") != null -> tokener.optJSONArray("list")
                    tokener.optJSONArray("stock_info") != null -> JSONArray().put(tokener)
                    data is JSONObject && data.optJSONArray("list") != null -> data.optJSONArray("list")
                    data is JSONArray -> data
                    else -> null
                }
            }
            else -> null
        }

        val rows = mutableListOf<RawPositionRow>()
        val usExtraList = (tokener as? JSONObject)?.optJSONObject("us")?.optJSONArray("option_list")
        if (rootArray != null) {
            for (i in 0 until rootArray.length()) {
                val ch = rootArray.optJSONObject(i) ?: continue
                val stocks = ch.optJSONArray("stock_info")
                if (stocks != null) {
                    for (j in 0 until stocks.length()) {
                        parseSingleStockObj(stocks.optJSONObject(j))?.let { rows.add(it) }
                    }
                } else {
                    parseSingleStockObj(ch)?.let { rows.add(it) }
                }
            }
        }
        if (usExtraList != null) {
            for (i in 0 until usExtraList.length()) {
                parseSingleStockObj(usExtraList.optJSONObject(i))?.let { rows.add(it) }
            }
        }
        return rows
    }

    private fun parseSingleStockObj(obj: JSONObject?): RawPositionRow? {
        if (obj == null) return null
        val symbol = optCleanString(obj, "symbol") ?: return null
        val qty = optFiniteDouble(obj, "quantity") ?: 0.0
        val initQty = optFiniteDouble(obj, "init_quantity")
        if (abs(qty) < 1e-9 && abs(initQty ?: 0.0) < 1e-9) return null
        val market = (optCleanString(obj, "market") ?: inferMarketFromSymbol(symbol)).uppercase(Locale.US)
        val defaultCurrency = when (market) {
            "HK" -> "HKD"
            "US" -> "USD"
            "CN", "SH", "SZ" -> "CNY"
            "SG" -> "SGD"
            else -> "HKD"
        }
        val currency = LongbridgeFullSnapshot.normalizeCurrency(optCleanString(obj, "currency") ?: defaultCurrency)
        return RawPositionRow(
            symbol = symbol,
            symbolName = optCleanString(obj, "symbol_name") ?: symbol,
            market = market,
            currency = currency,
            quantity = qty,
            initQuantity = initQty,
            costPrice = optFiniteDouble(obj, "cost_price"),
            presetLastDone = optFiniteDouble(obj, "last_done") ?: optFiniteDouble(obj, "market_price"),
            presetPrevClose = optFiniteDouble(obj, "prev_close"),
            presetMarketValue = optFiniteDouble(obj, "market_value"),
            presetTodayPl = optFiniteDouble(obj, "today_pl")
        )
    }

    private fun parseExchangeRates(rawText: String): List<LongbridgeRatePair> {
        val list = unwrapToArray(rawText, arrayKeys = listOf("exchanges", "list", "rates"))
            ?: return emptyList()
        return list.mapNotNull { obj ->
            val base = optCleanString(obj, "base_currency") ?: return@mapNotNull null
            val other = optCleanString(obj, "other_currency") ?: return@mapNotNull null
            val avg = optFiniteDouble(obj, "average_rate")
                ?: optFiniteDouble(obj, "bid_rate")
                ?: optFiniteDouble(obj, "offer_rate")
                ?: return@mapNotNull null
            LongbridgeRatePair(baseCurrency = base, otherCurrency = other, averageRate = avg)
        }
    }

    private fun parseQuotes(rawText: String, includeExtendedHours: Boolean): Map<String, QuoteMetric> {
        val list = unwrapToArray(rawText, arrayKeys = listOf("list", "secu_quote", "quotes"))
            ?: return emptyMap()
        val map = HashMap<String, QuoteMetric>(list.size)
        for (obj in list) {
            val symbol = optCleanString(obj, "symbol") ?: continue
            val regLast = optFiniteDouble(obj, "last_done") ?: continue
            val regPrev = optFiniteDouble(obj, "prev_close")
            val regTs = parseQuoteTimestampMs(optCleanString(obj, "timestamp"))

            var chosenLast = regLast
            var chosenPrev = regPrev
            var chosenTag: String? = null

            if (includeExtendedHours && symbol.endsWith(".US", ignoreCase = true)) {
                val candidates = listOf(
                    "盘前" to obj.optJSONObject("pre_market"),
                    "盘后" to obj.optJSONObject("post_market"),
                    "夜盘" to obj.optJSONObject("overnight")
                )
                var bestTs = regTs ?: Long.MIN_VALUE
                for ((label, sub) in candidates) {
                    if (sub == null) continue
                    val subLast = optFiniteDouble(sub, "last_done") ?: continue
                    if (subLast <= 0.0) continue
                    val subTs = parseQuoteTimestampMs(optCleanString(sub, "timestamp"))
                    if (subTs != null && subTs > bestTs) {
                        bestTs = subTs
                        chosenLast = subLast
                        chosenTag = label
                        val subPrev = optFiniteDouble(sub, "prev_close")
                        chosenPrev = when {
                            subPrev != null && subPrev > 0.0 -> subPrev
                            label == "盘前" -> regLast
                            else -> regPrev
                        }
                    }
                }
            }

            map[symbol] = QuoteMetric(
                symbol = symbol,
                lastDone = chosenLast,
                prevClose = regPrev,
                effectivePrevClose = chosenPrev,
                sessionTag = chosenTag
            )
        }
        return map
    }

    private fun parseTodayExecutions(rawText: String): Map<String, ExecutionStats> {
        val list = unwrapToArray(rawText, arrayKeys = listOf("trades", "executions", "list"))
            ?: return emptyMap()
        val acc = HashMap<String, ExecutionStats>()
        for (obj in list) {
            val symbol = optCleanString(obj, "symbol") ?: continue
            val side = optCleanString(obj, "side")?.lowercase(Locale.US).orEmpty()
            val qty = optFiniteDouble(obj, "quantity") ?: continue
            val price = optFiniteDouble(obj, "price") ?: continue
            val prev = acc[symbol] ?: ExecutionStats()
            acc[symbol] = when {
                side.contains("buy") -> prev.copy(
                    buyQty = prev.buyQty + qty,
                    buyCost = prev.buyCost + qty * price
                )
                side.contains("sell") -> prev.copy(
                    sellQty = prev.sellQty + qty,
                    sellProceeds = prev.sellProceeds + qty * price
                )
                else -> prev
            }
        }
        return acc
    }

    private fun parseFundOverview(rawText: String): List<Pair<String, Double?>> {
        val list = unwrapToArray(rawText, arrayKeys = listOf("list", "funds")) ?: return emptyList()
        return list.mapNotNull { obj ->
            val curr = LongbridgeFullSnapshot.normalizeCurrency(optCleanString(obj, "currency") ?: "HKD")
            val recent = optFiniteDouble(obj, "recent_profit")
            if (recent != null) curr to recent else null
        }
    }

    private fun parseProfitAnalysis(rawText: String): Pair<Double?, String?> {
        val root = parseJsonObjectSafe(rawText) ?: return null to null
        val summary = root.optJSONObject("summary")
            ?: root.optJSONObject("data")?.optJSONObject("summary")
            ?: root
        val sumProfit = optFiniteDouble(summary, "sum_profit")
        val currency = optCleanString(summary, "currency")?.let { LongbridgeFullSnapshot.normalizeCurrency(it) }
        return sumProfit to currency
    }

    private fun fetchBenchmarkIndices(mcpUrl: String, accessToken: String): List<LongbridgeIndexQuote> {
        val allSymbols = BENCHMARK_INDICES.map { it.symbol }
        val quotesMap = LinkedHashMap<String, QuoteMetric>()
        when (val batchOut = callMcpTool(mcpUrl, accessToken, "quote", JSONObject().put("symbols", JSONArray(allSymbols)))) {
            is McpToolOutcome.Ok -> quotesMap.putAll(parseQuotes(batchOut.rawText, includeExtendedHours = false))
            else -> {}
        }

        // 若单次跨市场批量报价受限或为空，按 US / HK / CN 分别小批降级查询，确保某地权限缺失不阻塞其余两地
        if (quotesMap.isEmpty()) {
            BENCHMARK_INDICES.groupBy { it.market }.forEach { (_, specs) ->
                val subSymbols = specs.map { it.symbol }
                when (val subOut = callMcpTool(mcpUrl, accessToken, "quote", JSONObject().put("symbols", JSONArray(subSymbols)))) {
                    is McpToolOutcome.Ok -> quotesMap.putAll(parseQuotes(subOut.rawText, includeExtendedHours = false))
                    else -> {}
                }
            }
        }

        if (quotesMap.isEmpty()) return emptyList()
        return BENCHMARK_INDICES.mapNotNull { spec ->
            val metric = findIndexQuote(quotesMap, spec.symbol) ?: return@mapNotNull null
            val prev = metric.prevClose ?: metric.effectivePrevClose
            val changeVal = prev?.takeIf { it > 0.0 }?.let { metric.lastDone - it }
            val changeRate = prev?.takeIf { it > 0.0 }?.let { ((metric.lastDone - it) / it) * 100.0 }
            LongbridgeIndexQuote(
                symbol = spec.symbol,
                shortName = spec.shortName,
                market = spec.market,
                lastDone = metric.lastDone,
                prevClose = prev,
                changeVal = changeVal,
                changeRatePercent = changeRate
            )
        }
    }

    private fun findIndexQuote(quotes: Map<String, QuoteMetric>, targetSymbol: String): QuoteMetric? {
        quotes[targetSymbol]?.let { return it }
        val normalizedTarget = normalizeIndexSymbol(targetSymbol)
        return quotes.entries.firstOrNull { (k, _) ->
            k.equals(targetSymbol, ignoreCase = true) || normalizeIndexSymbol(k) == normalizedTarget
        }?.value
    }

    private fun normalizeIndexSymbol(sym: String): String {
        val upper = sym.trim().uppercase(Locale.US)
        return if (upper.startsWith("00") && upper.endsWith(".HK") && upper.length > 5) {
            upper.removePrefix("00")
        } else {
            upper
        }
    }

    private fun fetchMarketTemperatures(mcpUrl: String, accessToken: String): List<LongbridgeMarketTemp> {
        val result = mutableListOf<LongbridgeMarketTemp>()
        for (mkt in listOf("US", "HK", "CN")) {
            when (val out = callMcpTool(mcpUrl, accessToken, "market_temperature", JSONObject().put("market", mkt))) {
                is McpToolOutcome.Ok -> parseMarketTemperature(mkt, out.rawText)?.let { result.add(it) }
                else -> {}
            }
        }
        return result
    }

    private fun parseMarketTemperature(market: String, rawText: String): LongbridgeMarketTemp? {
        val root = parseJsonObjectSafe(rawText) ?: return null
        val target = root.optJSONObject("data") ?: root
        val temp = optFiniteDouble(target, "temperature")?.toInt()?.coerceIn(0, 100) ?: return null
        val desc = optCleanString(target, "description") ?: ""
        val valuation = optFiniteDouble(target, "valuation")?.toInt()?.coerceIn(0, 100)
        val sentiment = optFiniteDouble(target, "sentiment")?.toInt()?.coerceIn(0, 100)
        val updatedAt = optFiniteDouble(target, "updated_at")?.toLong()
            ?: parseQuoteTimestampMs(optCleanString(target, "updated_at"))?.div(1000L)
        return LongbridgeMarketTemp(
            market = market,
            temperature = temp,
            description = desc,
            valuation = valuation,
            sentiment = sentiment,
            updatedAt = updatedAt
        )
    }

    private fun unwrapToArray(rawText: String, arrayKeys: List<String>): List<JSONObject>? {
        val tokener = parseJsonTokener(rawText) ?: return null
        val arr: JSONArray? = when (tokener) {
            is JSONArray -> tokener
            is JSONObject -> {
                var found: JSONArray? = null
                for (k in arrayKeys) {
                    val direct = tokener.optJSONArray(k)
                    if (direct != null) {
                        found = direct
                        break
                    }
                }
                if (found == null) {
                    val data = tokener.opt("data")
                    when (data) {
                        is JSONArray -> found = data
                        is JSONObject -> {
                            for (k in arrayKeys) {
                                val nested = data.optJSONArray(k)
                                if (nested != null) {
                                    found = nested
                                    break
                                }
                            }
                        }
                    }
                }
                found
            }
            else -> null
        }
        if (arr == null) return null
        val out = ArrayList<JSONObject>(arr.length())
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { out.add(it) }
        }
        return out
    }

    private fun parseJsonTokener(rawText: String): Any? {
        val clean = rawText.trim()
        if (clean.isEmpty()) return null
        return try {
            JSONTokener(clean).nextValue()
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJsonObjectSafe(rawText: String): JSONObject? {
        return parseJsonTokener(rawText) as? JSONObject
    }

    private fun inferMarketFromSymbol(symbol: String): String {
        val suffix = symbol.substringAfterLast('.', "").uppercase(Locale.US)
        return when (suffix) {
            "HK", "US", "SG" -> suffix
            "SH", "SZ" -> "CN"
            else -> "HK"
        }
    }

    private fun parseQuoteTimestampMs(raw: String?): Long? {
        val clean = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        clean.toLongOrNull()?.let { n ->
            return if (n < 100_000_000_000L) n * 1000L else n
        }
        try {
            return Instant.parse(clean).toEpochMilli()
        } catch (_: Exception) {}
        try {
            return OffsetDateTime.parse(clean).toInstant().toEpochMilli()
        } catch (_: Exception) {}
        try {
            return LocalDateTime.parse(clean.replace(' ', 'T')).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (_: Exception) {}
        return null
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        if (query.isBlank()) return result
        for (pair in query.split("&")) {
            val eqIdx = pair.indexOf('=')
            val k = if (eqIdx >= 0) pair.substring(0, eqIdx) else pair
            val v = if (eqIdx >= 0) pair.substring(eqIdx + 1) else ""
            if (k.isNotEmpty()) {
                result[java.net.URLDecoder.decode(k, StandardCharsets.UTF_8.name())] =
                    java.net.URLDecoder.decode(v, StandardCharsets.UTF_8.name())
            }
        }
        return result
    }

    private fun extractHttpErrorMessage(raw: String, code: Int): String {
        if (raw.isBlank()) return "HTTP $code"
        return try {
            val json = JSONObject(raw)
            optCleanString(json, "error_description")
                ?: optCleanString(json, "message")
                ?: optCleanString(json, "error")
                ?: raw.take(160)
        } catch (_: Exception) {
            raw.trim().take(160)
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val BENCHMARK_INDICES = listOf(
            BenchmarkIndexSpec(".IXIC.US", "纳指", "US"),
            BenchmarkIndexSpec(".SPX.US", "标普500", "US"),
            BenchmarkIndexSpec(".DJI.US", "道指", "US"),
            BenchmarkIndexSpec("HSI.HK", "恒指", "HK"),
            BenchmarkIndexSpec("HSTECH.HK", "恒科", "HK"),
            BenchmarkIndexSpec("000001.SH", "上证", "CN"),
            BenchmarkIndexSpec("000300.SH", "沪深300", "CN")
        )

        private fun optCleanString(obj: JSONObject, key: String): String? {
            if (!obj.has(key) || obj.isNull(key)) return null
            val raw = obj.opt(key)?.toString()?.trim() ?: return null
            return if (raw.isEmpty() || raw.equals("null", ignoreCase = true)) null else raw
        }

        private fun optFiniteDouble(obj: JSONObject, key: String): Double? {
            if (!obj.has(key) || obj.isNull(key)) return null
            val raw = obj.opt(key) ?: return null
            val d = when (raw) {
                is Number -> raw.toDouble()
                is String -> raw.trim().toDoubleOrNull()
                else -> raw.toString().trim().toDoubleOrNull()
            }
            return if (d != null && d.isFinite()) d else null
        }

        /**
         * 解析 `/agent` `authenticate` 工具返回文本里的 Access Token（兼容 JSON、Bearer 头与纯文本）
         */
        fun extractTokenFromAuthenticateText(rawText: String): LongbridgeParsedCredential? {
            val trimmed = rawText.trim()
            if (trimmed.isEmpty()) return null

            if (trimmed.startsWith("{")) {
                parseCredentialInput(trimmed)?.let { return it }
            }

            // 匹配 Authorization: Bearer <token> 或 Bearer <token>
            Regex("""(?:Authorization\s*[:=]\s*)?Bearer\s+([A-Za-z0-9\-_.~+/=]{12,})""", RegexOption.IGNORE_CASE)
                .find(trimmed)
                ?.groupValues?.getOrNull(1)
                ?.let { tok -> return LongbridgeParsedCredential(accessToken = tok) }

            // 匹配 token: xxx 或 access_token=xxx
            Regex("""(?:access_token|token)\s*[:=]\s*["'`]?([A-Za-z0-9\-_.~+/=]{12,})["'`]?""", RegexOption.IGNORE_CASE)
                .find(trimmed)
                ?.groupValues?.getOrNull(1)
                ?.let { tok -> return LongbridgeParsedCredential(accessToken = tok) }

            // 整行即 Token（不含空格与换行，排除未识别的 JSON）
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[") &&
                !trimmed.contains(' ') && !trimmed.contains('\n') && trimmed.length >= 16
            ) {
                return LongbridgeParsedCredential(accessToken = trimmed)
            }
            return null
        }

        /**
         * 解析用户输入的长桥凭据（`~/.longbridge/openapi/tokens/<id>` JSON 或裸 Access Token）
         */
        fun parseCredentialInput(raw: String): LongbridgeParsedCredential? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.startsWith("{")) {
                return try {
                    val root = JSONObject(trimmed)
                    val dataRaw = root.opt("data")
                    if (dataRaw is String) {
                        val directData = dataRaw.removePrefix("Bearer ").removePrefix("bearer ").trim()
                        if (directData.length >= 10 && !directData.contains('\n') && !directData.contains(' ')) {
                            return LongbridgeParsedCredential(accessToken = directData)
                        }
                    }
                    val target = root.optJSONObject("data")
                        ?: root.optJSONObject("credentials")
                        ?: root
                    val access = optCleanString(target, "access_token")
                        ?: optCleanString(target, "accessToken")
                        ?: optCleanString(target, "token")
                        ?: optCleanString(target, "bearer_token")
                        ?: return null
                    val cleanAccess = access.removePrefix("Bearer ").removePrefix("bearer ").trim()
                    if (cleanAccess.isEmpty()) return null
                    val refresh = optCleanString(target, "refresh_token")
                        ?: optCleanString(target, "refreshToken")
                    val clientId = optCleanString(target, "client_id")
                        ?: optCleanString(target, "clientId")
                    val expUnixSec = optFiniteDouble(target, "expires_at")?.toLong()
                    val expInSec = optFiniteDouble(target, "expires_in")?.toLong()
                    val expiresAtMs = when {
                        expUnixSec != null && expUnixSec > 100_000_000_000L -> expUnixSec
                        expUnixSec != null && expUnixSec > 0L -> expUnixSec * 1000L
                        expInSec != null && expInSec > 0L -> System.currentTimeMillis() + expInSec * 1000L
                        else -> Long.MAX_VALUE
                    }
                    LongbridgeParsedCredential(
                        accessToken = cleanAccess,
                        refreshToken = refresh,
                        expiresAtMs = expiresAtMs,
                        clientId = clientId
                    )
                } catch (_: Exception) {
                    null
                }
            }

            val stripped = trimmed.removePrefix("Bearer ").removePrefix("bearer ").trim()
            return if (stripped.length >= 10 && !stripped.contains('\n') && !stripped.contains(' ')) {
                LongbridgeParsedCredential(accessToken = stripped)
            } else {
                null
            }
        }

        fun checkTcpReachable(host: String, port: Int, timeoutMs: Int = 2000): Boolean {
            return try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
