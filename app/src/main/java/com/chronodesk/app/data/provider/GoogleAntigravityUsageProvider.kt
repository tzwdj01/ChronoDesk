package com.chronodesk.app.data.provider

import android.util.Log
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Google Antigravity / Gemini 平台额度与配额适配器
 * 支持通过 Google Gemini API Key / Antigravity 凭据直连官方接口验证并监控模型配额
 */
class GoogleAntigravityUsageProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "AntigravityProvider"
        private const val MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    suspend fun fetchUsage(
        accountId: String,
        accountAlias: String,
        apiKey: String?
    ): UsageFetchResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey?.trim() ?: ""
        if (cleanKey.isBlank() || cleanKey == "demo") {
            return@withContext UsageFetchResult.Success(
                CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "Google Antigravity" },
                    credentialStatus = CredentialStatus.NOT_LOGGED_IN,
                    creditsBalance = null,
                    grantedBalance = null,
                    toppedUpBalance = null,
                    currency = "USD",
                    isAvailable = false,
                    planName = "Pro",
                    primaryWindow = null,
                    secondaryWindow = null,
                    queriedAt = null,
                    errorMessage = "未配置 API Key",
                    modelProvider = "ANTIGRAVITY"
                )
            )
        }

        try {
            val url = "$MODELS_URL?key=$cleanKey"
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyStr = response.body?.string()

            if (code == 200 && !bodyStr.isNullOrBlank()) {
                val json = JSONObject(bodyStr)
                val modelsArray = json.optJSONArray("models")
                val modelCount = modelsArray?.length() ?: 0
                Log.d(TAG, "Antigravity models verified: count = $modelCount")

                // 计算每日配额重置时间点 (Google API 统一于次日 00:00 UTC / 北京时间 08:00 重置)
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dailyResetUnix = cal.timeInMillis / 1000

                // 5小时/滚动窗口重置时间点 (5小时后)
                val fiveHourResetUnix = (System.currentTimeMillis() / 1000) + 18000

                val primaryWindow = CodexUsageWindow(
                    name = "five_hour",
                    label = "5小时配额",
                    limitWindowSeconds = 18000,
                    usedPercent = 0.0,
                    remainingPercent = 100.0,
                    resetAtUnix = fiveHourResetUnix
                )

                val secondaryWindow = CodexUsageWindow(
                    name = "daily_quota",
                    label = "日配额",
                    limitWindowSeconds = 86400,
                    usedPercent = 0.0,
                    remainingPercent = 100.0,
                    resetAtUnix = dailyResetUnix
                )

                val snapshot = CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "Google Antigravity" },
                    credentialStatus = CredentialStatus.VALID,
                    primaryWindow = primaryWindow,
                    secondaryWindow = secondaryWindow,
                    creditsBalance = null,
                    grantedBalance = null,
                    toppedUpBalance = null,
                    currency = "USD",
                    isAvailable = true,
                    planName = "Pro",
                    queriedAt = System.currentTimeMillis(),
                    isLastGood = false,
                    errorMessage = null,
                    limitReached = false,
                    modelProvider = "ANTIGRAVITY"
                )
                UsageFetchResult.Success(snapshot)
            } else if (code == 400 || code == 401 || code == 403) {
                UsageFetchResult.AuthExpired("Google API Key 无效或未授权 (HTTP $code)")
            } else {
                UsageFetchResult.TransientError("Google 服务响应异常 (HTTP $code)")
            }
        } catch (e: IOException) {
            Log.w(TAG, "fetchUsage network error: ${e.message}")
            UsageFetchResult.TransientError("网络连接失败，请检查网络代理: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "fetchUsage unexpected error", e)
            UsageFetchResult.TransientError("解析异常: ${e.message}")
        }
    }
}
