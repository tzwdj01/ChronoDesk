package com.chronodesk.app.ui.clock

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.ui.settings.AccountManagerDialog
import com.chronodesk.app.ui.settings.DeviceAuthDialog
import com.chronodesk.app.ui.settings.SettingsDialog
import com.chronodesk.app.ui.theme.AppleNightBorder
import com.chronodesk.app.ui.theme.AppleNightRedMuted
import com.chronodesk.app.ui.theme.AppleNightRedPrimary
import com.chronodesk.app.ui.theme.AppleNightRedSecondary
import com.chronodesk.app.ui.theme.AppleNightSurfaceElevated
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.NightBackground
import com.chronodesk.app.ui.theme.PureBlack
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ClockScreen(
    viewModel: ClockViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val timeState by viewModel.timeState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val weather by viewModel.weather.collectAsState()

    val accounts by viewModel.accounts.collectAsState()
    val snapshots by viewModel.snapshots.collectAsState()
    val loadingMap by viewModel.loadingMap.collectAsState()
    val isRefreshingAll by viewModel.isRefreshingAll.collectAsState()

    val isNightMode by viewModel.isNightMode.collectAsState()
    val burnInShift by viewModel.burnInShift.collectAsState()

    // 交互式 HUD 控制状态：默认隐匿，仅展示极简时钟与额度看板，轻触呼出，6秒无操作自动隐藏
    var areControlsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible) {
            delay(6000L)
            areControlsVisible = false
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedAccountToManage by remember { mutableStateOf<CodexAccount?>(null) }
    var showDeviceAuthDialogForAccount by remember { mutableStateOf<CodexAccount?>(null) }

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated -> viewModel.updateSettings(updated) }
        )
    }

    selectedAccountToManage?.let { account ->
        AccountManagerDialog(
            initialSlot = if (accounts.indexOfFirst { it.id == account.id } == 1) 1 else 0,
            accountA = accounts.getOrNull(0),
            accountB = accounts.getOrNull(1),
            onDismiss = { selectedAccountToManage = null },
            onSaveAccount = { slot, acc, ref, accTok ->
                viewModel.saveAccount(slot, acc, ref, accTok)
                selectedAccountToManage = null
            },
            onRemoveAccount = { slot ->
                viewModel.removeAccount(slot)
                selectedAccountToManage = null
            },
            onUpdateAlias = { slot, alias ->
                viewModel.updateAccountAlias(slot, alias)
            }
        )
    }

    showDeviceAuthDialogForAccount?.let { account ->
        val slotIndex = accounts.indexOfFirst { it.id == account.id }.coerceAtLeast(0)
        DeviceAuthDialog(
            slot = slotIndex,
            onDismiss = { showDeviceAuthDialogForAccount = null },
            onSuccess = { updatedAcc, refreshToken, accessToken ->
                viewModel.saveAccount(slotIndex, updatedAcc.copy(id = account.id), refreshToken, accessToken)
                showDeviceAuthDialogForAccount = null
                Toast.makeText(context, "账号授权成功！", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 颜色系统：夜间采用 Apple StandBy 航空暗红单色体系；日间采用极客高对比荧光冷白与赛博青
    val bgColor = if (isNightMode) NightBackground else PureBlack
    val clockTextColor = if (isNightMode) AppleNightRedPrimary else TextPrimary
    val dateTextColor = if (isNightMode) AppleNightRedSecondary else TextSecondary
    val dayOfWeekTextColor = if (isNightMode) AppleNightRedPrimary else TechCyan
    val dotTextColor = if (isNightMode) AppleNightRedMuted else TextTertiary

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            }
            .offset {
                // OLED 烧屏微幅像素位移
                IntOffset(burnInShift.first.roundToInt(), burnInShift.second.roundToInt())
            }
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        val screenHeight = maxHeight
        val isCompactHeight = screenHeight < 380.dp

        if (isLandscape) {
            // ================= 黄金比例横屏布局 =================
            // 左侧占 60% (1.5f)，右侧占 40% (1.0f)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧黄金区域：大号机械翻牌时钟 + 日期星期 + 天气胶囊
                Column(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 大气自适应机械翻页时钟 (小米 11 Ultra 饱满铺满，小米 6 自适应紧凑)
                    LargeFlipClockDisplay(
                        hour = timeState.hour,
                        minute = timeState.minute,
                        second = if (settings.showSeconds && !isNightMode) timeState.second else null,
                        textColor = clockTextColor,
                        isNightMode = isNightMode,
                        isCompact = isCompactHeight
                    )

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 16.dp))

                    // 日期与星期
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = timeState.dateString,
                            color = dateTextColor,
                            fontSize = if (isCompactHeight) 14.sp else 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "·",
                            color = dotTextColor,
                            fontSize = if (isCompactHeight) 14.sp else 16.sp
                        )
                        Text(
                            text = timeState.dayOfWeek,
                            color = dayOfWeekTextColor,
                            fontSize = if (isCompactHeight) 14.sp else 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 14.dp))

                    // 天气胶囊卡片（位于时间下方）
                    WeatherCapsule(
                        weather = weather,
                        isNightMode = isNightMode,
                        onCardClick = {
                            viewModel.refreshAll()
                            Toast.makeText(context, "正在刷新天气与位置…", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 右侧区域：ChatGPT / Codex 多账号额度监控
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    MultiAccountQuotaSection(
                        accounts = accounts,
                        snapshots = snapshots,
                        loadingMap = loadingMap,
                        isRefreshingAll = isRefreshingAll,
                        nightModeOption = settings.nightModeOption,
                        isNightMode = isNightMode,
                        areControlsVisible = areControlsVisible,
                        onAddAccount = {
                            viewModel.addAccount()
                        },
                        onRefreshAccount = { id ->
                            viewModel.refreshAccount(id)
                        },
                        onDeleteAccount = { id ->
                            viewModel.removeAccountById(id)
                        },
                        onAccountClick = { acc ->
                            if (!acc.isConfigured) {
                                showDeviceAuthDialogForAccount = acc
                            } else {
                                selectedAccountToManage = acc
                            }
                        },
                        onRefreshAll = {
                            viewModel.refreshAll()
                        },
                        onToggleNightMode = {
                            val newMode = viewModel.toggleNightMode()
                            val modeMsg = when (newMode) {
                                "FORCE_DAY" -> "已切换至：强制日间模式"
                                "FORCE_NIGHT" -> "已切换至：强制夜间模式 (Apple StandBy 极夜红)"
                                else -> "已切换至：自动模式 (22:00-07:00)"
                            }
                            Toast.makeText(context, modeMsg, Toast.LENGTH_SHORT).show()
                        },
                        onSettingsClick = {
                            showSettingsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // ================= 竖屏优雅备用布局 =================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                LargeFlipClockDisplay(
                    hour = timeState.hour,
                    minute = timeState.minute,
                    second = if (settings.showSeconds && !isNightMode) timeState.second else null,
                    textColor = clockTextColor,
                    isNightMode = isNightMode,
                    isCompact = false
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeState.dateString,
                        color = dateTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "·", color = dotTextColor, fontSize = 15.sp)
                    Text(
                        text = timeState.dayOfWeek,
                        color = dayOfWeekTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                WeatherCapsule(
                    weather = weather,
                    isNightMode = isNightMode,
                    onCardClick = { viewModel.refreshAll() },
                    modifier = Modifier.fillMaxWidth()
                )

                MultiAccountQuotaSection(
                    accounts = accounts,
                    snapshots = snapshots,
                    loadingMap = loadingMap,
                    isRefreshingAll = isRefreshingAll,
                    nightModeOption = settings.nightModeOption,
                    isNightMode = isNightMode,
                    areControlsVisible = areControlsVisible,
                    onAddAccount = { viewModel.addAccount() },
                    onRefreshAccount = { id -> viewModel.refreshAccount(id) },
                    onDeleteAccount = { id -> viewModel.removeAccountById(id) },
                    onAccountClick = { acc ->
                        if (!acc.isConfigured) {
                            showDeviceAuthDialogForAccount = acc
                        } else {
                            selectedAccountToManage = acc
                        }
                    },
                    onRefreshAll = { viewModel.refreshAll() },
                    onToggleNightMode = { viewModel.toggleNightMode() },
                    onSettingsClick = { showSettingsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 呼出控制时的底部智能浮动胶囊提示
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isNightMode) AppleNightSurfaceElevated else DarkSurfaceElevated)
                    .border(
                        1.dp,
                        if (isNightMode) AppleNightBorder else DarkBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "控制面板已呼出 · 轻触任意处收起 (6秒自动隐匿)",
                    color = if (isNightMode) AppleNightRedSecondary else TextSecondary,
                    fontSize = 10.sp
                )
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
    isCompact: Boolean = false
) {
    // 动态比例：小米 11 Ultra 大气铺满；小米 6 (高度较窄) 采用紧凑尺寸，杜绝重叠与裁切
    val digitWidth = if (isCompact) 74.dp else 96.dp
    val digitHeight = if (isCompact) 110.dp else 142.dp
    val fontSize = if (isCompact) 72.sp else 94.sp

    val secDigitWidth = if (isCompact) 28.dp else 34.dp
    val secDigitHeight = if (isCompact) 42.dp else 52.dp
    val secFontSize = if (isCompact) 26.sp else 32.sp

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

        // 呼吸冒号分割符
        BlinkingColon(isNightMode = isNightMode)

        // 分钟翻牌
        FlipDigitPair(
            value = minute,
            digitWidth = digitWidth,
            digitHeight = digitHeight,
            fontSize = fontSize,
            textColor = textColor,
            isNightMode = isNightMode
        )

        // 秒钟翻牌 (可配置，日间可选)
        if (second != null) {
            Spacer(modifier = Modifier.width(8.dp))
            FlipDigitPair(
                value = second,
                digitWidth = secDigitWidth,
                digitHeight = secDigitHeight,
                fontSize = secFontSize,
                textColor = if (isNightMode) AppleNightRedSecondary else TechCyan,
                isNightMode = isNightMode
            )
        }
    }
}

@Composable
fun BlinkingColon(isNightMode: Boolean) {
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    val colonColor = if (isNightMode) AppleNightRedPrimary else TechCyan

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colonColor.copy(alpha = alphaAnim.value))
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colonColor.copy(alpha = alphaAnim.value))
            )
        }
    }
}
