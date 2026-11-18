package com.chronodesk.app.ui.clock

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.ui.settings.AccountManagerDialog
import com.chronodesk.app.ui.settings.SettingsDialog
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.NightBackground
import com.chronodesk.app.ui.theme.PureBlack
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary
import kotlin.math.roundToInt

@Composable
fun ClockScreen(
    viewModel: ClockViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val timeState by viewModel.timeState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val weather by viewModel.weather.collectAsState()

    val accountA by viewModel.accountA.collectAsState()
    val accountB by viewModel.accountB.collectAsState()
    val snapshotA by viewModel.snapshotA.collectAsState()
    val snapshotB by viewModel.snapshotB.collectAsState()
    val isLoadingA by viewModel.isLoadingA.collectAsState()
    val isLoadingB by viewModel.isLoadingB.collectAsState()
    val isRefreshingAll by viewModel.isRefreshingAll.collectAsState()

    val isNightMode by viewModel.isNightMode.collectAsState()
    val burnInShift by viewModel.burnInShift.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var accountManagerSlot by remember { mutableStateOf<Int?>(null) }

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated -> viewModel.updateSettings(updated) }
        )
    }

    accountManagerSlot?.let { slot ->
        AccountManagerDialog(
            initialSlot = slot,
            accountA = accountA,
            accountB = accountB,
            onDismiss = { accountManagerSlot = null },
            onSaveAccount = { s, acc, ref, accTok ->
                viewModel.saveAccount(s, acc, ref, accTok)
            },
            onRemoveAccount = { s ->
                viewModel.removeAccount(s)
            },
            onUpdateAlias = { s, alias ->
                viewModel.updateAccountAlias(s, alias)
            }
        )
    }

    val bgColor = if (isNightMode) NightBackground else PureBlack
    val clockTextColor = if (isNightMode) Color(0xFFAAAAAA) else TextPrimary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .offset {
                // OLED 烧屏微幅像素位移
                IntOffset(burnInShift.first.roundToInt(), burnInShift.second.roundToInt())
            }
            .padding(16.dp)
    ) {
        if (isLandscape) {
            // ================= 横屏布局 =================
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左半边：大翻页时钟 + 日期星期
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LargeFlipClockDisplay(
                        hour = timeState.hour,
                        minute = timeState.minute,
                        second = if (settings.showSeconds && !isNightMode) timeState.second else null,
                        textColor = clockTextColor,
                        isNightMode = isNightMode,
                        isLarge = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = timeState.dateString,
                            color = if (isNightMode) Color(0xFF555555) else TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "·",
                            color = TextTertiary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = timeState.dayOfWeek,
                            color = if (isNightMode) Color(0xFF555555) else TechCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 右半边：天气与双账号卡片
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeatherCard(
                        weather = weather,
                        isNightMode = isNightMode,
                        onCardClick = { viewModel.refreshAll() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DualAccountQuotaSection(
                        accountA = accountA,
                        snapshotA = snapshotA,
                        isLoadingA = isLoadingA,
                        accountB = accountB,
                        snapshotB = snapshotB,
                        isLoadingB = isLoadingB,
                        isRefreshingAll = isRefreshingAll,
                        isNightMode = isNightMode,
                        onRefreshA = { viewModel.refreshSlot(0) },
                        onRefreshB = { viewModel.refreshSlot(1) },
                        onRefreshAll = { viewModel.refreshAll() },
                        onAccountAClick = { accountManagerSlot = 0 },
                        onAccountBClick = { accountManagerSlot = 1 },
                        onSettingsClick = { showSettingsDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // ================= 竖屏布局 =================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 顶部：翻页时钟
                LargeFlipClockDisplay(
                    hour = timeState.hour,
                    minute = timeState.minute,
                    second = if (settings.showSeconds && !isNightMode) timeState.second else null,
                    textColor = clockTextColor,
                    isNightMode = isNightMode,
                    isLarge = false
                )

                // 日期与星期
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeState.dateString,
                        color = if (isNightMode) Color(0xFF555555) else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "·",
                        color = TextTertiary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = timeState.dayOfWeek,
                        color = if (isNightMode) Color(0xFF555555) else TechCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 中间：天气卡片
                WeatherCard(
                    weather = weather,
                    isNightMode = isNightMode,
                    onCardClick = { viewModel.refreshAll() },
                    modifier = Modifier.fillMaxWidth()
                )

                // 底部：双账号监控卡片
                DualAccountQuotaSection(
                    accountA = accountA,
                    snapshotA = snapshotA,
                    isLoadingA = isLoadingA,
                    accountB = accountB,
                    snapshotB = snapshotB,
                    isLoadingB = isLoadingB,
                    isRefreshingAll = isRefreshingAll,
                    isNightMode = isNightMode,
                    onRefreshA = { viewModel.refreshSlot(0) },
                    onRefreshB = { viewModel.refreshSlot(1) },
                    onRefreshAll = { viewModel.refreshAll() },
                    onAccountAClick = { accountManagerSlot = 0 },
                    onAccountBClick = { accountManagerSlot = 1 },
                    onSettingsClick = { showSettingsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LargeFlipClockDisplay(
    hour: Int,
    minute: Int,
    second: Int?,
    textColor: Color,
    isNightMode: Boolean,
    isLarge: Boolean
) {
    val digitWidth = if (isLarge) 72.dp else 64.dp
    val digitHeight = if (isLarge) 104.dp else 94.dp
    val fontSize = if (isLarge) 68.sp else 58.sp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 小时翻牌
        FlipDigitPair(
            value = hour,
            digitWidth = digitWidth,
            digitHeight = digitHeight,
            fontSize = fontSize,
            textColor = textColor,
            isNightMode = isNightMode
        )

        // 冒号分隔符
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isNightMode) Color(0xFF444444) else TechCyan)
                    .width(6.dp)
                    .height(6.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isNightMode) Color(0xFF444444) else TechCyan)
                    .width(6.dp)
                    .height(6.dp)
            )
        }

        // 分钟翻牌
        FlipDigitPair(
            value = minute,
            digitWidth = digitWidth,
            digitHeight = digitHeight,
            fontSize = fontSize,
            textColor = textColor,
            isNightMode = isNightMode
        )

        // 可选秒钟计数翻牌
        if (second != null) {
            Spacer(modifier = Modifier.width(10.dp))
            FlipDigitPair(
                value = second,
                digitWidth = (digitWidth.value * 0.55f).dp,
                digitHeight = (digitHeight.value * 0.55f).dp,
                fontSize = (fontSize.value * 0.55f).sp,
                textColor = textColor.copy(alpha = 0.7f),
                isNightMode = isNightMode,
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
    }
}
