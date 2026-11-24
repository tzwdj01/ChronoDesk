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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronodesk.app.ui.longbridge.LongbridgeScreen
import com.chronodesk.app.ui.longbridge.LongbridgeViewModel
import com.chronodesk.app.ui.settings.LongbridgeSettingsDialog
import com.chronodesk.app.ui.tesla.TeslaViewModel
import com.chronodesk.app.ui.tesla.TeslaScreen
import com.chronodesk.app.ui.settings.TeslaSettingsDialog
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.chronodesk.app.data.model.ChargingSnapshot
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.model.DailyDriveItem
import com.chronodesk.app.data.model.DeepSeekBalance
import com.chronodesk.app.data.model.DriveSummary
import com.chronodesk.app.data.model.LastDriveInfo
import com.chronodesk.app.data.model.LongbridgeFullSnapshot
import com.chronodesk.app.data.model.LongbridgeSettings
import com.chronodesk.app.data.model.TeslaConnectionState
import com.chronodesk.app.data.model.TeslaFullSnapshot
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.model.TeslaVehicle
import com.chronodesk.app.data.model.VehicleLocation
import com.chronodesk.app.data.model.VehicleSnapshot
import com.chronodesk.app.data.model.VehicleState
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.data.repository.LongbridgeRepository
import com.chronodesk.app.ui.settings.SettingsDialog
import com.chronodesk.app.ui.settings.SettingsTab
import com.chronodesk.app.ui.theme.DarkBackground
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkColon
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBackground
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkColon
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkSurfaceElevated
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkTextTertiary
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary
import com.chronodesk.app.ui.theme.LocalPureInkMode
import com.chronodesk.app.ui.theme.PureInkTextPrimary
import com.chronodesk.app.ui.theme.resolveChronoPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class ChronoPage(val label: String) {
    CLOCK("时钟"),
    EXTENSION("详情"),
    TESLA("车辆"),
    LONGBRIDGE("资产")
}

@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ClockScreen(
    viewModel: ClockViewModel,
    debugNavCommand: com.chronodesk.app.DebugNavCommand = com.chronodesk.app.DebugNavCommand(),
    onRequestLocationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val timeState by viewModel.timeState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val weatherList by viewModel.weatherList.collectAsState()

    val accounts by viewModel.accounts.collectAsState()
    val snapshots by viewModel.snapshots.collectAsState()
    val loadingMap by viewModel.loadingMap.collectAsState()
    val isRefreshingAll by viewModel.isRefreshingAll.collectAsState()

    val isNightMode by viewModel.isNightMode.collectAsState()
    val burnInShift by viewModel.burnInShift.collectAsState()

    val teslaViewModel: TeslaViewModel = viewModel()
    val teslaSettings by teslaViewModel.settings.collectAsState()
    val teslaSnapshot by teslaViewModel.snapshot.collectAsState()

    val longbridgeViewModel: LongbridgeViewModel = viewModel()
    val longbridgeSettings by longbridgeViewModel.settings.collectAsState()
    val longbridgeSnapshot by longbridgeViewModel.snapshot.collectAsState()

    var debugRichPreview by remember { mutableStateOf(false) }

    val previewBundle = remember(debugRichPreview) {
        if (debugRichPreview) buildDebugRichPreviewQuota() else null
    }
    val previewWeather = remember(debugRichPreview) {
        if (debugRichPreview) buildDebugRichPreviewWeather() else null
    }
    val previewTesla = remember(debugRichPreview) {
        if (debugRichPreview) buildDebugRichPreviewTesla() else null
    }
    val previewLongbridge = remember(debugRichPreview) {
        if (debugRichPreview) buildDebugRichPreviewLongbridge() else null
    }
    val displayAccounts = previewBundle?.first ?: accounts
    val displaySnapshots = previewBundle?.second ?: snapshots
    val displayWeatherList = previewWeather ?: weatherList
    val displayTeslaSettings = previewTesla?.first ?: teslaSettings
    val displayTeslaSnapshot = previewTesla?.second ?: teslaSnapshot
    val displayLongbridgeSettings = previewLongbridge?.first ?: longbridgeSettings
    val displayLongbridgeSnapshot = previewLongbridge?.second ?: longbridgeSnapshot

    val pages = remember(displayTeslaSettings.enabled, displayLongbridgeSettings.enabled) {
        buildList {
            add(ChronoPage.CLOCK)
            add(ChronoPage.EXTENSION)
            if (displayTeslaSettings.enabled) add(ChronoPage.TESLA)
            if (displayLongbridgeSettings.enabled) add(ChronoPage.LONGBRIDGE)
        }
    }
    val pageCount = pages.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()

    // 页面数量变动时的越界安全保护
    LaunchedEffect(pageCount) {
        if (pagerState.currentPage >= pageCount) {
            pagerState.scrollToPage(0)
        }
    }

    // 同步全局演示数据开关到长桥仓库
    LaunchedEffect(settings.useMockData) {
        longbridgeViewModel.setMockMode(settings.useMockData)
    }

    // 绑定前后台生命周期与车辆页/长桥页轮询
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    teslaViewModel.onAppForeground()
                    longbridgeViewModel.onAppForeground()
                }
                Lifecycle.Event.ON_STOP -> {
                    teslaViewModel.onAppBackground()
                    longbridgeViewModel.onAppBackground()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(
        pagerState.currentPage,
        pages,
        teslaSettings.enabled,
        teslaSettings.isConfigured,
        longbridgeSettings.enabled,
        longbridgeSettings.showOnHome
    ) {
        val currentPageType = pages.getOrNull(pagerState.currentPage) ?: ChronoPage.CLOCK
        teslaViewModel.setPageVisible(currentPageType == ChronoPage.TESLA && teslaSettings.enabled)
        longbridgeViewModel.setActiveVisibility(
            isHomePage = currentPageType == ChronoPage.CLOCK,
            isLongbridgePage = currentPageType == ChronoPage.LONGBRIDGE
        )
    }

    var areControlsVisible by remember { mutableStateOf(false) }
    var controlsRevealTick by remember { mutableIntStateOf(0) }

    val toggleControlsVisibility: () -> Unit = {
        if (areControlsVisible) {
            areControlsVisible = false
        } else {
            areControlsVisible = true
            controlsRevealTick++
        }
    }
    val revealControls: () -> Unit = {
        areControlsVisible = true
        controlsRevealTick++
    }

    LaunchedEffect(areControlsVisible, controlsRevealTick) {
        if (areControlsVisible) {
            delay(6000L)
            areControlsVisible = false
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { mutableStateOf(SettingsTab.DISPLAY) }
    var settingsInitialAccountId by remember { mutableStateOf<String?>(null) }
    var showTeslaSettingsDialog by remember { mutableStateOf(false) }
    var showLongbridgeSettingsDialog by remember { mutableStateOf(false) }

    val currentPages by rememberUpdatedState(pages)
    LaunchedEffect(debugNavCommand.tick) {
        if (debugNavCommand.tick > 0) {
            var nextSettings = settings
            debugNavCommand.mockMode?.let { mock ->
                nextSettings = nextSettings.copy(useMockData = mock)
            }
            debugNavCommand.displayMode?.let { mode ->
                val normalized = if (mode.equals("PROGRESS_BAR", ignoreCase = true)) "PROGRESS_BAR" else "NUMBERS_ONLY"
                nextSettings = nextSettings.copy(quotaDisplayMode = normalized)
            }
            debugNavCommand.providerOrder?.let { order ->
                val canonical = listOf("CHATGPT", "ANTIGRAVITY", "DEEPSEEK")
                val parsed = order.split(",")
                    .map { it.trim().uppercase(java.util.Locale.US) }
                    .filter { it in canonical }
                    .distinct()
                val normalizedOrder = (parsed + canonical.filterNot { it in parsed }).joinToString(",")
                nextSettings = nextSettings.copy(quotaProviderOrder = normalizedOrder)
            }
            debugNavCommand.themeMode?.let { tm ->
                nextSettings = when (tm.uppercase(java.util.Locale.US)) {
                    "DAY_COLOR" -> nextSettings.copy(nightModeOption = "FORCE_DAY", dayThemeStyle = "COLOR")
                    "DAY_INK" -> nextSettings.copy(nightModeOption = "FORCE_DAY", dayThemeStyle = "INK")
                    "NIGHT" -> nextSettings.copy(nightModeOption = "FORCE_NIGHT")
                    else -> nextSettings
                }
            }
            if (nextSettings != settings) {
                viewModel.updateSettings(nextSettings)
            }
            debugNavCommand.controlsVisible?.let { cv ->
                if (cv) {
                    revealControls()
                } else {
                    areControlsVisible = false
                }
            }
            debugNavCommand.richPreview?.let { rp ->
                debugRichPreview = rp
            }
            debugNavCommand.previewLongbridge?.let { previewLb ->
                if (longbridgeSettings.enabled != previewLb) {
                    longbridgeViewModel.updateSettings { it.copy(enabled = previewLb, showOnHome = true) }
                }
            }
            debugNavCommand.settingsTab?.let { tabStr ->
                if (tabStr.equals("CLOSE", ignoreCase = true)) {
                    showSettingsDialog = false
                } else {
                    val tab = SettingsTab.entries.find { it.name.equals(tabStr, ignoreCase = true) }
                    if (tab != null) {
                        settingsInitialTab = tab
                        settingsInitialAccountId = null
                        showSettingsDialog = true
                    }
                }
            }
            debugNavCommand.page?.let { pageStr ->
                delay(180L)
                val target = ChronoPage.entries.find { it.name.equals(pageStr, ignoreCase = true) }
                if (target != null) {
                    val idx = currentPages.indexOf(target)
                    if (idx >= 0) {
                        pagerState.scrollToPage(idx)
                    }
                }
            }
        }
    }

    // 首页已开启 Tesla 时，若冷启动尚无缓存快照则单次补拉一次，让首页续航胶囊即时展示（不改变后台/熄屏轮询规则）
    LaunchedEffect(teslaSettings.enabled, teslaSettings.isConfigured) {
        if (teslaSettings.enabled && teslaSettings.isConfigured && !teslaSnapshot.isAvailable && !teslaSnapshot.connectionState.isLoading) {
            teslaViewModel.refreshNow()
        }
    }

    // 启用长桥且已授权但尚无资产快照时，冷启动单次补拉一次
    LaunchedEffect(longbridgeSettings.enabled, settings.useMockData) {
        if (longbridgeSettings.enabled &&
            longbridgeViewModel.hasCredentials() &&
            !longbridgeSnapshot.hasValidAssetData &&
            !longbridgeSnapshot.connectionState.isLoading &&
            !settings.useMockData
        ) {
            longbridgeViewModel.refreshNow()
        }
    }

    val teslaSummaryText = when {
        !teslaSettings.enabled -> "未启用车辆监控"
        !teslaSettings.isConfigured -> "已启用但尚未完成 Grafana / 数据源配置"
        else -> {
            val name = teslaSnapshot.car?.displayName ?: teslaSettings.carName.ifBlank { "Tesla #${teslaSettings.carId}" }
            val soc = teslaSnapshot.snapshot.batteryLevel?.let { "$it%" } ?: "待同步"
            val range = teslaSnapshot.snapshot.ratedBatteryRangeKm?.let { "${it.toInt()}km" } ?: ""
            listOf(name, soc, range).filter { it.isNotBlank() }.joinToString(" · ")
        }
    }

    val isPureInk = !isNightMode && settings.isPureInkDayStyle
    val theme = resolveChronoPalette(isNightMode, isPureInk)

    CompositionLocalProvider(LocalPureInkMode provides isPureInk) {
        if (showSettingsDialog) {
        val chatGptAccs = accounts.filter { it.modelProvider == "CHATGPT" }
        SettingsDialog(
            currentSettings = settings,
            isNightMode = isNightMode,
            initialTab = settingsInitialTab,
            teslaStatusSummary = teslaSummaryText,
            accounts = accounts,
            initialAccountId = settingsInitialAccountId,
            getDeepSeekApiKey = { id -> viewModel.getDeepSeekApiKey(id) },
            getAntigravityApiKey = { id -> viewModel.getAntigravityApiKey(id) },
            onSaveCodexAccount = { slot, acc, ref, accTok ->
                viewModel.saveAccount(slot, acc, ref, accTok)
            },
            onRemoveCodexAccount = { slot -> viewModel.removeAccount(slot) },
            onUpdateCodexAlias = { slot, alias ->
                val targetAcc = chatGptAccs.getOrNull(slot)
                if (targetAcc != null) {
                    viewModel.updateAccountAliasById(targetAcc.id, alias)
                } else {
                    viewModel.updateAccountAlias(slot, alias)
                }
            },
            onSaveDeepSeekAccount = { id, key, alias -> viewModel.saveDeepSeekAccount(id, key, alias) },
            onAddDeepSeekAccount = { viewModel.addDeepSeekAccount() },
            onRemoveDeepSeekAccount = { id -> viewModel.removeDeepSeekAccount(id) },
            onSaveAntigravityAccount = { id, key, alias -> viewModel.saveAntigravityAccount(id, key, alias) },
            onSaveAntigravityOAuthAccount = { id, alias, email, refresh, access, expSec, idTok, pType ->
                viewModel.saveAntigravityOAuthAccount(id, alias, email, refresh, access, expSec, idTok, pType)
            },
            onUpdateAntigravityAlias = { id, alias -> viewModel.updateAntigravityAlias(id, alias) },
            onAddAntigravityAccount = { viewModel.addAntigravityAccount() },
            onRemoveAntigravityAccount = { id -> viewModel.removeAntigravityAccount(id) },
            onUpdateAccountVisibility = { accId, showMain, showSecond, isPinned ->
                viewModel.updateAccountVisibility(accId, showMain, showSecond, isPinned)
            },
            onDismiss = {
                showSettingsDialog = false
                settingsInitialAccountId = null
            },
            onSave = { updated -> viewModel.updateSettings(updated) },
            onOpenTeslaSettings = {
                showSettingsDialog = false
                showTeslaSettingsDialog = true
            },
            teslaViewModel = teslaViewModel,
            longbridgeViewModel = longbridgeViewModel,
            longbridgeSettings = longbridgeSettings,
            longbridgeHasCredentials = longbridgeViewModel.hasCredentials(),
            onUpdateLongbridgeSettings = { transform -> longbridgeViewModel.updateSettings(transform) },
            onOpenLongbridgeSettings = {
                showSettingsDialog = false
                showLongbridgeSettingsDialog = true
            },
            onExportSettings = { viewModel.exportSettingsJson() },
            onValidateSettings = { json -> viewModel.validateAndParseSettingsJson(json) },
            onApplySettingsBackup = { backup -> viewModel.applySettingsBackup(backup) },
            onRequestLocationPermission = onRequestLocationPermission,
            onSearchLocations = { query -> viewModel.searchLocations(query) }
        )
    }

    if (showTeslaSettingsDialog) {
        TeslaSettingsDialog(
            viewModel = teslaViewModel,
            isNightMode = isNightMode,
            onDismiss = { showTeslaSettingsDialog = false }
        )
    }

    if (showLongbridgeSettingsDialog) {
        LongbridgeSettingsDialog(
            viewModel = longbridgeViewModel,
            isNightMode = isNightMode,
            onDismiss = { showLongbridgeSettingsDialog = false }
        )
    }

    // 主题色彩体系自适应
    val bgColor = theme.bg

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                toggleControlsVisibility()
            }
            .offset {
                IntOffset(burnInShift.first.roundToInt(), burnInShift.second.roundToInt())
            }
    ) {
        val screenHeight = maxHeight
        val isCompactHeight = screenHeight < 380.dp
        val containerWidthDp = maxWidth.value
        val containerHeightDp = maxHeight.value

        // 背景绘制：日间水墨写意山水（全水墨纯灰阶 / 彩色暖宣）或夜间赛博深空微光流线
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (!isNightMode) {
                // 远山
                val farMountain = Path().apply {
                    moveTo(0f, h * 0.88f)
                    cubicTo(w * 0.15f, h * 0.72f, w * 0.38f, h * 0.86f, w * 0.58f, h * 0.76f)
                    cubicTo(w * 0.78f, h * 0.68f, w * 0.90f, h * 0.82f, w, h * 0.74f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                val farColor = if (isPureInk) Color(0xFFDADADA).copy(alpha = 0.25f) else Color(0xFFDDD7CC).copy(alpha = 0.22f)
                drawPath(farMountain, color = farColor)

                // 近山
                val nearMountain = Path().apply {
                    moveTo(0f, h * 0.93f)
                    cubicTo(w * 0.25f, h * 0.85f, w * 0.48f, h * 0.96f, w * 0.75f, h * 0.86f)
                    cubicTo(w * 0.88f, h * 0.82f, w * 0.95f, h * 0.90f, w, h * 0.84f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                val nearColor = if (isPureInk) Color(0xFFCCCCCC).copy(alpha = 0.30f) else Color(0xFFD0C9BC).copy(alpha = 0.28f)
                drawPath(nearMountain, color = nearColor)
            } else {
                // 夜间赛博微光曲线（科技深蓝微光与青蓝流线）
                val cyberCurve1 = Path().apply {
                    moveTo(0f, h * 0.95f)
                    cubicTo(w * 0.2f, h * 0.80f, w * 0.4f, h * 0.98f, w * 0.6f, h * 0.90f)
                }
                drawPath(cyberCurve1, color = TechCyan.copy(alpha = 0.08f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))

                val cyberCurve2 = Path().apply {
                    moveTo(0f, h * 0.88f)
                    cubicTo(w * 0.3f, h * 0.75f, w * 0.55f, h * 0.92f, w * 0.85f, h * 0.85f)
                }
                drawPath(cyberCurve2, color = TechCyan.copy(alpha = 0.04f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // ================= 底层主体：HorizontalPager 多页滑屏铺满画布，最大化空间利用率 =================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageType = pages.getOrNull(page) ?: ChronoPage.CLOCK
                when (pageType) {
                    ChronoPage.CLOCK -> {
                    // 第一页：主页面 (大时钟 + 日期/星期/农历 + 气象/车辆续航 + AI 额度看板)
                    val showHomeTesla = (settings.showTeslaOnHome || debugRichPreview) && displayTeslaSettings.enabled && displayTeslaSettings.isConfigured
                    val showHomeLongbridge = displayLongbridgeSettings.enabled && displayLongbridgeSettings.showOnHome
                    val hasSecondCapsuleRow = showHomeTesla || showHomeLongbridge
                    val teslaDisplayLabel = displayTeslaSnapshot.car?.normalizedModel
                        ?: displayTeslaSettings.carName.takeIf { it.isNotBlank() && it.length <= 12 }
                        ?: "Tesla"

                    if (isLandscape) {
                        val effectiveWidthPercent = if (isCompactHeight && containerWidthDp < 700f && settings.quotaWidthPercent == 38) {
                            42
                        } else {
                            settings.quotaWidthPercent
                        }
                        val quotaFraction = (effectiveWidthPercent / 100f).coerceIn(0.28f, 0.52f)
                        val clockFraction = 1f - quotaFraction
                        val outerHPadDp = if (isCompactHeight) 10f else 22f
                        val rowGapDp = if (isCompactHeight) 10f else 24f
                        val leftColWidthDp = ((containerWidthDp - outerHPadDp * 2f - rowGapDp) * clockFraction).coerceAtLeast(160f)
                        val reservedHeightDp = if (isCompactHeight) {
                            20f + 34f + 24f + (if (hasSecondCapsuleRow) 68f else if (displayWeatherList.size > 1) 42f else 32f)
                        } else {
                            28f + 62f + 24f + (if (hasSecondCapsuleRow) 90f else if (displayWeatherList.size > 1) 46f else 36f)
                        }
                        val maxClockHeightDp = (containerHeightDp - reservedHeightDp).coerceAtLeast(80f)

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = if (isCompactHeight) 10.dp else 22.dp,
                                    end = if (isCompactHeight) 10.dp else 22.dp,
                                    top = if (isCompactHeight) 7.dp else 10.dp,
                                    bottom = if (isCompactHeight) 10.dp else 18.dp
                                ),
                            horizontalArrangement = Arrangement.spacedBy(if (isCompactHeight) 10.dp else 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧：视觉中心 —— 大尺寸翻牌时钟 + 公历农历 + 气象/车辆胶囊
                            Column(
                                modifier = Modifier
                                    .weight(clockFraction)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // 翻牌数字时钟：由组件按真实布局尺寸缩放，永不挤压或覆盖下方日期行
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                settings.clockOffsetX.roundToInt(),
                                                settings.clockOffsetY.roundToInt()
                                            )
                                        }
                                        .then(
                                            if (areControlsVisible) {
                                                Modifier.pointerInput(settings.clockScale, settings.clockOffsetX, settings.clockOffsetY) {
                                                    detectTransformGestures { _, pan, zoom, _ ->
                                                        val newScale = (settings.clockScale * zoom).coerceIn(0.6f, 1.8f)
                                                        val newX = (settings.clockOffsetX + pan.x).coerceIn(-250f, 250f)
                                                        val newY = (settings.clockOffsetY + pan.y).coerceIn(-180f, 180f)
                                                        viewModel.updateSettings(
                                                            settings.copy(
                                                                clockScale = newScale,
                                                                clockOffsetX = newX,
                                                                clockOffsetY = newY
                                                            )
                                                        )
                                                    }
                                                }
                                            } else Modifier
                                        )
                                ) {
                                    LargeFlipClockDisplay(
                                        hour = timeState.hour,
                                        minute = timeState.minute,
                                        second = if (settings.showSeconds) timeState.second else null,
                                        isNightMode = isNightMode,
                                        isCompact = isCompactHeight,
                                        scale = settings.clockScale,
                                        maxWidthDp = leftColWidthDp - 4f,
                                        maxHeightDp = maxClockHeightDp,
                                        onClick = toggleControlsVisibility,
                                        onToggleSeconds = {
                                            val newState = viewModel.toggleShowSeconds()
                                            val tip = if (newState) "已开启秒钟显示" else "已隐藏秒钟（可在设置中重新开启）"
                                            Toast.makeText(context, tip, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                // 固定保持轻触时托高的视觉黄金间距，彻底消除呼出/隐藏顶栏时的跳变位移
                                Spacer(modifier = Modifier.height(if (isCompactHeight) 25.dp else 36.dp))

                                // 公历日期 + 星期 + 农历完整呈现
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(if (isCompactHeight) 6.dp else 8.dp)
                                ) {
                                    Text(
                                        text = timeState.dateString,
                                        color = theme.primaryText,
                                        fontSize = if (isCompactHeight) 14.sp else 16.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = "·",
                                        color = theme.mutedText,
                                        fontSize = if (isCompactHeight) 12.5.sp else 14.sp
                                    )

                                    Text(
                                        text = timeState.dayOfWeek,
                                        color = if (isNightMode) TechCyan else theme.primaryText,
                                        fontSize = if (isCompactHeight) 14.sp else 16.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = "·",
                                        color = theme.mutedText,
                                        fontSize = if (isCompactHeight) 12.5.sp else 14.sp
                                    )

                                    Text(
                                        text = timeState.lunarDateString,
                                        color = theme.secondaryText,
                                        fontSize = if (isCompactHeight) 13.5.sp else 15.5.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }

                                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 14.dp))

                                // 天气与车辆电量 / 长桥盈亏对齐胶囊区（上下外边界齐整）
                                if (!hasSecondCapsuleRow) {
                                    MultiCityWeatherCapsule(
                                        weatherList = displayWeatherList,
                                        isNightMode = isNightMode,
                                        isCompact = isCompactHeight,
                                        onCardClick = {
                                            settingsInitialTab = SettingsTab.WEATHER
                                            showSettingsDialog = true
                                        }
                                    )
                                } else {
                                    AlignedWeatherAndAssetCapsules(
                                        isCompact = isCompactHeight,
                                        modifier = Modifier.widthIn(max = leftColWidthDp.dp),
                                        weatherContent = {
                                            MultiCityWeatherCapsule(
                                                weatherList = displayWeatherList,
                                                isNightMode = isNightMode,
                                                isCompact = isCompactHeight,
                                                onCardClick = {
                                                    settingsInitialTab = SettingsTab.WEATHER
                                                    showSettingsDialog = true
                                                }
                                            )
                                        },
                                        bottomCapsules = {
                                            if (showHomeTesla) {
                                                HomeTeslaCapsule(
                                                    vehicleLabel = teslaDisplayLabel,
                                                    batteryLevel = displayTeslaSnapshot.snapshot.batteryLevel,
                                                    ratedRangeKm = displayTeslaSnapshot.snapshot.ratedBatteryRangeKm,
                                                    isCharging = displayTeslaSnapshot.state.isCharging,
                                                    chargerPowerKw = displayTeslaSnapshot.charging.chargerPower,
                                                    isNightMode = isNightMode,
                                                    isCompact = isCompactHeight,
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val teslaIdx = pages.indexOf(ChronoPage.TESLA)
                                                            if (teslaIdx >= 0) pagerState.animateScrollToPage(teslaIdx)
                                                        }
                                                    }
                                                )
                                            }
                                            if (showHomeLongbridge) {
                                                HomeLongbridgeCapsule(
                                                    settings = displayLongbridgeSettings,
                                                    snapshot = displayLongbridgeSnapshot,
                                                    isNightMode = isNightMode,
                                                    isCompact = isCompactHeight,
                                                    onClick = {
                                                        if (!longbridgeViewModel.hasCredentials() && !settings.useMockData && !debugRichPreview) {
                                                            settingsInitialTab = SettingsTab.PORTFOLIO
                                                            showSettingsDialog = true
                                                        } else {
                                                            val lbIdx = pages.indexOf(ChronoPage.LONGBRIDGE)
                                                            if (lbIdx >= 0) {
                                                                coroutineScope.launch { pagerState.animateScrollToPage(lbIdx) }
                                                            } else {
                                                                settingsInitialTab = SettingsTab.PORTFOLIO
                                                                showSettingsDialog = true
                                                            }
                                                        }
                                                    },
                                                    onRefreshClick = {
                                                        longbridgeViewModel.refreshNow()
                                                        Toast.makeText(context, "正在刷新长桥盈亏…", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            // 右侧：多模型/多账号 AI 额度监控看板（与左侧翻牌时钟完全居中对称，不留顶栏死区）
                            Column(
                                modifier = Modifier
                                    .weight(quotaFraction)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                MultiAccountQuotaSection(
                                    accounts = displayAccounts,
                                    snapshots = displaySnapshots,
                                    loadingMap = loadingMap,
                                    isRefreshingAll = isRefreshingAll,
                                    nightModeOption = settings.nightModeOption,
                                    isNightMode = isNightMode,
                                    areControlsVisible = areControlsVisible,
                                    isCompact = isCompactHeight,
                                    isMockMode = settings.useMockData,
                                    refreshIntervalMinutes = settings.autoRefreshIntervalMinutes,
                                    cardDensity = settings.quotaCardDensity,
                                    quotaDisplayMode = settings.quotaDisplayMode,
                                    providerOrder = settings.orderedProviders(),
                                    onAddAccount = {
                                        settingsInitialTab = SettingsTab.ACCOUNTS
                                        settingsInitialAccountId = null
                                        showSettingsDialog = true
                                    },
                                    onRefreshAccount = { id ->
                                        revealControls()
                                        viewModel.refreshAccount(id)
                                    },
                                    onDeleteAccount = { id ->
                                        revealControls()
                                        val acc = accounts.find { it.id == id }
                                        if (acc?.modelProvider == "DEEPSEEK") {
                                            viewModel.removeDeepSeekAccount(id)
                                        } else if (acc?.modelProvider == "ANTIGRAVITY") {
                                            viewModel.removeAntigravityAccount(id)
                                        } else {
                                            viewModel.removeAccountById(id)
                                        }
                                    },
                                    onAccountClick = { acc ->
                                        settingsInitialTab = SettingsTab.ACCOUNTS
                                        settingsInitialAccountId = acc.id
                                        showSettingsDialog = true
                                    },
                                    onRevealControls = revealControls,
                                    onRefreshAll = {
                                        revealControls()
                                        viewModel.refreshAll()
                                    },
                                    onToggleNightMode = {
                                        revealControls()
                                        viewModel.toggleNightMode()
                                    },
                                    onSettingsClick = {
                                        settingsInitialTab = SettingsTab.ACCOUNTS
                                        settingsInitialAccountId = null
                                        showSettingsDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        // 竖屏备用自适应
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LargeFlipClockDisplay(
                                hour = timeState.hour,
                                minute = timeState.minute,
                                second = if (settings.showSeconds) timeState.second else null,
                                isNightMode = isNightMode,
                                isCompact = false,
                                scale = settings.clockScale,
                                maxWidthDp = (containerWidthDp - 32f).coerceAtLeast(160f)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = timeState.dateString, color = theme.primaryText, fontSize = 15.sp)
                                Text(text = timeState.dayOfWeek, color = if (isNightMode) TechCyan else theme.primaryText, fontSize = 15.sp)
                                Text(text = "·", color = theme.mutedText)
                                Text(text = timeState.lunarDateString, color = theme.secondaryText, fontSize = 15.sp)
                            }

                            if (!hasSecondCapsuleRow) {
                                MultiCityWeatherCapsule(
                                    weatherList = displayWeatherList,
                                    isNightMode = isNightMode,
                                    onCardClick = {
                                        settingsInitialTab = SettingsTab.WEATHER
                                        showSettingsDialog = true
                                    }
                                )
                            } else {
                                AlignedWeatherAndAssetCapsules(
                                    isCompact = false,
                                    modifier = Modifier.widthIn(max = (containerWidthDp - 32f).coerceAtLeast(160f).dp),
                                    weatherContent = {
                                        MultiCityWeatherCapsule(
                                            weatherList = displayWeatherList,
                                            isNightMode = isNightMode,
                                            onCardClick = {
                                                settingsInitialTab = SettingsTab.WEATHER
                                                showSettingsDialog = true
                                            }
                                        )
                                    },
                                    bottomCapsules = {
                                        if (showHomeTesla) {
                                            HomeTeslaCapsule(
                                                vehicleLabel = teslaDisplayLabel,
                                                batteryLevel = displayTeslaSnapshot.snapshot.batteryLevel,
                                                ratedRangeKm = displayTeslaSnapshot.snapshot.ratedBatteryRangeKm,
                                                isCharging = displayTeslaSnapshot.state.isCharging,
                                                chargerPowerKw = displayTeslaSnapshot.charging.chargerPower,
                                                isNightMode = isNightMode,
                                                isCompact = false,
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val teslaIdx = pages.indexOf(ChronoPage.TESLA)
                                                        if (teslaIdx >= 0) pagerState.animateScrollToPage(teslaIdx)
                                                    }
                                                }
                                            )
                                        }
                                        if (showHomeLongbridge) {
                                            HomeLongbridgeCapsule(
                                                settings = displayLongbridgeSettings,
                                                snapshot = displayLongbridgeSnapshot,
                                                isNightMode = isNightMode,
                                                isCompact = false,
                                                onClick = {
                                                    if (!longbridgeViewModel.hasCredentials() && !settings.useMockData && !debugRichPreview) {
                                                        settingsInitialTab = SettingsTab.PORTFOLIO
                                                        showSettingsDialog = true
                                                    } else {
                                                        val lbIdx = pages.indexOf(ChronoPage.LONGBRIDGE)
                                                        if (lbIdx >= 0) {
                                                            coroutineScope.launch { pagerState.animateScrollToPage(lbIdx) }
                                                        } else {
                                                            settingsInitialTab = SettingsTab.PORTFOLIO
                                                            showSettingsDialog = true
                                                        }
                                                    }
                                                },
                                                onRefreshClick = {
                                                    longbridgeViewModel.refreshNow()
                                                    Toast.makeText(context, "正在刷新长桥盈亏…", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                )
                            }

                            MultiAccountQuotaSection(
                                accounts = displayAccounts,
                                snapshots = displaySnapshots,
                                loadingMap = loadingMap,
                                isRefreshingAll = isRefreshingAll,
                                nightModeOption = settings.nightModeOption,
                                isNightMode = isNightMode,
                                areControlsVisible = areControlsVisible,
                                isMockMode = settings.useMockData,
                                refreshIntervalMinutes = settings.autoRefreshIntervalMinutes,
                                quotaDisplayMode = settings.quotaDisplayMode,
                                providerOrder = settings.orderedProviders(),
                                enableScroll = false,
                                onAddAccount = {
                                    settingsInitialTab = SettingsTab.ACCOUNTS
                                    settingsInitialAccountId = null
                                    showSettingsDialog = true
                                },
                                onAccountClick = { acc ->
                                    settingsInitialTab = SettingsTab.ACCOUNTS
                                    settingsInitialAccountId = acc.id
                                    showSettingsDialog = true
                                },
                                onRevealControls = revealControls,
                                onRefreshAccount = { id ->
                                    revealControls()
                                    viewModel.refreshAccount(id)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    }

                    ChronoPage.EXTENSION -> {
                        // 第二页：扩展页面 (大字 AI 额度监控与接口状态)
                        ClockExtensionPage(
                            accounts = displayAccounts,
                            snapshots = displaySnapshots,
                            loadingMap = loadingMap,
                            settings = settings,
                            isNightMode = isNightMode,
                            isCompact = isCompactHeight,
                            onRefreshAll = {
                                viewModel.refreshAll()
                                if (teslaSettings.enabled && teslaSettings.isConfigured) {
                                    teslaViewModel.refreshNow()
                                }
                                if (longbridgeSettings.enabled && longbridgeViewModel.hasCredentials()) {
                                    longbridgeViewModel.refreshNow()
                                }
                                Toast.makeText(context, "已触发全量网关同步", Toast.LENGTH_SHORT).show()
                            },
                            onToggleDisplayMode = {
                                val nextMode = if (settings.isNumbersOnlyMode) "PROGRESS_BAR" else "NUMBERS_ONLY"
                                viewModel.updateSettings(settings.copy(quotaDisplayMode = nextMode))
                                val label = if (nextMode == "NUMBERS_ONLY") "已切换纯数字看板" else "已切换进度条看板"
                                Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                            },
                            onOpenSettings = {
                                settingsInitialTab = SettingsTab.DISPLAY
                                settingsInitialAccountId = null
                                showSettingsDialog = true
                            },
                            onManageAccount = {
                                settingsInitialTab = SettingsTab.ACCOUNTS
                                settingsInitialAccountId = accounts.firstOrNull()?.id
                                showSettingsDialog = true
                            },
                            onAccountClick = { acc ->
                                settingsInitialTab = SettingsTab.ACCOUNTS
                                settingsInitialAccountId = acc.id
                                showSettingsDialog = true
                            },
                            onToggleControls = toggleControlsVisibility,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    ChronoPage.TESLA -> {
                        // 第三页：Tesla 车辆监控页 (TeslaMate / Grafana API)
                        TeslaScreen(
                            snapshot = displayTeslaSnapshot,
                            settings = displayTeslaSettings,
                            isNightMode = isNightMode,
                            currentTimeString = String.format("%02d:%02d:%02d", timeState.hour, timeState.minute, timeState.second),
                            isCompact = isCompactHeight,
                            onRefresh = {
                                teslaViewModel.refreshNow()
                                Toast.makeText(context, "正在刷新车辆实时状态…", Toast.LENGTH_SHORT).show()
                            },
                            onOpenSettings = {
                                settingsInitialTab = SettingsTab.VEHICLE_SYNC
                                settingsInitialAccountId = null
                                showSettingsDialog = true
                            },
                            onVisibilityChanged = { visible ->
                                teslaViewModel.setPageVisible(visible)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    ChronoPage.LONGBRIDGE -> {
                        // 长桥证券独立全屏资产看板
                        LongbridgeScreen(
                            snapshot = displayLongbridgeSnapshot,
                            settings = displayLongbridgeSettings,
                            isNightMode = isNightMode,
                            currentTimeString = String.format(java.util.Locale.US, "%02d:%02d:%02d", timeState.hour, timeState.minute, timeState.second),
                            isCompact = isCompactHeight,
                            onRefresh = {
                                longbridgeViewModel.refreshNow()
                                Toast.makeText(context, "正在刷新长桥资产与当日盈亏…", Toast.LENGTH_SHORT).show()
                            },
                            onOpenSettings = {
                                settingsInitialTab = SettingsTab.PORTFOLIO
                                settingsInitialAccountId = null
                                showSettingsDialog = true
                            },
                            onTogglePrivacy = { longbridgeViewModel.togglePrivacyMask() },
                            onCycleCurrency = { longbridgeViewModel.cycleDisplayCurrency() },
                            onToggleColorScheme = {
                                longbridgeViewModel.updateSettings { s ->
                                    val next = if (s.isRedUpGreenDown) "GREEN_UP_RED_DOWN" else "RED_UP_GREEN_DOWN"
                                    s.copy(colorScheme = next)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // ================= 顶部默认隐藏悬浮控制栏：轻触屏幕/卡片后平滑显现，6秒无操作自动隐去 =================
            val currentPageType = pages.getOrNull(pagerState.currentPage) ?: ChronoPage.CLOCK
            AnimatedVisibility(
                visible = areControlsVisible && (currentPageType == ChronoPage.CLOCK || currentPageType == ChronoPage.EXTENSION),
                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 2 },
                exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = if (isCompactHeight) 6.dp else 10.dp, end = if (isCompactHeight) 12.dp else 18.dp)
            ) {
                ClockTopBar(
                    isNightMode = isNightMode,
                    isPureInk = isPureInk,
                    hasCustomClockTransform = settings.clockScale != 1.0f || settings.clockOffsetX != 0f || settings.clockOffsetY != 0f,
                    accountCount = accounts.count { it.modelProvider in listOf("CHATGPT", "DEEPSEEK", "ANTIGRAVITY") },
                    isCompact = isCompactHeight,
                    onResetClock = {
                        revealControls()
                        viewModel.updateSettings(
                            settings.copy(
                                clockScale = 1.0f,
                                clockOffsetX = 0f,
                                clockOffsetY = 0f
                            )
                        )
                        Toast.makeText(context, "已复位时钟大小与位置", Toast.LENGTH_SHORT).show()
                    },
                    onAddAccount = {
                        areControlsVisible = false
                        settingsInitialTab = SettingsTab.ACCOUNTS
                        settingsInitialAccountId = null
                        showSettingsDialog = true
                    },
                    onRefreshAll = {
                        revealControls()
                        viewModel.refreshAll()
                        if (teslaSettings.enabled && teslaSettings.isConfigured) {
                            teslaViewModel.refreshNow()
                        }
                        if (longbridgeSettings.enabled && longbridgeViewModel.hasCredentials()) {
                            longbridgeViewModel.refreshNow()
                        }
                        Toast.makeText(context, "已触发全量数据刷新", Toast.LENGTH_SHORT).show()
                    },
                    onToggleNightMode = {
                        revealControls()
                        val msg = when (viewModel.cycleThemeMode()) {
                            "DAY_INK" -> "已切换为：日间全水墨模式"
                            "NIGHT" -> "已切换为：夜间深色模式"
                            else -> "已切换为：日间彩色模式"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onOpenSettings = {
                        areControlsVisible = false
                        settingsInitialTab = SettingsTab.DISPLAY
                        settingsInitialAccountId = null
                        showSettingsDialog = true
                    }
                )
            }

            // ================= 底部悬浮多页面指示器小圆点 (48dp 触控热区) =================
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isCompactHeight) 1.dp else 3.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 0 until pageCount) {
                    val isSelected = pagerState.currentPage == i
                    val pageName = pages.getOrNull(i)?.label ?: "页面 ${i + 1}"
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clickable(
                                role = Role.Tab,
                                onClickLabel = "切换至$pageName"
                            ) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(i)
                                }
                            }
                            .semantics {
                                contentDescription = pageName
                            }
                            .padding(bottom = if (isCompactHeight) 4.dp else 6.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(4.5.dp)
                                .width(if (isSelected) 16.dp else 5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(
                                    if (isSelected) {
                                        if (isNightMode) TechCyan else theme.primaryText
                                    } else {
                                        theme.border
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
    }
}

/**
 * 默认隐藏的悬浮控制胶囊：轻触画面唤醒，整合复位时钟、账号接口、刷新、三态主题一键循环与系统设置，不影响常规看板对称布局。
 */
@Composable
private fun ClockTopBar(
    isNightMode: Boolean,
    isPureInk: Boolean = false,
    hasCustomClockTransform: Boolean = false,
    accountCount: Int,
    isCompact: Boolean = false,
    onResetClock: () -> Unit = {},
    onAddAccount: () -> Unit,
    onRefreshAll: () -> Unit,
    onToggleNightMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val capsuleBg = theme.surface.copy(alpha = 0.95f)
    val capsuleBorder = theme.border
    val buttonText = theme.primaryText
    val (themeIcon, themeLabel, themeIconTint) = when {
        isNightMode -> Triple(Icons.Default.DarkMode, "夜间", TechAmber)
        isPureInk -> Triple(Icons.Default.Contrast, "全水墨", PureInkTextPrimary)
        else -> Triple(Icons.Default.LightMode, "彩色", Color(0xFFD97706))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(capsuleBg)
            .border(1.dp, capsuleBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = if (isCompact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 4.dp)
    ) {
        if (hasCustomClockTransform) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = "复位时钟居中" }
                    .clickable(role = Role.Button, onClickLabel = "复位时钟居中") { onResetClock() }
                    .padding(horizontal = if (isCompact) 5.dp else 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = if (isPureInk) buttonText else theme.accent,
                        modifier = Modifier.size(if (isCompact) 12.dp else 13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "复位",
                        color = buttonText,
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Box(modifier = Modifier.width(1.dp).height(16.dp).background(capsuleBorder))
        }

        // 账号接口设置入口
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "账号与接口管理" }
                .clickable(role = Role.Button, onClickLabel = "账号与接口管理") { onAddAccount() }
                .padding(horizontal = if (isCompact) 6.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isPureInk) buttonText else theme.accent,
                    modifier = Modifier.size(if (isCompact) 12.dp else 13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "账号接口($accountCount)",
                    color = buttonText,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(modifier = Modifier.width(1.dp).height(16.dp).background(capsuleBorder))

        // 全部刷新按钮
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "刷新全部数据" }
                .clickable(role = Role.Button, onClickLabel = "刷新全部数据") { onRefreshAll() }
                .padding(horizontal = if (isCompact) 6.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = buttonText,
                    modifier = Modifier.size(if (isCompact) 12.dp else 13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "刷新",
                    color = buttonText,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(modifier = Modifier.width(1.dp).height(16.dp).background(capsuleBorder))

        // 三态主题一键循环切换：日间彩色 -> 全水墨 -> 夜间 -> 日间彩色
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "切换日间彩色、全水墨与夜间主题" }
                .clickable(role = Role.Button, onClickLabel = "切换主题模式") { onToggleNightMode() }
                .padding(horizontal = if (isCompact) 6.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = themeIcon,
                    contentDescription = null,
                    tint = themeIconTint,
                    modifier = Modifier.size(if (isCompact) 12.5.dp else 13.5.dp)
                )
                Text(
                    text = themeLabel,
                    color = buttonText,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(modifier = Modifier.width(1.dp).height(16.dp).background(capsuleBorder))

        // 系统设置按钮
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "系统设置" }
                .clickable(role = Role.Button, onClickLabel = "系统设置") { onOpenSettings() }
                .padding(horizontal = if (isCompact) 6.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = buttonText,
                    modifier = Modifier.size(if (isCompact) 12.dp else 13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "设置",
                    color = buttonText,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 大号翻牌时钟组件（时、分大翻牌，秒针双位水平并排放置于右侧居中）
 */
@Composable
fun LargeFlipClockDisplay(
    hour: Int,
    minute: Int,
    second: Int?,
    isNightMode: Boolean,
    isCompact: Boolean = false,
    scale: Float = 1.0f,
    maxWidthDp: Float? = null,
    maxHeightDp: Float? = null,
    onClick: () -> Unit = {},
    onToggleSeconds: () -> Unit = {}
) {
    val hasSeconds = second != null
    val baseWidthDp = if (isCompact) 356f else if (hasSeconds) 414f else 390f
    val baseHeightDp = if (hasSeconds) (if (isCompact) 136f else 146f) else (if (isCompact) 160f else 166f)
    val maxScaleByW = if (maxWidthDp != null && maxWidthDp > 0f) maxWidthDp / baseWidthDp else 1.8f
    val maxScaleByH = if (maxHeightDp != null && maxHeightDp > 0f) maxHeightDp / baseHeightDp else 1.8f
    val effectiveScale = minOf(scale.coerceIn(0.6f, 1.8f), maxScaleByW, maxScaleByH).coerceIn(0.55f, 1.8f)

    // 动态黄金比例并参与真实排版尺寸计算，彻底消除缩放时覆盖下方日期的行距失衡问题
    val digitWidth = (if (hasSeconds) {
        if (isCompact) 68f else 76f
    } else {
        if (isCompact) 82f else 88f
    } * effectiveScale).dp
    val digitHeight = (if (hasSeconds) {
        if (isCompact) 136f else 146f
    } else {
        if (isCompact) 160f else 166f
    } * effectiveScale).dp
    val fontSize = (if (hasSeconds) {
        if (isCompact) 86f else 94f
    } else {
        if (isCompact) 102f else 108f
    } * effectiveScale).sp
    val digitSpacing = (4.5f * effectiveScale).dp

    val secDigitWidth = ((if (isCompact) 22f else 28f) * effectiveScale).dp
    val secDigitHeight = ((if (isCompact) 38f else 46f) * effectiveScale).dp
    val secFontSize = ((if (isCompact) 18.5f else 22f) * effectiveScale).sp

    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 小时翻牌
        FlipDigitPair(
            value = hour,
            digitWidth = digitWidth,
            digitHeight = digitHeight,
            fontSize = fontSize,
            digitSpacing = digitSpacing,
            isNightMode = isNightMode
        )

        // 冒号
        BlinkingColon(isNightMode = isNightMode, isCompact = isCompact, scale = effectiveScale)

        // 分钟翻牌
        FlipDigitPair(
            value = minute,
            digitWidth = digitWidth,
            digitHeight = digitHeight,
            fontSize = fontSize,
            digitSpacing = digitSpacing,
            isNightMode = isNightMode
        )

        // 秒钟翻牌：水平双位并排翻牌；点击秒钟可快速隐藏/切换
        if (second != null) {
            Spacer(modifier = Modifier.width(((if (isCompact) 8f else 12f) * effectiveScale).dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((3f * effectiveScale).dp),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleSeconds() }
            ) {
                val tens = (second.coerceIn(0, 99)) / 10
                val units = (second.coerceIn(0, 99)) % 10
                FlipDigit(
                    digit = tens,
                    width = secDigitWidth,
                    height = secDigitHeight,
                    fontSize = secFontSize,
                    textColor = if (isNightMode) TechCyan else null,
                    isNightMode = isNightMode
                )
                FlipDigit(
                    digit = units,
                    width = secDigitWidth,
                    height = secDigitHeight,
                    fontSize = secFontSize,
                    textColor = if (isNightMode) TechCyan else null,
                    isNightMode = isNightMode
                )
            }
        }
    }
}

@Composable
fun BlinkingColon(isNightMode: Boolean, isCompact: Boolean = false, scale: Float = 1.0f) {
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

    val effectiveScale = scale.coerceIn(0.55f, 1.8f)
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val colonColor = when {
        isNightMode -> DarkColon
        isPureInk -> Color(0xFF444444)
        else -> InkColon
    }
    val dotSize = ((if (isCompact) 4.5f else 6.5f) * effectiveScale).dp
    val dotSpacing = ((if (isCompact) 10f else 16f) * effectiveScale).dp
    val colonHeight = ((if (isCompact) 58f else 84f) * effectiveScale).dp
    val hPadding = ((if (isCompact) 4.5f else 10f) * effectiveScale).dp

    Box(
        modifier = Modifier
            .padding(horizontal = hPadding)
            .height(colonHeight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dotSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(colonColor.copy(alpha = alphaAnim.value))
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(colonColor.copy(alpha = alphaAnim.value))
            )
        }
    }
}

/**
 * 仅供 Debug 视觉走查与开源画廊截图使用的全模块脱敏数据叠加（完全内存生成，不触及磁盘设置/缓存与测试隔离）
 */
private fun buildDebugRichPreviewQuota(): Pair<List<CodexAccount>, Map<String, CodexUsageSnapshot>> {
    val nowMs = System.currentTimeMillis()
    val nowUnix = nowMs / 1000L

    val gpt1 = CodexAccount(
        id = "preview_gpt_1",
        alias = "Studio Pro",
        email = "studio***@example.com",
        credentialStatus = CredentialStatus.VALID,
        modelProvider = "CHATGPT",
        isPinnedMain = true
    )
    val gpt2 = CodexAccount(
        id = "preview_gpt_2",
        alias = "Dev Lab",
        email = "dev***@example.com",
        credentialStatus = CredentialStatus.VALID,
        modelProvider = "CHATGPT"
    )
    val ag1 = CodexAccount(
        id = "preview_ag_1",
        alias = "ag***@example.com",
        email = "ag***@example.com",
        credentialStatus = CredentialStatus.VALID,
        modelProvider = "ANTIGRAVITY",
        isPinnedMain = true
    )
    val ds1 = CodexAccount(
        id = "preview_ds_1",
        alias = "Core API",
        credentialStatus = CredentialStatus.VALID,
        modelProvider = "DEEPSEEK",
        isPinnedMain = true
    )
    val ds2 = CodexAccount(
        id = "preview_ds_2",
        alias = "Batch API",
        credentialStatus = CredentialStatus.VALID,
        modelProvider = "DEEPSEEK"
    )

    val g5h = CodexUsageWindow.fromAntigravityBucket("gemini-5h", 0.95, java.time.Instant.ofEpochSecond(nowUnix + 3 * 3600 + 42 * 60).toString())!!
    val gWk = CodexUsageWindow.fromAntigravityBucket("gemini-weekly", 0.84, java.time.Instant.ofEpochSecond(nowUnix + 3 * 86400 + 14 * 3600).toString())!!
    val c5h = CodexUsageWindow.fromAntigravityBucket("3p-5h", 1.0, java.time.Instant.ofEpochSecond(nowUnix + 4 * 3600 + 15 * 60).toString())!!
    val cWk = CodexUsageWindow.fromAntigravityBucket("3p-weekly", 0.62, java.time.Instant.ofEpochSecond(nowUnix + 2 * 86400 + 9 * 3600).toString())!!

    val snapshots = mapOf(
        gpt1.id to CodexUsageSnapshot(
            accountId = gpt1.id,
            accountAlias = gpt1.alias,
            credentialStatus = CredentialStatus.VALID,
            planName = "Plus",
            resetCreditsCount = 3,
            primaryWindow = CodexUsageWindow("5h", "5小时限额", 18000L, 6.0, 94.0, nowUnix + 3 * 3600 + 25 * 60),
            secondaryWindow = CodexUsageWindow("7d", "7天限额", 604800L, 32.0, 68.0, nowUnix + 4 * 86400 + 7 * 3600),
            queriedAt = nowMs,
            lastSuccessTime = nowMs,
            modelProvider = "CHATGPT"
        ),
        gpt2.id to CodexUsageSnapshot(
            accountId = gpt2.id,
            accountAlias = gpt2.alias,
            credentialStatus = CredentialStatus.VALID,
            planName = "Plus",
            resetCreditsCount = 2,
            primaryWindow = CodexUsageWindow("5h", "5小时限额", 18000L, 0.0, 100.0, nowUnix + 4 * 3600 + 50 * 60),
            secondaryWindow = CodexUsageWindow("7d", "7天限额", 604800L, 18.0, 82.0, nowUnix + 5 * 86400 + 11 * 3600),
            queriedAt = nowMs,
            lastSuccessTime = nowMs,
            modelProvider = "CHATGPT"
        ),
        ag1.id to CodexUsageSnapshot(
            accountId = ag1.id,
            accountAlias = ag1.alias,
            credentialStatus = CredentialStatus.VALID,
            planName = "AI Ultra 20x",
            primaryWindow = g5h,
            secondaryWindow = c5h,
            quotaWindows = listOf(g5h, gWk, c5h, cWk),
            queriedAt = nowMs,
            lastSuccessTime = nowMs,
            modelProvider = "ANTIGRAVITY"
        ),
        ds1.id to CodexUsageSnapshot(
            accountId = ds1.id,
            accountAlias = ds1.alias,
            credentialStatus = CredentialStatus.VALID,
            isAvailable = true,
            balanceList = listOf(DeepSeekBalance("CNY", 128.50)),
            queriedAt = nowMs,
            lastSuccessTime = nowMs,
            modelProvider = "DEEPSEEK"
        ),
        ds2.id to CodexUsageSnapshot(
            accountId = ds2.id,
            accountAlias = ds2.alias,
            credentialStatus = CredentialStatus.VALID,
            isAvailable = true,
            balanceList = listOf(DeepSeekBalance("CNY", 64.20)),
            queriedAt = nowMs,
            lastSuccessTime = nowMs,
            modelProvider = "DEEPSEEK"
        )
    )
    return listOf(gpt1, gpt2, ag1, ds1, ds2) to snapshots
}

private fun buildDebugRichPreviewWeather(): List<WeatherInfo> = listOf(
    WeatherInfo(
        cityName = "上海 · 浦东",
        latitude = 31.2304,
        longitude = 121.4737,
        temperature = 21.0,
        apparentTemperature = 20.5,
        weatherCode = 0,
        weatherDescription = "晴朗",
        humidity = 52,
        windSpeed = 1.8,
        isLoaded = true
    )
)

private fun buildDebugRichPreviewTesla(): Pair<TeslaSettings, TeslaFullSnapshot> {
    val nowMs = System.currentTimeMillis()
    val nowIso = java.time.Instant.ofEpochMilli(nowMs - 120_000L).toString()
    val settings = TeslaSettings(
        enabled = true,
        baseUrl = "https://teslamate.example.local",
        dataSourceUid = "demo_pg",
        carId = 1,
        carName = "Model 3",
        refreshIntervalSec = 60,
        timeZone = "Asia/Shanghai",
        allowHttp = false,
        vehicleColor = "RED"
    )
    val startLoc = VehicleLocation(
        latitude = 31.2155,
        longitude = 121.5444,
        geofenceName = "世纪公园超充站",
        resolvedAddress = "上海市浦东新区锦绣路",
        sampledAt = nowIso
    )
    val endLoc = VehicleLocation(
        latitude = 31.2397,
        longitude = 121.4998,
        geofenceName = "陆家嘴金融中心 · B2",
        resolvedAddress = "上海市浦东新区世纪大道 88 号",
        sampledAt = nowIso
    )
    val recentDrives = listOf(
        DailyDriveItem("11-19", 24.5, 134.0),
        DailyDriveItem("11-20", 41.2, 128.0),
        DailyDriveItem("11-21", 18.0, 142.0),
        DailyDriveItem("11-22", 52.4, 126.0),
        DailyDriveItem("11-23", 31.8, 131.0),
        DailyDriveItem("11-24", 27.6, 125.0),
        DailyDriveItem("11-25", 38.6, 129.0)
    )
    val snap = TeslaFullSnapshot(
        car = TeslaVehicle(
            id = 1,
            name = "Model 3 Performance",
            model = "3",
            trimBadging = "78D",
            exteriorColor = "UltraRed"
        ),
        state = VehicleState(state = "online", sampledAt = nowMs - 60_000L),
        snapshot = VehicleSnapshot(
            sampledAt = nowIso,
            batteryLevel = 88,
            ratedBatteryRangeKm = 446.0,
            estBatteryRangeKm = 432.0,
            odometer = 18642.5,
            insideTemp = 21.5,
            outsideTemp = 19.0,
            tpmsPressureFl = 2.9,
            tpmsPressureFr = 2.9,
            tpmsPressureRl = 2.9,
            tpmsPressureRr = 2.9
        ),
        charging = ChargingSnapshot(
            isCharging = false,
            todayCharge = 18.4,
            monthCharge = 216.5
        ),
        drives = DriveSummary(
            todayDistanceKm = 38.6,
            todayDurationMin = 54,
            todayTrips = 2,
            todayEstimatedWhPerKm = 129.0,
            todayEnergyKwh = 4.98,
            monthDistanceKm = 1128.4,
            monthTrips = 46,
            recent7DaysDrives = recentDrives,
            lastDrive = LastDriveInfo(
                distanceKm = 19.4,
                durationMin = 26,
                eventEpochMs = nowMs - 1_800_000L,
                startEpochMs = nowMs - 3_360_000L,
                estimatedWhPerKm = 127.0,
                startLocation = startLoc,
                endLocation = endLoc
            )
        ),
        location = endLoc,
        connectionState = TeslaConnectionState(
            isConfigured = true,
            isLoading = false,
            lastSuccessTime = nowMs
        ),
        queriedAt = nowMs
    )
    return settings to snap
}

private fun buildDebugRichPreviewLongbridge(): Pair<LongbridgeSettings, LongbridgeFullSnapshot> {
    val settings = LongbridgeSettings(
        enabled = true,
        accountAlias = "Longbridge Global",
        region = "GLOBAL",
        displayCurrency = "HKD",
        colorScheme = "RED_UP_GREEN_DOWN",
        showOnHome = true
    )
    return settings to LongbridgeRepository.createDemoSnapshot(settings = settings, isMock = false)
}
