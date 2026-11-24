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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.remember
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
import com.chronodesk.app.data.model.CodexUsageWindow
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
import com.chronodesk.app.ui.theme.LocalPureInkMode
import com.chronodesk.app.ui.theme.PureInkBorder
import com.chronodesk.app.ui.theme.PureInkSurfaceElevated
import com.chronodesk.app.ui.theme.PureInkTextSecondary
import com.chronodesk.app.ui.theme.resolveChronoPalette

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
    quotaDisplayMode: String = "NUMBERS_ONLY",
    providerOrder: List<String> = listOf("CHATGPT", "ANTIGRAVITY", "DEEPSEEK"),
    isMockMode: Boolean = false,
    refreshIntervalMinutes: Int = 15,
    enableScroll: Boolean = true,
    onAddAccount: () -> Unit = {},
    onRefreshAccount: (accountId: String) -> Unit = {},
    onDeleteAccount: (accountId: String) -> Unit = {},
    onAccountClick: (account: CodexAccount) -> Unit = {},
    onRevealControls: () -> Unit = {},
    onRefreshAll: () -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val chatGptAccounts = accounts.filter { it.modelProvider == "CHATGPT" && it.isVisibleOnMain }
        .sortedByDescending { it.isPinnedMain }
    val deepSeekAccounts = accounts.filter { it.modelProvider == "DEEPSEEK" && it.isVisibleOnMain }
        .sortedByDescending { it.isPinnedMain }
    val antigravityAccounts = accounts.filter { it.modelProvider == "ANTIGRAVITY" && it.isVisibleOnMain }
        .sortedByDescending { it.isPinnedMain }

    val visibleProviderCount = (if (chatGptAccounts.isNotEmpty()) 1 else 0) +
        (if (deepSeekAccounts.isNotEmpty()) 1 else 0) +
        (if (antigravityAccounts.isNotEmpty()) 1 else 0)
    val isNumbersOnly = !quotaDisplayMode.equals("PROGRESS_BAR", ignoreCase = true)
    val isRelaxedVertical = visibleProviderCount <= 2

    val spacing = when (cardDensity) {
        "COMPACT" -> if (isCompact) 3.5.dp else 6.dp
        "SPACIOUS" -> if (isCompact) 8.5.dp else 12.dp
        else -> if (visibleProviderCount >= 3) (if (isCompact) (if (isNumbersOnly) 4.dp else 3.dp) else 5.dp) else (if (isCompact) 7.5.dp else 10.dp)
    }

    val normalizedOrder = remember(providerOrder) {
        val canonical = listOf("CHATGPT", "ANTIGRAVITY", "DEEPSEEK")
        val parsed = providerOrder.map { it.trim().uppercase(java.util.Locale.US) }
            .filter { it in canonical }
            .distinct()
        parsed + canonical.filterNot { it in parsed }
    }

    Column(
        modifier = if (enableScroll) modifier.verticalScroll(rememberScrollState()) else modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        normalizedOrder.forEach { providerKey ->
            when (providerKey) {
                "CHATGPT" -> if (chatGptAccounts.isNotEmpty()) {
                    ChatGptUnifiedSubscriptionBox(
                        accounts = chatGptAccounts,
                        snapshots = snapshots,
                        loadingMap = loadingMap,
                        isNightMode = isNightMode,
                        areControlsVisible = areControlsVisible,
                        isCompact = isCompact,
                        isNumbersOnly = isNumbersOnly,
                        isRelaxedVertical = isRelaxedVertical,
                        isMockMode = isMockMode,
                        refreshIntervalMinutes = refreshIntervalMinutes,
                        onAccountClick = onAccountClick,
                        onRevealControls = onRevealControls,
                        onRefreshAccount = onRefreshAccount,
                        onDeleteAccount = onDeleteAccount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "ANTIGRAVITY" -> if (antigravityAccounts.isNotEmpty()) {
                    GoogleAntigravityUnifiedSubscriptionBox(
                        accounts = antigravityAccounts,
                        snapshots = snapshots,
                        loadingMap = loadingMap,
                        isNightMode = isNightMode,
                        areControlsVisible = areControlsVisible,
                        isCompact = isCompact,
                        isNumbersOnly = isNumbersOnly,
                        isRelaxedVertical = isRelaxedVertical,
                        isMockMode = isMockMode,
                        refreshIntervalMinutes = refreshIntervalMinutes,
                        onAccountClick = onAccountClick,
                        onRevealControls = onRevealControls,
                        onRefreshAccount = onRefreshAccount,
                        onDeleteAccount = onDeleteAccount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "DEEPSEEK" -> if (deepSeekAccounts.isNotEmpty()) {
                    DeepSeekUnifiedSubscriptionBox(
                        accounts = deepSeekAccounts,
                        snapshots = snapshots,
                        loadingMap = loadingMap,
                        isNightMode = isNightMode,
                        areControlsVisible = areControlsVisible,
                        isCompact = isCompact,
                        isRelaxedVertical = isRelaxedVertical,
                        isMockMode = isMockMode,
                        refreshIntervalMinutes = refreshIntervalMinutes,
                        onAccountClick = onAccountClick,
                        onRevealControls = onRevealControls,
                        onRefreshAccount = onRefreshAccount,
                        onDeleteAccount = onDeleteAccount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)

    // 4. 空状态与账号指引
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.surface)
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp))
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
                        tint = theme.accent,
                        modifier = Modifier.size(if (isCompact) 26.dp else 32.dp)
                    )
                    Text(
                        text = "欢迎使用 ChronoDesk",
                        color = theme.primaryText,
                        fontSize = if (isCompact) 13.5.sp else 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "点击配置你的第一个 AI 账号",
                        color = theme.accent,
                        fontSize = if (isCompact) 11.sp else 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "支持 ChatGPT (Codex) / DeepSeek / Antigravity",
                        color = theme.mutedText,
                        fontSize = if (isCompact) 9.5.sp else 10.5.sp
                    )
                }
            }
        } else if (chatGptAccounts.isEmpty() && deepSeekAccounts.isEmpty() && antigravityAccounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.surface)
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp))
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
                        color = theme.primaryText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "账号已配置在第二屏显示或已隐藏\n点击或滑动至第二屏查看，或在账号管理中开启主屏显示",
                        color = theme.secondaryText,
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
 * 统一三平台订阅框顶栏：完全一致的 Logo 尺寸、标题层级、状态微标与快捷刷新
 */
@Composable
private fun UnifiedProviderCardHeader(
    provider: AiModelProvider,
    title: String,
    isNightMode: Boolean,
    isCompact: Boolean,
    isMockMode: Boolean,
    planPill: String? = null,
    trailingText: String? = null,
    trailingColor: androidx.compose.ui.graphics.Color? = null,
    showRefresh: Boolean = false,
    isRefreshing: Boolean = false,
    onRefreshClick: (() -> Unit)? = null
) {
    val theme = resolveChronoPalette(isNightMode, !isNightMode && LocalPureInkMode.current)
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText

    val infiniteTransition = rememberInfiniteTransition(label = "spinHeader")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinHeaderAngle"
    )

    val compactWithPill = isCompact && !planPill.isNullOrBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compactWithPill) 4.dp else if (isCompact) 5.5.dp else 7.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            ModelProviderLogo(
                provider = provider,
                size = if (compactWithPill) 16.dp else if (isCompact) 18.dp else 21.dp,
                isNightMode = isNightMode
            )
            Text(
                text = title,
                color = primaryText,
                fontSize = if (compactWithPill) 11.2.sp else if (isCompact) 12.5.sp else 14.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            if (!planPill.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.surfaceElevated)
                        .padding(horizontal = if (isCompact) 3.5.dp else 4.5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = planPill,
                        color = secondaryText,
                        fontSize = if (isCompact) 7.5.sp else 9.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            if (isMockMode) {
                MockBadge(isNightMode = isNightMode)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!trailingText.isNullOrBlank()) {
                Text(
                    text = trailingText,
                    color = trailingColor ?: mutedText,
                    fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (showRefresh && onRefreshClick != null) {
                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isRefreshing,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新 $title",
                        tint = if (isRefreshing) theme.accent else secondaryText,
                        modifier = Modifier
                            .size(11.dp)
                            .rotate(if (isRefreshing) spinAngle else 0f)
                    )
                }
            }
        }
    }
}

private data class DualWindowRowData(
    val id: String,
    val title: String,
    val subtitleHint: String? = null,
    val badge: String? = null,
    val resetCredits: Int? = null,
    val isPinned: Boolean = false,
    val syncStatus: QuotaSyncStatus,
    val isLoading: Boolean,
    val isCoolingDown: Boolean,
    val remainingCooldownSec: Long,
    val isMockEmpty: Boolean = false,
    val cachedTimeSuffix: String = "",
    val fiveHourWindow: CodexUsageWindow?,
    val weeklyWindow: CodexUsageWindow?,
    val statusText: String,
    val statusColor: androidx.compose.ui.graphics.Color,
    val statusBg: androidx.compose.ui.graphics.Color
)

private fun buildDualWindowRowData(
    id: String,
    title: String,
    subtitleHint: String?,
    badge: String?,
    resetCredits: Int?,
    isPinned: Boolean,
    isConfigured: Boolean,
    snapshot: CodexUsageSnapshot?,
    fiveHourWindow: CodexUsageWindow?,
    weeklyWindow: CodexUsageWindow?,
    isLoading: Boolean,
    isNightMode: Boolean,
    isPureInk: Boolean = false,
    isCompact: Boolean,
    refreshIntervalMinutes: Int
): DualWindowRowData {
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val mutedText = theme.mutedText
    val syncStatus = snapshot?.computeStatus(
        isConfigured = isConfigured,
        isRefreshing = isLoading,
        refreshIntervalMinutes = refreshIntervalMinutes
    ) ?: if (isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

    val remainingCooldown = snapshot?.getRemainingRetrySeconds() ?: 0L
    val isCoolingDown = remainingCooldown > 0L
    val isMockEmpty = snapshot?.isMock == true && snapshot.errorMessage == "暂无演示数据"
    val isPrimaryExhausted = (fiveHourWindow?.remainingPercent != null && fiveHourWindow.remainingPercent <= 0.0) || snapshot?.limitReached == true
    val isSecondaryExhausted = weeklyWindow?.remainingPercent != null && weeklyWindow.remainingPercent <= 0.0
    val isWarning = !isPrimaryExhausted && !isSecondaryExhausted && (
        (fiveHourWindow?.remainingPercent != null && fiveHourWindow.remainingPercent in 0.01..15.0) ||
            (weeklyWindow?.remainingPercent != null && weeklyWindow.remainingPercent in 0.01..15.0)
        )

    val (statusText, statusColor, statusBg) = if (isMockEmpty) {
        val c = theme.warning
        Triple("暂无演示", c, c.copy(alpha = 0.14f))
    } else when (syncStatus) {
        QuotaSyncStatus.NOT_CONFIGURED -> Triple(
            "未配置",
            mutedText,
            theme.surfaceElevated
        )
        QuotaSyncStatus.AWAITING_SYNC -> Triple(
            if (snapshot?.errorMessage != null) "待同步·异常" else "待同步",
            theme.warning,
            theme.warning.copy(alpha = 0.14f)
        )
        QuotaSyncStatus.SYNCING -> Triple(
            "同步中",
            theme.accent,
            theme.accent.copy(alpha = 0.14f)
        )
        QuotaSyncStatus.UPDATED -> {
            val label = when {
                isSecondaryExhausted -> "周额耗尽"
                isPrimaryExhausted -> "5h限额"
                isWarning -> "偏低"
                else -> "正常"
            }
            val color = when {
                isSecondaryExhausted -> theme.danger
                isPrimaryExhausted || isWarning -> theme.warning
                else -> theme.positive
            }
            Triple(label, color, color.copy(alpha = 0.14f))
        }
        QuotaSyncStatus.CACHED_STALE -> Triple(
            if (snapshot?.lastErrorType == SyncErrorType.ACCESS_DENIED) "访问受限" else "缓存·${snapshot?.errorMessage?.take(if (isCompact) 4 else 6) ?: "异常"}",
            theme.warning,
            theme.warning.copy(alpha = 0.14f)
        )
        QuotaSyncStatus.CACHE_EXPIRED -> Triple(
            "缓存过期",
            theme.danger,
            theme.danger.copy(alpha = 0.14f)
        )
        QuotaSyncStatus.AUTH_EXPIRED -> Triple(
            "授权失效",
            theme.danger,
            theme.danger.copy(alpha = 0.14f)
        )
        QuotaSyncStatus.SERVICE_UNAVAILABLE -> Triple(
            if (isCoolingDown) "${remainingCooldown}s" else "暂不可用",
            theme.warning,
            theme.warning.copy(alpha = 0.14f)
        )
        QuotaSyncStatus.DATA_FORMAT_ERROR -> Triple(
            "格式异常",
            theme.warning,
            theme.warning.copy(alpha = 0.14f)
        )
    }

    val cachedSuffix = if (syncStatus == QuotaSyncStatus.CACHED_STALE || syncStatus == QuotaSyncStatus.CACHE_EXPIRED) {
        val ts = snapshot?.formattedSuccessTime ?: snapshot?.formattedQueriedAt
        if (ts != null) "($ts)" else ""
    } else ""

    return DualWindowRowData(
        id = id,
        title = title,
        subtitleHint = subtitleHint,
        badge = badge,
        resetCredits = resetCredits,
        isPinned = isPinned,
        syncStatus = syncStatus,
        isLoading = isLoading,
        isCoolingDown = isCoolingDown,
        remainingCooldownSec = remainingCooldown,
        isMockEmpty = isMockEmpty,
        cachedTimeSuffix = cachedSuffix,
        fiveHourWindow = fiveHourWindow,
        weeklyWindow = weeklyWindow,
        statusText = statusText,
        statusColor = statusColor,
        statusBg = statusBg
    )
}

/**
 * OpenAI / ChatGPT 统一大框（支持纯数字仪表盘与进度条两种统一形态）
 */
@Composable
fun ChatGptUnifiedSubscriptionBox(
    accounts: List<CodexAccount>,
    snapshots: Map<String, CodexUsageSnapshot>,
    loadingMap: Map<String, Boolean>,
    isNightMode: Boolean,
    areControlsVisible: Boolean,
    isCompact: Boolean,
    isNumbersOnly: Boolean = true,
    isRelaxedVertical: Boolean = false,
    isMockMode: Boolean = false,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: (CodexAccount) -> Unit,
    onRevealControls: () -> Unit = {},
    onRefreshAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = theme.surface
    val cardBorder = theme.border
    val cornerRadius = if (isCompact) 11.dp else 14.dp
    val outerVPad = when {
        !isCompact && !isRelaxedVertical -> 6.5.dp
        !isCompact -> 9.dp
        isRelaxedVertical -> 7.dp
        !isNumbersOnly -> 4.dp
        else -> 5.dp
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(cornerRadius))
            .padding(horizontal = if (isCompact) 9.dp else 13.dp, vertical = outerVPad)
    ) {
        val colGap = when {
            !isCompact && !isRelaxedVertical -> 4.dp
            !isCompact -> 6.dp
            isRelaxedVertical -> 5.dp
            !isNumbersOnly -> 2.8.dp
            else -> 3.5.dp
        }
        Column(verticalArrangement = Arrangement.spacedBy(colGap)) {
            val primarySnap = accounts.firstOrNull()?.let { snapshots[it.id] }
            val maxCredits = accounts.mapNotNull { snapshots[it.id]?.resetCreditsCount }.firstOrNull { it > 0 }
            val trailingSummary = buildString {
                if (maxCredits != null && maxCredits > 0) append("重置:${maxCredits}次 · ")
                append(if (accounts.size > 1) "${accounts.size} 账号" else (primarySnap?.formattedSuccessTime?.let { "更新 $it" } ?: "双窗额度"))
            }

            UnifiedProviderCardHeader(
                provider = AiModelProvider.CHATGPT,
                title = "ChatGPT",
                isNightMode = isNightMode,
                isCompact = isCompact,
                isMockMode = isMockMode,
                planPill = if (accounts.size == 1) primarySnap?.planName?.takeIf { it.isNotBlank() } else null,
                trailingText = trailingSummary
            )

            accounts.forEach { acc ->
                val snap = snapshots[acc.id]
                val rowData = buildDualWindowRowData(
                    id = acc.id,
                    title = acc.displayTitle,
                    subtitleHint = null,
                    badge = snap?.planName?.ifBlank { "Plus" } ?: "Plus",
                    resetCredits = snap?.resetCreditsCount,
                    isPinned = acc.isPinnedMain,
                    isConfigured = acc.isConfigured,
                    snapshot = snap,
                    fiveHourWindow = snap?.primaryWindow,
                    weeklyWindow = snap?.secondaryWindow,
                    isLoading = loadingMap[acc.id] == true,
                    isNightMode = isNightMode,
                    isPureInk = isPureInk,
                    isCompact = isCompact,
                    refreshIntervalMinutes = refreshIntervalMinutes
                )
                UnifiedDualQuotaRow(
                    row = rowData,
                    isNightMode = isNightMode,
                    isCompact = isCompact,
                    isNumbersOnly = isNumbersOnly,
                    isRelaxedVertical = isRelaxedVertical,
                    areControlsVisible = areControlsVisible,
                    showDelete = accounts.size > 1 || acc.isConfigured,
                    onRowClick = { onAccountClick(acc) },
                    onRevealControls = onRevealControls,
                    onRefreshClick = { onRefreshAccount(acc.id) },
                    onDeleteClick = { onDeleteAccount(acc.id) }
                )
            }
        }
    }
}

private inline fun handleAccountCardTap(
    syncStatus: QuotaSyncStatus,
    areControlsVisible: Boolean,
    onAccountClick: () -> Unit,
    onRevealControls: () -> Unit
) {
    if (areControlsVisible || syncStatus == QuotaSyncStatus.NOT_CONFIGURED || syncStatus == QuotaSyncStatus.AUTH_EXPIRED) {
        onAccountClick()
    } else {
        onRevealControls()
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
    val theme = resolveChronoPalette(isNightMode, !isNightMode && LocalPureInkMode.current)
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText

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

@Composable
private fun UnifiedDualQuotaRow(
    row: DualWindowRowData,
    isNightMode: Boolean,
    isCompact: Boolean,
    isNumbersOnly: Boolean,
    isRelaxedVertical: Boolean,
    areControlsVisible: Boolean,
    showDelete: Boolean,
    onRowClick: () -> Unit,
    onRevealControls: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (isNumbersOnly) {
        PureNumberDualQuotaRow(
            row = row,
            isNightMode = isNightMode,
            isCompact = isCompact,
            isRelaxedVertical = isRelaxedVertical,
            areControlsVisible = areControlsVisible,
            showDelete = showDelete,
            onRowClick = onRowClick,
            onRevealControls = onRevealControls,
            onRefreshClick = onRefreshClick,
            onDeleteClick = onDeleteClick
        )
    } else {
        ProgressBarDualQuotaRow(
            row = row,
            isNightMode = isNightMode,
            isCompact = isCompact,
            isRelaxedVertical = isRelaxedVertical,
            areControlsVisible = areControlsVisible,
            showDelete = showDelete,
            onRowClick = onRowClick,
            onRevealControls = onRevealControls,
            onRefreshClick = onRefreshClick,
            onDeleteClick = onDeleteClick
        )
    }
}

/**
 * 纯数字桌面仪表盘行（重点打磨样式）：
 * 微浮底圆角条 + 3 列精密对齐网格（左：身份与套餐 ｜ 中：5h 剩余% 与时刻 ｜ 右：周剩余% 与倒计时）
 */
@Composable
private fun PureNumberDualQuotaRow(
    row: DualWindowRowData,
    isNightMode: Boolean,
    isCompact: Boolean,
    isRelaxedVertical: Boolean,
    areControlsVisible: Boolean,
    showDelete: Boolean,
    onRowClick: () -> Unit,
    onRevealControls: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText
    val rowBg = theme.surfaceElevated.copy(alpha = if (isNightMode) 0.62f else 0.72f)
    val dividerColor = theme.border.copy(alpha = 0.7f)

    val nowUnix = System.currentTimeMillis() / 1000L
    val hPad = if (isCompact) 5.5.dp else 10.dp
    val vPad = when {
        !isCompact && !isRelaxedVertical -> 4.8.dp
        !isCompact -> 6.5.dp
        isRelaxedVertical -> 6.5.dp
        else -> 4.5.dp
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spinNum")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinNumAngle"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                handleAccountCardTap(row.syncStatus, areControlsVisible, onRowClick, onRevealControls)
            }
            .padding(horizontal = hPad, vertical = vPad)
    ) {
        val isNarrow = maxWidth < 238.dp
        val isAuthOrUnconf = row.isMockEmpty ||
            row.syncStatus == QuotaSyncStatus.AUTH_EXPIRED ||
            row.syncStatus == QuotaSyncStatus.NOT_CONFIGURED ||
            (row.fiveHourWindow == null && row.weeklyWindow == null)

        if (isAuthOrUnconf) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.5.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(modifier = Modifier.size(4.5.dp).background(row.statusColor, CircleShape))
                    if (row.isPinned) {
                        Text("★", color = if (isPureInk) theme.primaryText else theme.warning, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = row.title,
                        color = primaryText,
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val promptText = when {
                        row.isMockEmpty -> "暂无演示数据"
                        row.syncStatus == QuotaSyncStatus.AUTH_EXPIRED -> "凭据失效 · 重连 ›"
                        row.syncStatus == QuotaSyncStatus.NOT_CONFIGURED -> "未配置 · 授权 ›"
                        row.syncStatus == QuotaSyncStatus.SYNCING -> "同步中…"
                        else -> "${row.statusText} · 点击刷新"
                    }
                    Text(
                        text = promptText,
                        color = if (row.syncStatus == QuotaSyncStatus.NOT_CONFIGURED) theme.accent else row.statusColor,
                        fontSize = if (isCompact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (areControlsVisible) {
                        IconButton(
                            onClick = onRefreshClick,
                            enabled = !row.isLoading && !row.isCoolingDown,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新 ${row.title}",
                                tint = secondaryText,
                                modifier = Modifier.size(10.dp).rotate(if (row.isLoading) spinAngle else 0f)
                            )
                        }
                    }
                }
            }
            return@BoxWithConstraints
        }

        val pWin = row.fiveHourWindow
        val sWin = row.weeklyWindow
        val pColor = when {
            pWin?.remainingPercent != null && pWin.remainingPercent <= 0.0 -> theme.danger
            pWin?.remainingPercent != null && pWin.remainingPercent <= 15.0 -> theme.warning
            else -> primaryText
        }
        val sColor = when {
            sWin?.remainingPercent != null && sWin.remainingPercent <= 0.0 -> theme.danger
            sWin?.remainingPercent != null && sWin.remainingPercent <= 15.0 -> theme.warning
            else -> primaryText
        }

        val heroNumberSize = when {
            isNarrow -> 13.sp
            isCompact -> if (isRelaxedVertical) 15.5.sp else 14.sp
            isRelaxedVertical -> 16.5.sp
            else -> 15.5.sp
        }
        val titleSize = if (isNarrow) 8.8.sp else if (isCompact) 10.5.sp else if (isRelaxedVertical) 12.5.sp else 11.8.sp
        val captionSize = if (isNarrow) 7.5.sp else if (isCompact) 8.sp else 9.sp

        val pResetText = when {
            pWin == null -> "—"
            pWin.resetAtUnix == null -> "未知"
            nowUnix >= pWin.resetAtUnix -> "待更新"
            else -> pWin.formattedResetTime(nowUnix)
        }
        val sCountdownText = when {
            sWin == null -> "—"
            else -> if (isCompact) sWin.formattedCompactCountdown(nowUnix) else sWin.formattedCountdown(nowUnix)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 2.5.dp else if (isCompact) 4.dp else 7.dp)
        ) {
            // 左区：状态点 + 名称 + 套餐/重置次数或轨道标记
            Column(
                modifier = Modifier.weight(if (isNarrow) 1.25f else if (isCompact) 1.06f else 1.12f),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 2.dp else 3.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.5.dp)
                            .background(row.statusColor, CircleShape)
                    )
                    if (row.isPinned) {
                        Text(
                            text = "★",
                            color = if (isPureInk) theme.primaryText else theme.warning,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = row.title,
                        color = primaryText,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val subText = buildString {
                    row.badge?.takeIf { it.isNotBlank() }?.let { append(it) }
                    row.subtitleHint?.takeIf { it.isNotBlank() }?.let {
                        if (isNotEmpty()) append("·")
                        append(it)
                    }
                    val credits = row.resetCredits ?: 0
                    if (credits > 0 && !isNarrow) {
                        if (isNotEmpty()) append(" · ")
                        append("重置$credits")
                    }
                    if (row.cachedTimeSuffix.isNotEmpty()) {
                        if (isNotEmpty()) append(" ")
                        append(row.cachedTimeSuffix)
                    }
                }
                if (subText.isNotEmpty()) {
                    Text(
                        text = subText,
                        color = mutedText,
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(0.8.dp)
                    .height(if (isCompact) 22.dp else 26.dp)
                    .background(dividerColor)
            )

            // 中区：5 小时额度（标签 + 重置时刻 / 等宽大数字%）
            Column(
                modifier = Modifier.weight(if (isNarrow) 0.88f else 0.95f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "5h",
                        color = secondaryText,
                        fontSize = captionSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = pResetText,
                        color = mutedText,
                        fontSize = captionSize,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Text(
                    text = pWin?.formattedRemaining() ?: "—",
                    color = pColor,
                    fontSize = heroNumberSize,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                modifier = Modifier
                    .width(0.8.dp)
                    .height(if (isCompact) 22.dp else 26.dp)
                    .background(dividerColor)
            )

            // 右区：周额度（标签 + 倒计时 / 等宽大数字%）
            Column(
                modifier = Modifier.weight(if (isNarrow) 0.94f else 1.00f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "周",
                        color = secondaryText,
                        fontSize = captionSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = sCountdownText,
                        color = mutedText,
                        fontSize = captionSize,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Text(
                    text = sWin?.formattedRemaining() ?: "—",
                    color = sColor,
                    fontSize = heroNumberSize,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }

            AnimatedVisibility(visible = areControlsVisible) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !row.isLoading && !row.isCoolingDown,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新 ${row.title}",
                            tint = if (row.isLoading) theme.accent else secondaryText,
                            modifier = Modifier.size(11.dp).rotate(if (row.isLoading) spinAngle else 0f)
                        )
                    }
                    if (showDelete) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "删除 ${row.title}",
                                tint = secondaryText.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 统一双额度进度条行（ChatGPT 与 Antigravity 共用，保证双平台视觉完全一致）
 */
@Composable
private fun ProgressBarDualQuotaRow(
    row: DualWindowRowData,
    isNightMode: Boolean,
    isCompact: Boolean,
    isRelaxedVertical: Boolean,
    areControlsVisible: Boolean,
    showDelete: Boolean,
    onRowClick: () -> Unit,
    onRevealControls: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText
    val trackColor = theme.progressTrack
    val rowBg = theme.surfaceElevated.copy(alpha = if (isNightMode) 0.55f else 0.65f)

    val nowUnix = System.currentTimeMillis() / 1000
    val primaryWindow = row.fiveHourWindow
    val secondaryWindow = row.weeklyWindow

    val isPrimaryExhausted = (primaryWindow?.remainingPercent != null && primaryWindow.remainingPercent <= 0.0)
    val isSecondaryExhausted = secondaryWindow?.remainingPercent != null && secondaryWindow.remainingPercent <= 0.0

    val primaryProgressColor = when {
        isPrimaryExhausted -> theme.danger
        primaryWindow?.remainingPercent != null && primaryWindow.remainingPercent <= 15.0 -> theme.warning
        else -> theme.progressActive
    }

    val secondaryProgressColor = when {
        isSecondaryExhausted -> theme.danger
        secondaryWindow?.remainingPercent != null && secondaryWindow.remainingPercent <= 15.0 -> theme.warning
        else -> theme.progressActive
    }

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
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                handleAccountCardTap(row.syncStatus, areControlsVisible, onRowClick, onRevealControls)
            }
            .padding(horizontal = if (isCompact) 7.dp else 9.dp, vertical = if (isCompact) (if (isRelaxedVertical) 4.dp else 2.8.dp) else 6.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) (if (isRelaxedVertical) 2.dp else 1.5.dp) else 3.dp)
    ) {
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
                        .background(row.statusColor, CircleShape)
                )
                if (row.isPinned) {
                    Text(
                        text = "★",
                        color = if (isPureInk) theme.primaryText else theme.warning,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = row.title,
                    color = primaryText,
                    fontSize = if (isCompact) 10.5.sp else 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                row.badge?.takeIf { it.isNotBlank() }?.let { b ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(theme.surface)
                            .padding(horizontal = 3.5.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = b,
                            color = secondaryText,
                            fontSize = if (isCompact) 7.5.sp else 8.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                val resetCredits = row.resetCredits ?: 0
                if (resetCredits > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(theme.accent.copy(alpha = 0.14f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "重置:${resetCredits}次",
                            color = theme.accent,
                            fontSize = 7.5.sp,
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
                        .background(row.statusBg)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = row.statusText,
                        color = row.statusColor,
                        fontSize = 8.sp,
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
                            enabled = !row.isLoading && !row.isCoolingDown,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新 ${row.title} 额度",
                                tint = if (row.isLoading) theme.accent
                                else if (row.isCoolingDown) secondaryText.copy(alpha = 0.35f)
                                else secondaryText,
                                modifier = Modifier
                                    .size(10.5.dp)
                                    .rotate(if (row.isLoading) spinAngle else 0f)
                            )
                        }
                        if (showDelete) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除 ${row.title}",
                                    tint = secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.size(10.5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (row.isMockEmpty) {
            Text(
                text = "当前暂无该平台演示数据",
                color = secondaryText,
                fontSize = if (isCompact) 9.sp else 10.sp
            )
        } else if (row.syncStatus == QuotaSyncStatus.AUTH_EXPIRED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "登录凭据已失效 · 点击重新授权",
                    color = theme.danger,
                    fontSize = if (isCompact) 8.5.sp else 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "重新授权 ›",
                    color = theme.accent,
                    fontSize = if (isCompact) 8.sp else 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (row.syncStatus == QuotaSyncStatus.NOT_CONFIGURED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "未配置账号 · 点击进行连接",
                    color = secondaryText,
                    fontSize = if (isCompact) 8.5.sp else 10.sp
                )
                Text(
                    text = "连接 ›",
                    color = theme.accent,
                    fontSize = if (isCompact) 8.sp else 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            val primaryPercentColor = when {
                isPrimaryExhausted -> theme.danger
                primaryWindow?.remainingPercent != null && primaryWindow.remainingPercent <= 15.0 -> theme.warning
                else -> primaryText
            }
            val secondaryPercentColor = when {
                isSecondaryExhausted -> theme.danger
                secondaryWindow?.remainingPercent != null && secondaryWindow.remainingPercent <= 15.0 -> theme.warning
                else -> primaryText
            }

            // 5 小时行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
            ) {
                Text(
                    text = if (isCompact) "5h" else "5小时",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.sp else 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(if (isCompact) 24.dp else 38.dp)
                )
                val pPercent = primaryWindow?.remainingPercent
                LinearProgressIndicator(
                    progress = { if (pPercent != null && pPercent.isFinite()) (pPercent / 100.0).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = if (isCompact) 36.dp else 56.dp)
                        .height(if (isCompact) 4.5.dp else 6.5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = primaryProgressColor,
                    trackColor = trackColor
                )
                val primaryResetDesc = when {
                    primaryWindow == null -> if (row.isLoading) "同步中" else "待同步"
                    primaryWindow.resetAtUnix == null -> "未知"
                    nowUnix >= primaryWindow.resetAtUnix -> "待更新"
                    else -> primaryWindow.formattedResetTime(nowUnix)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 3.dp else 5.dp)
                ) {
                    Text(
                        text = primaryWindow?.formattedRemaining() ?: "—",
                        color = primaryPercentColor,
                        fontSize = if (isCompact) 11.sp else 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = primaryResetDesc,
                        color = mutedText,
                        fontSize = if (isCompact) 8.sp else 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // 周额度行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
            ) {
                Text(
                    text = if (isCompact) "周" else "周额度",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.sp else 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(if (isCompact) 24.dp else 38.dp)
                )
                val sPercent = secondaryWindow?.remainingPercent
                LinearProgressIndicator(
                    progress = { if (sPercent != null && sPercent.isFinite()) (sPercent / 100.0).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = if (isCompact) 36.dp else 56.dp)
                        .height(if (isCompact) 4.5.dp else 6.5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = secondaryProgressColor,
                    trackColor = trackColor
                )
                val secondaryResetDesc = when {
                    secondaryWindow == null -> if (row.isLoading) "同步中" else "待同步"
                    else -> if (isCompact) secondaryWindow.formattedCompactCountdown(nowUnix) else secondaryWindow.formattedCountdown(nowUnix)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 3.dp else 5.dp)
                ) {
                    Text(
                        text = secondaryWindow?.formattedRemaining() ?: "—",
                        color = secondaryPercentColor,
                        fontSize = if (isCompact) 11.sp else 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "$secondaryResetDesc${row.cachedTimeSuffix}",
                        color = mutedText,
                        fontSize = if (isCompact) 8.sp else 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * DeepSeek 精简紧凑卡片（减少主屏占用比例，同时保留真实余额、零余额、多币种与鉴权状态）
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
    isRelaxedVertical: Boolean = false,
    isMockMode: Boolean = false,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: (CodexAccount) -> Unit = {},
    onRevealControls: () -> Unit = {},
    onRefreshAccount: (String) -> Unit = {},
    onDeleteAccount: (String) -> Unit = {}
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = theme.surface
    val cardBorder = theme.border
    val cornerRadius = if (isCompact) 11.dp else 14.dp
    val outerVPad = when {
        !isCompact && !isRelaxedVertical -> 6.dp
        !isCompact -> 8.dp
        isRelaxedVertical -> 6.5.dp
        else -> 4.5.dp
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(cornerRadius))
            .padding(horizontal = if (isCompact) 9.dp else 13.dp, vertical = outerVPad)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) (if (isRelaxedVertical) 4.5.dp else 3.5.dp) else (if (isRelaxedVertical) 5.5.dp else 4.dp))) {
            UnifiedProviderCardHeader(
                provider = AiModelProvider.DEEPSEEK,
                title = "DeepSeek",
                isNightMode = isNightMode,
                isCompact = isCompact,
                isMockMode = isMockMode,
                trailingText = if (accounts.size > 1) "${accounts.size} 账号" else "API 余额"
            )

            if (accounts.size == 1) {
                SingleDeepSeekAccountItem(
                    account = accounts.first(),
                    snapshot = snapshots[accounts.first().id],
                    isLoading = loadingMap[accounts.first().id] == true,
                    isNightMode = isNightMode,
                    isCompact = isCompact,
                    isRelaxedVertical = isRelaxedVertical,
                    showDelete = false,
                    areControlsVisible = areControlsVisible,
                    refreshIntervalMinutes = refreshIntervalMinutes,
                    onAccountClick = { onAccountClick(accounts.first()) },
                    onRevealControls = onRevealControls,
                    onRefreshClick = { onRefreshAccount(accounts.first().id) },
                    onDeleteClick = { onDeleteAccount(accounts.first().id) }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 3.5.dp else 5.dp)) {
                    accounts.chunked(2).forEach { rowAccounts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 5.dp else 7.dp)
                        ) {
                            rowAccounts.forEach { acc ->
                                CompactDeepSeekTile(
                                    account = acc,
                                    snapshot = snapshots[acc.id],
                                    isLoading = loadingMap[acc.id] == true,
                                    isNightMode = isNightMode,
                                    isCompact = isCompact,
                                    isRelaxedVertical = isRelaxedVertical,
                                    areControlsVisible = areControlsVisible,
                                    refreshIntervalMinutes = refreshIntervalMinutes,
                                    onAccountClick = { onAccountClick(acc) },
                                    onRevealControls = onRevealControls,
                                    onRefreshClick = { onRefreshAccount(acc.id) },
                                    onDeleteClick = { onDeleteAccount(acc.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowAccounts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDeepSeekTile(
    account: CodexAccount,
    snapshot: CodexUsageSnapshot?,
    isLoading: Boolean,
    isNightMode: Boolean,
    isCompact: Boolean,
    isRelaxedVertical: Boolean,
    areControlsVisible: Boolean,
    refreshIntervalMinutes: Int,
    onAccountClick: () -> Unit,
    onRevealControls: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText
    val tileBg = theme.surfaceElevated.copy(alpha = if (isNightMode) 0.62f else 0.72f)

    val syncStatus = snapshot?.computeStatus(
        isConfigured = account.isConfigured,
        isRefreshing = isLoading,
        refreshIntervalMinutes = refreshIntervalMinutes
    ) ?: if (account.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

    val dotColor = when {
        syncStatus == QuotaSyncStatus.AUTH_EXPIRED || snapshot?.isAvailable == false -> theme.danger
        syncStatus == QuotaSyncStatus.UPDATED && snapshot?.creditsBalance == 0.0 -> theme.warning
        syncStatus == QuotaSyncStatus.UPDATED -> theme.positive
        else -> theme.warning
    }

    val valueText = when {
        snapshot?.isMock == true && snapshot.errorMessage == "暂无演示数据" -> "暂无演示"
        syncStatus == QuotaSyncStatus.NOT_CONFIGURED -> "未配置"
        syncStatus == QuotaSyncStatus.AUTH_EXPIRED -> "Key失效"
        syncStatus == QuotaSyncStatus.SERVICE_UNAVAILABLE || snapshot?.isAvailable == false -> "服务不可用"
        else -> {
            val validMultiCount = snapshot?.balanceList?.count { it.totalBalance != null } ?: 0
            if (validMultiCount > 1 && isCompact) {
                "查看余额"
            } else {
                val raw = snapshot?.formattedBalanceText ?: "暂无余额数据"
                if (raw.contains("CNY")) raw.replace(" CNY", "") else raw
            }
        }
    }

    val valueColor = when {
        syncStatus == QuotaSyncStatus.AUTH_EXPIRED || snapshot?.isAvailable == false -> theme.danger
        valueText.contains("余额为零") || syncStatus == QuotaSyncStatus.CACHED_STALE || syncStatus == QuotaSyncStatus.CACHE_EXPIRED -> theme.warning
        syncStatus == QuotaSyncStatus.NOT_CONFIGURED -> mutedText
        else -> primaryText
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tileBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                handleAccountCardTap(syncStatus, areControlsVisible, onAccountClick, onRevealControls)
            }
            .padding(
                horizontal = if (isCompact) 7.dp else 9.dp,
                vertical = if (isCompact) (if (isRelaxedVertical) 5.5.dp else 3.5.dp) else (if (isRelaxedVertical) 5.5.dp else 4.2.dp)
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .background(dotColor, CircleShape)
                )
                if (account.isPinnedMain) {
                    Text(
                        text = "★",
                        color = if (isPureInk) theme.primaryText else theme.warning,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = account.displayTitle,
                    color = secondaryText,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (areControlsVisible) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !isLoading,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新 ${account.displayTitle}",
                            tint = secondaryText,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除 ${account.displayTitle}",
                            tint = mutedText,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = valueText,
            color = valueColor,
            fontSize = if (isCompact) (if (isRelaxedVertical) 13.sp else 11.5.sp) else 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    isRelaxedVertical: Boolean = false,
    showDelete: Boolean,
    areControlsVisible: Boolean,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: () -> Unit,
    onRevealControls: () -> Unit = {},
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText

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
            theme.warning,
            theme.warning.copy(alpha = 0.12f)
        )
    } else when (syncStatus) {
        QuotaSyncStatus.NOT_CONFIGURED -> Triple(
            "未配置",
            mutedText,
            theme.surfaceElevated
        )
        QuotaSyncStatus.AWAITING_SYNC -> Triple(
            if (snapshot?.errorMessage != null) "待同步 · 异常" else "待同步",
            theme.warning,
            theme.warning.copy(alpha = 0.12f)
        )
        QuotaSyncStatus.SYNCING -> Triple(
            "同步中",
            theme.accent,
            theme.accent.copy(alpha = 0.12f)
        )
        QuotaSyncStatus.UPDATED -> {
            when {
                snapshot?.isAvailable == false -> Triple(
                    "服务不可用",
                    theme.danger,
                    theme.danger.copy(alpha = 0.12f)
                )
                snapshot?.creditsBalance != null && snapshot.creditsBalance <= 0.0 -> Triple(
                    "余额为零",
                    theme.warning,
                    theme.warning.copy(alpha = 0.12f)
                )
                else -> Triple(
                    "正常",
                    theme.positive,
                    theme.positive.copy(alpha = 0.12f)
                )
            }
        }
        QuotaSyncStatus.CACHED_STALE -> Triple(
            "缓存 · ${snapshot?.errorMessage?.take(6) ?: "异常"}",
            theme.warning,
            theme.warning.copy(alpha = 0.12f)
        )
        QuotaSyncStatus.CACHE_EXPIRED -> Triple(
            "缓存已过期",
            theme.danger,
            theme.danger.copy(alpha = 0.12f)
        )
        QuotaSyncStatus.AUTH_EXPIRED -> Triple(
            "授权失效",
            theme.danger,
            theme.danger.copy(alpha = 0.12f)
        )
        QuotaSyncStatus.SERVICE_UNAVAILABLE -> Triple(
            if (isCoolingDown) "${remainingCooldown}s后重试" else "暂不可用",
            theme.warning,
            theme.warning.copy(alpha = 0.12f)
        )
        QuotaSyncStatus.DATA_FORMAT_ERROR -> Triple(
            "数据格式异常",
            theme.warning,
            theme.warning.copy(alpha = 0.12f)
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

    val tileBg = theme.surfaceElevated.copy(alpha = if (isNightMode) 0.62f else 0.72f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(tileBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                handleAccountCardTap(syncStatus, areControlsVisible, onAccountClick, onRevealControls)
            }
            .padding(
                horizontal = if (isCompact) 7.5.dp else 10.dp,
                vertical = if (isCompact) (if (isRelaxedVertical) 6.dp else 4.5.dp) else 6.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 3.dp)
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
                            when (syncStatus) {
                                QuotaSyncStatus.UPDATED -> theme.positive
                                QuotaSyncStatus.AUTH_EXPIRED -> theme.danger
                                else -> theme.warning
                            },
                            CircleShape
                        )
                )
                if (account.isPinnedMain) {
                    Text(
                        text = "★",
                        color = if (isPureInk) theme.primaryText else theme.warning,
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
                        .background(theme.surfaceElevated)
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
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新 ${account.displayTitle} 额度",
                                tint = if (isLoading) theme.accent
                                       else if (isCoolingDown) secondaryText.copy(alpha = 0.35f)
                                       else secondaryText,
                                modifier = Modifier
                                    .size(if (isCompact) 11.dp else 13.dp)
                                    .rotate(if (isLoading) spinAngle else 0f)
                            )
                        }
                        if (showDelete) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除 ${account.displayTitle}",
                                    tint = secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
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
                    color = theme.danger,
                    fontSize = if (isCompact) 10.sp else 11.sp
                )
                Text(
                    text = "重新授权",
                    color = theme.accent,
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
                    fontSize = if (isCompact) (if (isRelaxedVertical) 13.sp else 11.5.sp) else 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
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
 * Antigravity 专属大框：与 ChatGPT 完全同构（均分展示 Gemini 与 Claude/GPT 双轨的 5h + 周额度）
 * 同步响应全局「纯数字看板」与「进度条看板」设置。
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
    isNumbersOnly: Boolean = true,
    isRelaxedVertical: Boolean = false,
    isMockMode: Boolean = false,
    refreshIntervalMinutes: Int = 15,
    onAccountClick: (CodexAccount) -> Unit = {},
    onRevealControls: () -> Unit = {},
    onRefreshAccount: (String) -> Unit = {},
    onDeleteAccount: (String) -> Unit = {}
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = theme.surface
    val cardBorder = theme.border
    val cornerRadius = if (isCompact) 11.dp else 14.dp
    val outerVPad = when {
        !isCompact && !isRelaxedVertical -> 6.5.dp
        !isCompact -> 9.dp
        isRelaxedVertical -> 7.dp
        !isNumbersOnly -> 4.dp
        else -> 5.dp
    }

    val primaryAcc = accounts.first()
    val primarySnap = snapshots[primaryAcc.id]
    val primaryLoading = loadingMap[primaryAcc.id] == true
    val primarySyncStatus = primarySnap?.computeStatus(
        isConfigured = primaryAcc.isConfigured,
        isRefreshing = primaryLoading,
        refreshIntervalMinutes = refreshIntervalMinutes
    ) ?: if (primaryAcc.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED
    val remainingCooldown = primarySnap?.getRemainingRetrySeconds() ?: 0L
    val isCoolingDown = remainingCooldown > 0L

    val headerTrailing = if (accounts.size > 1) {
        "${accounts.size} 账号"
    } else {
        val tiers = primarySnap?.quotaWindows.orEmpty()
        val isDemoMock = primarySnap?.isMock == true && primarySnap.errorMessage == "暂无演示数据"
        val allExh = tiers.isNotEmpty() && tiers.all { (it.remainingPercent ?: 100.0) <= 0.0 }
        val anyLow = tiers.any { it.remainingPercent != null && it.remainingPercent <= 15.0 }
        when {
            isDemoMock -> "演示模式"
            primarySyncStatus == QuotaSyncStatus.AUTH_EXPIRED -> "需授权"
            primarySyncStatus == QuotaSyncStatus.NOT_CONFIGURED -> "未配置"
            primarySyncStatus == QuotaSyncStatus.SYNCING -> "同步中"
            primarySyncStatus == QuotaSyncStatus.UPDATED -> when {
                allExh -> "耗尽"
                anyLow -> "偏低"
                else -> primarySnap?.formattedSuccessTime?.let { if (isCompact && areControlsVisible) it else "更新 $it" } ?: "已授权"
            }
            primarySyncStatus == QuotaSyncStatus.SERVICE_UNAVAILABLE -> if (isCoolingDown) "${remainingCooldown}s" else "暂不可用"
            else -> "已授权"
        }
    }

    val rowPairs = remember(accounts, snapshots, loadingMap, isNightMode, isPureInk, isCompact, refreshIntervalMinutes) {
        buildAntigravityRowSpecs(
            accounts = accounts,
            snapshots = snapshots,
            loadingMap = loadingMap,
            isNightMode = isNightMode,
            isPureInk = isPureInk,
            isCompact = isCompact,
            refreshIntervalMinutes = refreshIntervalMinutes
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(cornerRadius))
            .padding(horizontal = if (isCompact) 9.dp else 13.dp, vertical = outerVPad)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(
                when {
                    !isCompact && !isRelaxedVertical -> 4.dp
                    !isCompact -> 6.dp
                    isRelaxedVertical -> 5.dp
                    !isNumbersOnly -> 2.8.dp
                    else -> 3.5.dp
                }
            )
        ) {
            UnifiedProviderCardHeader(
                provider = AiModelProvider.ANTIGRAVITY,
                title = "Antigravity",
                isNightMode = isNightMode,
                isCompact = isCompact,
                isMockMode = isMockMode,
                planPill = if (accounts.size == 1) primarySnap?.planName?.takeIf { it.isNotBlank() } else null,
                trailingText = headerTrailing,
                showRefresh = accounts.size == 1 && areControlsVisible,
                isRefreshing = primaryLoading,
                onRefreshClick = { if (!isCoolingDown) onRefreshAccount(primaryAcc.id) }
            )

            rowPairs.forEach { (acc, rowData) ->
                UnifiedDualQuotaRow(
                    row = rowData,
                    isNightMode = isNightMode,
                    isCompact = isCompact,
                    isNumbersOnly = isNumbersOnly,
                    isRelaxedVertical = isRelaxedVertical,
                    areControlsVisible = areControlsVisible && accounts.size > 1,
                    showDelete = accounts.size > 1 || acc.isConfigured,
                    onRowClick = { onAccountClick(acc) },
                    onRevealControls = onRevealControls,
                    onRefreshClick = { onRefreshAccount(acc.id) },
                    onDeleteClick = { onDeleteAccount(acc.id) }
                )
            }
        }
    }
}

private fun buildAntigravityRowSpecs(
    accounts: List<CodexAccount>,
    snapshots: Map<String, CodexUsageSnapshot>,
    loadingMap: Map<String, Boolean>,
    isNightMode: Boolean,
    isPureInk: Boolean = false,
    isCompact: Boolean,
    refreshIntervalMinutes: Int
): List<Pair<CodexAccount, DualWindowRowData>> {
    val out = mutableListOf<Pair<CodexAccount, DualWindowRowData>>()
    val multi = accounts.size > 1
    for (acc in accounts) {
        val snap = snapshots[acc.id]
        val isLoading = loadingMap[acc.id] == true
        val tiers = snap?.quotaWindows.orEmpty()
        val syncStatus = snap?.computeStatus(
            isConfigured = acc.isConfigured,
            isRefreshing = isLoading,
            refreshIntervalMinutes = refreshIntervalMinutes
        ) ?: if (acc.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

        val isMockEmpty = snap?.isMock == true && snap.errorMessage == "暂无演示数据"
        if (isMockEmpty || syncStatus == QuotaSyncStatus.NOT_CONFIGURED || syncStatus == QuotaSyncStatus.AUTH_EXPIRED || tiers.isEmpty()) {
            out += acc to buildDualWindowRowData(
                id = "${acc.id}_status",
                title = acc.displayTitle,
                subtitleHint = null,
                badge = snap?.planName,
                resetCredits = null,
                isPinned = acc.isPinnedMain,
                isConfigured = acc.isConfigured,
                snapshot = snap,
                fiveHourWindow = null,
                weeklyWindow = null,
                isLoading = isLoading,
                isNightMode = isNightMode,
                isPureInk = isPureInk,
                isCompact = isCompact,
                refreshIntervalMinutes = refreshIntervalMinutes
            )
            continue
        }

        val g5h = tiers.find { it.name == CodexUsageWindow.TIER_AGY_GEMINI_FIVE_HOUR }
        val gWeek = tiers.find {
            it.name == CodexUsageWindow.TIER_AGY_GEMINI_WEEKLY ||
                it.name == CodexUsageWindow.TIER_AGY_GEMINI_ARGON_WEEKLY
        }
        val c5h = tiers.find { it.name == CodexUsageWindow.TIER_AGY_THIRD_PARTY_FIVE_HOUR }
        val cWeek = tiers.find { it.name == CodexUsageWindow.TIER_AGY_THIRD_PARTY_WEEKLY }

        if (g5h != null || gWeek != null || c5h != null || cWeek != null) {
            if (g5h != null || gWeek != null) {
                out += acc to buildDualWindowRowData(
                    id = "${acc.id}_gemini",
                    title = if (multi) "${acc.displayTitle}·Gemini" else "Gemini",
                    subtitleHint = if (multi) null else "Google AI",
                    badge = null,
                    resetCredits = null,
                    isPinned = if (multi) acc.isPinnedMain else false,
                    isConfigured = acc.isConfigured,
                    snapshot = snap,
                    fiveHourWindow = g5h,
                    weeklyWindow = gWeek,
                    isLoading = isLoading,
                    isNightMode = isNightMode,
                    isPureInk = isPureInk,
                    isCompact = isCompact,
                    refreshIntervalMinutes = refreshIntervalMinutes
                )
            }
            if (c5h != null || cWeek != null) {
                out += acc to buildDualWindowRowData(
                    id = "${acc.id}_3p",
                    title = if (multi) "${acc.displayTitle}·Claude" else "Claude/GPT",
                    subtitleHint = if (multi) null else "3P 模型",
                    badge = null,
                    resetCredits = null,
                    isPinned = if (multi) acc.isPinnedMain else false,
                    isConfigured = acc.isConfigured,
                    snapshot = snap,
                    fiveHourWindow = c5h,
                    weeklyWindow = cWeek,
                    isLoading = isLoading,
                    isNightMode = isNightMode,
                    isPureInk = isPureInk,
                    isCompact = isCompact,
                    refreshIntervalMinutes = refreshIntervalMinutes
                )
            }
        } else {
            // 自定义兜底：将多桶两两拆组展示
            tiers.chunked(2).forEachIndexed { idx, chunk ->
                out += acc to buildDualWindowRowData(
                    id = "${acc.id}_tier_$idx",
                    title = if (multi) "${acc.displayTitle}·${chunk.first().label}" else chunk.first().label,
                    subtitleHint = null,
                    badge = null,
                    resetCredits = null,
                    isPinned = acc.isPinnedMain,
                    isConfigured = acc.isConfigured,
                    snapshot = snap,
                    fiveHourWindow = chunk.getOrNull(0),
                    weeklyWindow = chunk.getOrNull(1),
                    isLoading = isLoading,
                    isNightMode = isNightMode,
                    isPureInk = isPureInk,
                    isCompact = isCompact,
                    refreshIntervalMinutes = refreshIntervalMinutes
                )
            }
        }
    }
    return out
}

@Composable
fun MockBadge(
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    isPureInk: Boolean = LocalPureInkMode.current
) {
    val usePureInk = !isNightMode && isPureInk
    val badgeBg = when {
        usePureInk -> PureInkSurfaceElevated
        isNightMode -> TechAmber.copy(alpha = 0.2f)
        else -> InkWarningAmber.copy(alpha = 0.15f)
    }
    val badgeBorder = when {
        usePureInk -> PureInkBorder
        isNightMode -> TechAmber
        else -> InkWarningAmber
    }
    val badgeText = when {
        usePureInk -> PureInkTextSecondary
        isNightMode -> TechAmber
        else -> InkWarningAmber
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(badgeBg)
            .border(0.8.dp, badgeBorder, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = "演示数据",
            color = badgeText,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
