package com.chronodesk.app.data.provider

import android.util.Log
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * DeepSeek AI 平台配额与余额查询适配器
 * 支持官方 API Key 配额查询与桌面时钟模拟数据
 */
class DeepSeekUsageProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "DeepSeekProvider"
        private const val BALANCE_URL = "https://api.deepseek.com/user/balance"
    }

    suspend fun fetchUsage(
        accountId: String,
        accountAlias: String,
        apiKey: String?
    ): UsageFetchResult = withContext(Dispatchers.IO) {
        if (apiKey.isNullOrBlank() || apiKey.startsWith("mock_") || apiKey == "demo") {
            delay(300)
            return@withContext getMockSnapshot(accountId, accountAlias)
        }

        try {
            val request = Request.Builder()
                .url(BALANCE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyStr = response.body?.string()

            if (code == 200 && !bodyStr.isNullOrBlank()) {
                val json = JSONObject(bodyStr)
                val isAvailable = json.optBoolean("is_available", true)
                val balanceInfos = json.optJSONArray("balance_infos")
                var totalBalance = 0.0
                var currency = "CNY"

                if (balanceInfos != null && balanceInfos.length() > 0) {
                    val first = balanceInfos.getJSONObject(0)
                    totalBalance = first.optDouble("total_balance", 0.0)
                    currency = first.optString("currency", "CNY")
                }

                // 将余额折算为额度百分比（如充值余额 > 0 则为 100% 充裕，低于 10 元递减）
                val remainingPercent = (totalBalance / 50.0 * 100.0).coerceIn(10.0, 100.0)
                val nowSec = System.currentTimeMillis() / 1000
                val primaryWindow = CodexUsageWindow.fromRaw(
                    usedPercent = 100.0 - remainingPercent,
                    limitWindowSeconds = 18000L,
                    resetAtUnix = nowSec + 18000L
                )

                val snapshot = CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "DeepSeek" },
                    email = if (accountAlias.contains("@")) accountAlias else null,
                    credentialStatus = if (isAvailable) CredentialStatus.VALID else CredentialStatus.EXPIRED,
                    primaryWindow = primaryWindow,
                    secondaryWindow = null,
                    creditsBalance = totalBalance,
                    planName = "V3",
                    queriedAt = System.currentTimeMillis(),
                    isLastGood = false,
                    errorMessage = null
                )
                UsageFetchResult.Success(snapshot)
            } else if (code == 401 || code == 403) {
                UsageFetchResult.AuthExpired("DeepSeek API Key 无效或已过期")
            } else {
                UsageFetchResult.TransientError("DeepSeek 服务响应异常 (HTTP $code)")
            }
        } catch (e: IOException) {
            Log.w(TAG, "fetchUsage network error: ${e.message}")
            UsageFetchResult.TransientError("网络连接失败，保留上次成功数据: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "fetchUsage unexpected error", e)
            UsageFetchResult.TransientError("解析异常: ${e.message}")
        }
    }

    private fun getMockSnapshot(accountId: String, accountAlias: String): UsageFetchResult {
        val nowSec = System.currentTimeMillis() / 1000
        val snapshot = CodexUsageSnapshot(
            accountId = accountId,
            accountAlias = accountAlias.ifBlank { "DeepSeek" },
            email = null,
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(
                usedPercent = 14.0, // 剩余 86%
                limitWindowSeconds = 18000L,
                resetAtUnix = nowSec + 5160L // 1小时26分后
            ),
            secondaryWindow = CodexUsageWindow.fromRaw(
                usedPercent = 0.0,
                limitWindowSeconds = 604800L,
                resetAtUnix = nowSec + 5 * 86400L
            ),
            creditsBalance = 88.50,
            planName = "Plus",
            queriedAt = System.currentTimeMillis()
        )
        return UsageFetchResult.Success(snapshot)
    }
}
