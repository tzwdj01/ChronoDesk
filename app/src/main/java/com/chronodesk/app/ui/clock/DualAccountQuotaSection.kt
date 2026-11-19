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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
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
import com.chronodesk.app.ui.theme.AppleNightBorder
import com.chronodesk.app.ui.theme.AppleNightRedMuted
import com.chronodesk.app.ui.theme.AppleNightRedPrimary
import com.chronodesk.app.ui.theme.AppleNightRedSecondary
import com.chronodesk.app.ui.theme.AppleNightSurface
import com.chronodesk.app.ui.theme.AppleNightSurfaceElevated
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

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
    onAddAccount: () -> Unit = {},
    onRefreshAccount: (accountId: String) -> Unit = {},
    onDeleteAccount: (accountId: String) -> Unit = {},
    onAccountClick: (account: CodexAccount) -> Unit = {},
    onRefreshAll: () -> Unit = {},
    onToggleNightMode: () -> Unit = {},
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

    val buttonBg = if (isNightMode) AppleNightSurface else DarkSurface
    val buttonBorder = if (isNightMode) AppleNightBorder else DarkBorder
    val buttonText = if (isNightMode) AppleNightRedPrimary else TextPrimary
    val buttonIcon = if (isNightMode) AppleNightRedPrimary else TechCyan

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 顶部操作行：标题、账号数、以及呼出时才显示的按钮组（添加、全部刷新、日夜切换、设置）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "CODEX 额度监控",
                    color = if (isNightMode) AppleNightRedPrimary else TechCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                val configuredCount = accounts.count { it.isConfigured }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isNightMode) AppleNightSurfaceElevated else DarkSurfaceElevated)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${configuredCount}个账号",
                        color = if (isNightMode) AppleNightRedSecondary else TextTertiary,
                        fontSize = 9.sp
                    )
                }
            }

            // 右侧区域：呼出时显示操作按钮；常态下展示轻触提示
            if (areControlsVisible) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 添加账号按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(buttonBg)
                            .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                            .clickable { onAddAccount() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加账号",
                                tint = buttonIcon,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "添加",
                                color = buttonText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // 全部刷新按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(buttonBg)
                            .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                            .clickable(enabled = !isRefreshingAll) { onRefreshAll() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "全部刷新",
                                tint = if (isRefreshingAll) (if (isNightMode) AppleNightRedPrimary else TechCyan) else buttonText,
                                modifier = Modifier
                                    .size(12.dp)
                                    .rotate(if (isRefreshingAll) spinAngle else 0f)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "刷新",
                                color = buttonText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // 日夜切换快捷按钮
                    val nightIcon = when (nightModeOption) {
                        "FORCE_DAY" -> Icons.Default.LightMode
                        "FORCE_NIGHT" -> Icons.Default.Nightlight
                        else -> Icons.Default.DarkMode
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(buttonBg)
                            .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                            .clickable { onToggleNightMode() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = nightIcon,
                            contentDescription = "日夜模式切换",
                            tint = buttonIcon,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // 设置按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(buttonBg)
                            .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                            .clickable { onSettingsClick() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = buttonText,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "轻触呼出",
                    color = if (isNightMode) AppleNightRedMuted else TextTertiary,
                    fontSize = 9.5.sp
                )
            }
        }

        // 账号列表卡片
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { account ->
                CodexQuotaCard(
                    account = account,
                    snapshot = snapshots[account.id],
                    isLoading = loadingMap[account.id] == true,
                    isNightMode = isNightMode,
                    areControlsVisible = areControlsVisible,
                    onRefreshClick = { onRefreshAccount(account.id) },
                    onDeleteClick = { onDeleteAccount(account.id) },
                    onCardClick = { onAccountClick(account) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// 兼容老版本的 DualAccountQuotaSection 别名
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
    val accounts = listOfNotNull(accountA, accountB)
    val snapshots = mutableMapOf<String, CodexUsageSnapshot>()
    val loadingMap = mutableMapOf<String, Boolean>()
    accountA?.let {
        snapshotA?.let { s -> snapshots[it.id] = s }
        loadingMap[it.id] = isLoadingA
    }
    accountB?.let {
        snapshotB?.let { s -> snapshots[it.id] = s }
        loadingMap[it.id] = isLoadingB
    }

    MultiAccountQuotaSection(
        accounts = if (accounts.isEmpty()) listOf(CodexAccount.createPlaceholder(), CodexAccount.createPlaceholder()) else accounts,
        snapshots = snapshots,
        loadingMap = loadingMap,
        isRefreshingAll = isRefreshingAll,
        nightModeOption = "AUTO",
        modifier = modifier,
        isNightMode = isNightMode,
        areControlsVisible = true,
        onAddAccount = onAccountBClick,
        onRefreshAccount = { id -> if (id == accountA?.id) onRefreshA() else onRefreshB() },
        onDeleteAccount = {},
        onAccountClick = { acc -> if (acc.id == accountA?.id) onAccountAClick() else onAccountBClick() },
        onRefreshAll = onRefreshAll,
        onToggleNightMode = {},
        onSettingsClick = onSettingsClick
    )
}
