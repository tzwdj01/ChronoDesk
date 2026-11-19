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
import com.chronodesk.app.ui.theme.AppleNightBorder
import com.chronodesk.app.ui.theme.AppleNightRedMuted
import com.chronodesk.app.ui.theme.AppleNightRedPrimary
import com.chronodesk.app.ui.theme.AppleNightRedSecondary
import com.chronodesk.app.ui.theme.AppleNightSurface
import com.chronodesk.app.ui.theme.AppleNightSurfaceElevated
import com.chronodesk.app.ui.theme.AppleNightTrack
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
    areControlsVisible: Boolean = true,
    isCompact: Boolean = false,
    onRefreshClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onCardClick: () -> Unit = {}
) {
    val cardBg = if (isNightMode) AppleNightSurface else DarkSurface
    val cardBorder = if (isNightMode) AppleNightBorder else DarkBorder
    val primaryText = if (isNightMode) AppleNightRedPrimary else TextPrimary
    val secondaryText = if (isNightMode) AppleNightRedSecondary else TextSecondary
    val mutedText = if (isNightMode) AppleNightRedMuted else TextTertiary

    // 未配置账号占位卡片
    if (!account.isConfigured) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
                .clickable { onCardClick() }
                .padding(
                    horizontal = if (isCompact) 10.dp else 14.dp,
                    vertical = if (isCompact) 8.dp else 12.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 24.dp else 28.dp)
                            .clip(CircleShape)
                            .background(
                                (if (isNightMode) AppleNightRedPrimary else TechCyan)
                                    .copy(alpha = if (isNightMode) 0.15f else 0.18f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isNightMode) AppleNightRedPrimary else TechCyan,
                            modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "绑定 ChatGPT / Codex 账号",
                            color = primaryText,
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isCompact) "点击进行 OpenAI 授权或导入 Token" else "支持 OpenAI 官方授权登录或导入 Token",
                            color = secondaryText,
                            fontSize = if (isCompact) 9.sp else 10.sp
                        )
                    }
                }

                // 移除空槽位按钮（仅在呼出操作面板时显示）
                if (areControlsVisible) {
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
            .clip(RoundedCornerShape(if (isCompact) 11.dp else 13.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 11.dp else 13.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 11.dp else 14.dp,
                vertical = if (isCompact) 8.dp else 11.dp
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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
                    // 账号邮箱 / 显示名称 (优先显示真实邮箱)
                    val displayEmail = snapshot?.email?.takeIf { it.isNotBlank() } ?: account.displayTitle
                    Text(
                        text = displayEmail,
                        color = if (isNightMode) AppleNightRedPrimary else TechCyan,
                        fontSize = if (isCompact) 12.sp else 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 套餐标签 (如 Plus, Pro 200, Free)
                    snapshot?.planName?.let { plan ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isNightMode) AppleNightSurfaceElevated else DarkSurfaceElevated)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = plan,
                                color = if (isNightMode) AppleNightRedSecondary else TextTertiary,
                                fontSize = if (isCompact) 8.5.sp else 9.5.sp,
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
                        isNightMode -> {
                            when {
                                isLoading -> AppleNightRedPrimary
                                snapshot?.isLastGood == true -> AppleNightRedSecondary
                                account.credentialStatus == CredentialStatus.EXPIRED -> AppleNightRedPrimary
                                account.credentialStatus == CredentialStatus.VALID -> AppleNightRedSecondary
                                else -> AppleNightRedMuted
                            }
                        }
                        else -> {
                            when {
                                isLoading -> TechCyan
                                snapshot?.isLastGood == true -> TechAmber
                                account.credentialStatus == CredentialStatus.EXPIRED -> TechRose
                                account.credentialStatus == CredentialStatus.VALID -> TechEmerald
                                else -> TextTertiary
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = if (isNightMode) 0.2f else 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 仅当用户主动呼出控制时，显示单卡片刷新与删除按钮
                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 单账号刷新按钮
                            IconButton(
                                onClick = onRefreshClick,
                                enabled = !isLoading,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新",
                                    tint = if (isLoading) (if (isNightMode) AppleNightRedPrimary else TechCyan) else secondaryText,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .rotate(if (isLoading) spinAngle else 0f)
                                )
                            }

                            // 删除/移除账号按钮
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除账号",
                                    tint = secondaryText.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5小时额度窗口
            val primaryWindow = snapshot?.primaryWindow
            QuotaProgressRow(
                label = "5小时额度",
                window = primaryWindow,
                isNightMode = isNightMode,
                isCompact = isCompact
            )

            // 周额度窗口
            val secondaryWindow = snapshot?.secondaryWindow
            QuotaProgressRow(
                label = "周额度",
                window = secondaryWindow,
                isNightMode = isNightMode,
                isCompact = isCompact
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
                            tint = if (isNightMode) AppleNightRedPrimary else TechAmber,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = snapshot.errorMessage ?: "",
                            color = if (isNightMode) AppleNightRedSecondary else TechAmber,
                            fontSize = 8.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "⚡ 凭证有效 · 会话保活",
                        color = if (isNightMode) AppleNightRedMuted else TextTertiary,
                        fontSize = 8.5.sp
                    )
                }

                Text(
                    text = "● 同步: ${snapshot?.formattedQueriedAt ?: "未同步"}",
                    color = mutedText,
                    fontSize = 8.5.sp
                )
            }
        }
    }
}

@Composable
private fun QuotaProgressRow(
    label: String,
    window: CodexUsageWindow?,
    isNightMode: Boolean,
    isCompact: Boolean = false
) {
    val remainingPercent = window?.remainingPercent ?: 0.0
    val progressFraction = (remainingPercent / 100.0).toFloat().coerceIn(0f, 1f)

    val progressColor = when {
        isNightMode -> AppleNightRedPrimary
        remainingPercent > 30.0 -> TechEmerald
        remainingPercent > 15.0 -> TechAmber
        else -> TechRose
    }
    val trackColor = if (isNightMode) AppleNightTrack else Color(0xFF1E222A)

    Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.5.dp else 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isNightMode) AppleNightRedSecondary else TextSecondary,
                fontSize = if (isCompact) 10.sp else 11.sp
            )

            if (window != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "剩余 ${remainingPercent.toInt()}%",
                        color = if (isNightMode) AppleNightRedPrimary else progressColor,
                        fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val countdown = window.formattedCountdown()
                    if (countdown != "已就绪") {
                        Text(
                            text = "重置 $countdown",
                            color = if (isNightMode) AppleNightRedMuted else TextTertiary,
                            fontSize = if (isCompact) 8.5.sp else 9.5.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "暂无数据",
                    color = if (isNightMode) AppleNightRedMuted else TextTertiary,
                    fontSize = if (isCompact) 8.5.sp else 9.5.sp
                )
            }
        }

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 5.5.dp else 6.5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = trackColor
        )
    }
}
