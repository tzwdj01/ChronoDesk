package com.chronodesk.app.ui.clock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.model.QuotaSyncStatus
import com.chronodesk.app.data.model.SyncErrorType
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkProgressTrack
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkBorderSubtle
import com.chronodesk.app.ui.theme.InkProgress
import com.chronodesk.app.ui.theme.InkProgressTrack
import com.chronodesk.app.ui.theme.InkStatusGreen
import com.chronodesk.app.ui.theme.InkStatusTagBg
import com.chronodesk.app.ui.theme.InkWarningRed
import com.chronodesk.app.ui.theme.InkWarningAmber
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkSurfaceElevated
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkTextTertiary
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

/**
 * AI 平台多模型配额监控区域
 * 核心按照“同一订阅商在同一框内”的设计原则，铺平展示：
 * 1. OpenAI / ChatGPT 专属大框（整合两个 ChatGPT 账号的 5小时额度与周额度）
 * 2. DeepSeek AI 专属大框（参照官方 /user/balance 接口展示余额、明细与额度）
 */
@Composable
fun MultiAccountQuotaSection(
    accounts: List<CodexAccount>,
    snapshots: Map<String, CodexUsageSnapshot>,
    loadingMap: Map<String, Boolean>,
    isRefreshingAll: Boolean,
    nightModeOption: String,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    areControlsVisible: Boolean = false,
    isCompact: Boolean = false,
    cardDensity: String = "NORMAL",
    isMockMode: Boolean = false,
    refreshIntervalMinutes: Int = 15,
    onAddAccount: () -> Unit = {},
    onRefreshAccount: (accountId: String) -> Unit = {},
    onDeleteAccount: (accountId: String) -> Unit = {},
    onAccountClick: (account: CodexAccount) -> Unit = {},
    onRefreshAll: () -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val chatGptAccounts = accounts.filter { (it.modelProvider == "CHATGPT" || it.modelProvider == null) && it.isVisibleOnMain }
        .sortedByDescending { it.isPinnedMain }
    val deepSeekAccounts = accounts.filter { it.modelProvider == "DEEPSEEK" && it.isVisibleOnMain }
        .sortedByDescending { it.isPinnedMain }
    val antigravityAccounts = accounts.filter { it.modelProvider == "ANTIGRAVITY" && it.isVisibleOnMain }
        .sortedByDescending { it.isPinnedMain }

    val hasMultipleProviders = (if (chatGptAccounts.isNotEmpty()) 1 else 0) +
        (if (deepSeekAccounts.isNotEmpty()) 1 else 0) +
        (if (antigravityAccounts.isNotEmpty()) 1 else 0) >= 3

    val spacing = when (cardDensity) {
        "COMPACT" -> if (isCompact) 3.dp else 5.dp
        "SPACIOUS" -> if (isCompact) 7.dp else 10.dp
        else -> if (hasMultipleProviders) (if (isCompact) 3.dp else 5.5.dp) else (if (isCompact) 6.dp else 9.dp)
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // 1. 同一订阅商：OpenAI / ChatGPT 专属大框
        if (chatGptAccounts.isNotEmpty()) {
            ChatGptUnifiedSubscriptionBox(
                accounts = chatGptAccounts,
                snapshots = snapshots,
                loadingMap = loadingMap,
                isNightMode = isNightMode,
                areControlsVisible = areControlsVisible,
                isCompact = isCompact,
                isMockMode = isMockMode,
                refreshIntervalMinutes = refreshIntervalMinutes,
                onAccountClick = onAccountClick,
                onRefreshAccount = onRefreshAccount,
                onDeleteAccount = onDeleteAccount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. 同一订阅商：DeepSeek 专属大框（支持多账号独立显示金额与自定义别名）
        if (deepSeekAccounts.isNotEmpty()) {
            DeepSeekUnifiedSubscriptionBox(
                accounts = deepSeekAccounts,
                snapshots = snapshots,
                loadingMap = loadingMap,
                isNightMode = isNightMode,
                areControlsVisible = areControlsVisible,
                isCompact = isCompact,
                isMockMode = isMockMode,
                refreshIntervalMinutes = refreshIntervalMinutes,
                onAccountClick = onAccountClick,
                onRefreshAccount = onRefreshAccount,
                onDeleteAccount = onDeleteAccount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. 同一订阅商：Gemini API 专属大框（连接状态与模型验证）
        if (antigravityAccounts.isNotEmpty()) {
            GoogleAntigravityUnifiedSubscriptionBox(
                accounts = antigravityAccounts,
                snapshots = snapshots,
                loadingMap = loadingMap,
                isNightMode = isNightMode,
                areControlsVisible = areControlsVisible,
                isCompact = isCompact,
                isMockMode = isMockMode,
                refreshIntervalMinutes = refreshIntervalMinutes,
                onAccountClick = onAccountClick,
                onRefreshAccount = onRefreshAccount,
                onDeleteAccount = onDeleteAccount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. 空状态与账号指引
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isNightMode) DarkSurface else InkSurface)
                    .border(1.dp, if (isNightMode) DarkBorder else InkBorder, RoundedCornerShape(12.dp))
                    .clickable { onAddAccount() }
                    .padding(vertical = if (isCompact) 18.dp else 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "添加第一个 AI 账号",
                        tint = if (isNightMode) TechCyan else InkProgress,
                        modifier = Modifier.size(if (isCompact) 26.dp else 32.dp)
                    )
                    Text(
                        text = "欢迎使用 ChronoDesk",
                        color = if (isNightMode) TextPrimary else InkTextPrimary,
                        fontSize = if (isCompact) 13.5.sp else 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "点击配置你的第一个 AI 账号",
                        color = if (isNightMode) TechCyan else InkProgress,
                        fontSize = if (isCompact) 11.sp else 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "支持 ChatGPT (Codex) / DeepSeek / Gemini API",
                        color = if (isNightMode) TextTertiary else InkTextTertiary,
                        fontSize = if (isCompact) 9.5.sp else 10.5.sp
                    )
                }
            }
        } else if (chatGptAccounts.isEmpty() && deepSeekAccounts.isEmpty() && antigravityAccounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isNightMode) DarkSurface else InkSurface)
                    .border(1.dp, if (isNightMode) DarkBorder else InkBorder, RoundedCornerShape(12.dp))
                    .clickable { onSettingsClick() }
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "当前无主屏幕显示的 AI 账号",
                        color = if (isNightMode) TextPrimary else InkTextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "账号已配置在第二屏显示或已隐藏\n点击或滑动至第二屏查看，或在账号管理中开启主屏显示",
                        color = if (isNightMode) TextSecondary else InkTextSecondary,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * OpenAI / ChatGPT 统一大框（包含多账号归集与 5小时/周额度双进度条）
 */
@Composable
fun ChatGptUnifiedSubscriptionBox(
    accounts: List<CodexAccount>,
    snapshots: Map<String, CodexUsageSnapshot>,
    loadingMap: Map<String, Boolean>,
    isNightMode: Boolean,
    areControlsVisible: Boolean,
    isCompact: Boolean,
    isMockMode: Boolean = false,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: (CodexAccount) -> Unit,
    onRefreshAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isNightMode) DarkSurface else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 9.dp else 12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 9.dp else 12.dp))
            .padding(horizontal = if (isCompact) 9.dp else 13.dp, vertical = if (isCompact) 4.dp else 6.5.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 4.5.dp)) {
            // 大框顶栏：OpenAI Logo + 订阅商名称 + 演示标签 + 账号汇总
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModelProviderLogo(
                        provider = AiModelProvider.CHATGPT,
                        size = if (isCompact) 22.dp else 28.dp,
                        isNightMode = isNightMode
                    )
                    Text(
                        text = "ChatGPT",
                        color = primaryText,
                        fontSize = if (isCompact) 12.5.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isMockMode) {
                        MockBadge(isNightMode = isNightMode)
                    }
                }

                // 状态标签
                val statusColor = if (isNightMode) TechEmerald else InkStatusGreen
                val statusBg = if (isNightMode) statusColor.copy(alpha = 0.12f) else InkStatusTagBg
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(statusBg)
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = if (accounts.size > 1) "${accounts.size}个账号" else "正常",
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 框内遍历展示该订阅商下的各个账号（双额度）
            accounts.forEachIndexed { index, acc ->
                if (index > 0) {
                    HorizontalDivider(
                        color = if (isNightMode) DarkBorder.copy(alpha = 0.5f) else InkBorderSubtle,
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(vertical = if (isCompact) 1.5.dp else 3.dp)
                    )
                }
                SingleAccountQuotaItem(
                    account = acc,
                    snapshot = snapshots[acc.id],
                    isLoading = loadingMap[acc.id] == true,
                    isNightMode = isNightMode,
                    isCompact = isCompact,
                    areControlsVisible = areControlsVisible,
                    refreshIntervalMinutes = refreshIntervalMinutes,
                    onAccountClick = { onAccountClick(acc) },
                    onRefreshClick = { onRefreshAccount(acc.id) },
                    onDeleteClick = { onDeleteAccount(acc.id) }
                )
            }
        }
    }
}

/**
 * 紧凑型副账号行（用于 16:9 紧凑屏折叠展示副账号，空间充裕的长屏则展开完整双额度条）
 */
@Composable
private fun CompactSecondaryAccountQuotaItem(
    account: CodexAccount,
    snapshot: CodexUsageSnapshot?,
    isLoading: Boolean,
    isNightMode: Boolean,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary

    val syncStatus = snapshot?.computeStatus(
        isConfigured = account.isConfigured,
        isRefreshing = isLoading,
        refreshIntervalMinutes = refreshIntervalMinutes
    ) ?: if (account.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

    val remainingCooldown = snapshot?.getRemainingRetrySeconds() ?: 0L
    val isCoolingDown = remainingCooldown > 0L

    val pw = snapshot?.primaryWindow
    val sw = snapshot?.secondaryWindow
    val pwRemaining = pw?.formattedRemaining() ?: "—"
    val swRemaining = sw?.formattedRemaining() ?: "—"

    val isPwExhausted = (pw?.remainingPercent != null && pw.remainingPercent <= 0.0) || snapshot?.limitReached == true
    val isSwExhausted = sw?.remainingPercent != null && sw.remainingPercent <= 0.0
    val isWarning = !isPwExhausted && !isSwExhausted && (
        (pw?.remainingPercent != null && pw.remainingPercent in 0.01..15.0) ||
        (sw?.remainingPercent != null && sw.remainingPercent in 0.01..15.0)
    )

    val statusText = when (syncStatus) {
        QuotaSyncStatus.NOT_CONFIGURED -> "未配置"
        QuotaSyncStatus.AWAITING_SYNC -> "待同步"
        QuotaSyncStatus.SYNCING -> "同步中"
        QuotaSyncStatus.UPDATED -> when {
            isSwExhausted -> "周额耗尽"
            isPwExhausted -> "5h限额"
            isWarning -> "即将耗尽"
            else -> "正常"
        }
        QuotaSyncStatus.CACHED_STALE -> if (snapshot?.lastErrorType == SyncErrorType.ACCESS_DENIED) "访问受限" else "缓存"
        QuotaSyncStatus.CACHE_EXPIRED -> "已过期"
        QuotaSyncStatus.AUTH_EXPIRED -> "失效"
        QuotaSyncStatus.SERVICE_UNAVAILABLE -> if (isCoolingDown) "${remainingCooldown}s" else "暂不可用"
        QuotaSyncStatus.DATA_FORMAT_ERROR -> "格式异常"
    }
    val statusColor = when (syncStatus) {
        QuotaSyncStatus.NOT_CONFIGURED -> mutedText
        QuotaSyncStatus.UPDATED -> when {
            isSwExhausted -> if (isNightMode) TechRose else InkWarningRed
            isPwExhausted || isWarning -> if (isNightMode) TechAmber else InkWarningAmber
            else -> if (isNightMode) TechEmerald else InkStatusGreen
        }
        QuotaSyncStatus.AUTH_EXPIRED, QuotaSyncStatus.CACHE_EXPIRED -> if (isNightMode) TechRose else InkWarningRed
        else -> if (isNightMode) TechAmber else InkWarningAmber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isNightMode) DarkSurfaceElevated.copy(alpha = 0.45f) else InkSurfaceElevated.copy(alpha = 0.45f))
            .clickable { onAccountClick() }
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(statusColor, CircleShape)
            )
            if (account.isPinnedMain) {
                Text(
                    text = "★",
                    color = if (isNightMode) TechAmber else InkWarningAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = account.displayTitle,
                color = primaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (syncStatus != QuotaSyncStatus.AUTH_EXPIRED && syncStatus != QuotaSyncStatus.NOT_CONFIGURED) {
                Text(
                    text = "5h: $pwRemaining · 周: $swRemaining",
                    color = secondaryText,
                    fontSize = 8.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 单个账号展示项：包含邮箱别名、5 小时额度进度、周额度进度与重置倒计时
 */
@Composable
private fun SingleAccountQuotaItem(
    account: CodexAccount,
    snapshot: CodexUsageSnapshot?,
    isLoading: Boolean,
    isNightMode: Boolean,
    isCompact: Boolean,
    areControlsVisible: Boolean,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary
    val trackColor = if (isNightMode) DarkProgressTrack else InkProgressTrack

    val nowUnix = System.currentTimeMillis() / 1000
    val primaryWindow = snapshot?.primaryWindow
    val secondaryWindow = snapshot?.secondaryWindow

    val primaryRemainingStr = primaryWindow?.formattedRemaining() ?: "—"
    val secondaryRemainingStr = secondaryWindow?.formattedRemaining() ?: "—"

    val isPrimaryExhausted = (primaryWindow?.remainingPercent != null && primaryWindow.remainingPercent <= 0.0) || snapshot?.limitReached == true
    val isSecondaryExhausted = secondaryWindow?.remainingPercent != null && secondaryWindow.remainingPercent <= 0.0
    val isWarning = !isPrimaryExhausted && !isSecondaryExhausted && (
        (primaryWindow?.remainingPercent != null && primaryWindow.remainingPercent in 0.01..15.0) ||
        (secondaryWindow?.remainingPercent != null && secondaryWindow.remainingPercent in 0.01..15.0)
    )

    val syncStatus = snapshot?.computeStatus(
        isConfigured = account.isConfigured,
        isRefreshing = isLoading,
        refreshIntervalMinutes = refreshIntervalMinutes
    ) ?: if (account.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

    val remainingCooldown = snapshot?.getRemainingRetrySeconds() ?: 0L
    val isCoolingDown = remainingCooldown > 0L

    // 动态额度条颜色
    val primaryProgressColor = if (isNightMode) {
        when {
            isPrimaryExhausted -> TechRose
            primaryWindow?.remainingPercent != null && primaryWindow.remainingPercent <= 15.0 -> TechAmber
            else -> TechEmerald
        }
    } else {
        when {
            isPrimaryExhausted -> InkWarningRed
            primaryWindow?.remainingPercent != null && primaryWindow.remainingPercent <= 15.0 -> InkWarningAmber
            else -> InkProgress
        }
    }

    val secondaryProgressColor = if (isNightMode) {
        when {
            isSecondaryExhausted -> TechRose
            secondaryWindow?.remainingPercent != null && secondaryWindow.remainingPercent <= 15.0 -> TechAmber
            else -> TechEmerald
        }
    } else {
        when {
            isSecondaryExhausted -> InkWarningRed
            secondaryWindow?.remainingPercent != null && secondaryWindow.remainingPercent <= 15.0 -> InkWarningAmber
            else -> InkProgress
        }
    }

    // 状态文本与徽标色彩：统一共用规则
    val (statusText, statusColor, statusBg) = when (syncStatus) {
        QuotaSyncStatus.NOT_CONFIGURED -> Triple(
            "未配置",
            mutedText,
            if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
        )
        QuotaSyncStatus.AWAITING_SYNC -> Triple(
            if (snapshot?.errorMessage != null) "待同步 · 异常" else "待同步",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.14f)
        )
        QuotaSyncStatus.SYNCING -> Triple(
            "同步中",
            if (isNightMode) TechCyan else InkProgress,
            (if (isNightMode) TechCyan else InkProgress).copy(alpha = 0.14f)
        )
        QuotaSyncStatus.UPDATED -> {
            val label = when {
                isSecondaryExhausted -> "周额度耗尽"
                isPrimaryExhausted -> "5h限额"
                isWarning -> "即将耗尽"
                else -> "正常"
            }
            val color = when {
                isSecondaryExhausted -> if (isNightMode) TechRose else InkWarningRed
                isPrimaryExhausted || isWarning -> if (isNightMode) TechAmber else InkWarningAmber
                else -> if (isNightMode) TechEmerald else InkStatusGreen
            }
            Triple(label, color, color.copy(alpha = 0.14f))
        }
        QuotaSyncStatus.CACHED_STALE -> Triple(
            if (snapshot?.lastErrorType == SyncErrorType.ACCESS_DENIED) "访问受限" else "缓存 · ${snapshot?.errorMessage?.take(if (isCompact) 4 else 6) ?: "异常"}",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.14f)
        )
        QuotaSyncStatus.CACHE_EXPIRED -> Triple(
            "缓存已过期",
            if (isNightMode) TechRose else InkWarningRed,
            (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.14f)
        )
        QuotaSyncStatus.AUTH_EXPIRED -> Triple(
            "授权失效",
            if (isNightMode) TechRose else InkWarningRed,
            (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.14f)
        )
        QuotaSyncStatus.SERVICE_UNAVAILABLE -> Triple(
            if (isCoolingDown) "${remainingCooldown}s后重试" else "暂不可用",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.14f)
        )
        QuotaSyncStatus.DATA_FORMAT_ERROR -> Triple(
            "数据格式异常",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.14f)
        )
    }

    // 旋转动画（用于刷新中状态）
    val infiniteTransition = rememberInfiniteTransition(label = "spinSingle")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngleSingle"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAccountClick() },
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.5.dp else 3.dp)
    ) {
        // 账号标识行：别名/脱敏邮箱 + 套餐徽标(Plus/Pro) + 重置次数 + 状态徽标 + 呼出操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 3.5.dp else 5.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .background(
                            if (isNightMode) {
                                if (isSecondaryExhausted || syncStatus == QuotaSyncStatus.AUTH_EXPIRED) TechRose
                                else if (isPrimaryExhausted) TechAmber
                                else TechCyan
                            } else {
                                if (isSecondaryExhausted || syncStatus == QuotaSyncStatus.AUTH_EXPIRED) InkWarningRed
                                else if (isPrimaryExhausted) InkWarningAmber
                                else InkStatusGreen
                            },
                            CircleShape
                        )
                )
                if (account.isPinnedMain) {
                    Text(
                        text = "★",
                        color = if (isNightMode) TechAmber else InkWarningAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = account.displayTitle,
                    color = primaryText,
                    fontSize = if (isCompact) 10.5.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                val plan = snapshot?.planName?.ifBlank { "Plus" } ?: "Plus"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated)
                        .padding(horizontal = 3.5.dp, vertical = 0.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = plan,
                        color = secondaryText,
                        fontSize = if (isCompact) 7.5.sp else 8.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                val resetCredits = snapshot?.resetCreditsCount
                if (resetCredits != null && resetCredits > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isNightMode) TechCyan.copy(alpha = 0.15f) else InkProgressTrack)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "重置:${resetCredits}次",
                            color = if (isNightMode) TechCyan else InkProgress,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                AnimatedVisibility(
                    visible = areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshClick,
                            enabled = !isLoading && !isCoolingDown,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新 ${account.displayTitle} 额度",
                                tint = if (isLoading) (if (isNightMode) TechCyan else InkProgress)
                                       else if (isCoolingDown) secondaryText.copy(alpha = 0.35f)
                                       else secondaryText,
                                modifier = Modifier
                                    .size(11.dp)
                                    .rotate(if (isLoading) spinAngle else 0f)
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "删除 ${account.displayTitle}",
                                tint = secondaryText.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }

        if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isCompact) 2.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "登录凭据已失效 · 点击重新授权",
                    color = if (isNightMode) TechRose else InkWarningRed,
                    fontSize = if (isCompact) 8.5.sp else 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "重新授权",
                    color = if (isNightMode) TechCyan else InkProgress,
                    fontSize = if (isCompact) 8.sp else 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (syncStatus == QuotaSyncStatus.NOT_CONFIGURED) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isCompact) 2.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "未配置账号 · 点击进行连接",
                    color = secondaryText,
                    fontSize = if (isCompact) 8.5.sp else 10.sp
                )
                Text(
                    text = "连接账号",
                    color = if (isNightMode) TechCyan else InkProgress,
                    fontSize = if (isCompact) 8.sp else 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // 5 小时 / 主额度行 (对齐起始位置与 100% 进度条长度)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 3.dp else 4.dp)
                ) {
                    val primaryLabel = if (isCompact) {
                        if (primaryWindow?.limitWindowSeconds == 18000L) "5小时" else (primaryWindow?.label ?: "额度")
                    } else {
                        primaryWindow?.label ?: "额度窗口"
                    }
                    Text(
                        text = primaryLabel,
                        color = secondaryText,
                        fontSize = if (isCompact) 8.5.sp else 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.width(if (isCompact) 34.dp else 56.dp)
                    )
                    val pPercent = primaryWindow?.remainingPercent
                    LinearProgressIndicator(
                        progress = { if (pPercent != null && pPercent.isFinite()) (pPercent / 100.0).toFloat().coerceIn(0f, 1f) else 0f },
                        modifier = Modifier
                            .width(if (isCompact) 48.dp else 88.dp)
                            .height(if (isCompact) 3.5.dp else 4.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = primaryProgressColor,
                        trackColor = trackColor
                    )
                }
                val primaryResetDesc = when {
                    primaryWindow == null -> if (isLoading) "同步中..." else "待同步"
                    primaryWindow.resetAtUnix == null -> "重置时间未知"
                    nowUnix >= primaryWindow.resetAtUnix -> "等待更新"
                    else -> "${primaryWindow.formattedResetTime(nowUnix)} 重置"
                }
                Text(
                    text = "剩余 $primaryRemainingStr · $primaryResetDesc",
                    color = if (isPrimaryExhausted) (if (isNightMode) TechRose else InkWarningRed) else mutedText,
                    fontSize = if (isCompact) 7.5.sp else 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(start = 4.dp)
                )
            }

            // 周额度 / 次额度行 (对齐起始位置与 100% 进度条长度)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 3.dp else 4.dp)
                ) {
                    val secondaryLabel = if (isCompact) {
                        if (secondaryWindow?.limitWindowSeconds == 604800L) "周额度" else (secondaryWindow?.label ?: "额度")
                    } else {
                        secondaryWindow?.label ?: "周额度"
                    }
                    Text(
                        text = secondaryLabel,
                        color = secondaryText,
                        fontSize = if (isCompact) 8.5.sp else 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.width(if (isCompact) 34.dp else 56.dp)
                    )
                    val sPercent = secondaryWindow?.remainingPercent
                    LinearProgressIndicator(
                        progress = { if (sPercent != null && sPercent.isFinite()) (sPercent / 100.0).toFloat().coerceIn(0f, 1f) else 0f },
                        modifier = Modifier
                            .width(if (isCompact) 48.dp else 88.dp)
                            .height(if (isCompact) 3.5.dp else 4.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = secondaryProgressColor,
                        trackColor = trackColor
                    )
                }
                val secondaryResetDesc = when {
                    secondaryWindow == null -> if (isLoading) "同步中..." else "待同步"
                    secondaryWindow.resetAtUnix == null -> "重置时间未知"
                    nowUnix >= secondaryWindow.resetAtUnix -> "等待更新"
                    else -> secondaryWindow.formattedCountdown(nowUnix)
                }
                val cachedTimeSuffix = if (syncStatus == QuotaSyncStatus.CACHED_STALE || syncStatus == QuotaSyncStatus.CACHE_EXPIRED) {
                    val timeStr = snapshot?.formattedSuccessTime ?: snapshot?.formattedQueriedAt
                    if (timeStr != null) {
                        if (isCompact) " ($timeStr)" else " (更新于 $timeStr)"
                    } else ""
                } else ""
                Text(
                    text = "剩余 $secondaryRemainingStr · $secondaryResetDesc$cachedTimeSuffix",
                    color = if (isSecondaryExhausted) (if (isNightMode) TechRose else InkWarningRed) else mutedText,
                    fontSize = if (isCompact) 7.5.sp else 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * DeepSeek 专属大框（严格直连官方 /user/balance 接口展示真实余额与状态，支持多账号与自定义账号名称）
 */
@Composable
fun DeepSeekUnifiedSubscriptionBox(
    accounts: List<CodexAccount>,
    snapshots: Map<String, CodexUsageSnapshot>,
    loadingMap: Map<String, Boolean>,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    areControlsVisible: Boolean = true,
    isCompact: Boolean = false,
    isMockMode: Boolean = false,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: (CodexAccount) -> Unit = {},
    onRefreshAccount: (String) -> Unit = {},
    onDeleteAccount: (String) -> Unit = {}
) {
    val cardBg = if (isNightMode) DarkSurface else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 9.dp else 12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 9.dp else 12.dp))
            .padding(horizontal = if (isCompact) 9.dp else 13.dp, vertical = if (isCompact) 4.dp else 6.5.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 4.5.dp)) {
            // 大框头部：官方 DeepSeek Logo + DeepSeek 名称 + 账号数状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModelProviderLogo(
                        provider = AiModelProvider.DEEPSEEK,
                        size = if (isCompact) 22.dp else 28.dp,
                        isNightMode = isNightMode
                    )
                    Text(
                        text = "DeepSeek",
                        color = primaryText,
                        fontSize = if (isCompact) 12.5.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isMockMode) {
                        MockBadge(isNightMode = isNightMode)
                    }
                }

                val statusColor = if (isNightMode) TechEmerald else InkStatusGreen
                val statusBg = if (isNightMode) statusColor.copy(alpha = 0.12f) else InkStatusTagBg
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(statusBg)
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = if (accounts.size > 1) "${accounts.size}个账号" else "正常",
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 框内遍历展示该订阅商下的各个 DeepSeek 账号
            accounts.forEachIndexed { index, acc ->
                if (index > 0) {
                    HorizontalDivider(
                        color = if (isNightMode) DarkBorder.copy(alpha = 0.5f) else InkBorderSubtle,
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(vertical = 1.5.dp)
                    )
                }
                SingleDeepSeekAccountItem(
                    account = acc,
                    snapshot = snapshots[acc.id],
                    isLoading = loadingMap[acc.id] == true,
                    isNightMode = isNightMode,
                    isCompact = isCompact,
                    showDelete = accounts.size > 1,
                    areControlsVisible = areControlsVisible,
                    refreshIntervalMinutes = refreshIntervalMinutes,
                    onAccountClick = { onAccountClick(acc) },
                    onRefreshClick = { onRefreshAccount(acc.id) },
                    onDeleteClick = { onDeleteAccount(acc.id) }
                )
            }
        }
    }
}

/**
 * 单个 DeepSeek 账号行（支持自定义别名、余额、充值赠送明细与实时状态）
 */
@Composable
private fun SingleDeepSeekAccountItem(
    account: CodexAccount,
    snapshot: CodexUsageSnapshot?,
    isLoading: Boolean,
    isNightMode: Boolean,
    isCompact: Boolean,
    showDelete: Boolean,
    areControlsVisible: Boolean,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary

    val syncStatus = snapshot?.computeStatus(
        isConfigured = account.isConfigured,
        isRefreshing = isLoading,
        refreshIntervalMinutes = refreshIntervalMinutes
    ) ?: if (account.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

    val remainingCooldown = snapshot?.getRemainingRetrySeconds() ?: 0L
    val isCoolingDown = remainingCooldown > 0L

    // 状态徽标与文本
    val isDemoMock = snapshot?.isMock == true && snapshot.errorMessage == "暂无演示数据"
    val (statusText, statusColor, statusBg) = if (isDemoMock) {
        Triple(
            "暂无演示数据",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
    } else when (syncStatus) {
        QuotaSyncStatus.NOT_CONFIGURED -> Triple(
            "未配置",
            mutedText,
            if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
        )
        QuotaSyncStatus.AWAITING_SYNC -> Triple(
            if (snapshot?.errorMessage != null) "待同步 · 异常" else "待同步",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.SYNCING -> Triple(
            "同步中",
            if (isNightMode) TechCyan else InkProgress,
            if (isNightMode) TechCyan.copy(alpha = 0.12f) else InkStatusTagBg
        )
        QuotaSyncStatus.UPDATED -> {
            when {
                snapshot?.isAvailable == false -> Triple(
                    "服务不可用",
                    if (isNightMode) TechRose else InkWarningRed,
                    (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.12f)
                )
                snapshot?.creditsBalance != null && snapshot.creditsBalance <= 0.0 -> Triple(
                    "余额为零",
                    if (isNightMode) TechAmber else InkWarningAmber,
                    (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
                )
                else -> Triple(
                    "正常",
                    if (isNightMode) TechEmerald else InkStatusGreen,
                    if (isNightMode) TechEmerald.copy(alpha = 0.12f) else InkStatusTagBg
                )
            }
        }
        QuotaSyncStatus.CACHED_STALE -> Triple(
            "缓存 · ${snapshot?.errorMessage?.take(6) ?: "异常"}",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.CACHE_EXPIRED -> Triple(
            "缓存已过期",
            if (isNightMode) TechRose else InkWarningRed,
            (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.AUTH_EXPIRED -> Triple(
            "授权失效",
            if (isNightMode) TechRose else InkWarningRed,
            (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.SERVICE_UNAVAILABLE -> Triple(
            if (isCoolingDown) "${remainingCooldown}s后重试" else "暂不可用",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.DATA_FORMAT_ERROR -> Triple(
            "数据格式异常",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spinDs")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngleDs"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAccountClick() },
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 2.5.dp)
    ) {
        // 账号标识行：圆点 + 自定义账号名称 + 状态徽标 + 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .background(
                            if (isNightMode) {
                                if (syncStatus == QuotaSyncStatus.UPDATED) TechEmerald else if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) TechRose else TechAmber
                            } else {
                                if (syncStatus == QuotaSyncStatus.UPDATED) InkStatusGreen else if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) InkWarningRed else InkWarningAmber
                            },
                            CircleShape
                        )
                )
                if (account.isPinnedMain) {
                    Text(
                        text = "★",
                        color = if (isNightMode) TechAmber else InkWarningAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = account.displayTitle,
                    color = primaryText,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated)
                        .padding(horizontal = 3.5.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = "API",
                        color = secondaryText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedVisibility(
                    visible = areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshClick,
                            enabled = !isLoading && !isCoolingDown,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新 ${account.displayTitle} 额度",
                                tint = if (isLoading) (if (isNightMode) TechCyan else InkProgress)
                                       else if (isCoolingDown) secondaryText.copy(alpha = 0.35f)
                                       else secondaryText,
                                modifier = Modifier
                                    .size(13.dp)
                                    .rotate(if (isLoading) spinAngle else 0f)
                            )
                        }
                        if (showDelete) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除 ${account.displayTitle}",
                                    tint = secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 余额与用量数据详情
        if (syncStatus == QuotaSyncStatus.NOT_CONFIGURED) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isCompact) 1.dp else 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "未绑定 API Key · 点击呼出配置",
                    color = secondaryText,
                    fontSize = if (isCompact) 10.sp else 11.sp
                )
                Text(
                    text = "直连官方余额",
                    color = mutedText,
                    fontSize = if (isCompact) 8.5.sp else 9.sp
                )
            }
        } else if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isCompact) 1.dp else 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "API Key 已失效 · 点击重新授权",
                    color = if (isNightMode) TechRose else InkWarningRed,
                    fontSize = if (isCompact) 10.sp else 11.sp
                )
                Text(
                    text = "重新授权",
                    color = if (isNightMode) TechCyan else InkProgress,
                    fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val balanceDisplay = snapshot?.formattedBalanceText ?: "暂无余额数据"
                Text(
                    text = balanceDisplay,
                    color = primaryText,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                val updateTime = snapshot?.formattedSuccessTime ?: snapshot?.formattedQueriedAt ?: "刚刚"
                val timeSuffix = if (syncStatus == QuotaSyncStatus.CACHED_STALE || syncStatus == QuotaSyncStatus.CACHE_EXPIRED) " (缓存)" else ""
                Text(
                    text = "更新: $updateTime$timeSuffix",
                    color = mutedText,
                    fontSize = if (isCompact) 8.5.sp else 9.sp
                )
            }
        }
    }
}

/**
 * Gemini API 专属大框（直连 Google 官方接口展示实时连接状态与模型验证，支持多账号与自定义别名）
 */
@Composable
fun GoogleAntigravityUnifiedSubscriptionBox(
    accounts: List<CodexAccount>,
    snapshots: Map<String, CodexUsageSnapshot>,
    loadingMap: Map<String, Boolean>,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    areControlsVisible: Boolean = true,
    isCompact: Boolean = false,
    isMockMode: Boolean = false,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: (CodexAccount) -> Unit = {},
    onRefreshAccount: (String) -> Unit = {},
    onDeleteAccount: (String) -> Unit = {}
) {
    val cardBg = if (isNightMode) DarkSurface else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 9.dp else 12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 9.dp else 12.dp))
            .padding(horizontal = if (isCompact) 9.dp else 13.dp, vertical = if (isCompact) 4.dp else 6.5.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 4.5.dp)) {
            // 大框头部：官方 Google 4 色 Logo + Gemini API 名称 + 账号数状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModelProviderLogo(
                        provider = AiModelProvider.ANTIGRAVITY,
                        size = if (isCompact) 22.dp else 28.dp,
                        isNightMode = isNightMode
                    )
                    Text(
                        text = "Gemini API",
                        color = primaryText,
                        fontSize = if (isCompact) 12.5.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isMockMode) {
                        MockBadge(isNightMode = isNightMode)
                    }
                }

                val hasConfigured = accounts.any { it.credentialStatus == CredentialStatus.VALID }
                val statusColor = if (isNightMode) {
                    if (hasConfigured) TechEmerald else TextTertiary
                } else {
                    if (hasConfigured) InkStatusGreen else InkTextTertiary
                }
                val statusBg = if (isNightMode) statusColor.copy(alpha = 0.12f) else if (hasConfigured) InkStatusTagBg else InkSurfaceElevated
                val statusTitle = when {
                    accounts.size > 1 -> "${accounts.size}个账号"
                    hasConfigured -> "正常"
                    else -> "未配置"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(statusBg)
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = statusTitle,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 框内遍历展示各个 Gemini API 账号
            accounts.forEachIndexed { index, acc ->
                if (index > 0) {
                    HorizontalDivider(
                        color = if (isNightMode) DarkBorder.copy(alpha = 0.5f) else InkBorderSubtle,
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(vertical = 1.5.dp)
                    )
                }
                SingleAntigravityAccountItem(
                    account = acc,
                    snapshot = snapshots[acc.id],
                    isLoading = loadingMap[acc.id] == true,
                    isNightMode = isNightMode,
                    isCompact = isCompact,
                    showDelete = accounts.size > 1,
                    areControlsVisible = areControlsVisible,
                    refreshIntervalMinutes = refreshIntervalMinutes,
                    onAccountClick = { onAccountClick(acc) },
                    onRefreshClick = { onRefreshAccount(acc.id) },
                    onDeleteClick = { onDeleteAccount(acc.id) }
                )
            }
        }
    }
}

/**
 * 单个 Gemini API 账号行（展示连接状态与模型接口验证时间）
 */
@Composable
private fun SingleAntigravityAccountItem(
    account: CodexAccount,
    snapshot: CodexUsageSnapshot?,
    isLoading: Boolean,
    isNightMode: Boolean,
    isCompact: Boolean,
    showDelete: Boolean,
    areControlsVisible: Boolean,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary

    val syncStatus = snapshot?.computeStatus(
        isConfigured = account.isConfigured,
        isRefreshing = isLoading,
        refreshIntervalMinutes = refreshIntervalMinutes
    ) ?: if (account.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

    val remainingCooldown = snapshot?.getRemainingRetrySeconds() ?: 0L
    val isCoolingDown = remainingCooldown > 0L

    val isDemoMock = snapshot?.isMock == true && snapshot.errorMessage == "暂无演示数据"
    val (statusText, statusColor, statusBg) = if (isDemoMock) {
        Triple(
            "暂无演示数据",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
    } else when (syncStatus) {
        QuotaSyncStatus.NOT_CONFIGURED -> Triple(
            "未配置",
            mutedText,
            if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
        )
        QuotaSyncStatus.AWAITING_SYNC -> Triple(
            if (snapshot?.errorMessage != null) "待同步 · 异常" else "待同步",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.SYNCING -> Triple(
            "同步中",
            if (isNightMode) TechCyan else InkProgress,
            (if (isNightMode) TechCyan else InkProgress).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.UPDATED -> Triple(
            "连接正常",
            if (isNightMode) TechEmerald else InkStatusGreen,
            if (isNightMode) TechEmerald.copy(alpha = 0.12f) else InkStatusTagBg
        )
        QuotaSyncStatus.CACHED_STALE -> Triple(
            "缓存 · ${snapshot?.errorMessage?.take(6) ?: "异常"}",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.CACHE_EXPIRED -> Triple(
            "缓存已过期",
            if (isNightMode) TechRose else InkWarningRed,
            (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.AUTH_EXPIRED -> Triple(
            "授权失效",
            if (isNightMode) TechRose else InkWarningRed,
            (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.SERVICE_UNAVAILABLE -> Triple(
            if (isCoolingDown) "${remainingCooldown}s后重试" else "暂不可用",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
        QuotaSyncStatus.DATA_FORMAT_ERROR -> Triple(
            "数据格式异常",
            if (isNightMode) TechAmber else InkWarningAmber,
            (if (isNightMode) TechAmber else InkWarningAmber).copy(alpha = 0.12f)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spinAg")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngleAg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAccountClick() },
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.5.dp else 3.dp)
    ) {
        // 账号标识行：圆点 + 自定义账号名称 + API 徽标 + 状态徽标 + 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .background(
                            if (isNightMode) {
                                if (syncStatus == QuotaSyncStatus.UPDATED) TechCyan else if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) TechRose else TechAmber
                            } else {
                                if (syncStatus == QuotaSyncStatus.UPDATED) InkStatusGreen else if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) InkWarningRed else InkWarningAmber
                            },
                            CircleShape
                        )
                )
                if (account.isPinnedMain) {
                    Text(
                        text = "★",
                        color = if (isNightMode) TechAmber else InkWarningAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = account.displayTitle,
                    color = primaryText,
                    fontSize = if (isCompact) 10.5.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated)
                        .padding(horizontal = 3.dp, vertical = 0.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "API",
                        color = secondaryText,
                        fontSize = if (isCompact) 7.5.sp else 8.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedVisibility(
                    visible = areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshClick,
                            enabled = !isLoading && !isCoolingDown,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新 ${account.displayTitle} 额度",
                                tint = if (isLoading) (if (isNightMode) TechCyan else InkProgress)
                                       else if (isCoolingDown) secondaryText.copy(alpha = 0.35f)
                                       else secondaryText,
                                modifier = Modifier
                                    .size(13.dp)
                                    .rotate(if (isLoading) spinAngle else 0f)
                            )
                        }
                        if (showDelete) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除 ${account.displayTitle}",
                                    tint = secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gemini API 连接状态描述行（真实反映上游模型列表接口调用结果）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when {
                syncStatus == QuotaSyncStatus.NOT_CONFIGURED -> {
                    Text(
                        text = "未配置 API Key · 点击呼出配置",
                        color = secondaryText,
                        fontSize = if (isCompact) 9.sp else 10.sp
                    )
                    Text(
                        text = "Gemini 连接",
                        color = mutedText,
                        fontSize = if (isCompact) 7.5.sp else 8.5.sp
                    )
                }
                syncStatus == QuotaSyncStatus.AUTH_EXPIRED -> {
                    Text(
                        text = "API Key 鉴权失败 · 点击重新授权",
                        color = if (isNightMode) TechRose else InkWarningRed,
                        fontSize = if (isCompact) 9.sp else 10.sp
                    )
                    Text(
                        text = "重新授权",
                        color = if (isNightMode) TechCyan else InkProgress,
                        fontSize = if (isCompact) 8.sp else 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                else -> {
                    Text(
                        text = "官方接口: 连接正常",
                        color = secondaryText,
                        fontSize = if (isCompact) 9.sp else 10.sp
                    )
                    val verifiedTime = snapshot?.formattedVerifiedTime ?: snapshot?.formattedQueriedAt
                    val timeSuffix = if (syncStatus == QuotaSyncStatus.CACHED_STALE || syncStatus == QuotaSyncStatus.CACHE_EXPIRED) " (缓存)" else ""
                    Text(
                        text = if (verifiedTime != null) "验证于 $verifiedTime$timeSuffix" else "刚刚验证",
                        color = mutedText,
                        fontSize = if (isCompact) 7.5.sp else 8.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MockBadge(
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isNightMode) TechAmber.copy(alpha = 0.2f) else InkWarningAmber.copy(alpha = 0.15f))
            .border(0.8.dp, if (isNightMode) TechAmber else InkWarningAmber, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = "演示数据",
            color = if (isNightMode) TechAmber else InkWarningAmber,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
