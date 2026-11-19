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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 顶部操作行：标题、账号数、添加按钮、全部刷新、日夜切换、设置
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
                    color = if (isNightMode) Color(0xFF666666) else TechCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                val configuredCount = accounts.count { it.isConfigured }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isNightMode) Color(0x22333333) else DarkSurfaceElevated)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${configuredCount}个账号",
                        color = TextTertiary,
                        fontSize = 9.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 添加账号按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isNightMode) Color(0xFF111111) else DarkSurface)
                        .border(1.dp, if (isNightMode) Color(0xFF1E1E1E) else DarkBorder, RoundedCornerShape(6.dp))
                        .clickable { onAddAccount() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加账号",
                            tint = if (isNightMode) Color(0xFF777777) else TechCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "添加",
                            color = if (isNightMode) Color(0xFF777777) else TextPrimary,
                            fontSize = 11.sp
                        )
                    }
                }

                // 全部刷新按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isNightMode) Color(0xFF111111) else DarkSurface)
                        .border(1.dp, if (isNightMode) Color(0xFF1E1E1E) else DarkBorder, RoundedCornerShape(6.dp))
                        .clickable(enabled = !isRefreshingAll) { onRefreshAll() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "全部刷新",
                            tint = if (isRefreshingAll) TechCyan else (if (isNightMode) Color(0xFF777777) else TextPrimary),
                            modifier = Modifier
                                .size(12.dp)
                                .rotate(if (isRefreshingAll) spinAngle else 0f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "刷新",
                            color = if (isNightMode) Color(0xFF777777) else TextPrimary,
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
                        .background(if (isNightMode) Color(0xFF111111) else DarkSurface)
                        .border(1.dp, if (isNightMode) Color(0xFF1E1E1E) else DarkBorder, RoundedCornerShape(6.dp))
                        .clickable { onToggleNightMode() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = nightIcon,
                        contentDescription = "日夜模式切换",
                        tint = if (isNightMode) Color(0xFF777777) else TextPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // 设置按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isNightMode) Color(0xFF111111) else DarkSurface)
                        .border(1.dp, if (isNightMode) Color(0xFF1E1E1E) else DarkBorder, RoundedCornerShape(6.dp))
                        .clickable { onSettingsClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = if (isNightMode) Color(0xFF777777) else TextPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
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
        onAddAccount = onAccountBClick,
        onRefreshAccount = { id -> if (id == accountA?.id) onRefreshA() else onRefreshB() },
        onDeleteAccount = {},
        onAccountClick = { acc -> if (acc.id == accountA?.id) onAccountAClick() else onAccountBClick() },
        onRefreshAll = onRefreshAll,
        onToggleNightMode = {},
        onSettingsClick = onSettingsClick
    )
}
