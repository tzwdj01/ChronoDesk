package com.chronodesk.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CodexUsageSnapshot(
    val accountId: String,
    val accountAlias: String,
    val credentialStatus: CredentialStatus,
    val primaryWindow: CodexUsageWindow? = null,    // 通常是 5小时限额
    val secondaryWindow: CodexUsageWindow? = null,  // 通常是 7天/周限额
    val creditsBalance: Double? = null,
    val planName: String? = null,
    val queriedAt: Long? = null,
    val isLastGood: Boolean = false,
    val errorMessage: String? = null
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
            lastGood: CodexUsageSnapshot? = null
        ): CodexUsageSnapshot {
            return lastGood?.copy(
                isLastGood = true,
                errorMessage = message
            ) ?: CodexUsageSnapshot(
                accountId = accountId,
                accountAlias = alias,
                credentialStatus = status,
                queriedAt = System.currentTimeMillis(),
                isLastGood = false,
                errorMessage = message
            )
        }
    }
}
