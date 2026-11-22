package com.chronodesk.app.data.provider

import android.util.Log
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.network.NetworkClient
import com.chronodesk.app.data.security.TokenRedactor
import org.json.JSONObject
import java.io.IOException

class RealCodexUsageProvider : CodexUsageProvider {

    override suspend fun startDeviceAuth(): DeviceCodeResult {
        return try {
            val response = NetworkClient.authApi.requestDeviceCode(
                mapOf("client_id" to NetworkClient.CODEX_CLIENT_ID)
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                DeviceCodeResult.Success(
                    deviceAuthId = body.deviceAuthId,
                    userCode = body.userCode,
                    verificationUrl = NetworkClient.DEVICE_VERIFY_URL,
                    intervalSeconds = body.interval ?: 5L,
                    expiresInSeconds = body.expiresIn ?: 900L
                )
            } else {
                val errText = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                DeviceCodeResult.Error("启动设备码授权失败: $errText")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "startDeviceAuth error", e)
            DeviceCodeResult.Error("网络连接失败: ${e.message}")
        }
    }

    override suspend fun pollDeviceAuth(deviceAuthId: String, userCode: String): PollResult {
        return try {
            val response = NetworkClient.authApi.pollDeviceToken(
                mapOf(
                    "device_auth_id" to deviceAuthId,
                    "user_code" to userCode
                )
            )
            when (response.code()) {
                200 -> {
                    val body = response.body()
                    if (body?.authorizationCode != null && body.codeVerifier != null) {
                        PollResult.Success(body.authorizationCode, body.codeVerifier)
                    } else {
                        PollResult.Error("授权响应缺少凭据")
                    }
                }
                403, 404 -> PollResult.Pending
                410 -> PollResult.Expired("Device Code 已过期，请重新启动登录")
                else -> {
                    val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                    PollResult.Error("轮询错误: $err")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "pollDeviceAuth transient error: ${e.message}")
            PollResult.Pending
        }
    }

    override suspend fun exchangeToken(code: String, codeVerifier: String): TokenResult {
        return try {
            val fields = mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to NetworkClient.DEVICE_REDIRECT_URI,
                "client_id" to NetworkClient.CODEX_CLIENT_ID,
                "code_verifier" to codeVerifier
            )
            val response = NetworkClient.authApi.exchangeToken(fields)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                TokenResult.Success(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    idToken = body.idToken,
                    expiresInSeconds = body.expiresIn ?: 3600L
                )
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                TokenResult.TransientError("换取 Token 失败: $err")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "exchangeToken error", e)
            TokenResult.TransientError("网络连接超时: ${e.message}")
        }
    }

    override suspend fun refreshToken(refreshToken: String): TokenResult {
        return try {
            val fields = mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to NetworkClient.CODEX_CLIENT_ID,
                "scope" to "openid profile email"
            )
            val response = NetworkClient.authApi.exchangeToken(fields)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                TokenResult.Success(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken ?: refreshToken,
                    idToken = body.idToken,
                    expiresInSeconds = body.expiresIn ?: 3600L
                )
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                val isPermanent = code == 401 || isPermanentRefreshErrorCode(errBody)
                if (isPermanent) {
                    TokenResult.ExpiredOrInvalid("Refresh Token 已失效或被吊销，请重新登录")
                } else {
                    TokenResult.TransientError("Token 刷新遇到瞬时异常 (HTTP $code): $errBody")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "refreshToken network error: ${e.message}")
            TokenResult.TransientError("刷新 Token 网络失败: ${e.message}")
        }
    }

    override suspend fun fetchUsage(
        accountId: String,
        accountAlias: String,
        accessToken: String,
        chatgptAccountId: String?
    ): UsageFetchResult {
        return try {
            val authHeader = "Bearer $accessToken"
            val effectiveAccountId = chatgptAccountId
                ?: com.chronodesk.app.data.security.JwtUtils.extractChatGptAccountId(accessToken)
            val response = NetworkClient.usageApi.getUsage(
                authHeader = authHeader,
                accountId = effectiveAccountId
            )

            when (response.code()) {
                401, 403 -> {
                    UsageFetchResult.AuthExpired("Codex 访问令牌已过期或拒绝访问 (HTTP ${response.code()})")
                }
                200 -> {
                    val body = response.body()
                    val limitReached = body?.rateLimit?.limitReached == true || body?.rateLimit?.allowed == false
                    val primary = body?.rateLimit?.primaryWindow?.let {
                        CodexUsageWindow.fromRaw(it.usedPercent, it.limitWindowSeconds, it.resetAt)
                    }
                    val secondary = body?.rateLimit?.secondaryWindow?.let {
                        CodexUsageWindow.fromRaw(it.usedPercent, it.limitWindowSeconds, it.resetAt)
                    }
                    val credits = body?.credits?.parseBalance()
                    val plan = mapPlanType(body?.planType)

                    // 自动探针：调用 /backend-api/me 获取真实注册邮箱，或从 Token 提取
                    var detectedEmail: String? = null
                    try {
                        val meResp = NetworkClient.usageApi.getMe(authHeader)
                        if (meResp.isSuccessful && !meResp.body()?.email.isNullOrBlank()) {
                            detectedEmail = meResp.body()!!.email
                        }
                    } catch (_: Exception) {}

                    if (detectedEmail.isNullOrBlank()) {
                        detectedEmail = com.chronodesk.app.data.security.JwtUtils.extractEmail(accessToken)
                    }

                    // 自动探针：调用 /backend-api/wham/rate-limit-reset-credits 查询重置卡可用次数与到期
                    var resetCount: Int? = null
                    var resetExpiresAt: String? = null
                    try {
                        val resetResp = NetworkClient.usageApi.getResetCredits(
                            authHeader = authHeader,
                            accountId = effectiveAccountId
                        )
                        if (resetResp.isSuccessful && resetResp.body()?.credits != null) {
                            val creditsList = resetResp.body()!!.credits!!
                            val available = creditsList.filter { it.status == null || it.status.equals("available", ignoreCase = true) }
                            resetCount = available.size
                            resetExpiresAt = available.firstOrNull()?.expiresAt
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "getResetCredits non-fatal: ${e.message}")
                    }

                    Log.i(TAG, "fetchUsage success for account=$accountId: limitReached=$limitReached, primary=${primary?.remainingPercent}%, secondary=${secondary?.remainingPercent}%, resetCredits=$resetCount")

                    val snapshot = CodexUsageSnapshot(
                        accountId = accountId,
                        accountAlias = if (accountAlias.isNotBlank() && !accountAlias.contains("@")) accountAlias else (detectedEmail ?: accountAlias),
                        email = detectedEmail,
                        credentialStatus = CredentialStatus.VALID,
                        primaryWindow = primary,
                        secondaryWindow = secondary,
                        creditsBalance = credits,
                        planName = plan,
                        resetCreditsCount = resetCount,
                        resetCreditsExpiresAt = resetExpiresAt,
                        queriedAt = System.currentTimeMillis(),
                        isLastGood = false,
                        errorMessage = null,
                        limitReached = limitReached
                    )
                    UsageFetchResult.Success(snapshot)
                }
                else -> {
                    val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                    UsageFetchResult.TransientError("查询用量服务异常 (HTTP ${response.code()}): $err")
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "fetchUsage network error: ${e.message}")
            UsageFetchResult.TransientError("网络连接失败，保留上次成功数据: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "fetchUsage unexpected error", e)
            UsageFetchResult.TransientError("解析或未知错误: ${e.message}")
        }
    }

    private fun isPermanentRefreshErrorCode(body: String): Boolean {
        return try {
            val json = JSONObject(body)
            val errCode = json.optString("error", "").lowercase()
            val code = json.optString("code", "").lowercase()
            val subCode = json.optJSONObject("error")?.optString("code", "")?.lowercase() ?: ""

            listOf(errCode, code, subCode).any {
                it in listOf(
                    "invalid_grant",
                    "refresh_token_expired",
                    "refresh_token_reused",
                    "refresh_token_invalidated"
                )
            }
        } catch (_: Exception) {
            body.contains("invalid_grant", ignoreCase = true)
        }
    }

    private fun mapPlanType(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return when (raw.trim().lowercase()) {
            "plus" -> "Plus"
            "pro" -> "Pro 200"
            "promax" -> "Pro 500"
            "prolite" -> "Pro 100"
            "team", "business" -> "Business"
            "free" -> "Free"
            "edu" -> "Edu"
            else -> raw.replaceFirstChar { it.uppercase() }
        }
    }

    companion object {
        private const val TAG = "RealCodexUsageProvider"
    }
}
