package com.chronodesk.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CodexUsageSnapshot(
    val accountId: String,
    val accountAlias: String,
    val credentialStatus: CredentialStatus,
    val email: String? = null,
    val primaryWindow: CodexUsageWindow? = null,    // 通常是 5小时限额
    val secondaryWindow: CodexUsageWindow? = null,  // 通常是 7天/周限额
    val creditsBalance: Double? = null,             // 账户总余额 (DeepSeek 等)
    val grantedBalance: Double? = null,             // 真实赠送金额 (若官方接口实际返回)
    val toppedUpBalance: Double? = null,           // 真实充值金额 (若官方接口实际返回)
    val currency: String? = null,                   // 货币符号 ("CNY", "USD")
    val isAvailable: Boolean? = null,               // 真实账户是否可用
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
            return CodexUsageSnapshot(
                accountId = accountId,
                accountAlias = alias,
                credentialStatus = status,
                email = lastGood?.email,
                primaryWindow = lastGood?.primaryWindow,
                secondaryWindow = lastGood?.secondaryWindow,
                creditsBalance = lastGood?.creditsBalance,
                grantedBalance = lastGood?.grantedBalance,
                toppedUpBalance = lastGood?.toppedUpBalance,
                currency = lastGood?.currency,
                isAvailable = lastGood?.isAvailable,
                planName = lastGood?.planName,
                resetCreditsCount = lastGood?.resetCreditsCount,
                resetCreditsExpiresAt = lastGood?.resetCreditsExpiresAt,
                queriedAt = lastGood?.queriedAt,
                isLastGood = lastGood != null,
                errorMessage = message,
                limitReached = lastGood?.limitReached ?: false,
                modelProvider = modelProvider
            )
        }
    }
}
