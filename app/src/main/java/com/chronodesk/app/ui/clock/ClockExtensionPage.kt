package com.chronodesk.app.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.ClockSettings
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
 * 第二页（扩展页面）：AI 模型 Token 用量监控、官方接口遥测与系统运行状态
 */
@Composable
fun ClockExtensionPage(
    accounts: List<CodexAccount> = emptyList(),
    snapshots: Map<String, CodexUsageSnapshot> = emptyMap(),
    settings: ClockSettings,
    isNightMode: Boolean,
    onRefreshAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onManageAccount: () -> Unit = {},
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // 第一行：页面标题与控制胶囊
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AI 额度监控与接口状态",
                    color = primaryText,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "扩展页 · 滑动返回主页",
                        color = secondaryText,
                        fontSize = 10.sp
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 快速工具条 (触控区域 >= 48dp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onRefreshAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "全量同步",
                                tint = if (isNightMode) TechCyan else primaryText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "全量同步", color = primaryText, fontSize = 11.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onManageAccount() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "接口配置",
                                tint = if (isNightMode) TechCyan else primaryText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "接口配置", color = primaryText, fontSize = 11.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "高级设置",
                                tint = primaryText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "高级设置", color = primaryText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 第二部分：ChatGPT 多账号 Token 监控卡片 与 DeepSeek 官方 API 计费卡片 并列
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 左列：ChatGPT 多账号 Token 消耗统计
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(13.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            ModelProviderLogo(provider = AiModelProvider.CHATGPT, size = 22.dp, isNightMode = isNightMode)
                            Text(
                                text = "OpenAI / ChatGPT 额度与窗口详情",
                                color = primaryText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(text = "Codex Wham API", color = mutedText, fontSize = 9.sp)
                    }

                    chatGptAccounts.forEachIndexed { idx, acc ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = if (isNightMode) DarkBorder.copy(alpha = 0.5f) else InkBorderSubtle,
                                thickness = 0.8.dp
                            )
                        }
                        val snap = snapshots[acc.id]
                        val pw = snap?.primaryWindow
                        val sw = snap?.secondaryWindow
                        val syncStatus = snap?.computeStatus(
                            isConfigured = acc.isConfigured,
                            isRefreshing = false,
                            refreshIntervalMinutes = settings.autoRefreshIntervalMinutes
                        ) ?: if (acc.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

                        val statusDesc = when (syncStatus) {
                            QuotaSyncStatus.NOT_CONFIGURED -> "未配置"
                            QuotaSyncStatus.AWAITING_SYNC -> "等待同步"
                            QuotaSyncStatus.SYNCING -> "同步中"
                            QuotaSyncStatus.UPDATED -> "已更新 · ${snap?.formattedSuccessTime ?: snap?.formattedQueriedAt ?: "刚刚"}"
                            QuotaSyncStatus.CACHED_STALE -> if (snap?.lastErrorType == SyncErrorType.ACCESS_DENIED) "访问受限" else "缓存 · ${snap?.errorMessage?.take(6) ?: "异常"}"
                            QuotaSyncStatus.CACHE_EXPIRED -> "缓存已过期"
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

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(if (isNightMode) (if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) TechRose else TechCyan) else (if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) InkWarningRed else InkStatusGreen), CircleShape)
                                    )
                                    Text(
                                        text = acc.displayTitle,
                                        color = primaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = statusDesc,
                                    color = statusColor,
                                    fontSize = 8.5.sp
                                )
                            }

                            if (syncStatus == QuotaSyncStatus.AUTH_EXPIRED) {
                                Text(
                                    text = "登录凭据已失效，请重新授权",
                                    color = if (isNightMode) TechRose else InkWarningRed,
                                    fontSize = 9.5.sp
                                )
                            } else if (syncStatus == QuotaSyncStatus.NOT_CONFIGURED) {
                                Text(
                                    text = "未配置账号 · 点击主页卡片进行连接",
                                    color = mutedText,
                                    fontSize = 9.5.sp
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val pwLabel = pw?.windowLabel ?: "额度窗口"
                                    val pwRemaining = pw?.formattedRemaining() ?: "—"
                                    val pwReset = pw?.formattedResetTime() ?: "重置时间未知"
                                    Text(
                                        text = "$pwLabel: 剩余 $pwRemaining · $pwReset",
                                        color = secondaryText,
                                        fontSize = 9.5.sp
                                    )
                                    if (sw != null) {
                                        val swLabel = sw.windowLabel ?: "额度窗口"
                                        val swRemaining = sw.formattedRemaining()
                                        val swReset = sw.formattedResetTime()
                                        Text(
                                            text = "$swLabel: 剩余 $swRemaining · $swReset",
                                            color = mutedText,
                                            fontSize = 9.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 右列：DeepSeek 官方 API 真实余额与计费 (支持多账号)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(13.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            ModelProviderLogo(provider = AiModelProvider.DEEPSEEK, size = 22.dp, isNightMode = isNightMode)
                            Text(
                                text = "DeepSeek 官方 API",
                                color = primaryText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (deepSeekAccounts.size > 1) "${deepSeekAccounts.size}个账号" else "直连 /user/balance",
                            color = mutedText,
                            fontSize = 9.sp
                        )
                    }

                    if (deepSeekAccounts.isEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "尚未绑定 DeepSeek API Key", color = secondaryText, fontSize = 11.sp)
                            Text(text = "在账号设置中填入 API Key 即可直连官方查询真实余额与明细", color = mutedText, fontSize = 9.sp)
                        }
                    } else {
                        deepSeekAccounts.forEachIndexed { index, dsAcc ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = if (isNightMode) DarkBorder.copy(alpha = 0.5f) else InkBorderSubtle,
                                    thickness = 0.8.dp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            val dsSnap = snapshots[dsAcc.id]
                            val dsSyncStatus = dsSnap?.computeStatus(
                                isConfigured = dsAcc.isConfigured,
                                isRefreshing = false,
                                refreshIntervalMinutes = settings.autoRefreshIntervalMinutes
                            ) ?: if (dsAcc.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dsAcc.displayTitle,
                                        color = primaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val isDsDemo = dsSnap?.isMock == true && dsSnap.errorMessage == "暂无演示数据"
                                    val dsStatusText = if (isDsDemo) "暂无演示数据" else when (dsSyncStatus) {
                                        QuotaSyncStatus.NOT_CONFIGURED -> "未配置"
                                        QuotaSyncStatus.AWAITING_SYNC -> "等待同步"
                                        QuotaSyncStatus.SYNCING -> "同步中"
                                        QuotaSyncStatus.UPDATED -> if (dsSnap?.isAvailable == false) "服务不可用" else if (dsSnap?.creditsBalance != null && dsSnap.creditsBalance <= 0.0) "余额为零" else "正常可用"
                                        QuotaSyncStatus.CACHED_STALE -> "缓存 · ${dsSnap?.errorMessage?.take(6) ?: "异常"}"
                                        QuotaSyncStatus.CACHE_EXPIRED -> "缓存已过期"
                                        QuotaSyncStatus.AUTH_EXPIRED -> "Key失效"
                                        QuotaSyncStatus.SERVICE_UNAVAILABLE -> "暂不可用"
                                        QuotaSyncStatus.DATA_FORMAT_ERROR -> "数据格式异常"
                                    }
                                    val dsStatusColor = when (dsSyncStatus) {
                                        QuotaSyncStatus.NOT_CONFIGURED -> mutedText
                                        QuotaSyncStatus.AWAITING_SYNC -> if (isNightMode) TechAmber else InkWarningAmber
                                        QuotaSyncStatus.SYNCING -> if (isNightMode) TechCyan else InkProgress
                                        QuotaSyncStatus.UPDATED -> if (dsSnap?.isAvailable == false) (if (isNightMode) TechRose else InkWarningRed) else if (dsSnap?.creditsBalance != null && dsSnap.creditsBalance <= 0.0) (if (isNightMode) TechAmber else InkWarningAmber) else (if (isNightMode) TechEmerald else InkStatusGreen)
                                        QuotaSyncStatus.CACHED_STALE -> if (isNightMode) TechAmber else InkWarningAmber
                                        QuotaSyncStatus.CACHE_EXPIRED, QuotaSyncStatus.AUTH_EXPIRED -> if (isNightMode) TechRose else InkWarningRed
                                        QuotaSyncStatus.SERVICE_UNAVAILABLE, QuotaSyncStatus.DATA_FORMAT_ERROR -> if (isNightMode) TechAmber else InkWarningAmber
                                    }
                                    Text(
                                        text = dsStatusText,
                                        color = dsStatusColor,
                                        fontSize = 9.sp
                                    )
                                }

                                if (dsSyncStatus == QuotaSyncStatus.NOT_CONFIGURED) {
                                    Text(text = "未配置 API Key · 点击主页卡片进行配置", color = mutedText, fontSize = 9.5.sp)
                                } else if (dsSyncStatus == QuotaSyncStatus.AUTH_EXPIRED) {
                                    Text(text = "API Key 鉴权失败 · 点击主页卡片重新授权", color = if (isNightMode) TechRose else InkWarningRed, fontSize = 9.5.sp)
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = dsSnap?.formattedBalanceText ?: "暂无余额数据",
                                            color = primaryText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val timeStr = dsSnap?.formattedSuccessTime ?: dsSnap?.formattedQueriedAt ?: "刚刚"
                                        val timeSuffix = if (dsSyncStatus == QuotaSyncStatus.CACHED_STALE || dsSyncStatus == QuotaSyncStatus.CACHE_EXPIRED) " (缓存)" else ""
                                        Text(
                                            text = "更新: $timeStr$timeSuffix",
                                            color = mutedText,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 第三部分：Gemini API 官方接口状态卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                .padding(13.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        ModelProviderLogo(provider = AiModelProvider.ANTIGRAVITY, size = 22.dp, isNightMode = isNightMode)
                        Text(
                            text = "Gemini API 连接状态",
                            color = primaryText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (antigravityAccounts.size > 1) "${antigravityAccounts.size}个账号" else "Google Generative Language API",
                        color = mutedText,
                        fontSize = 9.sp
                    )
                }

                if (antigravityAccounts.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "尚未绑定 Gemini API Key", color = secondaryText, fontSize = 11.sp)
                        Text(text = "在账号管理中填入 API Key 即可直连官方验证接口连接状态", color = mutedText, fontSize = 9.sp)
                    }
                } else {
                    antigravityAccounts.forEachIndexed { index, agAcc ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = if (isNightMode) DarkBorder.copy(alpha = 0.5f) else InkBorderSubtle,
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        val agSnap = snapshots[agAcc.id]
                        val agSyncStatus = agSnap?.computeStatus(
                            isConfigured = agAcc.isConfigured,
                            isRefreshing = false,
                            refreshIntervalMinutes = settings.autoRefreshIntervalMinutes
                        ) ?: if (agAcc.isConfigured) QuotaSyncStatus.AWAITING_SYNC else QuotaSyncStatus.NOT_CONFIGURED

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(
                                                if (isNightMode) {
                                                    if (agSyncStatus == QuotaSyncStatus.UPDATED) TechCyan else if (agSyncStatus == QuotaSyncStatus.AUTH_EXPIRED) TechRose else TechAmber
                                                } else {
                                                    if (agSyncStatus == QuotaSyncStatus.UPDATED) InkStatusGreen else if (agSyncStatus == QuotaSyncStatus.AUTH_EXPIRED) InkWarningRed else InkWarningAmber
                                                },
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = agAcc.displayTitle,
                                        color = primaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                val isAgDemo = agSnap?.isMock == true && agSnap.errorMessage == "暂无演示数据"
                                val agStatusText = if (isAgDemo) "暂无演示数据" else when (agSyncStatus) {
                                    QuotaSyncStatus.NOT_CONFIGURED -> "待配置 API Key"
                                    QuotaSyncStatus.AWAITING_SYNC -> "等待同步"
                                    QuotaSyncStatus.SYNCING -> "同步中"
                                    QuotaSyncStatus.UPDATED -> "连接正常"
                                    QuotaSyncStatus.CACHED_STALE -> "缓存 · 连接异常"
                                    QuotaSyncStatus.CACHE_EXPIRED -> "缓存已过期"
                                    QuotaSyncStatus.AUTH_EXPIRED -> "Key失效"
                                    QuotaSyncStatus.SERVICE_UNAVAILABLE -> "暂不可用"
                                    QuotaSyncStatus.DATA_FORMAT_ERROR -> "数据格式异常"
                                }
                                val agStatusColor = when (agSyncStatus) {
                                    QuotaSyncStatus.NOT_CONFIGURED -> mutedText
                                    QuotaSyncStatus.UPDATED -> if (isNightMode) TechEmerald else InkStatusGreen
                                    QuotaSyncStatus.AUTH_EXPIRED, QuotaSyncStatus.CACHE_EXPIRED -> if (isNightMode) TechRose else InkWarningRed
                                    else -> if (isNightMode) TechAmber else InkWarningAmber
                                }
                                Text(
                                    text = agStatusText,
                                    color = agStatusColor,
                                    fontSize = 8.5.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val desc = when (agSyncStatus) {
                                    QuotaSyncStatus.NOT_CONFIGURED -> "未配置 API Key · 点击主页卡片进行配置"
                                    QuotaSyncStatus.AUTH_EXPIRED -> "API Key 鉴权失败 · 点击主页卡片重新授权"
                                    else -> "模型接口调用就绪"
                                }
                                Text(
                                    text = desc,
                                    color = secondaryText,
                                    fontSize = 9.5.sp
                                )
                                if (agSyncStatus != QuotaSyncStatus.NOT_CONFIGURED && agSyncStatus != QuotaSyncStatus.AUTH_EXPIRED) {
                                    val verifiedTime = agSnap?.formattedVerifiedTime ?: agSnap?.formattedQueriedAt ?: "刚刚"
                                    val timeSuffix = if (agSyncStatus == QuotaSyncStatus.CACHED_STALE || agSyncStatus == QuotaSyncStatus.CACHE_EXPIRED) " (缓存)" else ""
                                    Text(
                                        text = "验证于 $verifiedTime$timeSuffix",
                                        color = mutedText,
                                        fontSize = 9.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 第四部分：系统与桌面 Dock 状态遥测
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryItem("屏幕保活", "常亮已启用 (OLED 防烧屏)", isNightMode)
                Text(text = "·", color = mutedText)
                TelemetryItem("夜间模式", "自适应时间 (22:00-07:00)", isNightMode)
                Text(text = "·", color = mutedText)
                TelemetryItem("刷新周期", "${settings.autoRefreshIntervalMinutes} 分钟自动同步", isNightMode)
                Text(text = "·", color = mutedText)
                TelemetryItem("当前城市", "${settings.cityName} (自动定位)", isNightMode)
            }
        }
    }
}

@Composable
private fun TelemetryItem(
    title: String,
    value: String,
    isNightMode: Boolean
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextTertiary else InkTextSecondary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = secondaryText,
            fontSize = 9.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = primaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
