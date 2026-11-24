package com.chronodesk.app.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.ClockSettings
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.QuotaSyncStatus
import com.chronodesk.app.data.model.SyncErrorType
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkProgressTrack
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkProgress
import com.chronodesk.app.ui.theme.InkProgressTrack
import com.chronodesk.app.ui.theme.InkStatusGreen
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkSurfaceElevated
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkTextTertiary
import com.chronodesk.app.ui.theme.InkWarningAmber
import com.chronodesk.app.ui.theme.InkWarningRed
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary
import com.chronodesk.app.ui.theme.LocalPureInkMode
import com.chronodesk.app.ui.theme.resolveChronoPalette

/**
 * 第二页（扩展页面）：大字远距优先的 AI 额度监控面板。
 * 支持 ChatGPT / Antigravity / DeepSeek 按用户配置顺序展示，以及「纯数字 / 进度条」模式快速切换。
 */
@Composable
fun ClockExtensionPage(
    accounts: List<CodexAccount> = emptyList(),
    snapshots: Map<String, CodexUsageSnapshot> = emptyMap(),
    loadingMap: Map<String, Boolean> = emptyMap(),
    settings: ClockSettings,
    isNightMode: Boolean,
    isCompact: Boolean = false,
    onRefreshAll: () -> Unit = {},
    onToggleDisplayMode: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onManageAccount: () -> Unit = {},
    onAccountClick: (CodexAccount) -> Unit = { onManageAccount() },
    onToggleControls: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = theme.surface
    val cardBorder = theme.border
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText
    val isNumbersOnly = settings.isNumbersOnlyMode
    val providerOrder = settings.orderedProviders()

    val chatGptAccounts = accounts.filter { it.modelProvider == "CHATGPT" && it.isVisibleOnSecond }
        .sortedByDescending { it.isPinnedMain }
    val deepSeekAccounts = accounts.filter { it.modelProvider == "DEEPSEEK" && it.isVisibleOnSecond }
        .sortedByDescending { it.isPinnedMain }
    val antigravityAccounts = accounts.filter { it.modelProvider == "ANTIGRAVITY" && it.isVisibleOnSecond }
        .sortedByDescending { it.isPinnedMain }

    val nowUnix = System.currentTimeMillis() / 1000L
    val chatGptIndex = providerOrder.indexOf("CHATGPT")
    val splitChatGptHorizontal = isCompact && chatGptIndex == 1

    @Composable
    fun ChatGptSection(modifier: Modifier = Modifier) {
        Box(modifier = modifier.fillMaxWidth()) {
            if (chatGptAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .clickable { onManageAccount() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "尚未在扩展页显示 ChatGPT 账号",
                            color = primaryText,
                            fontSize = if (isCompact) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "点击管理或授权账号，在此以超大字号实时关注 5 小时额度与周额度",
                            color = secondaryText,
                            fontSize = if (isCompact) 11.sp else 12.5.sp
                        )
                    }
                }
            } else if (chatGptAccounts.size <= 2) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
                ) {
                    chatGptAccounts.forEach { acc ->
                        ChatGptHeroCard(
                            account = acc,
                            snapshot = snapshots[acc.id],
                            isRefreshing = loadingMap[acc.id] == true,
                            refreshIntervalMinutes = settings.autoRefreshIntervalMinutes,
                            nowUnix = nowUnix,
                            isNightMode = isNightMode,
                            isCompact = isCompact,
                            isNumbersOnly = isNumbersOnly,
                            horizontalWindows = splitChatGptHorizontal,
                            onClick = { onAccountClick(acc) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp)
                ) {
                    chatGptAccounts.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
                        ) {
                            pair.forEach { acc ->
                                ChatGptHeroCard(
                                    account = acc,
                                    snapshot = snapshots[acc.id],
                                    isRefreshing = loadingMap[acc.id] == true,
                                    refreshIntervalMinutes = settings.autoRefreshIntervalMinutes,
                                    nowUnix = nowUnix,
                                    isNightMode = isNightMode,
                                    isCompact = isCompact,
                                    isNumbersOnly = isNumbersOnly,
                                    horizontalWindows = splitChatGptHorizontal,
                                    onClick = { onAccountClick(acc) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DeepSeekStrip(modifier: Modifier = Modifier) {
        ExtensionDeepSeekCard(
            deepSeekAccounts = deepSeekAccounts,
            allAccounts = accounts,
            snapshots = snapshots,
            loadingMap = loadingMap,
            settings = settings,
            isNightMode = isNightMode,
            isCompact = isCompact,
            onAccountClick = onAccountClick,
            onManageAccount = onManageAccount,
            modifier = modifier
        )
    }

    @Composable
    fun AntigravityStrip(wideSideBySide: Boolean, modifier: Modifier = Modifier) {
        ExtensionAntigravityCard(
            antigravityAccounts = antigravityAccounts,
            allAccounts = accounts,
            snapshots = snapshots,
            loadingMap = loadingMap,
            settings = settings,
            nowUnix = nowUnix,
            isNightMode = isNightMode,
            isCompact = isCompact,
            isNumbersOnly = isNumbersOnly,
            wideSideBySide = wideSideBySide,
            onAccountClick = onAccountClick,
            onManageAccount = onManageAccount,
            modifier = modifier
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleControls() }
            .padding(
                start = if (isCompact) 14.dp else 20.dp,
                end = if (isCompact) 14.dp else 20.dp,
                top = if (isCompact) 10.dp else 14.dp,
                bottom = if (isCompact) 16.dp else 22.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 7.dp else 10.dp)
    ) {
        // 顶栏左侧标题与纯数字/进度条快速切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 30.dp else 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AI 额度监控",
                color = primaryText,
                fontSize = if (isCompact) 15.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.surfaceElevated)
                    .clickable { onRefreshAll() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "详情页 · 点击同步",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.sp else 10.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isNumbersOnly) theme.accent.copy(alpha = 0.14f) else theme.surfaceElevated
                    )
                    .border(
                        0.8.dp,
                        if (isNumbersOnly) theme.accent.copy(alpha = 0.5f) else cardBorder,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onToggleDisplayMode() }
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isNumbersOnly) "显示: 纯数字" else "显示: 进度条",
                    color = if (isNumbersOnly) theme.accent else secondaryText,
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (settings.useMockData) {
                MockBadge(isNightMode = isNightMode)
            }
        }

        // 按全局排序依次排布三大平台（相邻的 DeepSeek + Antigravity 自动并列双栏节省纵向空间）
        var idx = 0
        while (idx < providerOrder.size) {
            val current = providerOrder[idx]
            val next = providerOrder.getOrNull(idx + 1)
            if (current == "CHATGPT") {
                ChatGptSection(modifier = Modifier.weight(1f))
                idx += 1
            } else if ((current == "DEEPSEEK" && next == "ANTIGRAVITY") ||
                (current == "ANTIGRAVITY" && next == "DEEPSEEK")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (current == "DEEPSEEK") {
                        DeepSeekStrip(
                            modifier = Modifier.weight(if (antigravityAccounts.isEmpty()) 1.55f else 1.15f)
                        )
                        AntigravityStrip(
                            wideSideBySide = false,
                            modifier = Modifier.weight(1.35f)
                        )
                    } else {
                        AntigravityStrip(
                            wideSideBySide = false,
                            modifier = Modifier.weight(1.35f)
                        )
                        DeepSeekStrip(
                            modifier = Modifier.weight(if (antigravityAccounts.isEmpty()) 1.55f else 1.15f)
                        )
                    }
                }
                idx += 2
            } else if (current == "DEEPSEEK") {
                DeepSeekStrip(modifier = Modifier.fillMaxWidth())
                idx += 1
            } else {
                AntigravityStrip(wideSideBySide = true, modifier = Modifier.fillMaxWidth())
                idx += 1
            }
        }
    }
}

@Composable
private fun ExtensionDeepSeekCard(
    deepSeekAccounts: List<CodexAccount>,
    allAccounts: List<CodexAccount>,
    snapshots: Map<String, CodexUsageSnapshot>,
    loadingMap: Map<String, Boolean>,
    settings: ClockSettings,
    isNightMode: Boolean,
    isCompact: Boolean,
    onAccountClick: (CodexAccount) -> Unit,
    onManageAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = theme.surface
    val cardBorder = theme.border
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
            .clickable {
                val firstDs = deepSeekAccounts.firstOrNull()
                    ?: allAccounts.firstOrNull { it.modelProvider == "DEEPSEEK" }
                if (firstDs != null) onAccountClick(firstDs) else onManageAccount()
            }
            .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = if (isCompact) 6.dp else 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ModelProviderLogo(
                        provider = AiModelProvider.DEEPSEEK,
                        size = if (isCompact) 15.dp else 18.dp,
                        isNightMode = isNightMode
                    )
                    Text(
                        text = "DeepSeek API",
                        color = secondaryText,
                        fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (deepSeekAccounts.isEmpty()) "未配置" else "${deepSeekAccounts.size}个账号",
                    color = mutedText,
                    fontSize = if (isCompact) 8.5.sp else 9.5.sp
                )
            }

            if (deepSeekAccounts.isEmpty()) {
                Text(
                    text = "点击接入 DeepSeek Key 查看真实余额",
                    color = mutedText,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
                ) {
                    deepSeekAccounts.take(3).forEach { dsAcc ->
                        val dsSnap = snapshots[dsAcc.id]
                        val dsStatus = dsSnap?.computeStatus(
                            isConfigured = dsAcc.isConfigured,
                            isRefreshing = loadingMap[dsAcc.id] == true,
                            refreshIntervalMinutes = settings.autoRefreshIntervalMinutes
                        ) ?: if (dsAcc.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

                        val isDsDemo = dsSnap?.isMock == true && dsSnap.errorMessage == "暂无演示数据"
                        val valueText = when {
                            isDsDemo -> "暂无演示数据"
                            dsStatus == QuotaSyncStatus.NOT_CONFIGURED -> "未配置"
                            dsStatus == QuotaSyncStatus.AUTH_EXPIRED -> "Key失效"
                            dsStatus == QuotaSyncStatus.SERVICE_UNAVAILABLE || dsSnap?.isAvailable == false -> "服务不可用"
                            else -> dsSnap?.formattedBalanceText ?: "暂无余额数据"
                        }
                        val valueColor = when {
                            dsStatus == QuotaSyncStatus.AUTH_EXPIRED || dsSnap?.isAvailable == false -> theme.danger
                            valueText.contains("余额为零") || dsStatus == QuotaSyncStatus.CACHED_STALE || dsStatus == QuotaSyncStatus.CACHE_EXPIRED -> theme.warning
                            else -> primaryText
                        }
                        val timeStr = dsSnap?.formattedSuccessTime ?: dsSnap?.formattedQueriedAt
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.surfaceElevated.copy(alpha = if (isNightMode) 0.6f else 0.65f))
                                .padding(horizontal = 7.dp, vertical = if (isCompact) 3.5.dp else 4.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = dsAcc.displayTitle,
                                    color = secondaryText,
                                    fontSize = if (isCompact) 9.sp else 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = valueText,
                                    color = valueColor,
                                    fontSize = if (isCompact) 11.5.sp else 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!isCompact && timeStr != null && dsStatus != QuotaSyncStatus.NOT_CONFIGURED && dsStatus != QuotaSyncStatus.AUTH_EXPIRED) {
                                Text(
                                    text = timeStr,
                                    color = mutedText,
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtensionAntigravityCard(
    antigravityAccounts: List<CodexAccount>,
    allAccounts: List<CodexAccount>,
    snapshots: Map<String, CodexUsageSnapshot>,
    loadingMap: Map<String, Boolean>,
    settings: ClockSettings,
    nowUnix: Long,
    isNightMode: Boolean,
    isCompact: Boolean,
    isNumbersOnly: Boolean,
    wideSideBySide: Boolean,
    onAccountClick: (CodexAccount) -> Unit,
    onManageAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = theme.surface
    val cardBorder = theme.border
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText
    val innerBg = theme.surfaceElevated.copy(alpha = if (isNightMode) 0.6f else 0.65f)
    val trackColor = theme.progressTrack

    val agAcc = antigravityAccounts.firstOrNull()
    val agSnap = agAcc?.let { snapshots[it.id] }
    val agStatus = agSnap?.computeStatus(
        isConfigured = agAcc.isConfigured,
        isRefreshing = loadingMap[agAcc.id] == true,
        refreshIntervalMinutes = settings.autoRefreshIntervalMinutes
    ) ?: if (agAcc?.isConfigured == true) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED
    val agTiers = agSnap?.quotaWindows.orEmpty()
    val g5h = agTiers.find { it.name == CodexUsageWindow.TIER_AGY_GEMINI_FIVE_HOUR }
    val gWeek = agTiers.find {
        it.name == CodexUsageWindow.TIER_AGY_GEMINI_WEEKLY ||
            it.name == CodexUsageWindow.TIER_AGY_GEMINI_ARGON_WEEKLY
    }
    val c5h = agTiers.find { it.name == CodexUsageWindow.TIER_AGY_THIRD_PARTY_FIVE_HOUR }
    val cWeek = agTiers.find { it.name == CodexUsageWindow.TIER_AGY_THIRD_PARTY_WEEKLY }

    @Composable
    fun ModelDualTrackTile(
        trackTitle: String,
        fiveHourWindow: CodexUsageWindow?,
        weeklyWindow: CodexUsageWindow?,
        tileModifier: Modifier = Modifier
    ) {
        fun pctColor(rem: Double?): androidx.compose.ui.graphics.Color = when {
            rem == null -> mutedText
            rem <= 0.0 -> theme.danger
            rem <= 15.0 -> theme.warning
            else -> primaryText
        }

        Column(
            modifier = tileModifier
                .clip(RoundedCornerShape(6.dp))
                .background(innerBg)
                .padding(horizontal = if (isCompact) 7.dp else 9.dp, vertical = if (isCompact) 3.5.dp else 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 5.dp else 8.dp)
            ) {
                Text(
                    text = trackTitle,
                    color = secondaryText,
                    fontSize = if (isCompact) 9.5.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(if (isCompact) 52.dp else 66.dp)
                )

                val f5Str = fiveHourWindow?.formattedRemaining() ?: "—"
                val f5Reset = fiveHourWindow?.formattedResetTime(nowUnix) ?: ""
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "5h",
                        color = mutedText,
                        fontSize = if (isCompact) 8.5.sp else 9.5.sp
                    )
                    Text(
                        text = f5Str,
                        color = pctColor(fiveHourWindow?.remainingPercent),
                        fontSize = if (isCompact) 11.sp else 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (f5Reset.isNotBlank() && f5Reset != "重置时间未知") {
                        Text(
                            text = f5Reset,
                            color = mutedText,
                            fontSize = if (isCompact) 8.sp else 9.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val wkStr = weeklyWindow?.formattedRemaining() ?: "—"
                val wkCd = weeklyWindow?.formattedCompactCountdown(nowUnix) ?: ""
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "周",
                        color = mutedText,
                        fontSize = if (isCompact) 8.5.sp else 9.5.sp
                    )
                    Text(
                        text = wkStr,
                        color = pctColor(weeklyWindow?.remainingPercent),
                        fontSize = if (isCompact) 11.sp else 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (wkCd.isNotBlank() && wkCd != "未知") {
                        Text(
                            text = wkCd,
                            color = mutedText,
                            fontSize = if (isCompact) 8.sp else 9.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (!isNumbersOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    @Composable
                    fun MiniBar(win: CodexUsageWindow?, mod: Modifier) {
                        val rem = win?.remainingPercent
                        val barClr = when {
                            rem != null && rem <= 0.0 -> theme.danger
                            rem != null && rem <= 15.0 -> theme.warning
                            else -> theme.progressActive
                        }
                        LinearProgressIndicator(
                            progress = { if (rem != null && rem.isFinite()) (rem / 100.0).toFloat().coerceIn(0f, 1f) else 0f },
                            modifier = mod
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = barClr,
                            trackColor = trackColor
                        )
                    }
                    MiniBar(fiveHourWindow, Modifier.weight(1f))
                    MiniBar(weeklyWindow, Modifier.weight(1f))
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
            .clickable {
                val target = agAcc ?: allAccounts.firstOrNull { it.modelProvider == "ANTIGRAVITY" }
                if (target != null) onAccountClick(target) else onManageAccount()
            }
            .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = if (isCompact) 5.5.dp else 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 3.5.dp else 5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ModelProviderLogo(
                        provider = AiModelProvider.ANTIGRAVITY,
                        size = if (isCompact) 15.dp else 18.dp,
                        isNightMode = isNightMode
                    )
                    Text(
                        text = if (agAcc != null && agAcc.displayTitle != "Antigravity") {
                            "Antigravity · ${agAcc.displayTitle}"
                        } else "Antigravity",
                        color = secondaryText,
                        fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = agSnap?.planName?.takeIf { it.isNotBlank() }
                        ?: "${settings.autoRefreshIntervalMinutes}分钟同步",
                    color = mutedText,
                    fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }

            val isAgDemo = agSnap?.isMock == true && agSnap.errorMessage == "暂无演示数据"
            val hasBuckets = g5h != null || gWeek != null || c5h != null || cWeek != null || agTiers.isNotEmpty()
            when {
                agAcc == null || agSnap == null -> {
                    Text(
                        text = "尚未授权账号 · 点击连接 Antigravity",
                        color = mutedText,
                        fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                isAgDemo -> {
                    Text(
                        text = "${agAcc.displayTitle} · 暂无演示数据",
                        color = theme.warning,
                        fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                agStatus == QuotaSyncStatus.AUTH_EXPIRED -> {
                    Text(
                        text = "${agAcc.displayTitle} · 登录失效，点击重连",
                        color = theme.danger,
                        fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                !hasBuckets -> {
                    val hint = when (agStatus) {
                        QuotaSyncStatus.SYNCING -> "同步中…"
                        QuotaSyncStatus.SERVICE_UNAVAILABLE -> "暂不可用"
                        else -> agSnap.errorMessage ?: "等待同步"
                    }
                    Text(
                        text = "${agAcc.displayTitle} · $hint",
                        color = mutedText,
                        fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                wideSideBySide -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
                    ) {
                        ModelDualTrackTile(
                            trackTitle = "Gemini",
                            fiveHourWindow = g5h,
                            weeklyWindow = gWeek,
                            tileModifier = Modifier.weight(1f)
                        )
                        ModelDualTrackTile(
                            trackTitle = if (isCompact) "Claude/GPT" else "Claude / GPT",
                            fiveHourWindow = c5h,
                            weeklyWindow = cWeek,
                            tileModifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 3.dp else 4.dp)) {
                        ModelDualTrackTile(
                            trackTitle = "Gemini",
                            fiveHourWindow = g5h,
                            weeklyWindow = gWeek,
                            tileModifier = Modifier.fillMaxWidth()
                        )
                        ModelDualTrackTile(
                            trackTitle = if (isCompact) "Claude/GPT" else "Claude / GPT",
                            fiveHourWindow = c5h,
                            weeklyWindow = cWeek,
                            tileModifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatGptHeroCard(
    account: CodexAccount,
    snapshot: CodexUsageSnapshot?,
    isRefreshing: Boolean = false,
    refreshIntervalMinutes: Int,
    nowUnix: Long,
    isNightMode: Boolean,
    isCompact: Boolean,
    isNumbersOnly: Boolean = true,
    horizontalWindows: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = theme.surface
    val cardBorder = theme.border
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText

    val syncStatus = snapshot?.computeStatus(
        isConfigured = account.isConfigured,
        isRefreshing = isRefreshing,
        refreshIntervalMinutes = refreshIntervalMinutes
    ) ?: if (account.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

    val cachedSuffix = snapshot?.formattedSuccessTime ?: snapshot?.formattedQueriedAt
    val statusDesc = when (syncStatus) {
        QuotaSyncStatus.NOT_CONFIGURED -> "未配置"
        QuotaSyncStatus.AWAITING_SYNC -> "等待同步"
        QuotaSyncStatus.SYNCING -> "同步中"
        QuotaSyncStatus.UPDATED -> "更新于 ${cachedSuffix ?: "刚刚"}"
        QuotaSyncStatus.CACHED_STALE -> {
            val reason = if (snapshot?.lastErrorType == SyncErrorType.ACCESS_DENIED) "访问受限" else snapshot?.errorMessage?.take(6) ?: "异常"
            if (cachedSuffix != null) "缓存($cachedSuffix) · $reason" else "缓存 · $reason"
        }
        QuotaSyncStatus.CACHE_EXPIRED -> if (cachedSuffix != null) "缓存已过期 · $cachedSuffix" else "缓存已过期"
        QuotaSyncStatus.AUTH_EXPIRED -> "授权失效"
        QuotaSyncStatus.SERVICE_UNAVAILABLE -> "服务暂不可用"
        QuotaSyncStatus.DATA_FORMAT_ERROR -> "数据格式异常"
    }
    val statusColor = when (syncStatus) {
        QuotaSyncStatus.NOT_CONFIGURED -> mutedText
        QuotaSyncStatus.AWAITING_SYNC -> theme.warning
        QuotaSyncStatus.SYNCING -> theme.accent
        QuotaSyncStatus.UPDATED -> theme.positive
        QuotaSyncStatus.CACHED_STALE -> theme.warning
        QuotaSyncStatus.CACHE_EXPIRED, QuotaSyncStatus.AUTH_EXPIRED -> theme.danger
        QuotaSyncStatus.SERVICE_UNAVAILABLE, QuotaSyncStatus.DATA_FORMAT_ERROR -> theme.warning
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 11.dp else 14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 11.dp else 14.dp))
            .clickable { onClick() }
            .padding(horizontal = if (isCompact) 11.dp else 16.dp, vertical = if (isCompact) 9.dp else 13.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 7.dp else 10.dp)
        ) {
            // 账号标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    ModelProviderLogo(
                        provider = AiModelProvider.CHATGPT,
                        size = if (isCompact) 20.dp else 24.dp,
                        isNightMode = isNightMode
                    )
                    if (account.isPinnedMain) {
                        Text(
                            text = "★",
                            color = if (isPureInk) theme.primaryText else theme.warning,
                            fontSize = if (isCompact) 12.sp else 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = account.displayTitle,
                        color = primaryText,
                        fontSize = if (isCompact) 14.sp else 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val plan = snapshot?.planName?.ifBlank { "Plus" } ?: "Plus"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.surfaceElevated)
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = plan,
                            color = secondaryText,
                            fontSize = if (isCompact) 9.sp else 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    val resetCredits = snapshot?.resetCreditsCount
                    if (resetCredits != null && resetCredits > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(theme.accent.copy(alpha = 0.14f))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "重置:${resetCredits}次",
                                color = theme.accent,
                                fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.13f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusDesc,
                        color = statusColor,
                        fontSize = if (isCompact) 9.sp else 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // 账号窗口主体（5 小时 & 周额度并列放大呈现）
            if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.danger.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "登录凭据已失效 · 点击重新授权",
                        color = theme.danger,
                        fontSize = if (isCompact) 13.sp else 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (syncStatus == QuotaSyncStatus.NOT_CONFIGURED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未配置账号 · 点击进行连接授权",
                        color = secondaryText,
                        fontSize = if (isCompact) 12.sp else 14.sp
                    )
                }
            } else if (horizontalWindows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
                ) {
                    HeroQuotaWindowBlock(
                        window = snapshot?.primaryWindow,
                        fallbackLabel = "额度窗口",
                        nowUnix = nowUnix,
                        isPrimary = true,
                        isNightMode = isNightMode,
                        isCompact = isCompact,
                        isNumbersOnly = isNumbersOnly,
                        horizontalMode = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    HeroQuotaWindowBlock(
                        window = snapshot?.secondaryWindow,
                        fallbackLabel = "额度窗口",
                        nowUnix = nowUnix,
                        isPrimary = false,
                        isNightMode = isNightMode,
                        isCompact = isCompact,
                        isNumbersOnly = isNumbersOnly,
                        horizontalMode = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
                ) {
                    HeroQuotaWindowBlock(
                        window = snapshot?.primaryWindow,
                        fallbackLabel = "额度窗口",
                        nowUnix = nowUnix,
                        isPrimary = true,
                        isNightMode = isNightMode,
                        isCompact = isCompact,
                        isNumbersOnly = isNumbersOnly,
                        horizontalMode = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    HeroQuotaWindowBlock(
                        window = snapshot?.secondaryWindow,
                        fallbackLabel = "额度窗口",
                        nowUnix = nowUnix,
                        isPrimary = false,
                        isNightMode = isNightMode,
                        isCompact = isCompact,
                        isNumbersOnly = isNumbersOnly,
                        horizontalMode = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroQuotaWindowBlock(
    window: CodexUsageWindow?,
    fallbackLabel: String,
    nowUnix: Long,
    isPrimary: Boolean,
    isNightMode: Boolean,
    isCompact: Boolean,
    isNumbersOnly: Boolean = true,
    horizontalMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val mutedText = theme.mutedText
    val trackColor = theme.progressTrack
    val panelBg = theme.surfaceElevated.copy(alpha = if (isNightMode) 0.72f else 0.75f)

    val remaining = window?.remainingPercent
    val isExhausted = remaining != null && remaining <= 0.0
    val isLow = remaining != null && remaining in 0.01..15.0

    val barColor = when {
        isExhausted -> theme.danger
        isLow -> theme.warning
        else -> theme.progressActive
    }

    val percentColor = when {
        isExhausted -> theme.danger
        isLow -> theme.warning
        remaining == null -> mutedText
        else -> primaryText
    }

    val labelText = window?.windowLabel ?: fallbackLabel
    val remainingText = window?.formattedRemaining() ?: "—"
    val resetDesc = when {
        window == null -> "待同步"
        window.resetAtUnix == null -> "重置时间未知"
        nowUnix >= window.resetAtUnix -> "等待更新"
        isPrimary -> "${window.formattedResetTime(nowUnix)} 重置 · ${window.formattedCountdown(nowUnix)}"
        else -> "${window.formattedFullDateTime(nowUnix)} · ${window.formattedCountdown(nowUnix)}"
    }
    val compactResetMain = when {
        window == null -> "待同步"
        window.resetAtUnix == null -> "未知"
        nowUnix >= window.resetAtUnix -> "待更新"
        isPrimary -> "${window.formattedResetTime(nowUnix)} 重置"
        else -> window.formattedFullDateTime(nowUnix)
    }
    val compactResetSub = when {
        window == null || window.resetAtUnix == null -> ""
        nowUnix >= window.resetAtUnix -> ""
        else -> "剩余 ${window.formattedCompactCountdown(nowUnix)}"
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
            .background(panelBg)
            .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 6.dp else 9.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (isNumbersOnly) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = labelText,
                    color = secondaryText,
                    fontSize = if (isCompact) 11.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                if (!horizontalMode) {
                    Text(
                        text = compactResetMain,
                        color = mutedText,
                        fontSize = if (isCompact) 10.sp else 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = remainingText,
                    color = percentColor,
                    fontSize = when {
                        horizontalMode && isCompact -> 22.sp
                        isCompact -> 28.sp
                        else -> 36.sp
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
                Column(horizontalAlignment = Alignment.End) {
                    if (horizontalMode) {
                        Text(
                            text = compactResetMain,
                            color = mutedText,
                            fontSize = if (isCompact) 8.5.sp else 10.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (compactResetSub.isNotBlank()) {
                        Text(
                            text = compactResetSub,
                            color = if (isLow || isExhausted) percentColor.copy(alpha = 0.85f) else secondaryText,
                            fontSize = if (isCompact) 10.5.sp else 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = labelText,
                    color = secondaryText,
                    fontSize = if (isCompact) 11.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = resetDesc,
                    color = mutedText,
                    fontSize = if (isCompact) 9.5.sp else 11.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp)
            ) {
                LinearProgressIndicator(
                    progress = { if (remaining != null && remaining.isFinite()) (remaining / 100.0).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCompact) 7.dp else 9.5.dp)
                        .clip(RoundedCornerShape(if (isCompact) 3.5.dp else 5.dp)),
                    color = barColor,
                    trackColor = trackColor
                )
                Text(
                    text = remainingText,
                    color = percentColor,
                    fontSize = if (isCompact) 21.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
