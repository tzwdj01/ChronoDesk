package com.chronodesk.app.data.provider

import android.util.Log
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.model.DeepSeekBalance
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
        .build(),
    private val baseUrl: String = "https://api.deepseek.com/user/balance"
) {
    companion object {
        private const val TAG = "DeepSeekProvider"
    }

    suspend fun fetchUsage(
        accountId: String,
        accountAlias: String,
        apiKey: String?
    ): UsageFetchResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey?.trim() ?: ""
        if (cleanKey.isBlank() || cleanKey == "demo") {
            // 未填入有效 API Key 时返回未配置状态
            return@withContext UsageFetchResult.Success(
                CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "DeepSeek" },
                    credentialStatus = CredentialStatus.NOT_LOGGED_IN,
                    creditsBalance = null,
                    grantedBalance = null,
                    toppedUpBalance = null,
                    balanceList = null,
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
                .url(baseUrl)
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
                val balances = mutableListOf<DeepSeekBalance>()

                if (balanceInfos != null) {
                    for (i in 0 until balanceInfos.length()) {
                        val info = balanceInfos.optJSONObject(i) ?: continue
                        val curr = info.optString("currency", "CNY").ifBlank { "CNY" }
                        val totStr = info.optString("total_balance", "")
                        val tot = totStr.toDoubleOrNull() ?: if (info.has("total_balance")) {
                            val d = info.optDouble("total_balance")
                            if (d.isFinite()) d else null
                        } else null

                        val grStr = info.optString("granted_balance", "")
                        val gr = grStr.toDoubleOrNull() ?: if (info.has("granted_balance")) {
                            val d = info.optDouble("granted_balance")
                            if (d.isFinite()) d else null
                        } else null

                        val topStr = info.optString("topped_up_balance", "")
                        val top = topStr.toDoubleOrNull() ?: if (info.has("topped_up_balance")) {
                            val d = info.optDouble("topped_up_balance")
                            if (d.isFinite()) d else null
                        } else null

                        balances.add(DeepSeekBalance(curr, tot, gr, top))
                    }
                }

                val primaryBalance = balances.firstOrNull()
                val totalBalance = primaryBalance?.totalBalance
                val grantedBalance = primaryBalance?.grantedBalance
                val toppedUpBalance = primaryBalance?.toppedUpBalance
                val currency = primaryBalance?.currency ?: "CNY"

                // 只要 HTTP 200 鉴权成功，即使余额为 0 也是有效 Key，不可误判为凭据失效
                val snapshot = CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias.ifBlank { "DeepSeek" },
                    email = if (accountAlias.contains("@")) accountAlias else null,
                    credentialStatus = CredentialStatus.VALID,
                    primaryWindow = null,
                    secondaryWindow = null,
                    creditsBalance = totalBalance,
                    grantedBalance = grantedBalance,
                    toppedUpBalance = toppedUpBalance,
                    balanceList = if (balances.isNotEmpty()) balances else null,
                    currency = currency,
                    isAvailable = isAvailable,
                    planName = null,
                    queriedAt = System.currentTimeMillis(),
                    isLastGood = false,
                    errorMessage = null,
                    limitReached = !isAvailable || (totalBalance != null && totalBalance <= 0.0),
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
