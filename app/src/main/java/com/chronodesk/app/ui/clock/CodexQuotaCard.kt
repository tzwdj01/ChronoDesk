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
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkProgressTrack
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkProgress
import com.chronodesk.app.ui.theme.InkProgressTrack
import com.chronodesk.app.ui.theme.InkStatusGreen
import com.chronodesk.app.ui.theme.InkStatusTagBg
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
 * 极简精致 AI 模型额度监控卡片（支持 ChatGPT, DeepSeek, Gemini, Claude）
 * 日间水墨风 / 夜间深色科技风自动适配，精简进度条长度，展示品牌 Logo
 */
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
    val cardBg = if (isNightMode) DarkSurface else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary

    val provider = AiModelProvider.fromCode(snapshot?.modelProvider ?: account.modelProvider)

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

    // 计算主额度窗口与进度
    val window = snapshot?.primaryWindow
    val remainingPercent = window?.remainingPercent ?: 100.0
    val progressFraction = (remainingPercent / 100.0).toFloat().coerceIn(0f, 1f)

    val progressColor = if (isNightMode) {
        when {
            remainingPercent > 30.0 -> TechEmerald
            remainingPercent > 15.0 -> TechAmber
            else -> TechRose
        }
    } else {
        InkProgress
    }
    val trackColor = if (isNightMode) DarkProgressTrack else InkProgressTrack

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 12.dp else 14.dp,
                vertical = if (isNightMode) (if (isCompact) 5.5.dp else 7.dp) else (if (isCompact) 7.dp else 8.5.dp)
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(if (isNightMode) 3.dp else (if (isCompact) 5.dp else 6.5.dp))
        ) {
            // 第一行：模型 Logo + 模型名称 + 套餐标签 + 状态标签 + 呼出控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 模型提供商 Logo
                    ModelProviderLogo(
                        provider = provider,
                        size = if (isCompact) 28.dp else 30.dp,
                        isNightMode = isNightMode
                    )

                    // 模型名称 (例如: ChatGPT Plus, DeepSeek, Gemini, Claude)
                    val modelTitle = when (provider) {
                        AiModelProvider.CHATGPT -> snapshot?.accountAlias?.takeIf { it.isNotBlank() && !it.contains("@") } ?: "ChatGPT Plus"
                        AiModelProvider.DEEPSEEK -> "DeepSeek"
                        AiModelProvider.ANTIGRAVITY -> "Google Antigravity"
                        AiModelProvider.GEMINI -> "Gemini"
                        AiModelProvider.CLAUDE -> "Claude"
                    }

                    Text(
                        text = modelTitle,
                        color = primaryText,
                        fontSize = if (isCompact) 13.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 套餐标签 (例如: Plus) - 仅日间水墨模式且有真实套餐标识时展示
                    val planTag = snapshot?.planName ?: provider.defaultPlan
                    if (!isNightMode && !planTag.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(InkSurfaceElevated)
                                .padding(horizontal = 4.5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = planTag,
                                color = secondaryText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 右侧：状态徽标与操作控制
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
                        else -> "正常"
                    }
                    val statusColor = if (isNightMode) {
                        when {
                            isLoading -> TechCyan
                            account.credentialStatus == CredentialStatus.EXPIRED -> TechRose
                            else -> TechEmerald
                        }
                    } else {
                        InkStatusGreen
                    }
                    val statusBg = if (isNightMode) {
                        statusColor.copy(alpha = 0.12f)
                    } else {
                        InkStatusTagBg
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBg)
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 呼出时的单卡刷新与删除
                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onRefreshClick,
                                enabled = !isLoading,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新",
                                    tint = if (isLoading) (if (isNightMode) TechCyan else InkProgress) else secondaryText,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .rotate(if (isLoading) spinAngle else 0f)
                                )
                            }
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 卡片内容展示区（根据日间/夜间视觉设计分别排版）
            if (isNightMode) {
                // 夜间深色科技风设计：额度文本 -> 贯通微光翡翠绿进度条 -> 重置倒计时
                Text(
                    text = "额度 ${remainingPercent.toInt()}% (剩余 ${remainingPercent.toInt()}%)",
                    color = secondaryText,
                    fontSize = if (isCompact) 10.sp else 10.5.sp,
                    fontWeight = FontWeight.Normal
                )

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp)),
                    color = progressColor,
                    trackColor = trackColor
                )

                val countdown = window?.formattedCountdown() ?: "5小时后"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🕒 重置: ${countdown}后",
                        color = mutedText,
                        fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                        maxLines = 1
                    )
                }
            } else {
                // 日间水墨墨水屏设计：左侧进度条与额度标签，右侧上下排列剩余比例与重置时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = "5小时额度",
                            color = secondaryText,
                            fontSize = if (isCompact) 10.sp else 11.sp
                        )

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .width(if (isCompact) 85.dp else 115.dp)
                                .height(5.5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = trackColor
                        )
                    }

                    val countdown = window?.formattedCountdown() ?: "5小时后"
                    val resetText = if (countdown.contains("小时") || countdown.contains("分")) {
                        "重置: $countdown"
                    } else {
                        "重置: 每日 00:00"
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "剩余 ${remainingPercent.toInt()}%",
                            color = primaryText,
                            fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = resetText,
                            color = mutedText,
                            fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
