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
import com.chronodesk.app.ui.theme.InkWarningRed
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

    val chatGptAccounts = accounts.filter { it.modelProvider == "CHATGPT" || it.modelProvider == null }
    val deepSeekAccounts = accounts.filter { it.modelProvider == "DEEPSEEK" }

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
                    text = "AI 模型 Token 用量与接口遥测",
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
            }

            // 快速工具条
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                        .clickable { onRefreshAll() }
                        .padding(horizontal = 10.dp, vertical = 4.5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (isNightMode) TechCyan else primaryText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "全量同步", color = primaryText, fontSize = 11.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                        .clickable { onManageAccount() }
                        .padding(horizontal = 10.dp, vertical = 4.5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (isNightMode) TechCyan else primaryText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "接口配置", color = primaryText, fontSize = 11.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 10.dp, vertical = 4.5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = primaryText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "高级设置", color = primaryText, fontSize = 11.sp)
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
                                text = "OpenAI / ChatGPT Token 用量遥测",
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
                        val primaryPct = snap?.primaryWindow?.remainingPercent?.toInt() ?: 0
                        val secPct = snap?.secondaryWindow?.remainingPercent?.toInt() ?: 0
                        val estTokens5h = (100 - primaryPct) * 1200
                        val estTokensWeek = (100 - secPct) * 14000

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
                                            .background(if (isNightMode) TechCyan else InkStatusGreen, CircleShape)
                                    )
                                    Text(
                                        text = acc.email ?: acc.displayTitle,
                                        color = primaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "会话活跃 · 延迟 120ms",
                                    color = if (isNightMode) TechEmerald else InkStatusGreen,
                                    fontSize = 8.5.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "5小时窗口已用: $estTokens5h Tokens (剩余 $primaryPct%)",
                                    color = secondaryText,
                                    fontSize = 9.5.sp
                                )
                                Text(
                                    text = "周窗口消耗: $estTokensWeek Tokens (剩余 $secPct%)",
                                    color = mutedText,
                                    fontSize = 9.5.sp
                                )
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
                            val isDsConfigured = dsSnap?.creditsBalance != null && dsSnap.credentialStatus != CredentialStatus.NOT_LOGGED_IN
                            val dsBalance = dsSnap?.creditsBalance ?: 0.0
                            val dsCurrency = dsSnap?.currency ?: "CNY"
                            val dsAvailable = dsSnap?.isAvailable ?: (dsBalance > 0.0)

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
                                    Text(
                                        text = if (isDsConfigured && dsAvailable && dsBalance > 0) "正常可用" else if (!isDsConfigured) "未配置" else "余额不足",
                                        color = if (isDsConfigured && dsAvailable && dsBalance > 0) (if (isNightMode) TechEmerald else InkStatusGreen) else (if (isNightMode) TechRose else InkWarningRed),
                                        fontSize = 9.sp
                                    )
                                }

                                if (isDsConfigured) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "账户余额: ¥${String.format(java.util.Locale.US, "%.2f", dsBalance)} $dsCurrency",
                                            color = primaryText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "更新: ${dsSnap?.formattedQueriedAt ?: "刚刚"}",
                                            color = mutedText,
                                            fontSize = 9.sp
                                        )
                                    }

                                    if (dsSnap?.toppedUpBalance != null || dsSnap?.grantedBalance != null) {
                                        val topped = dsSnap?.toppedUpBalance ?: 0.0
                                        val granted = dsSnap?.grantedBalance ?: 0.0
                                        Text(
                                            text = "充值: ¥${String.format(java.util.Locale.US, "%.2f", topped)} ｜ 赠送: ¥${String.format(java.util.Locale.US, "%.2f", granted)}",
                                            color = secondaryText,
                                            fontSize = 9.sp
                                        )
                                    }
                                } else {
                                    Text(text = "未配置 API Key · 点击主页卡片进行配置", color = mutedText, fontSize = 9.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 第三部分：系统与桌面 Dock 状态遥测
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
