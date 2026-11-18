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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

@Composable
fun DualAccountQuotaSection(
    accountA: CodexAccount?,
    snapshotA: CodexUsageSnapshot?,
    isLoadingA: Boolean,
    accountB: CodexAccount?,
    snapshotB: CodexUsageSnapshot?,
    isLoadingB: Boolean,
    isRefreshingAll: Boolean,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    onRefreshA: () -> Unit = {},
    onRefreshB: () -> Unit = {},
    onRefreshAll: () -> Unit = {},
    onAccountAClick: () -> Unit = {},
    onAccountBClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinAll")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngleAll"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 顶部操作行：标题、全部刷新、设置
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CODEX 额度监控",
                    color = if (isNightMode) Color(0xFF666666) else TechCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 全部刷新按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isNightMode) Color(0xFF111111) else DarkSurface)
                        .border(1.dp, if (isNightMode) Color(0xFF1C1C1C) else DarkBorder, RoundedCornerShape(6.dp))
                        .clickable(enabled = !isRefreshingAll) { onRefreshAll() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "全部刷新",
                            tint = if (isRefreshingAll) TechCyan else TextSecondary,
                            modifier = Modifier
                                .size(12.dp)
                                .then(if (isRefreshingAll) Modifier.rotate(spinAngle) else Modifier)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRefreshingAll) "刷新中…" else "全部刷新",
                            color = if (isNightMode) Color(0xFF666666) else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 设置按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isNightMode) Color(0xFF111111) else DarkSurface)
                        .border(1.dp, if (isNightMode) Color(0xFF1C1C1C) else DarkBorder, RoundedCornerShape(6.dp))
                        .clickable { onSettingsClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = if (isNightMode) Color(0xFF666666) else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 账号 A 卡片
        CodexQuotaCard(
            slotLabel = "账号 A",
            account = accountA,
            snapshot = snapshotA,
            isLoading = isLoadingA || isRefreshingAll,
            isNightMode = isNightMode,
            onRefreshClick = onRefreshA,
            onCardClick = onAccountAClick
        )

        // 账号 B 卡片
        CodexQuotaCard(
            slotLabel = "账号 B",
            account = accountB,
            snapshot = snapshotB,
            isLoading = isLoadingB || isRefreshingAll,
            isNightMode = isNightMode,
            onRefreshClick = onRefreshB,
            onCardClick = onAccountBClick
        )
    }
}
