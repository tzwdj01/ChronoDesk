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
    onAddAccount: () -> Unit = {},
    onRefreshAccount: (accountId: String) -> Unit = {},
    onDeleteAccount: (accountId: String) -> Unit = {},
    onAccountClick: (account: CodexAccount) -> Unit = {},
    onRefreshAll: () -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val chatGptAccounts = accounts.filter { (it.modelProvider == "CHATGPT" || it.modelProvider == null) && it.isVisibleOnMain }
    val deepSeekAccounts = accounts.filter { it.modelProvider == "DEEPSEEK" && it.isVisibleOnMain }
    val antigravityAccounts = accounts.filter { it.modelProvider == "ANTIGRAVITY" && it.isVisibleOnMain }

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
                onAccountClick = onAccountClick,
                onRefreshAccount = onRefreshAccount,
                onDeleteAccount = onDeleteAccount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. 同一订阅商：Google Antigravity 专属大框（支持配额监控与多账号接入）
        if (antigravityAccounts.isNotEmpty()) {
            GoogleAntigravityUnifiedSubscriptionBox(
                accounts = antigravityAccounts,
                snapshots = snapshots,
                loadingMap = loadingMap,
                isNightMode = isNightMode,
                areControlsVisible = areControlsVisible,
                isCompact = isCompact,
                onAccountClick = onAccountClick,
                onRefreshAccount = onRefreshAccount,
                onDeleteAccount = onDeleteAccount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. 当主屏账号全部隐藏或全部分配至第二屏时的温馨指引
        if (chatGptAccounts.isEmpty() && deepSeekAccounts.isEmpty() && antigravityAccounts.isEmpty()) {
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
            // 大框顶栏：OpenAI Logo + 订阅商名称 + 套餐标签 + 账号汇总
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
                        modifier = Modifier.padding(vertical = 1.5.dp)
                    )
                }
                SingleAccountQuotaItem(
                    account = acc,
                    snapshot = snapshots[acc.id],
                    isLoading = loadingMap[acc.id] == true,
                    isNightMode = isNightMode,
                    isCompact = isCompact,
                    areControlsVisible = areControlsVisible,
                    onAccountClick = { onAccountClick(acc) },
                    onRefreshClick = { onRefreshAccount(acc.id) },
                    onDeleteClick = { onDeleteAccount(acc.id) }
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
    onAccountClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary
    val trackColor = if (isNightMode) DarkProgressTrack else InkProgressTrack

    val primaryWindow = snapshot?.primaryWindow
    val secondaryWindow = snapshot?.secondaryWindow

    val primaryPercent = primaryWindow?.remainingPercent ?: 0.0
    val secondaryPercent = secondaryWindow?.remainingPercent ?: 0.0

    val primaryCountdown = primaryWindow?.formattedCountdown() ?: "已就绪"
    val secondaryCountdown = secondaryWindow?.formattedCountdown() ?: "--"

    // 额度耗尽与预警状态判断：周额度耗尽或命中官方限额 (limitReached)
    val isLimitReached = snapshot?.limitReached == true
    val isPrimaryExhausted = primaryWindow != null && primaryPercent <= 0.0
    val isSecondaryExhausted = secondaryWindow != null && secondaryPercent <= 0.0
    val isFullyExhausted = isLimitReached || isSecondaryExhausted
    val isWarning = !isFullyExhausted && ((primaryWindow != null && primaryPercent in 0.1..15.0) || (secondaryWindow != null && secondaryPercent in 0.1..15.0))

    // 动态额度条颜色
    val primaryProgressColor = if (isNightMode) {
        when {
            isPrimaryExhausted -> TechRose
            primaryPercent <= 15.0 -> TechAmber
            else -> TechEmerald
        }
    } else {
        when {
            isPrimaryExhausted -> InkWarningRed
            primaryPercent <= 15.0 -> InkWarningAmber
            else -> InkProgress
        }
    }

    val secondaryProgressColor = if (isNightMode) {
        when {
            isSecondaryExhausted || isFullyExhausted -> TechRose
            secondaryPercent <= 15.0 -> TechAmber
            else -> TechEmerald
        }
    } else {
        when {
            isSecondaryExhausted || isFullyExhausted -> InkWarningRed
            secondaryPercent <= 15.0 -> InkWarningAmber
            else -> InkProgress
        }
    }

    // 状态文本与徽标色彩
    val statusText = when {
        isLoading -> "同步中"
        account.credentialStatus == CredentialStatus.EXPIRED -> "已过期"
        isFullyExhausted -> "额度耗尽"
        isWarning -> "即将耗尽"
        primaryWindow != null || secondaryWindow != null -> "正常"
        else -> "已连接"
    }

    val statusColor = if (isNightMode) {
        when {
            isLoading -> TechCyan
            account.credentialStatus == CredentialStatus.EXPIRED || isFullyExhausted -> TechRose
            isWarning -> TechAmber
            else -> TechEmerald
        }
    } else {
        when {
            isFullyExhausted -> InkWarningRed
            isWarning -> InkWarningAmber
            else -> InkStatusGreen
        }
    }
    val statusBg = if (isNightMode) statusColor.copy(alpha = 0.14f) else statusColor.copy(alpha = 0.10f)

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
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .background(if (isNightMode) (if (isFullyExhausted) TechRose else TechCyan) else (if (isFullyExhausted) InkWarningRed else InkStatusGreen), CircleShape)
                )
                Text(
                    text = account.displayTitle,
                    color = primaryText,
                    fontSize = if (isCompact) 10.5.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                        maxLines = 1
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
                            fontWeight = FontWeight.Medium
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
                            enabled = !isLoading,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新",
                                tint = if (isLoading) (if (isNightMode) TechCyan else InkProgress) else secondaryText,
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
                                contentDescription = "删除",
                                tint = secondaryText.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5 小时额度行 (对齐起始位置与 100% 进度条长度)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isCompact) "5小时" else "5小时额度",
                    color = secondaryText,
                    fontSize = if (isCompact) 8.5.sp else 10.sp,
                    modifier = Modifier.width(if (isCompact) 36.dp else 56.dp)
                )
                LinearProgressIndicator(
                    progress = { (primaryPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(if (isCompact) 62.dp else 88.dp)
                        .height(if (isCompact) 3.5.dp else 4.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = primaryProgressColor,
                    trackColor = trackColor
                )
            }
            val fiveHourResetText = when {
                primaryWindow?.resetAtUnix != null -> {
                    val resetTime = primaryWindow.formattedResetTime()
                    "剩余 ${primaryPercent.toInt()}% · $resetTime 重置"
                }
                primaryWindow != null -> {
                    if (primaryPercent >= 100.0) "剩余 100% · 额度充足"
                    else "剩余 ${primaryPercent.toInt()}% · 额度充足"
                }
                isLoading -> "同步中..."
                else -> "剩余 0% · 待同步"
            }
            Text(
                text = fiveHourResetText,
                color = if (isPrimaryExhausted && isNightMode) TechRose else mutedText,
                fontSize = if (isCompact) 7.5.sp else 9.sp,
                maxLines = 1
            )
        }

        // 周额度行 (对齐起始位置与 100% 进度条长度，展示精确到期/重置时间)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "周额度",
                    color = secondaryText,
                    fontSize = if (isCompact) 8.5.sp else 10.sp,
                    modifier = Modifier.width(if (isCompact) 36.dp else 56.dp)
                )
                LinearProgressIndicator(
                    progress = { (secondaryPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(if (isCompact) 62.dp else 88.dp)
                        .height(if (isCompact) 3.5.dp else 4.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = secondaryProgressColor,
                    trackColor = trackColor
                )
            }
            val weekResetText = when {
                secondaryWindow?.resetAtUnix != null -> {
                    val fullDate = secondaryWindow.formattedFullDateTime()
                    if (secondaryPercent <= 0.0 || isLimitReached) {
                        "剩余 0% · $fullDate 到期"
                    } else {
                        "剩余 ${secondaryPercent.toInt()}% · $fullDate 重置"
                    }
                }
                secondaryWindow != null -> {
                    if (secondaryPercent >= 100.0) "剩余 100% · 额度充足"
                    else "剩余 ${secondaryPercent.toInt()}% · 额度充足"
                }
                isLoading -> "同步中..."
                else -> "剩余 0% · 待同步"
            }
            Text(
                text = weekResetText,
                color = if (isSecondaryExhausted && isNightMode) TechRose else mutedText,
                fontSize = if (isCompact) 7.5.sp else 9.sp,
                maxLines = 1
            )
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
    onAccountClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary

    val isConfigured = snapshot?.creditsBalance != null && snapshot.credentialStatus != CredentialStatus.NOT_LOGGED_IN
    val isAvailable = snapshot?.isAvailable ?: true
    val balance = snapshot?.creditsBalance ?: 0.0
    val hasFunds = isConfigured && isAvailable && balance > 0.0

    // 状态徽标
    val (statusText, statusColor, statusBg) = when {
        isLoading -> Triple(
            "同步中",
            if (isNightMode) TechCyan else InkProgress,
            if (isNightMode) TechCyan.copy(alpha = 0.12f) else InkStatusTagBg
        )
        !isConfigured -> Triple(
            "未配置",
            mutedText,
            if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
        )
        snapshot?.credentialStatus == CredentialStatus.EXPIRED || snapshot?.errorMessage != null -> Triple(
            "Key失效",
            if (isNightMode) TechRose else InkWarningRed,
            (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.12f)
        )
        !hasFunds -> Triple(
            "无可用余额",
            if (isNightMode) TechRose else InkWarningRed,
            (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.12f)
        )
        else -> Triple(
            "正常",
            if (isNightMode) TechEmerald else InkStatusGreen,
            if (isNightMode) TechEmerald.copy(alpha = 0.12f) else InkStatusTagBg
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
                                if (hasFunds) TechEmerald else TechRose
                            } else {
                                if (hasFunds) InkStatusGreen else InkWarningRed
                            },
                            CircleShape
                        )
                )
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
                            enabled = !isLoading,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新",
                                tint = if (isLoading) (if (isNightMode) TechCyan else InkProgress) else secondaryText,
                                modifier = Modifier
                                    .size(11.dp)
                                    .rotate(if (isLoading) spinAngle else 0f)
                            )
                        }
                        if (showDelete) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 余额与用量数据详情
        if (!isConfigured) {
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
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currency = snapshot?.currency ?: "CNY"
                Text(
                    text = "账户余额: ¥${String.format(java.util.Locale.US, "%.2f", balance)} $currency",
                    color = primaryText,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "更新: ${snapshot?.formattedQueriedAt ?: "刚刚"}",
                    color = mutedText,
                    fontSize = if (isCompact) 8.5.sp else 9.sp
                )
            }
        }
    }
}

/**
 * Google Antigravity 专属大框（直连 Google 官方接口展示实时配额状态与重置倒计时，支持多账号与自定义别名）
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
            // 大框头部：官方 Google 4 色 Logo + Google Antigravity 名称 + 账号数状态
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
                        text = "Google Antigravity",
                        color = primaryText,
                        fontSize = if (isCompact) 12.5.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    )
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

            // 框内遍历展示各个 Antigravity 账号
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
                    onAccountClick = { onAccountClick(acc) },
                    onRefreshClick = { onRefreshAccount(acc.id) },
                    onDeleteClick = { onDeleteAccount(acc.id) }
                )
            }
        }
    }
}

/**
 * 单个 Google Antigravity 账号行（支持自定义别名、Pro 套餐标识、5小时配额与日配额双进度条及重置时间）
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
    onAccountClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary

    val isConfigured = account.credentialStatus == CredentialStatus.VALID
    val isExpired = account.credentialStatus == CredentialStatus.EXPIRED

    val primaryWindow = snapshot?.primaryWindow
    val secondaryWindow = snapshot?.secondaryWindow

    val primaryPercent = primaryWindow?.remainingPercent ?: 100.0
    val secondaryPercent = secondaryWindow?.remainingPercent ?: 100.0

    val primaryProgressColor = if (isNightMode) {
        when {
            primaryPercent <= 0.0 -> TechRose
            primaryPercent <= 15.0 -> TechAmber
            else -> TechCyan
        }
    } else {
        when {
            primaryPercent <= 0.0 -> InkWarningRed
            primaryPercent <= 15.0 -> InkWarningAmber
            else -> InkProgress
        }
    }

    val secondaryProgressColor = if (isNightMode) {
        when {
            secondaryPercent <= 0.0 -> TechRose
            secondaryPercent <= 15.0 -> TechAmber
            else -> TechEmerald
        }
    } else {
        when {
            secondaryPercent <= 0.0 -> InkWarningRed
            secondaryPercent <= 15.0 -> InkWarningAmber
            else -> InkStatusGreen
        }
    }

    val trackColor = if (isNightMode) DarkProgressTrack else InkProgressTrack

    val (statusText, statusColor, statusBg) = when {
        isLoading -> Triple(
            "同步中",
            if (isNightMode) TechCyan else InkProgress,
            (if (isNightMode) TechCyan else InkProgress).copy(alpha = 0.12f)
        )
        !isConfigured -> Triple(
            "未配置",
            mutedText,
            if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
        )
        isExpired -> Triple(
            "Key失效",
            if (isNightMode) TechRose else InkWarningRed,
            (if (isNightMode) TechRose else InkWarningRed).copy(alpha = 0.12f)
        )
        else -> Triple(
            "正常",
            if (isNightMode) TechEmerald else InkStatusGreen,
            if (isNightMode) TechEmerald.copy(alpha = 0.12f) else InkStatusTagBg
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
        // 账号标识行：圆点 + 自定义账号名称 + Pro 套餐徽标 + 状态徽标 + 操作按钮
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
                                if (isConfigured) TechCyan else TechRose
                            } else {
                                if (isConfigured) InkStatusGreen else InkWarningRed
                            },
                            CircleShape
                        )
                )
                Text(
                    text = account.displayTitle,
                    color = primaryText,
                    fontSize = if (isCompact) 10.5.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val plan = snapshot?.planName?.ifBlank { "Pro" } ?: "Pro"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated)
                        .padding(horizontal = 3.dp, vertical = 0.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = plan,
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
                            enabled = !isLoading,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新",
                                tint = if (isLoading) (if (isNightMode) TechCyan else InkProgress) else secondaryText,
                                modifier = Modifier
                                    .size(11.dp)
                                    .rotate(if (isLoading) spinAngle else 0f)
                            )
                        }
                        if (showDelete) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5 小时配额行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isCompact) "5小时" else "5小时配额",
                    color = secondaryText,
                    fontSize = if (isCompact) 8.5.sp else 10.sp,
                    modifier = Modifier.width(if (isCompact) 36.dp else 56.dp)
                )
                LinearProgressIndicator(
                    progress = { if (isConfigured) (primaryPercent / 100.0).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .width(if (isCompact) 62.dp else 88.dp)
                        .height(if (isCompact) 3.5.dp else 4.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = primaryProgressColor,
                    trackColor = trackColor
                )
            }
            val fiveHourResetText = when {
                !isConfigured -> "待配置 API Key"
                isLoading -> "同步中..."
                primaryWindow?.resetAtUnix != null -> {
                    val resetTime = primaryWindow.formattedResetTime()
                    "剩余 ${primaryPercent.toInt()}% · $resetTime 重置"
                }
                else -> "剩余 ${primaryPercent.toInt()}% · 额度已就绪"
            }
            Text(
                text = fiveHourResetText,
                color = mutedText,
                fontSize = if (isCompact) 7.5.sp else 9.sp,
                maxLines = 1
            )
        }

        // 日配额行
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "日配额",
                    color = secondaryText,
                    fontSize = if (isCompact) 8.5.sp else 10.sp,
                    modifier = Modifier.width(if (isCompact) 36.dp else 56.dp)
                )
                LinearProgressIndicator(
                    progress = { if (isConfigured) (secondaryPercent / 100.0).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .width(if (isCompact) 62.dp else 88.dp)
                        .height(if (isCompact) 3.5.dp else 4.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = secondaryProgressColor,
                    trackColor = trackColor
                )
            }
            val dailyResetText = when {
                !isConfigured -> "待配置 API Key"
                isLoading -> "同步中..."
                secondaryWindow?.resetAtUnix != null -> {
                    "剩余 ${secondaryPercent.toInt()}% · 08:00重置"
                }
                else -> "剩余 ${secondaryPercent.toInt()}% · 额度已就绪"
            }
            Text(
                text = dailyResetText,
                color = mutedText,
                fontSize = if (isCompact) 7.5.sp else 9.sp,
                maxLines = 1
            )
        }
    }
}
