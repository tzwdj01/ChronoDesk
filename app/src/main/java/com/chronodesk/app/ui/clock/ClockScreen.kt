package com.chronodesk.app.ui.clock

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronodesk.app.ui.tesla.TeslaViewModel
import com.chronodesk.app.ui.tesla.TeslaScreen
import com.chronodesk.app.ui.settings.TeslaSettingsDialog
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.ui.settings.AccountManagerDialog
import com.chronodesk.app.ui.settings.DeviceAuthDialog
import com.chronodesk.app.ui.settings.SettingsDialog
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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

    val pageCount = if (teslaSettings.enabled) 3 else 2
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()

    // 页面数量变动时的越界安全保护
    LaunchedEffect(pageCount) {
        if (pagerState.currentPage >= pageCount) {
            pagerState.scrollToPage(0)
        }
    }

    var areControlsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible) {
            delay(6000L)
            areControlsVisible = false
        }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showTeslaSettingsDialog by remember { mutableStateOf(false) }
    var selectedAccountToManage by remember { mutableStateOf<CodexAccount?>(null) }
    var showDeviceAuthDialogForAccount by remember { mutableStateOf<CodexAccount?>(null) }

    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = settings,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated -> viewModel.updateSettings(updated) },
            onOpenTeslaSettings = {
                showSettingsDialog = false
                showTeslaSettingsDialog = true
            },
            onExportSettings = { viewModel.exportSettingsJson() },
            onValidateSettings = { json -> viewModel.validateAndParseSettingsJson(json) },
            onApplySettingsBackup = { backup -> viewModel.applySettingsBackup(backup) }
        )
    }

    if (showTeslaSettingsDialog) {
        TeslaSettingsDialog(
            viewModel = teslaViewModel,
            isNightMode = isNightMode,
            onDismiss = { showTeslaSettingsDialog = false }
        )
    }

    selectedAccountToManage?.let { account ->
        val chatGptAccs = accounts.filter { it.modelProvider == "CHATGPT" || it.modelProvider == null }
        val deepSeekAccs = accounts.filter { it.modelProvider == "DEEPSEEK" }
        val antigravityAccs = accounts.filter { it.modelProvider == "ANTIGRAVITY" }
        val slotIdx = when (account.modelProvider) {
            "ANTIGRAVITY" -> 2 + deepSeekAccs.size + antigravityAccs.indexOfFirst { it.id == account.id }.coerceAtLeast(0)
            "DEEPSEEK" -> 2 + deepSeekAccs.indexOfFirst { it.id == account.id }.coerceAtLeast(0)
            else -> if (chatGptAccs.indexOfFirst { it.id == account.id } == 1) 1 else 0
        }

        AccountManagerDialog(
            initialSlot = slotIdx,
            initialAccountId = account.id,
            accountA = chatGptAccs.getOrNull(0),
            accountB = chatGptAccs.getOrNull(1),
            deepSeekAccounts = deepSeekAccs,
            getDeepSeekApiKey = { id -> viewModel.getDeepSeekApiKey(id) },
            antigravityAccounts = antigravityAccs,
            getAntigravityApiKey = { id -> viewModel.getAntigravityApiKey(id) },
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
                val targetAcc = if (slot == 0) chatGptAccs.getOrNull(0) else chatGptAccs.getOrNull(1)
                if (targetAcc != null) {
                    viewModel.updateAccountAliasById(targetAcc.id, alias)
                } else {
                    viewModel.updateAccountAlias(slot, alias)
                }
            },
            onSaveDeepSeekAccount = { accId, key, alias ->
                viewModel.saveDeepSeekAccount(accId, key, alias)
                selectedAccountToManage = null
            },
            onAddDeepSeekAccount = {
                viewModel.addDeepSeekAccount()
            },
            onRemoveDeepSeekAccount = { accId ->
                viewModel.removeDeepSeekAccount(accId)
                selectedAccountToManage = null
            },
            currentDeepSeekApiKey = viewModel.getDeepSeekApiKey(),
            onSaveDeepSeekApiKey = { key ->
                viewModel.saveDeepSeekApiKey(key)
                selectedAccountToManage = null
                Toast.makeText(context, "DeepSeek API Key 已更新并触发验证", Toast.LENGTH_SHORT).show()
            },
            onSaveAntigravityAccount = { accId, key, alias ->
                viewModel.saveAntigravityAccount(accId, key, alias)
                selectedAccountToManage = null
            },
            onAddAntigravityAccount = {
                viewModel.addAntigravityAccount()
            },
            onRemoveAntigravityAccount = { accId ->
                viewModel.removeAntigravityAccount(accId)
                selectedAccountToManage = null
            },
            currentAntigravityApiKey = viewModel.getAntigravityApiKey(),
            onSaveAntigravityApiKey = { key ->
                viewModel.saveAntigravityAccount("acc_antigravity", key, "Google Antigravity")
                selectedAccountToManage = null
                Toast.makeText(context, "Google Antigravity API Key 已更新并触发验证", Toast.LENGTH_SHORT).show()
            },
            onUpdateAccountVisibility = { accId, showMain, showSecond ->
                viewModel.updateAccountVisibility(accId, showMain, showSecond)
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

    // 主题色彩体系自适应
    val bgColor = if (isNightMode) DarkBackground else InkBackground

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
                IntOffset(burnInShift.first.roundToInt(), burnInShift.second.roundToInt())
            }
    ) {
        val screenHeight = maxHeight
        val isCompactHeight = screenHeight < 380.dp

        // 背景绘制：日间水墨写意山水 / 夜间赛博深空微光流线
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (!isNightMode) {
                // 远山 (浅灰淡墨)
                val farMountain = Path().apply {
                    moveTo(0f, h * 0.88f)
                    cubicTo(w * 0.15f, h * 0.72f, w * 0.38f, h * 0.86f, w * 0.58f, h * 0.76f)
                    cubicTo(w * 0.78f, h * 0.68f, w * 0.90f, h * 0.82f, w, h * 0.74f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(farMountain, color = Color(0xFFDDD7CC).copy(alpha = 0.22f))

                // 近山 (水墨层叠)
                val nearMountain = Path().apply {
                    moveTo(0f, h * 0.93f)
                    cubicTo(w * 0.25f, h * 0.85f, w * 0.48f, h * 0.96f, w * 0.75f, h * 0.86f)
                    cubicTo(w * 0.88f, h * 0.82f, w * 0.95f, h * 0.90f, w, h * 0.84f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(nearMountain, color = Color(0xFFD0C9BC).copy(alpha = 0.28f))
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

        Column(modifier = Modifier.fillMaxSize()) {
            // ================= 顶部公共导航栏 (页面 0 与 页面 1 显示，页面 2 为 Tesla 专属顶栏) =================
            if (pagerState.currentPage != 2) {
                ClockTopBar(
                    isNightMode = isNightMode,
                    accountCount = accounts.count { it.modelProvider in listOf("CHATGPT", "DEEPSEEK", "ANTIGRAVITY") },
                    nightModeOption = settings.nightModeOption,
                    isCompact = isCompactHeight,
                    onAddAccount = {
                        selectedAccountToManage = accounts.firstOrNull() ?: CodexAccount.createPlaceholder()
                    },
                    onRefreshAll = {
                        viewModel.refreshAll()
                        Toast.makeText(context, "已触发全量额度刷新", Toast.LENGTH_SHORT).show()
                    },
                    onToggleNightMode = {
                        val next = viewModel.toggleNightMode()
                        val msg = if (next == "FORCE_NIGHT") "已切换为：深色科技模式" else "已切换为：水墨墨水屏模式"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onOpenSettings = { showSettingsDialog = true }
                )
            }

            // ================= 中间区域：HorizontalPager 双页滑屏 =================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                if (page == 0) {
                    // 第一页：主页面 (大时钟 + 日期/星期/农历 + 气象 + AI 额度看板)
                    if (isLandscape) {
                        val quotaFraction = (settings.quotaWidthPercent / 100f).coerceIn(0.28f, 0.52f)
                        val clockFraction = 1f - quotaFraction

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (isCompactHeight) 14.dp else 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (isCompactHeight) 14.dp else 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧：视觉中心 —— 大尺寸翻牌时钟 + 公历农历 + 气象胶囊
                            Column(
                                modifier = Modifier
                                    .weight(clockFraction)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // 翻牌数字时钟（支持缩放与平移手势及设置）
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                settings.clockOffsetX.roundToInt(),
                                                settings.clockOffsetY.roundToInt()
                                            )
                                        }
                                        .graphicsLayer {
                                            scaleX = settings.clockScale
                                            scaleY = settings.clockScale
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
                                        onClick = { areControlsVisible = !areControlsVisible },
                                        onToggleSeconds = {
                                            val newState = viewModel.toggleShowSeconds()
                                            val tip = if (newState) "已开启秒钟显示" else "已隐藏秒钟（可在设置中重新开启）"
                                            Toast.makeText(context, tip, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                if (areControlsVisible && (settings.clockScale != 1.0f || settings.clockOffsetX != 0f || settings.clockOffsetY != 0f)) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated)
                                            .clickable {
                                                viewModel.updateSettings(
                                                    settings.copy(
                                                        clockScale = 1.0f,
                                                        clockOffsetX = 0f,
                                                        clockOffsetY = 0f
                                                    )
                                                )
                                                Toast.makeText(context, "已复位时钟大小与位置", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "时钟 ${(settings.clockScale * 100).roundToInt()}% · 点击复位居中",
                                            color = TechCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 15.dp))

                                // 公历日期 + 星期 + 农历完整呈现
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(if (isCompactHeight) 5.dp else 7.dp)
                                ) {
                                    // 公历日期 (如: 11月19日)
                                    Text(
                                        text = timeState.dateString,
                                        color = if (isNightMode) TextPrimary else InkTextPrimary,
                                        fontSize = if (isCompactHeight) 13.sp else 16.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = "·",
                                        color = if (isNightMode) TextTertiary else InkTextTertiary,
                                        fontSize = if (isCompactHeight) 12.sp else 14.sp
                                    )

                                    // 星期 (如: 星期四)
                                    Text(
                                        text = timeState.dayOfWeek,
                                        color = if (isNightMode) TechCyan else InkTextPrimary,
                                        fontSize = if (isCompactHeight) 13.sp else 16.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = "·",
                                        color = if (isNightMode) TextTertiary else InkTextTertiary,
                                        fontSize = if (isCompactHeight) 12.sp else 14.sp
                                    )

                                    // 农历 (如: 农历十月初十)
                                    Text(
                                        text = timeState.lunarDateString,
                                        color = if (isNightMode) TextSecondary else InkTextSecondary,
                                        fontSize = if (isCompactHeight) 12.5.sp else 15.5.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }

                                Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 14.dp))

                                // 天气胶囊卡片（支持单地区或多地区多行并列呈现）
                                MultiCityWeatherCapsule(
                                    weatherList = weatherList,
                                    isNightMode = isNightMode,
                                    isCompact = isCompactHeight,
                                    onCardClick = {
                                        viewModel.refreshAll()
                                        Toast.makeText(context, "正在刷新天气与位置…", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            // 右侧：多模型/多账号 AI 额度监控看板 (ChatGPT, DeepSeek)
                            Column(
                                modifier = Modifier
                                    .weight(quotaFraction)
                                    .fillMaxHeight(),
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
                                    isCompact = isCompactHeight,
                                    cardDensity = settings.quotaCardDensity,
                                    onAddAccount = { viewModel.addAccount() },
                                    onRefreshAccount = { id -> viewModel.refreshAccount(id) },
                                    onDeleteAccount = { id ->
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
                                        if (!acc.isConfigured && acc.modelProvider == "CHATGPT") {
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
                            }
                        }
                    } else {
                        // 竖屏备用自适应
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LargeFlipClockDisplay(
                                hour = timeState.hour,
                                minute = timeState.minute,
                                second = if (settings.showSeconds) timeState.second else null,
                                isNightMode = isNightMode,
                                isCompact = false
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = timeState.dateString, color = if (isNightMode) TextPrimary else InkTextPrimary, fontSize = 15.sp)
                                Text(text = timeState.dayOfWeek, color = if (isNightMode) TechCyan else InkTextPrimary, fontSize = 15.sp)
                                Text(text = "·", color = TextTertiary)
                                Text(text = timeState.lunarDateString, color = if (isNightMode) TextSecondary else InkTextSecondary, fontSize = 15.sp)
                            }

                            MultiCityWeatherCapsule(
                                weatherList = weatherList,
                                isNightMode = isNightMode,
                                onCardClick = { viewModel.refreshAll() }
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else if (page == 1) {
                    // 第二页：扩展页面 (AI 模型 Token 监控、官方接口遥测与系统运行状态)
                    ClockExtensionPage(
                        accounts = accounts,
                        snapshots = snapshots,
                        settings = settings,
                        isNightMode = isNightMode,
                        onRefreshAll = {
                            viewModel.refreshAll()
                            Toast.makeText(context, "已触发全量网关同步", Toast.LENGTH_SHORT).show()
                        },
                        onOpenSettings = { showSettingsDialog = true },
                        onManageAccount = {
                            selectedAccountToManage = accounts.firstOrNull() ?: CodexAccount.createPlaceholder()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (page == 2) {
                    // 第三页：Tesla 车辆监控页 (TeslaMate / Grafana API)
                    TeslaScreen(
                        snapshot = teslaSnapshot,
                        settings = teslaSettings,
                        isNightMode = isNightMode,
                        currentTimeString = String.format("%02d:%02d:%02d", timeState.hour, timeState.minute, timeState.second),
                        isCompact = isCompactHeight,
                        onRefresh = {
                            teslaViewModel.refreshNow()
                            Toast.makeText(context, "正在刷新车辆实时状态…", Toast.LENGTH_SHORT).show()
                        },
                        onOpenSettings = { showTeslaSettingsDialog = true },
                        onVisibilityChanged = { visible ->
                            teslaViewModel.setPageVisible(visible)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // ================= 底部：多页面指示器小圆点 [ ● ○ ] 或 [ ● ○ ○ ] =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isCompactHeight) 3.dp else 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until pageCount) {
                    val isSelected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(5.dp)
                            .width(if (isSelected) 16.dp else 5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(
                                if (isSelected) {
                                    if (isNightMode) TechCyan else InkTextPrimary
                                } else {
                                    if (isNightMode) DarkBorder else InkBorder
                                }
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(i)
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * 屏幕顶栏：左侧品牌标语，右侧控制胶囊按钮
 */
@Composable
private fun ClockTopBar(
    isNightMode: Boolean,
    accountCount: Int,
    nightModeOption: String,
    isCompact: Boolean = false,
    onAddAccount: () -> Unit,
    onRefreshAll: () -> Unit,
    onToggleNightMode: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonBg = if (isNightMode) DarkSurface else InkSurface
    val buttonBorder = if (isNightMode) DarkBorder else InkBorder
    val buttonText = if (isNightMode) TextPrimary else InkTextPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 14.dp else 20.dp, vertical = if (isCompact) 2.dp else 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 右侧操作按钮组
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
        ) {
            // 账号管理按钮
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(buttonBg)
                    .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                    .clickable { onAddAccount() }
                    .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = if (isCompact) 3.dp else 4.5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加账号",
                        tint = if (isNightMode) TechCyan else InkTextPrimary,
                        modifier = Modifier.size(if (isCompact) 11.dp else 12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "账号管理 ($accountCount)",
                        color = buttonText,
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 全部刷新按钮
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(buttonBg)
                    .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                    .clickable { onRefreshAll() }
                    .padding(horizontal = if (isCompact) 7.dp else 9.dp, vertical = if (isCompact) 3.dp else 4.5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新全部",
                        tint = buttonText,
                        modifier = Modifier.size(if (isCompact) 11.dp else 12.dp)
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

            // 日夜/水墨主题切换按钮
            val themeIcon = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(buttonBg)
                    .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                    .clickable { onToggleNightMode() }
                    .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = if (isCompact) 3.dp else 4.5.dp)
            ) {
                Icon(
                    imageVector = themeIcon,
                    contentDescription = "日夜主题切换",
                    tint = if (isNightMode) TechAmber else InkTextPrimary,
                    modifier = Modifier.size(if (isCompact) 11.5.dp else 12.5.dp)
                )
            }

            // 设置按钮
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(buttonBg)
                    .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = if (isCompact) 3.dp else 4.5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "系统设置",
                        tint = buttonText,
                        modifier = Modifier.size(if (isCompact) 11.dp else 12.dp)
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
    onClick: () -> Unit = {},
    onToggleSeconds: () -> Unit = {}
) {
    val hasSeconds = second != null
    // 动态黄金比例：当秒钟隐藏时，时分牌面适度放大，更加雄伟突出
    val digitWidth = if (hasSeconds) {
        if (isCompact) 56.dp else 74.dp
    } else {
        if (isCompact) 64.dp else 84.dp
    }
    val digitHeight = if (hasSeconds) {
        if (isCompact) 106.dp else 142.dp
    } else {
        if (isCompact) 118.dp else 156.dp
    }
    val fontSize = if (hasSeconds) {
        if (isCompact) 68.sp else 92.sp
    } else {
        if (isCompact) 76.sp else 102.sp
    }

    val secDigitWidth = if (isCompact) 20.dp else 28.dp
    val secDigitHeight = if (isCompact) 34.dp else 46.dp
    val secFontSize = if (isCompact) 17.sp else 22.sp

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
            isNightMode = isNightMode
        )

        // 冒号
        BlinkingColon(isNightMode = isNightMode, isCompact = isCompact)

        // 分钟翻牌
        FlipDigitPair(
            value = minute,
            digitWidth = digitWidth,
            digitHeight = digitHeight,
            fontSize = fontSize,
            isNightMode = isNightMode
        )

        // 秒钟翻牌：水平双位并排翻牌，完美对应参考图设计；点击秒钟可快速隐藏/切换
        if (second != null) {
            Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
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
fun BlinkingColon(isNightMode: Boolean, isCompact: Boolean = false) {
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

    val colonColor = if (isNightMode) DarkColon else InkColon
    val dotSize = if (isCompact) 4.5.dp else 6.5.dp
    val dotSpacing = if (isCompact) 10.dp else 16.dp
    val colonHeight = if (isCompact) 58.dp else 84.dp
    val hPadding = if (isCompact) 4.dp else 10.dp

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
