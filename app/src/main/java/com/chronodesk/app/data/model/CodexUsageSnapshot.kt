package com.chronodesk.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SyncErrorType {
    AUTH_FAILED,        // 401/403 明确鉴权失效
    RATE_LIMITED,       // 429 限流
    SERVER_FAULT,       // 5xx 服务端故障
    DATA_FORMAT,        // 非 JSON 或关键字段缺失
    NETWORK_TIMEOUT     // 网络超时 / 瞬时错误
}

enum class QuotaSyncStatus {
    NOT_CONFIGURED,       // 「未配置」＋「连接账号」
    AWAITING_SYNC,        // 「待同步」。失败时显示原因和「重试」
    SYNCING,              // 「同步中」。有旧数据时继续显示原更新时间
    UPDATED,              // 「已更新」或显示「更新于 HH:mm」
    CACHED_STALE,         // 「缓存数据 · 更新于 HH:mm」与原数值。显示失败原因和「重试」
    CACHE_EXPIRED,        // 「缓存已过期」，仍显示原时间与数值
    AUTH_EXPIRED,         // 「授权失效」＋「重新授权」，清除旧额度数值
    SERVICE_UNAVAILABLE,  // 「暂不可用」及重试倒计时或「重试」
    DATA_FORMAT_ERROR     // 「数据格式异常」＋「重试」
}

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
    val lastSuccessTime: Long? = null,              // 最后一次成功查询到的时间戳 (ms)
    val isLastGood: Boolean = false,
    val errorMessage: String? = null,
    val lastErrorType: SyncErrorType? = null,        // 失败类型
    val retryAfterUnixMs: Long? = null,             // 429 冷却截止时间戳 (ms)
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

    val formattedSuccessTime: String
        get() {
            val t = lastSuccessTime ?: (if (!isLastGood) queriedAt else null) ?: queriedAt ?: return "未同步"
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))
        }

    val formattedVerifiedTime: String
        get() {
            val t = lastSuccessTime ?: queriedAt ?: return "未同步"
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))
        }

    /**
     * 计算该账号的统一状态判定，首页和详情页共用此规则
     */
    fun computeStatus(
        isConfigured: Boolean,
        isRefreshing: Boolean,
        refreshIntervalMinutes: Int = 15,
        nowMs: Long = System.currentTimeMillis()
    ): QuotaSyncStatus {
        if (!isConfigured || credentialStatus == CredentialStatus.NOT_LOGGED_IN) {
            return QuotaSyncStatus.NOT_CONFIGURED
        }
        if (isRefreshing) {
            return QuotaSyncStatus.SYNCING
        }
        if (credentialStatus == CredentialStatus.EXPIRED || lastErrorType == SyncErrorType.AUTH_FAILED) {
            return QuotaSyncStatus.AUTH_EXPIRED
        }
        if (lastErrorType == SyncErrorType.RATE_LIMITED || lastErrorType == SyncErrorType.SERVER_FAULT || isAvailable == false) {
            return QuotaSyncStatus.SERVICE_UNAVAILABLE
        }
        if (lastErrorType == SyncErrorType.DATA_FORMAT) {
            return QuotaSyncStatus.DATA_FORMAT_ERROR
        }

        val successTime = lastSuccessTime ?: if (!isLastGood && queriedAt != null) queriedAt else null
        val hasData = (primaryWindow != null || secondaryWindow != null || creditsBalance != null || isAvailable == true)

        if (successTime != null) {
            val intervalMs = refreshIntervalMinutes * 60 * 1000L
            val maxAgeMs = intervalMs * 2
            if (nowMs - successTime >= maxAgeMs) {
                return QuotaSyncStatus.CACHE_EXPIRED
            }
        }

        if (errorMessage != null || isLastGood) {
            return if (hasData || successTime != null) {
                QuotaSyncStatus.CACHED_STALE
            } else {
                QuotaSyncStatus.AWAITING_SYNC
            }
        }

        if (successTime != null || hasData) {
            return QuotaSyncStatus.UPDATED
        }

        return QuotaSyncStatus.AWAITING_SYNC
    }

    fun getRemainingRetrySeconds(nowMs: Long = System.currentTimeMillis()): Long {
        val target = retryAfterUnixMs ?: return 0L
        val diff = (target - nowMs) / 1000
        return if (diff > 0) diff else 0L
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
            modelProvider: String = "CHATGPT",
            errorType: SyncErrorType = if (status == CredentialStatus.EXPIRED) SyncErrorType.AUTH_FAILED else SyncErrorType.NETWORK_TIMEOUT,
            retryAfterUnixMs: Long? = null
        ): CodexUsageSnapshot {
            return if (status == CredentialStatus.EXPIRED || errorType == SyncErrorType.AUTH_FAILED) {
                // 明确鉴权失效（401/403）：清除旧额度数值
                CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = alias,
                    credentialStatus = CredentialStatus.EXPIRED,
                    primaryWindow = null,
                    secondaryWindow = null,
                    creditsBalance = null,
                    balanceList = null,
                    queriedAt = null,
                    lastSuccessTime = null,
                    isLastGood = false,
                    errorMessage = message,
                    lastErrorType = SyncErrorType.AUTH_FAILED,
                    modelProvider = modelProvider
                )
            } else if (lastGood != null) {
                // 保留旧数据
                lastGood.copy(
                    credentialStatus = status,
                    isLastGood = true,
                    errorMessage = message,
                    lastErrorType = errorType,
                    retryAfterUnixMs = retryAfterUnixMs,
                    modelProvider = modelProvider
                )
            } else {
                CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = alias,
                    credentialStatus = status,
                    queriedAt = null,
                    lastSuccessTime = null,
                    isLastGood = false,
                    errorMessage = message,
                    lastErrorType = errorType,
                    retryAfterUnixMs = retryAfterUnixMs,
                    modelProvider = modelProvider
                )
            }
        }
    }
}
