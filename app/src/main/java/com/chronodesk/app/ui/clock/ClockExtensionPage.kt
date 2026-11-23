package com.chronodesk.app.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

/**
 * 第二页（扩展页面）：大字远距优先的 AI 额度监控面板。
 * 视觉重心聚焦 ChatGPT 多账号窗口残余额度，DeepSeek 与 Gemini 收敛为底部精简状态条。
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
    onOpenSettings: () -> Unit = {},
    onManageAccount: () -> Unit = {},
    onAccountClick: (CodexAccount) -> Unit = { onManageAccount() },
    modifier: Modifier = Modifier
) {
    val cardBg = if (isNightMode) DarkSurface else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary

    val chatGptAccounts = accounts.filter { (it.modelProvider == "CHATGPT" || it.modelProvider == null) && it.isVisibleOnSecond }
        .sortedByDescending { it.isPinnedMain }
    val deepSeekAccounts = accounts.filter { it.modelProvider == "DEEPSEEK" && it.isVisibleOnSecond }
        .sortedByDescending { it.isPinnedMain }
    val antigravityAccounts = accounts.filter { it.modelProvider == "ANTIGRAVITY" && it.isVisibleOnSecond }
        .sortedByDescending { it.isPinnedMain }

    val nowUnix = System.currentTimeMillis() / 1000L

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = if (isCompact) 14.dp else 20.dp,
                end = if (isCompact) 14.dp else 20.dp,
                top = if (isCompact) 10.dp else 14.dp,
                bottom = if (isCompact) 18.dp else 22.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp)
    ) {
        // 顶栏左侧标题区：右侧留白供全局悬浮 ClockTopBar 使用，不再重复绘制操作按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 32.dp else 36.dp),
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
                    .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated)
                    .clickable { onRefreshAll() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "详情页 · 点击同步",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.sp else 10.sp
                )
            }
            if (settings.useMockData) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isNightMode) TechAmber.copy(alpha = 0.2f) else InkWarningAmber.copy(alpha = 0.15f))
                        .border(0.8.dp, if (isNightMode) TechAmber else InkWarningAmber, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "演示数据",
                        color = if (isNightMode) TechAmber else InkWarningAmber,
                        fontSize = if (isCompact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ================= 主体 Hero 区 (~72%-76%): ChatGPT 高对比大字监控 =================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (chatGptAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                        .clickable { onManageAccount() }
                        .padding(20.dp),
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

        // ================= 底部精简状态条 (~22%-26%): DeepSeek + Gemini + 运行摘要 =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：DeepSeek 精简余额栏
            Box(
                modifier = Modifier
                    .weight(if (antigravityAccounts.isEmpty()) 1.75f else 1.45f)
                    .clip(RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
                    .clickable {
                        val firstDs = deepSeekAccounts.firstOrNull()
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
                                    dsStatus == QuotaSyncStatus.AUTH_EXPIRED || dsSnap?.isAvailable == false ->
                                        if (isNightMode) TechRose else InkWarningRed
                                    valueText.contains("余额为零") || dsStatus == QuotaSyncStatus.CACHED_STALE || dsStatus == QuotaSyncStatus.CACHE_EXPIRED ->
                                        if (isNightMode) TechAmber else InkWarningAmber
                                    else -> primaryText
                                }
                                val timeStr = dsSnap?.formattedSuccessTime ?: dsSnap?.formattedQueriedAt
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isNightMode) DarkSurfaceElevated.copy(alpha = 0.6f) else InkSurfaceElevated.copy(alpha = 0.65f))
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
                                            fontSize = if (isCompact) 10.5.sp else 12.sp,
                                            fontWeight = FontWeight.Bold,
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

            // 右侧：Gemini API 精简状态 + 系统简要状态
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
                    .clickable {
                        val agAcc = antigravityAccounts.firstOrNull()
                        if (agAcc != null) onAccountClick(agAcc) else onOpenSettings()
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
                                provider = AiModelProvider.ANTIGRAVITY,
                                size = if (isCompact) 15.dp else 18.dp,
                                isNightMode = isNightMode
                            )
                            Text(
                                text = "Gemini API",
                                color = secondaryText,
                                fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${settings.autoRefreshIntervalMinutes}分钟同步",
                            color = mutedText,
                            fontSize = if (isCompact) 8.5.sp else 9.5.sp
                        )
                    }

                    if (antigravityAccounts.isEmpty()) {
                        Text(
                            text = "尚未绑定 Key · 常亮与自动夜间已就绪",
                            color = mutedText,
                            fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        val agAcc = antigravityAccounts.first()
                        val agSnap = snapshots[agAcc.id]
                        val agStatus = agSnap?.computeStatus(
                            isConfigured = agAcc.isConfigured,
                            isRefreshing = loadingMap[agAcc.id] == true,
                            refreshIntervalMinutes = settings.autoRefreshIntervalMinutes
                        ) ?: if (agAcc.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED
                        val isAgDemo = agSnap?.isMock == true && agSnap.errorMessage == "暂无演示数据"
                        val agStatusText = if (isAgDemo) "暂无演示数据" else when (agStatus) {
                            QuotaSyncStatus.NOT_CONFIGURED -> "待配置 Key"
                            QuotaSyncStatus.AWAITING_SYNC -> "等待同步"
                            QuotaSyncStatus.SYNCING -> "同步中"
                            QuotaSyncStatus.UPDATED -> "连接正常"
                            QuotaSyncStatus.CACHED_STALE -> "缓存 · 连接异常"
                            QuotaSyncStatus.CACHE_EXPIRED -> "缓存已过期"
                            QuotaSyncStatus.AUTH_EXPIRED -> "Key失效"
                            QuotaSyncStatus.SERVICE_UNAVAILABLE -> "暂不可用"
                            QuotaSyncStatus.DATA_FORMAT_ERROR -> "数据格式异常"
                        }
                        val agColor = when (agStatus) {
                            QuotaSyncStatus.UPDATED -> if (isNightMode) TechEmerald else InkStatusGreen
                            QuotaSyncStatus.AUTH_EXPIRED, QuotaSyncStatus.CACHE_EXPIRED -> if (isNightMode) TechRose else InkWarningRed
                            QuotaSyncStatus.NOT_CONFIGURED -> mutedText
                            else -> if (isNightMode) TechAmber else InkWarningAmber
                        }
                        val verifiedTime = agSnap?.formattedVerifiedTime ?: agSnap?.formattedQueriedAt
                        val detailSuffix = if (agStatus != QuotaSyncStatus.NOT_CONFIGURED && agStatus != QuotaSyncStatus.AUTH_EXPIRED && verifiedTime != null) {
                            " · 验证于 $verifiedTime"
                        } else ""

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isNightMode) DarkSurfaceElevated.copy(alpha = 0.6f) else InkSurfaceElevated.copy(alpha = 0.65f))
                                .padding(horizontal = 7.dp, vertical = if (isCompact) 3.5.dp else 4.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = agAcc.displayTitle,
                                color = secondaryText,
                                fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "$agStatusText$detailSuffix",
                                color = agColor,
                                fontSize = if (isCompact) 9.5.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isNightMode) DarkSurface else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary

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
        QuotaSyncStatus.AWAITING_SYNC -> if (isNightMode) TechAmber else InkWarningAmber
        QuotaSyncStatus.SYNCING -> if (isNightMode) TechCyan else InkProgress
        QuotaSyncStatus.UPDATED -> if (isNightMode) TechEmerald else InkStatusGreen
        QuotaSyncStatus.CACHED_STALE -> if (isNightMode) TechAmber else InkWarningAmber
        QuotaSyncStatus.CACHE_EXPIRED, QuotaSyncStatus.AUTH_EXPIRED -> if (isNightMode) TechRose else InkWarningRed
        QuotaSyncStatus.SERVICE_UNAVAILABLE, QuotaSyncStatus.DATA_FORMAT_ERROR -> if (isNightMode) TechAmber else InkWarningAmber
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
                            color = if (isNightMode) TechAmber else InkWarningAmber,
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
                            .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated)
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
                                .background(if (isNightMode) TechCyan.copy(alpha = 0.14f) else InkProgressTrack)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "重置:${resetCredits}次",
                                color = if (isNightMode) TechCyan else InkProgress,
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
                        .background(if (isNightMode) TechRose.copy(alpha = 0.08f) else InkWarningRed.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "登录凭据已失效 · 点击重新授权",
                        color = if (isNightMode) TechRose else InkWarningRed,
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
                        .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未配置账号 · 点击进行连接授权",
                        color = secondaryText,
                        fontSize = if (isCompact) 12.sp else 14.sp
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
    modifier: Modifier = Modifier
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary
    val trackColor = if (isNightMode) DarkProgressTrack else InkProgressTrack
    val panelBg = if (isNightMode) DarkSurfaceElevated.copy(alpha = 0.72f) else InkSurfaceElevated.copy(alpha = 0.75f)

    val remaining = window?.remainingPercent
    val isExhausted = remaining != null && remaining <= 0.0
    val isLow = remaining != null && remaining in 0.01..15.0

    val barColor = if (isNightMode) {
        when {
            isExhausted -> TechRose
            isLow -> TechAmber
            else -> TechEmerald
        }
    } else {
        when {
            isExhausted -> InkWarningRed
            isLow -> InkWarningAmber
            else -> InkProgress
        }
    }

    val percentColor = when {
        isExhausted -> if (isNightMode) TechRose else InkWarningRed
        isLow -> if (isNightMode) TechAmber else InkWarningAmber
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

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
            .background(panelBg)
            .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 6.dp else 9.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
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
