package com.chronodesk.app.data.provider

import android.util.Log
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CredentialStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gemini API (Google AI) 平台连接状态与模型验证适配器
 * 保留已有账号、Key 和内部 ANTIGRAVITY 标识以兼容升级。
 * 请求成功时如实反映连接正常与验证时间，杜绝伪造 100% 进度条、虚假 5 小时/日配额与 Pro 套餐。
 */
class GoogleAntigravityUsageProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta/models"
) {
    companion object {
        private const val TAG = "GeminiApiProvider"
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
                    accountAlias = accountAlias.ifBlank { "Gemini API" },
                    credentialStatus = CredentialStatus.NOT_LOGGED_IN,
                    creditsBalance = null,
                    grantedBalance = null,
                    toppedUpBalance = null,
                    currency = null,
                    isAvailable = false,
                    planName = null,
                    primaryWindow = null,
                    secondaryWindow = null,
                    queriedAt = null,
                    errorMessage = "未配置 API Key",
                    modelProvider = "ANTIGRAVITY"
                )
            )
        }

        try {
            val delimiter = if (baseUrl.contains("?")) "&" else "?"
            val urlWithKey = "$baseUrl${delimiter}key=$cleanKey"
            val request = Request.Builder()
                .url(urlWithKey)
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyStr = response.body?.string()

            if (code == 200 && !bodyStr.isNullOrBlank()) {
                val json = JSONObject(bodyStr)
                val modelsArray = json.optJSONArray("models")
                val modelCount = modelsArray?.length() ?: 0
                Log.d(TAG, "Gemini API models verified: count = $modelCount")

                val snapshot = CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "Gemini API" },
                    credentialStatus = CredentialStatus.VALID,
                    primaryWindow = null,
                    secondaryWindow = null,
                    creditsBalance = null,
                    grantedBalance = null,
                    toppedUpBalance = null,
                    currency = null,
                    isAvailable = true,
                    planName = null,
                    queriedAt = System.currentTimeMillis(),
                    isLastGood = false,
                    errorMessage = null,
                    limitReached = false,
                    modelProvider = "ANTIGRAVITY"
                )
                UsageFetchResult.Success(snapshot)
            } else if (code == 400 || code == 401 || code == 403) {
                UsageFetchResult.AuthExpired("Gemini API Key 无效或未授权 (HTTP $code)")
            } else {
                UsageFetchResult.TransientError("Gemini 服务响应异常 (HTTP $code)")
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
