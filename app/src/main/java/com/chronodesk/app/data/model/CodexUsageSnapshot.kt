package com.chronodesk.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeepSeekBalance(
    val currency: String,
    val totalBalance: Double?,
    val grantedBalance: Double? = null,
    val toppedUpBalance: Double? = null
)

data class CodexUsageSnapshot(
    val accountId: String,
    val accountAlias: String,
    val credentialStatus: CredentialStatus,
    val email: String? = null,
    val primaryWindow: CodexUsageWindow? = null,    // 上游第一额度窗口
    val secondaryWindow: CodexUsageWindow? = null,  // 上游第二额度窗口
    val creditsBalance: Double? = null,             // 账户总余额 (DeepSeek 等)
    val grantedBalance: Double? = null,             // 真实赠送金额
    val toppedUpBalance: Double? = null,           // 真实充值金额
    val balanceList: List<DeepSeekBalance>? = null, // 多币种余额列表
    val currency: String? = null,                   // 货币符号 ("CNY", "USD")
    val isAvailable: Boolean? = null,               // 真实账户/服务是否可用
    val planName: String? = null,
    val resetCreditsCount: Int? = null,             // 官方额度重置卡可用次数
    val resetCreditsExpiresAt: String? = null,       // 重置卡/额度到期时间
    val queriedAt: Long? = null,
    val isLastGood: Boolean = false,
    val errorMessage: String? = null,
    val limitReached: Boolean = false,
    val modelProvider: String = "CHATGPT"
) {
    val formattedQueriedAt: String
        get() {
            if (queriedAt == null || queriedAt <= 0) return "未同步"
            val now = System.currentTimeMillis()
            val diffSec = (now - queriedAt) / 1000
            return when {
                diffSec < 60 -> "刚刚"
                diffSec < 3600 -> "${diffSec / 60}分钟前"
                diffSec < 86400 -> "${diffSec / 3600}小时前"
                else -> {
                    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    sdf.format(Date(queriedAt))
                }
            }
        }

    val formattedVerifiedTime: String
        get() {
            val t = queriedAt ?: return "未同步"
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))
        }

    /**
     * 针对 DeepSeek 格式化展示余额与可用性：
     * 1. 响应指明服务不可用时显示「服务不可用」
     * 2. 多币种分别列出金额
     * 3. 余额为零且鉴权成功时显示「余额为零」
     * 4. 余额缺失显示「暂无余额数据」
     */
    val formattedBalanceText: String
        get() {
            if (modelProvider == "DEEPSEEK") {
                if (isAvailable == false) return "服务不可用"
                val list = balanceList
                if (!list.isNullOrEmpty()) {
                    val validItems = list.filter { it.totalBalance != null }
                    if (validItems.isEmpty()) return "暂无余额数据"
                    return validItems.joinToString(" / ") { item ->
                        val symbol = if (item.currency == "USD") "$" else "¥"
                        val amount = item.totalBalance!!
                        if (amount == 0.0) "${item.currency} 余额为零"
                        else "$symbol${String.format(Locale.US, "%.2f", amount)} ${item.currency}"
                    }
                }
                if (creditsBalance == null) return "暂无余额数据"
                if (creditsBalance == 0.0) return "余额为零"
                val symbol = if (currency == "USD") "$" else "¥"
                return "$symbol${String.format(Locale.US, "%.2f", creditsBalance)} ${currency ?: "CNY"}"
            }
            return ""
        }

    companion object {
        fun empty(accountId: String, alias: String): CodexUsageSnapshot {
            return CodexUsageSnapshot(
                accountId = accountId,
                accountAlias = alias,
                credentialStatus = CredentialStatus.NOT_LOGGED_IN,
                queriedAt = null
            )
        }

        fun error(
            accountId: String,
            alias: String,
            status: CredentialStatus,
            message: String,
            lastGood: CodexUsageSnapshot? = null,
            modelProvider: String = "CHATGPT"
        ): CodexUsageSnapshot {
            return if (lastGood != null) {
                lastGood.copy(
                    credentialStatus = status,
                    isLastGood = true,
                    errorMessage = message,
                    modelProvider = modelProvider
                )
            } else {
                CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = alias,
                    credentialStatus = status,
                    queriedAt = null,
                    isLastGood = false,
                    errorMessage = message,
                    modelProvider = modelProvider
                )
            }
        }
    }
}
