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
 * DeepSeek AI 平台余额与配额查询适配器
 * 严格直连官方 /user/balance 接口，仅展示实际查询到的真实数据，杜绝一切虚构和臆测
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
        val cleanKey = apiKey?.trim() ?: ""
        if (cleanKey.isBlank() || cleanKey == "demo") {
            // 未填入真实有效 API Key 时返回未配置状态，绝不捏造假余额与假型号
            return@withContext UsageFetchResult.Success(
                CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "DeepSeek" },
                    credentialStatus = CredentialStatus.NOT_LOGGED_IN,
                    creditsBalance = null,
                    grantedBalance = null,
                    toppedUpBalance = null,
                    currency = "CNY",
                    isAvailable = false,
                    planName = null,
                    primaryWindow = null,
                    secondaryWindow = null,
                    queriedAt = null,
                    errorMessage = "未配置 API Key",
                    modelProvider = "DEEPSEEK"
                )
            )
        }

        try {
            val request = Request.Builder()
                .url(BALANCE_URL)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyStr = response.body?.string()

            if (code == 200 && !bodyStr.isNullOrBlank()) {
                val json = JSONObject(bodyStr)
                val isAvailable = json.optBoolean("is_available", true)
                val balanceInfos = json.optJSONArray("balance_infos")
                var totalBalance: Double? = null
                var grantedBalance: Double? = null
                var toppedUpBalance: Double? = null
                var currency = "CNY"

                if (balanceInfos != null && balanceInfos.length() > 0) {
                    val info = balanceInfos.getJSONObject(0)
                    val totalStr = info.optString("total_balance", "")
                    totalBalance = totalStr.toDoubleOrNull() ?: if (info.has("total_balance")) info.optDouble("total_balance") else null
                    val grantedStr = info.optString("granted_balance", "")
                    grantedBalance = grantedStr.toDoubleOrNull() ?: if (info.has("granted_balance")) info.optDouble("granted_balance") else null
                    val toppedStr = info.optString("topped_up_balance", "")
                    toppedUpBalance = toppedStr.toDoubleOrNull() ?: if (info.has("topped_up_balance")) info.optDouble("topped_up_balance") else null
                    currency = info.optString("currency", "CNY").ifBlank { "CNY" }
                }

                val hasFunds = (totalBalance ?: 0.0) > 0.0
                val snapshot = CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "DeepSeek" },
                    email = if (accountAlias.contains("@")) accountAlias else null,
                    credentialStatus = if (isAvailable && hasFunds) CredentialStatus.VALID else CredentialStatus.EXPIRED,
                    primaryWindow = null, // DeepSeek 官方没有 5 小时滚动窗口，不伪造
                    secondaryWindow = null,
                    creditsBalance = totalBalance,
                    grantedBalance = grantedBalance,
                    toppedUpBalance = toppedUpBalance,
                    currency = currency,
                    isAvailable = isAvailable,
                    planName = null, // 不妄加 "V3" 或 "Plus"，仅以真实账户为准
                    queriedAt = System.currentTimeMillis(),
                    isLastGood = false,
                    errorMessage = null,
                    limitReached = !isAvailable || !hasFunds,
                    modelProvider = "DEEPSEEK"
                )
                UsageFetchResult.Success(snapshot)
            } else if (code == 401 || code == 403) {
                UsageFetchResult.AuthExpired("API Key 无效或已过期 (HTTP $code)")
            } else {
                UsageFetchResult.TransientError("DeepSeek 服务响应异常 (HTTP $code)")
            }
        } catch (e: IOException) {
            Log.w(TAG, "fetchUsage network error: ${e.message}")
            UsageFetchResult.TransientError("网络连接失败，请检查网络: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "fetchUsage unexpected error", e)
            UsageFetchResult.TransientError("解析异常: ${e.message}")
        }
    }
}
