package com.chronodesk.app.ui.clock

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

@Composable
fun CodexQuotaCard(
    slotLabel: String,
    account: CodexAccount?,
    snapshot: CodexUsageSnapshot?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    onRefreshClick: () -> Unit = {},
    onCardClick: () -> Unit = {}
) {
    val cardBg = if (isNightMode) Color(0xFF0A0A0A) else DarkSurface
    val cardBorder = if (isNightMode) Color(0xFF141414) else DarkBorder
    val primaryText = if (isNightMode) Color(0xFF666666) else TextPrimary
    val secondaryText = if (isNightMode) Color(0xFF444444) else TextSecondary

    if (account == null) {
        // 未配置账号占位卡片
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                .clickable { onCardClick() }
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = TechCyan.copy(alpha = if (isNightMode) 0.3f else 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "添加 $slotLabel",
                    color = secondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    // 旋转动画（用于刷新中状态）
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { onCardClick() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 卡片头部：槽位、别名、套餐、状态标签与刷新按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TechCyan.copy(alpha = if (isNightMode) 0.1f else 0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = slotLabel,
                            color = if (isNightMode) Color(0xFF666666) else TechCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = account.displayTitle,
                        color = primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    snapshot?.planName?.let { plan ->
                        Text(
                            text = plan,
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 状态徽标
                    val statusText = when {
                        isLoading -> "同步中"
                        snapshot?.isLastGood == true -> "缓存数据"
                        account.credentialStatus == CredentialStatus.EXPIRED -> "需重新登录"
                        account.credentialStatus == CredentialStatus.VALID -> "正常"
                        else -> "未同步"
                    }
                    val statusColor = when {
                        isLoading -> TechCyan
                        snapshot?.isLastGood == true -> TechAmber
                        account.credentialStatus == CredentialStatus.EXPIRED -> TechRose
                        account.credentialStatus == CredentialStatus.VALID -> TechEmerald
                        else -> TextTertiary
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(24.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = if (isLoading) TechCyan else secondaryText,
                            modifier = Modifier
                                .size(14.dp)
                                .then(if (isLoading) Modifier.rotate(spinAngle) else Modifier)
                        )
                    }
                }
            }

            // 5 小时限额条 (Primary Window)
            val primaryWindow = snapshot?.primaryWindow
            if (primaryWindow != null) {
                QuotaTierRow(
                    tier = primaryWindow,
                    isNightMode = isNightMode
                )
            } else if (snapshot?.credentialStatus == CredentialStatus.VALID && !isLoading) {
                Text(
                    text = "5小时额度: 暂无限制数据",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            // 周限额条 (Secondary Window)
            val secondaryWindow = snapshot?.secondaryWindow
            if (secondaryWindow != null) {
                QuotaTierRow(
                    tier = secondaryWindow,
                    isNightMode = isNightMode
                )
            }

            // 错误提示（若处于 LastGood 状态或 Expired 状态）
            if (!snapshot?.errorMessage.isNullOrBlank() && !isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            (if (account.credentialStatus == CredentialStatus.EXPIRED) TechRose else TechAmber).copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (account.credentialStatus == CredentialStatus.EXPIRED) TechRose else TechAmber,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = snapshot?.errorMessage ?: "",
                        color = if (account.credentialStatus == CredentialStatus.EXPIRED) TechRose else TechAmber,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // 卡片底部：上次同步时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "同步: ${snapshot?.formattedQueriedAt ?: "从未"}",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (snapshot?.isLastGood == true) {
                    Text(
                        text = "保留上次成功数据",
                        color = TechAmber.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotaTierRow(
    tier: CodexUsageWindow,
    isNightMode: Boolean
) {
    val remaining = tier.remainingPercent
    val progressFraction = (remaining / 100.0).toFloat().coerceIn(0f, 1f)

    val progressColor = when {
        isNightMode -> Color(0xFF555555)
        remaining >= 35.0 -> TechEmerald
        remaining >= 15.0 -> TechAmber
        else -> TechRose
    }

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tier.label,
                color = if (isNightMode) Color(0xFF666666) else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "剩余 ${remaining.toInt()}%",
                    color = progressColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "重置 ${tier.formattedCountdown()}",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = if (isNightMode) Color(0xFF161616) else DarkSurfaceElevated
        )
    }
}
