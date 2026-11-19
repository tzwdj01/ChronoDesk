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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.text.style.TextOverflow
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
    account: CodexAccount,
    snapshot: CodexUsageSnapshot?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    onRefreshClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onCardClick: () -> Unit = {}
) {
    val cardBg = if (isNightMode) Color(0xFF090909) else DarkSurface
    val cardBorder = if (isNightMode) Color(0xFF161616) else DarkBorder
    val primaryText = if (isNightMode) Color(0xFF777777) else TextPrimary
    val secondaryText = if (isNightMode) Color(0xFF555555) else TextSecondary

    // 未配置账号占位卡片
    if (!account.isConfigured) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                .clickable { onCardClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(TechCyan.copy(alpha = if (isNightMode) 0.1f else 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isNightMode) Color(0xFF666666) else TechCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "绑定 ChatGPT / Codex 账号",
                            color = primaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "支持 OpenAI 官方授权登录或导入 Token",
                            color = secondaryText,
                            fontSize = 10.sp
                        )
                    }
                }

                // 移除空槽位按钮（当有多余占位时）
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "移除",
                        tint = secondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        return
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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { onCardClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // 卡片头部：账号名称/Email、套餐、状态标签与操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 账号邮箱 / 显示名称
                    Text(
                        text = account.displayTitle,
                        color = if (isNightMode) Color(0xFF888888) else TechCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 套餐标签 (如 Plus, Pro 200, Free)
                    snapshot?.planName?.let { plan ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isNightMode) Color(0x22333333) else DarkSurfaceElevated)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = plan,
                                color = TextTertiary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 状态徽标
                    val statusText = when {
                        isLoading -> "同步中"
                        snapshot?.isLastGood == true -> "缓存"
                        account.credentialStatus == CredentialStatus.EXPIRED -> "需重登"
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
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 单账号刷新按钮
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !isLoading,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = if (isLoading) TechCyan else secondaryText,
                            modifier = Modifier
                                .size(13.dp)
                                .rotate(if (isLoading) spinAngle else 0f)
                        )
                    }

                    // 删除/移除账号按钮
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除账号",
                            tint = secondaryText.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // 5小时额度窗口
            val primaryWindow = snapshot?.primaryWindow
            QuotaProgressRow(
                label = "5小时额度",
                window = primaryWindow,
                isNightMode = isNightMode
            )

            // 周额度窗口
            val secondaryWindow = snapshot?.secondaryWindow
            QuotaProgressRow(
                label = "周额度",
                window = secondaryWindow,
                isNightMode = isNightMode
            )

            // 底部同步状态与最后更新时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (snapshot?.isLastGood == true && !snapshot.errorMessage.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TechAmber,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = snapshot.errorMessage ?: "",
                            color = TechAmber,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Text(
                    text = "同步: ${snapshot?.formattedQueriedAt ?: "未同步"}",
                    color = TextTertiary,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun QuotaProgressRow(
    label: String,
    window: CodexUsageWindow?,
    isNightMode: Boolean
) {
    val remainingPercent = window?.remainingPercent ?: 0.0
    val progressFraction = (remainingPercent / 100.0).toFloat().coerceIn(0f, 1f)

    val progressColor = when {
        isNightMode -> Color(0xFF666666)
        remainingPercent > 30.0 -> TechEmerald
        remainingPercent > 15.0 -> TechAmber
        else -> TechRose
    }
    val trackColor = if (isNightMode) Color(0xFF141414) else Color(0xFF1E222A)

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isNightMode) Color(0xFF555555) else TextSecondary,
                fontSize = 10.sp
            )

            if (window != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "剩余 ${remainingPercent.toInt()}%",
                        color = if (isNightMode) Color(0xFF777777) else progressColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val countdown = window.formattedCountdown()
                    if (countdown != "已就绪") {
                        Text(
                            text = "重置 $countdown",
                            color = TextTertiary,
                            fontSize = 9.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "暂无数据",
                    color = TextTertiary,
                    fontSize = 9.sp
                )
            }
        }

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = progressColor,
            trackColor = trackColor
        )
    }
}
