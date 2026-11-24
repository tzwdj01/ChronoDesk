package com.chronodesk.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.chronodesk.app.data.model.LongbridgeSettings
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chronodesk.app.data.model.ClockSettings
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.network.GeocodingItem
import com.chronodesk.app.data.repository.GeneralSettingsBackup
import com.chronodesk.app.data.repository.SettingsValidationResult
import com.chronodesk.app.ui.clock.AiModelProvider
import com.chronodesk.app.ui.clock.ModelProviderLogo
import com.chronodesk.app.ui.longbridge.LongbridgeViewModel
import com.chronodesk.app.ui.tesla.TeslaViewModel
import com.chronodesk.app.ui.theme.DarkBackground
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBackground
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkBorderSubtle
import com.chronodesk.app.ui.theme.InkStatusGreen
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkSurfaceElevated
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkTextTertiary
import com.chronodesk.app.ui.theme.InkWarningAmber
import com.chronodesk.app.ui.theme.InkWarningRed
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary
import com.chronodesk.app.ui.theme.resolveChronoPalette
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SettingsTab(val label: String, val shortLabel: String) {
    ACCOUNTS("账号与接口", "账号接口"),
    DISPLAY("显示与外观", "显示外观"),
    CLOCK_LAYOUT("时钟与排版", "时钟排版"),
    WEATHER("天气与城市", "天气城市"),
    VEHICLE_SYNC("车辆与同步", "车辆同步"),
    PORTFOLIO("长桥与资产", "长桥资产"),
    BACKUP("备份与迁移", "备份迁移"),
    ABOUT("关于项目", "关于")
}

private data class SettingsPalette(
    val isNight: Boolean,
    val dialogBg: Color,
    val sidebarBg: Color,
    val cardBg: Color,
    val chipBg: Color,
    val border: Color,
    val divider: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val onAccent: Color,
    val positive: Color,
    val warning: Color,
    val danger: Color
) {
    companion object {
        fun from(isNightMode: Boolean, isPureInk: Boolean = false): SettingsPalette {
            val t = resolveChronoPalette(isNightMode, !isNightMode && isPureInk)
            return when {
                isNightMode -> SettingsPalette(
                    isNight = true,
                    dialogBg = DarkSurface,
                    sidebarBg = DarkBackground.copy(alpha = 0.72f),
                    cardBg = DarkSurfaceElevated,
                    chipBg = DarkBackground,
                    border = DarkBorder,
                    divider = DarkBorder.copy(alpha = 0.65f),
                    primaryText = TextPrimary,
                    secondaryText = TextSecondary,
                    mutedText = TextTertiary,
                    accent = TechCyan,
                    onAccent = Color.Black,
                    positive = TechEmerald,
                    warning = TechAmber,
                    danger = TechRose
                )
                isPureInk -> SettingsPalette(
                    isNight = false,
                    dialogBg = t.bg,
                    sidebarBg = t.surfaceElevated,
                    cardBg = t.surface,
                    chipBg = t.surfaceElevated,
                    border = t.border,
                    divider = t.dividerSubtle,
                    primaryText = t.primaryText,
                    secondaryText = t.secondaryText,
                    mutedText = t.mutedText,
                    accent = Color(0xFF222222),
                    onAccent = Color.White,
                    positive = t.positive,
                    warning = t.warning,
                    danger = t.danger
                )
                else -> SettingsPalette(
                    isNight = false,
                    dialogBg = InkBackground,
                    sidebarBg = InkSurfaceElevated,
                    cardBg = InkSurface,
                    chipBg = InkSurfaceElevated,
                    border = InkBorder,
                    divider = InkBorderSubtle,
                    primaryText = InkTextPrimary,
                    secondaryText = InkTextSecondary,
                    mutedText = InkTextTertiary,
                    accent = Color(0xFF0284C7),
                    onAccent = Color.White,
                    positive = InkStatusGreen,
                    warning = InkWarningAmber,
                    danger = InkWarningRed
                )
            }
        }
    }
}

private data class LocationItem(val name: String, val lat: Double, val lng: Double)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentSettings: ClockSettings,
    isNightMode: Boolean = false,
    initialTab: SettingsTab = SettingsTab.DISPLAY,
    teslaStatusSummary: String = "",
    accounts: List<CodexAccount> = emptyList(),
    initialAccountId: String? = null,
    getDeepSeekApiKey: (accountId: String) -> String? = { null },
    getAntigravityApiKey: (accountId: String) -> String? = { null },
    onSaveCodexAccount: (slot: Int, account: CodexAccount, refreshToken: String, accessToken: String?) -> Boolean = { _, _, _, _ -> false },
    onRemoveCodexAccount: (slot: Int) -> Unit = {},
    onUpdateCodexAlias: (slot: Int, newAlias: String) -> Unit = { _, _ -> },
    onSaveDeepSeekAccount: (accountId: String, apiKey: String, alias: String) -> Boolean = { _, _, _ -> false },
    onAddDeepSeekAccount: () -> CodexAccount? = { null },
    onRemoveDeepSeekAccount: (accountId: String) -> Unit = {},
    onSaveAntigravityAccount: (accountId: String, apiKey: String, alias: String) -> Boolean = { _, _, _ -> false },
    onSaveAntigravityOAuthAccount: (
        accountId: String,
        alias: String?,
        email: String?,
        refreshToken: String?,
        accessToken: String?,
        expiresInSeconds: Long,
        idToken: String?,
        providerType: String
    ) -> Boolean = { _, _, _, _, _, _, _, _ -> false },
    onUpdateAntigravityAlias: (accountId: String, newAlias: String) -> Unit = { _, _ -> },
    onAddAntigravityAccount: () -> CodexAccount? = { null },
    onRemoveAntigravityAccount: (accountId: String) -> Unit = {},
    onUpdateAccountVisibility: (accountId: String, showOnMain: Boolean, showOnSecond: Boolean, isPinnedMain: Boolean) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit,
    onSave: (ClockSettings) -> Unit,
    onOpenTeslaSettings: () -> Unit = {},
    onExportSettings: () -> String = { "" },
    onValidateSettings: (String) -> SettingsValidationResult = { SettingsValidationResult.Invalid("未实现") },
    longbridgeSettings: LongbridgeSettings = LongbridgeSettings(),
    longbridgeHasCredentials: Boolean = false,
    onUpdateLongbridgeSettings: ((LongbridgeSettings) -> LongbridgeSettings) -> Unit = {},
    onOpenLongbridgeSettings: () -> Unit = {},
    teslaViewModel: TeslaViewModel? = null,
    longbridgeViewModel: LongbridgeViewModel? = null,
    onApplySettingsBackup: (GeneralSettingsBackup) -> Unit = {},
    onRequestLocationPermission: () -> Unit = {},
    onSearchLocations: suspend (String) -> List<GeocodingItem> = { emptyList() }
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenHeightDp < 380 || configuration.screenWidthDp < 700
    var nightModeOption by remember(currentSettings) { mutableStateOf(currentSettings.nightModeOption) }
    var dayThemeStyle by remember(currentSettings) { mutableStateOf(currentSettings.normalizedDayThemeStyle) }
    val palette = remember(isNightMode, dayThemeStyle) { SettingsPalette.from(isNightMode, dayThemeStyle == "INK") }

    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val detailScrollState = rememberScrollState()
    LaunchedEffect(selectedTab) {
        detailScrollState.scrollTo(0)
    }

    var previewBackup by remember { mutableStateOf<GeneralSettingsBackup?>(null) }
    var previewSummary by remember { mutableStateOf<String?>(null) }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = onExportSettings()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "通用设置已成功导出", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                when (val result = onValidateSettings(json)) {
                    is SettingsValidationResult.Valid -> {
                        previewBackup = result.backup
                        previewSummary = result.previewSummary
                    }
                    is SettingsValidationResult.Invalid -> {
                        importErrorMessage = result.errorMessage
                    }
                }
            } catch (e: Exception) {
                importErrorMessage = "读取文件失败: ${e.message}"
            }
        }
    }

    if (previewBackup != null && previewSummary != null) {
        AlertDialog(
            onDismissRequest = {
                previewBackup = null
                previewSummary = null
            },
            containerColor = palette.cardBg,
            title = {
                Text("确认导入设置", color = palette.primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("检测到合法的通用设置备份文件，内容预览如下：", color = palette.secondaryText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.chipBg)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = previewSummary ?: "",
                            color = palette.accent,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "提示：此操作仅更新界面、时钟与显示偏好，不含账号与凭据数据。",
                        color = palette.mutedText,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backup = previewBackup
                        if (backup != null) {
                            onApplySettingsBackup(backup)
                            Toast.makeText(context, "已成功应用导入的设置", Toast.LENGTH_SHORT).show()
                            previewBackup = null
                            previewSummary = null
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent)
                ) {
                    Text("确认应用", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        previewBackup = null
                        previewSummary = null
                    },
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text("取消", color = palette.secondaryText)
                }
            }
        )
    }

    if (importErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { importErrorMessage = null },
            containerColor = palette.cardBg,
            title = {
                Text("导入失败", color = palette.danger, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(importErrorMessage ?: "", color = palette.secondaryText, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = { importErrorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent)
                ) {
                    Text("确定", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    var is24Hour by remember { mutableStateOf(currentSettings.is24Hour) }
    var showSeconds by remember { mutableStateOf(currentSettings.showSeconds) }
    var screenOnPolicy by remember { mutableStateOf(currentSettings.screenOnPolicy) }
    val autoNightDim = currentSettings.autoNightDim
    var allowNightSleep by remember { mutableStateOf(currentSettings.allowNightSleep) }
    var nightBrightness by remember { mutableFloatStateOf(currentSettings.nightBrightness) }
    var refreshInterval by remember { mutableIntStateOf(currentSettings.autoRefreshIntervalMinutes) }
    var autoLocation by remember { mutableStateOf(currentSettings.autoLocation) }
    var clockScale by remember { mutableFloatStateOf(currentSettings.clockScale) }
    var clockOffsetX by remember { mutableFloatStateOf(currentSettings.clockOffsetX) }
    var clockOffsetY by remember { mutableFloatStateOf(currentSettings.clockOffsetY) }
    var quotaWidthPercent by remember { mutableIntStateOf(currentSettings.quotaWidthPercent) }
    var quotaCardDensity by remember { mutableStateOf(currentSettings.quotaCardDensity) }
    var quotaDisplayMode by remember { mutableStateOf(currentSettings.quotaDisplayMode) }
    var quotaProviderOrder by remember { mutableStateOf(currentSettings.orderedProviders()) }
    var showTeslaOnHome by remember { mutableStateOf(currentSettings.showTeslaOnHome) }
    var useMockData by remember { mutableStateOf(currentSettings.useMockData) }

    // 车辆监控内嵌草稿状态（直接接入主设置「车辆同步」页，保持原生 UI）
    val currentTeslaSettings = teslaViewModel?.let { it.settings.collectAsState().value } ?: TeslaSettings()
    val isTeslaTesting = teslaViewModel?.let { it.isTestingConnection.collectAsState().value } ?: false
    val teslaTestResult = teslaViewModel?.let { it.testResult.collectAsState().value }
    val discoveredDatasources = teslaViewModel?.let { it.discoveredDatasources.collectAsState().value }.orEmpty()
    val discoveredCars = teslaViewModel?.let { it.discoveredCars.collectAsState().value }.orEmpty()
    val initialTeslaToken = remember(teslaViewModel) { teslaViewModel?.getStoredToken().orEmpty() }

    var teslaEnabled by remember(currentTeslaSettings) { mutableStateOf(currentTeslaSettings.enabled) }
    var teslaBaseUrl by remember(currentTeslaSettings) { mutableStateOf(currentTeslaSettings.baseUrl) }
    var teslaToken by remember(initialTeslaToken) { mutableStateOf(initialTeslaToken) }
    var isTeslaTokenVisible by remember { mutableStateOf(false) }
    var teslaDataSourceUid by remember(currentTeslaSettings) { mutableStateOf(currentTeslaSettings.dataSourceUid) }
    var teslaDataSourceType by remember(currentTeslaSettings) { mutableStateOf(currentTeslaSettings.dataSourceType) }
    var teslaCarId by remember(currentTeslaSettings) { mutableIntStateOf(currentTeslaSettings.carId) }
    var teslaCarName by remember(currentTeslaSettings) { mutableStateOf(currentTeslaSettings.carName) }
    var teslaRefreshSec by remember(currentTeslaSettings) { mutableIntStateOf(currentTeslaSettings.refreshIntervalSec) }
    var teslaTimeZone by remember(currentTeslaSettings) { mutableStateOf(currentTeslaSettings.timeZone) }
    var teslaAllowHttp by remember(currentTeslaSettings) { mutableStateOf(currentTeslaSettings.allowHttp) }
    var teslaVehicleColor by remember(currentTeslaSettings) { mutableStateOf(currentTeslaSettings.vehicleColor) }

    LaunchedEffect(discoveredDatasources) {
        if (teslaDataSourceUid.isBlank() && discoveredDatasources.isNotEmpty()) {
            teslaDataSourceUid = discoveredDatasources.first().uid
        }
    }
    LaunchedEffect(discoveredCars) {
        if (discoveredCars.isNotEmpty() && discoveredCars.none { it.id == teslaCarId }) {
            teslaCarId = discoveredCars.first().id
            teslaCarName = discoveredCars.first().name ?: "Tesla"
        }
    }

    // 长桥授权子弹窗（内嵌触发，无需先跳离设置页）
    var showLongbridgeAuthModal by remember { mutableStateOf(false) }
    if (showLongbridgeAuthModal && longbridgeViewModel != null) {
        LongbridgeAuthModalDialog(
            viewModel = longbridgeViewModel,
            selectedRegion = longbridgeSettings.normalizedRegion,
            isNightMode = isNightMode,
            onRegionSyncBeforeAuth = { chosenRegion ->
                longbridgeViewModel.updateSettings { it.copy(region = chosenRegion, enabled = true) }
            },
            onAuthSuccess = {
                showLongbridgeAuthModal = false
                Toast.makeText(context, "长桥只读授权成功，已自动同步真实资产", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showLongbridgeAuthModal = false }
        )
    }

    var locationList by remember {
        val list = mutableListOf<LocationItem>()
        if (currentSettings.cityName.isNotBlank()) {
            list.add(LocationItem(currentSettings.cityName, currentSettings.latitude, currentSettings.longitude))
        }
        if (currentSettings.additionalCities.isNotBlank()) {
            currentSettings.additionalCities.split(";").forEach { item ->
                val parts = item.split(":")
                if (parts.size >= 3) {
                    val n = parts[0].trim()
                    val la = parts[1].toDoubleOrNull()
                    val lo = parts[2].toDoubleOrNull()
                    if (n.isNotBlank() && la != null && lo != null) {
                        val key = String.format(Locale.US, "%.4f_%.4f", la, lo)
                        if (list.none { String.format(Locale.US, "%.4f_%.4f", it.lat, it.lng) == key }) {
                            list.add(LocationItem(n, la, lo))
                        }
                    }
                }
            }
        }
        mutableStateOf<List<LocationItem>>(list)
    }

    var weatherAddMode by remember { mutableIntStateOf(0) } // 0: 搜索地名, 1: 经纬度录入, 2: 常用预设
    var weatherSearchQuery by remember { mutableStateOf("") }
    var weatherSearchResults by remember { mutableStateOf<List<GeocodingItem>>(emptyList()) }
    var isWeatherSearching by remember { mutableStateOf(false) }
    var weatherSearchMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var manualLat by remember { mutableStateOf("") }
    var manualLng by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }

    val triggerWeatherSearch: () -> Unit = {
        if (weatherSearchQuery.isBlank()) {
            weatherSearchMessage = "请输入地名或拼音关键词"
        } else {
            isWeatherSearching = true
            weatherSearchMessage = null
            coroutineScope.launch {
                try {
                    val results = onSearchLocations(weatherSearchQuery)
                    weatherSearchResults = results
                    if (results.isEmpty()) {
                        weatherSearchMessage = "未找到匹配地点，请换个关键词或使用经纬度录入"
                    }
                } catch (e: Exception) {
                    weatherSearchMessage = "搜索异常: ${e.message}"
                } finally {
                    isWeatherSearching = false
                }
            }
        }
    }

    val cityPresets = listOf(
        Triple("北京", 39.9042, 116.4074),
        Triple("上海", 31.2304, 121.4737),
        Triple("深圳", 22.5431, 114.0579),
        Triple("广州", 23.1291, 113.2644),
        Triple("杭州", 30.2741, 120.1551),
        Triple("温州", 28.0006, 120.6994),
        Triple("延吉", 42.8913, 129.5089),
        Triple("南通", 31.9802, 120.8943),
        Triple("成都", 30.5728, 104.0668),
        Triple("武汉", 30.5928, 114.3055),
        Triple("西安", 34.3416, 108.9398),
        Triple("南京", 32.0603, 118.7969),
        Triple("重庆", 29.5630, 106.5516),
        Triple("香港", 22.3193, 114.1694),
        Triple("台北", 25.0330, 121.5654),
        Triple("东京", 35.6762, 139.6503),
        Triple("纽约", 40.7128, -74.0060)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 0.96f else 0.92f)
                .fillMaxHeight(if (isCompact) 0.94f else 0.90f)
                .widthIn(max = 820.dp)
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.dialogBg)
                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. 顶部极简标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 38.dp else 48.dp)
                        .padding(start = 16.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(if (isCompact) 17.dp else 20.dp)
                        )
                        Text(
                            text = "系统与显示设置",
                            color = palette.primaryText,
                            fontSize = if (isCompact) 14.5.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = if (isCompact) 38.dp else 48.dp, minHeight = if (isCompact) 38.dp else 48.dp)
                            .clickable(role = Role.Button, onClickLabel = "关闭设置") { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = palette.secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = palette.divider, thickness = 1.dp)

                // 2. 中部双栏主从视图 (左：分组导航栏，右：圆角卡片细节区)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // 左侧导航栏
                    val navItems = listOf(
                        SettingsTab.ACCOUNTS to Icons.Default.VpnKey,
                        SettingsTab.DISPLAY to Icons.Default.DarkMode,
                        SettingsTab.CLOCK_LAYOUT to Icons.Default.Tune,
                        SettingsTab.WEATHER to Icons.Default.Place,
                        SettingsTab.VEHICLE_SYNC to Icons.Default.DirectionsCar,
                        SettingsTab.PORTFOLIO to Icons.Default.AccountBalanceWallet,
                        SettingsTab.BACKUP to Icons.Default.FileUpload,
                        SettingsTab.ABOUT to Icons.Default.Info
                    )

                    Column(
                        modifier = Modifier
                            .width(if (isCompact) 114.dp else 148.dp)
                            .fillMaxHeight()
                            .background(palette.sidebarBg)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = if (isCompact) 4.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isCompact) 1.5.dp else 6.dp)
                    ) {
                        navItems.forEach { (tab, icon) ->
                            val isSelected = selectedTab == tab
                            SettingsSidebarItem(
                                label = if (isCompact) tab.shortLabel else tab.label,
                                icon = icon,
                                isSelected = isSelected,
                                isCompact = isCompact,
                                palette = palette,
                                onClick = { selectedTab = tab }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(palette.divider)
                    )

                    // 右侧详情内容滚动区
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(detailScrollState)
                            .padding(
                                horizontal = if (isCompact) 12.dp else 18.dp,
                                vertical = if (isCompact) 10.dp else 14.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 12.dp)
                    ) {
                        when (selectedTab) {
                            SettingsTab.ACCOUNTS -> {
                                AccountsSettingsTabContent(
                                    accounts = accounts,
                                    initialAccountId = initialAccountId,
                                    refreshInterval = refreshInterval,
                                    onRefreshIntervalChange = { refreshInterval = it },
                                    useMockData = useMockData,
                                    onUseMockDataChange = { useMockData = it },
                                    isNightMode = isNightMode,
                                    isCompact = isCompact,
                                    palette = palette,
                                    getDeepSeekApiKey = getDeepSeekApiKey,
                                    getAntigravityApiKey = getAntigravityApiKey,
                                    onSaveCodexAccount = onSaveCodexAccount,
                                    onRemoveCodexAccount = onRemoveCodexAccount,
                                    onUpdateCodexAlias = onUpdateCodexAlias,
                                    onSaveDeepSeekAccount = onSaveDeepSeekAccount,
                                    onAddDeepSeekAccount = onAddDeepSeekAccount,
                                    onRemoveDeepSeekAccount = onRemoveDeepSeekAccount,
                                    onSaveAntigravityAccount = onSaveAntigravityAccount,
                                    onSaveAntigravityOAuthAccount = onSaveAntigravityOAuthAccount,
                                    onUpdateAntigravityAlias = onUpdateAntigravityAlias,
                                    onAddAntigravityAccount = onAddAntigravityAccount,
                                    onRemoveAntigravityAccount = onRemoveAntigravityAccount,
                                    onUpdateAccountVisibility = onUpdateAccountVisibility
                                )
                            }

                            SettingsTab.DISPLAY -> {
                                SettingsGroupCard(
                                    title = "主题与日夜模式",
                                    palette = palette
                                ) {
                                    SegmentedOptionRow(
                                        options = listOf(
                                            "AUTO" to "自动 (22:00-07:00)",
                                            "FORCE_DAY" to "强制日间",
                                            "FORCE_NIGHT" to "强制夜间"
                                        ),
                                        selectedKey = nightModeOption,
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { nightModeOption = it }
                                    )
                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                    Text("日间视觉风格", color = palette.secondaryText, fontSize = 11.sp)
                                    SegmentedOptionRow(
                                        options = listOf(
                                            "COLOR" to "日间彩色（原彩图标）",
                                            "INK" to "全水墨（纯黑白灰）"
                                        ),
                                        selectedKey = dayThemeStyle,
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { dayThemeStyle = it }
                                    )
                                    if (nightModeOption != "FORCE_DAY") {
                                        HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("夜间窗口暗度", color = palette.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Text("${(nightBrightness * 100).roundToInt()}%", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("超暗省电且夜间防刺眼", color = palette.mutedText, fontSize = 10.sp)
                                            Slider(
                                                value = nightBrightness,
                                                onValueChange = { nightBrightness = it },
                                                valueRange = 0.02f..0.30f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = palette.accent,
                                                    activeTrackColor = palette.accent,
                                                    inactiveTrackColor = palette.border
                                                )
                                            )
                                        }
                                    }
                                }

                                SettingsGroupCard(
                                    title = "屏幕常亮策略",
                                    subtitle = "控制应用前台常亮锁（FLAG_KEEP_SCREEN_ON），避免充电静置时被系统压暗或息屏",
                                    palette = palette
                                ) {
                                    SegmentedOptionRow(
                                        options = listOf(
                                            "CHARGING_ONLY" to "仅充电时常亮",
                                            "ALWAYS" to "前台常亮",
                                            "SYSTEM" to "跟随系统"
                                        ),
                                        selectedKey = screenOnPolicy,
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { screenOnPolicy = it }
                                    )
                                    if (screenOnPolicy != "SYSTEM") {
                                        HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                        SettingSwitchRow(
                                            title = "夜间时段允许系统自动息屏 (22:00-07:00)",
                                            subtitle = "开启后在夜间时段释放常亮锁交由系统变暗/息屏；关闭则全天候按策略保持亮屏",
                                            checked = allowNightSleep,
                                            palette = palette,
                                            onCheckedChange = { allowNightSleep = it }
                                        )
                                    }
                                }
                            }

                            SettingsTab.CLOCK_LAYOUT -> {
                                SettingsGroupCard(
                                    title = "右侧额度看板排版",
                                    subtitle = "支持「纯数字 / 进度条」切换，与 ChatGPT / Antigravity / DeepSeek 自由排序",
                                    palette = palette
                                ) {
                                    Text("额度展示模式", color = palette.secondaryText, fontSize = 11.sp)
                                    SegmentedOptionRow(
                                        options = listOf(
                                            "NUMBERS_ONLY" to "纯数字看板",
                                            "PROGRESS_BAR" to "进度条看板"
                                        ),
                                        selectedKey = if (quotaDisplayMode.equals("PROGRESS_BAR", ignoreCase = true)) {
                                            "PROGRESS_BAR"
                                        } else {
                                            "NUMBERS_ONLY"
                                        },
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { quotaDisplayMode = it }
                                    )
                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                    Text("平台卡片显示顺序", color = palette.secondaryText, fontSize = 11.sp)
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        quotaProviderOrder.forEachIndexed { index, code ->
                                            val (providerEnum, labelText, subHint) = when (code) {
                                                "CHATGPT" -> Triple(AiModelProvider.CHATGPT, "ChatGPT", "5h 与周双额度")
                                                "ANTIGRAVITY" -> Triple(AiModelProvider.ANTIGRAVITY, "Antigravity", "Gemini · Claude/GPT 双轨")
                                                else -> Triple(AiModelProvider.DEEPSEEK, "DeepSeek", "官方 API 余额")
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(palette.chipBg)
                                                    .border(0.8.dp, palette.border, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = if (isCompact) 4.dp else 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "${index + 1}.",
                                                        color = palette.accent,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    ModelProviderLogo(
                                                        provider = providerEnum,
                                                        size = 18.dp,
                                                        isNightMode = isNightMode
                                                    )
                                                    Text(
                                                        text = labelText,
                                                        color = palette.primaryText,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = subHint,
                                                        color = palette.mutedText,
                                                        fontSize = 9.5.sp
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (index > 0) {
                                                                val next = quotaProviderOrder.toMutableList()
                                                                val tmp = next[index - 1]
                                                                next[index - 1] = next[index]
                                                                next[index] = tmp
                                                                quotaProviderOrder = next
                                                            }
                                                        },
                                                        enabled = index > 0,
                                                        modifier = Modifier.size(if (isCompact) 26.dp else 30.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowUp,
                                                            contentDescription = "上移 $labelText",
                                                            tint = if (index > 0) palette.primaryText else palette.mutedText.copy(alpha = 0.35f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            if (index < quotaProviderOrder.lastIndex) {
                                                                val next = quotaProviderOrder.toMutableList()
                                                                val tmp = next[index + 1]
                                                                next[index + 1] = next[index]
                                                                next[index] = tmp
                                                                quotaProviderOrder = next
                                                            }
                                                        },
                                                        enabled = index < quotaProviderOrder.lastIndex,
                                                        modifier = Modifier.size(if (isCompact) 26.dp else 30.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowDown,
                                                            contentDescription = "下移 $labelText",
                                                            tint = if (index < quotaProviderOrder.lastIndex) palette.primaryText else palette.mutedText.copy(alpha = 0.35f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("右侧额度宽度占比: $quotaWidthPercent%", color = palette.secondaryText, fontSize = 11.5.sp)
                                        Text("时钟区: ${100 - quotaWidthPercent}%", color = palette.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = quotaWidthPercent.toFloat(),
                                        onValueChange = { quotaWidthPercent = it.roundToInt() },
                                        valueRange = 28f..50f,
                                        steps = 21,
                                        colors = SliderDefaults.colors(
                                            thumbColor = palette.accent,
                                            activeTrackColor = palette.accent,
                                            inactiveTrackColor = palette.border
                                        )
                                    )
                                    SegmentedOptionRow(
                                        options = listOf(
                                            34 to "紧凑 34%",
                                            38 to "标准 38%",
                                            43 to "均衡 43%",
                                            48 to "宽体 48%"
                                        ),
                                        selectedKey = quotaWidthPercent,
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { quotaWidthPercent = it }
                                    )
                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                    Text("右侧额度卡片间距密度", color = palette.secondaryText, fontSize = 11.sp)
                                    SegmentedOptionRow(
                                        options = listOf(
                                            "COMPACT" to "紧凑",
                                            "NORMAL" to "标准",
                                            "SPACIOUS" to "宽松"
                                        ),
                                        selectedKey = quotaCardDensity,
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { quotaCardDensity = it }
                                    )
                                }

                                SettingsGroupCard(
                                    title = "时钟显示格式",
                                    palette = palette
                                ) {
                                    SettingSwitchRow(
                                        title = "24 小时制",
                                        subtitle = "关闭则采用 12 小时制显示",
                                        checked = is24Hour,
                                        palette = palette,
                                        onCheckedChange = { is24Hour = it }
                                    )
                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                    SettingSwitchRow(
                                        title = "显示秒钟",
                                        subtitle = "在主时钟右侧并排显示翻页秒针",
                                        checked = showSeconds,
                                        palette = palette,
                                        onCheckedChange = { showSeconds = it }
                                    )
                                }

                                SettingsGroupCard(
                                    title = "时钟尺寸与位置微调",
                                    headerTrailing = {
                                        TextButton(
                                            onClick = {
                                                clockScale = 1.0f
                                                clockOffsetX = 0f
                                                clockOffsetY = 0f
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("一键复位 (100%)", color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    },
                                    palette = palette
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("时钟大小比例", color = palette.secondaryText, fontSize = 11.sp)
                                        Text("${(clockScale * 100).roundToInt()}%", color = palette.accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = clockScale,
                                        onValueChange = { clockScale = (it * 20).roundToInt() / 20f },
                                        valueRange = 0.6f..1.8f,
                                        steps = 23,
                                        colors = SliderDefaults.colors(
                                            thumbColor = palette.accent,
                                            activeTrackColor = palette.accent,
                                            inactiveTrackColor = palette.border
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(0.8f to "80%", 1.0f to "100%", 1.25f to "125%", 1.5f to "150%").forEach { (sc, label) ->
                                            val isSel = abs(clockScale - sc) < 0.05f
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSel) palette.accent.copy(alpha = 0.18f) else palette.chipBg)
                                                    .border(1.dp, if (isSel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                                                    .clickable { clockScale = sc }
                                                    .padding(vertical = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSel) palette.accent else palette.secondaryText,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("水平偏移", color = palette.secondaryText, fontSize = 11.sp)
                                        Text(
                                            text = if (clockOffsetX.toInt() == 0) "居中" else "${clockOffsetX.toInt()} px",
                                            color = if (clockOffsetX.toInt() == 0) palette.mutedText else palette.accent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = clockOffsetX,
                                        onValueChange = { clockOffsetX = it.roundToInt().toFloat() },
                                        valueRange = -250f..250f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = palette.accent,
                                            activeTrackColor = palette.accent,
                                            inactiveTrackColor = palette.border
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("垂直偏移", color = palette.secondaryText, fontSize = 11.sp)
                                        Text(
                                            text = if (clockOffsetY.toInt() == 0) "居中" else "${clockOffsetY.toInt()} px",
                                            color = if (clockOffsetY.toInt() == 0) palette.mutedText else palette.accent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = clockOffsetY,
                                        onValueChange = { clockOffsetY = it.roundToInt().toFloat() },
                                        valueRange = -180f..180f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = palette.accent,
                                            activeTrackColor = palette.accent,
                                            inactiveTrackColor = palette.border
                                        )
                                    )
                                }
                            }

                            SettingsTab.WEATHER -> {
                                SettingsGroupCard(
                                    title = "定位与已配置地点 (${locationList.size} 处)",
                                    palette = palette
                                ) {
                                    SettingSwitchRow(
                                        title = "开启自动定位",
                                        subtitle = "依据设备 GPS/网络获取首选城市天气，关闭则固定使用列表地点",
                                        checked = autoLocation,
                                        palette = palette,
                                        onCheckedChange = { checked ->
                                            autoLocation = checked
                                            if (checked) onRequestLocationPermission()
                                        }
                                    )

                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)

                                    if (locationList.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(palette.warning.copy(alpha = 0.12f))
                                                .border(1.dp, palette.warning.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "当前未配置任何天气地点，主屏天气胶囊将显示「选择城市」",
                                                color = palette.warning,
                                                fontSize = 11.sp
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            locationList.forEachIndexed { index, loc ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(palette.chipBg)
                                                        .border(
                                                            1.dp,
                                                            if (index == 0) palette.accent.copy(alpha = 0.45f) else palette.border,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(
                                                                    if (index == 0) palette.accent.copy(alpha = 0.16f)
                                                                    else palette.border.copy(alpha = 0.4f)
                                                                )
                                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (index == 0) "主地点" else "地点${index + 1}",
                                                                color = if (index == 0) palette.accent else palette.mutedText,
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                        Text(
                                                            text = loc.name,
                                                            color = palette.primaryText,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Text(
                                                            text = String.format(Locale.US, "(%.2f, %.2f)", loc.lat, loc.lng),
                                                            color = palette.mutedText,
                                                            fontSize = 10.sp
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        if (index > 0) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(palette.accent.copy(alpha = 0.14f))
                                                                    .clickable {
                                                                        locationList = listOf(loc) + locationList.filterIndexed { i, _ -> i != index }
                                                                    }
                                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                                            ) {
                                                                Text("设为首选", color = palette.accent, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                                                            }
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    locationList = locationList.filterIndexed { i, _ -> i != index }
                                                                }
                                                                .padding(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "删除 ${loc.name}",
                                                                tint = palette.danger,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                SettingsGroupCard(
                                    title = "添加新城市或地点",
                                    subtitle = "支持全国省市县中文/拼音秒搜、多源线上补全、经纬度录入与快捷预设",
                                    palette = palette
                                ) {
                                    SegmentedOptionRow(
                                        options = listOf(0 to "地名搜索", 1 to "经纬度录入", 2 to "常用预设"),
                                        selectedKey = weatherAddMode,
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { weatherAddMode = it }
                                    )

                                    if (weatherAddMode == 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = weatherSearchQuery,
                                                onValueChange = {
                                                    weatherSearchQuery = it
                                                    weatherSearchMessage = null
                                                },
                                                placeholder = {
                                                    Text(
                                                        text = "输入地名或拼音，如：温州、延吉、海淀、东京",
                                                        fontSize = 11.sp,
                                                        color = palette.mutedText
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        tint = palette.mutedText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                keyboardActions = KeyboardActions(onSearch = { triggerWeatherSearch() }),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = palette.primaryText,
                                                    unfocusedTextColor = palette.primaryText,
                                                    focusedBorderColor = palette.accent,
                                                    unfocusedBorderColor = palette.border
                                                )
                                            )

                                            Button(
                                                onClick = triggerWeatherSearch,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                if (isWeatherSearching) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        color = palette.onAccent,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Text("搜索", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        if (weatherSearchMessage != null) {
                                            Text(
                                                text = weatherSearchMessage!!,
                                                color = palette.warning,
                                                fontSize = 11.sp
                                            )
                                        }

                                        if (weatherSearchResults.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                weatherSearchResults.take(8).forEach { item ->
                                                    val key = String.format(Locale.US, "%.4f_%.4f", item.latitude, item.longitude)
                                                    val isAlreadyAdded = locationList.any {
                                                        String.format(Locale.US, "%.4f_%.4f", it.lat, it.lng) == key
                                                    }
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(palette.chipBg)
                                                            .border(
                                                                1.dp,
                                                                if (isAlreadyAdded) palette.positive.copy(alpha = 0.45f) else palette.border,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = item.displayName,
                                                                color = palette.primaryText,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Text(
                                                                text = item.subtitle,
                                                                color = palette.mutedText,
                                                                fontSize = 10.sp
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isAlreadyAdded) palette.border else palette.accent.copy(alpha = 0.16f))
                                                                .border(1.dp, if (isAlreadyAdded) Color.Transparent else palette.accent, RoundedCornerShape(6.dp))
                                                                .clickable(enabled = !isAlreadyAdded) {
                                                                    locationList = locationList + LocationItem(item.displayName, item.latitude, item.longitude)
                                                                    Toast.makeText(context, "已添加 ${item.displayName}", Toast.LENGTH_SHORT).show()
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                                        ) {
                                                            Text(
                                                                text = if (isAlreadyAdded) "已添加" else "+ 添加",
                                                                color = if (isAlreadyAdded) palette.mutedText else palette.accent,
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (weatherAddMode == 1) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = manualLat,
                                                onValueChange = {
                                                    manualLat = it
                                                    manualError = null
                                                },
                                                placeholder = { Text("纬度 (-90 ~ 90)", fontSize = 11.sp, color = palette.mutedText) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = palette.primaryText,
                                                    unfocusedTextColor = palette.primaryText,
                                                    focusedBorderColor = palette.accent,
                                                    unfocusedBorderColor = palette.border
                                                )
                                            )
                                            OutlinedTextField(
                                                value = manualLng,
                                                onValueChange = {
                                                    manualLng = it
                                                    manualError = null
                                                },
                                                placeholder = { Text("经度 (-180 ~ 180)", fontSize = 11.sp, color = palette.mutedText) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = palette.primaryText,
                                                    unfocusedTextColor = palette.primaryText,
                                                    focusedBorderColor = palette.accent,
                                                    unfocusedBorderColor = palette.border
                                                )
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = manualName,
                                                onValueChange = { manualName = it },
                                                placeholder = { Text("地点名称 (可选)", fontSize = 11.sp, color = palette.mutedText) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = palette.primaryText,
                                                    unfocusedTextColor = palette.primaryText,
                                                    focusedBorderColor = palette.accent,
                                                    unfocusedBorderColor = palette.border
                                                )
                                            )

                                            Button(
                                                onClick = {
                                                    val pLat = manualLat.trim().toDoubleOrNull()
                                                    val pLng = manualLng.trim().toDoubleOrNull()
                                                    if (pLat == null || pLat < -90.0 || pLat > 90.0) {
                                                        manualError = "纬度格式无效，范围须在 -90 到 90 之间"
                                                        return@Button
                                                    }
                                                    if (pLng == null || pLng < -180.0 || pLng > 180.0) {
                                                        manualError = "经度格式无效，范围须在 -180 到 180 之间"
                                                        return@Button
                                                    }
                                                    val key = String.format(Locale.US, "%.4f_%.4f", pLat, pLng)
                                                    if (locationList.any { String.format(Locale.US, "%.4f_%.4f", it.lat, it.lng) == key }) {
                                                        manualError = "该坐标地点已在列表中"
                                                        return@Button
                                                    }
                                                    val dName = manualName.trim().ifBlank {
                                                        String.format(Locale.US, "%.2f, %.2f", pLat, pLng)
                                                    }
                                                    locationList = locationList + LocationItem(dName, pLat, pLng)
                                                    manualName = ""
                                                    manualLat = ""
                                                    manualLng = ""
                                                    manualError = null
                                                    Toast.makeText(context, "已添加坐标地点 $dName", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = palette.positive, contentColor = Color.White),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Text("+ 添加坐标", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (manualError != null) {
                                            Text(text = manualError!!, color = palette.danger, fontSize = 11.sp)
                                        }
                                    } else {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            cityPresets.forEach { (name, cLat, cLng) ->
                                                val key = String.format(Locale.US, "%.4f_%.4f", cLat, cLng)
                                                val isAdded = locationList.any {
                                                    String.format(Locale.US, "%.4f_%.4f", it.lat, it.lng) == key
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isAdded) palette.positive.copy(alpha = 0.16f) else palette.chipBg)
                                                        .border(1.dp, if (isAdded) palette.positive else palette.border, RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            locationList = if (isAdded) {
                                                                locationList.filter { String.format(Locale.US, "%.4f_%.4f", it.lat, it.lng) != key }
                                                            } else {
                                                                locationList + LocationItem(name, cLat, cLng)
                                                            }
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = if (isAdded) "✓ $name" else "+ $name",
                                                        color = if (isAdded) palette.positive else palette.secondaryText,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsTab.PORTFOLIO -> {
                                SettingsGroupCard(
                                    title = "长桥证券与资产看板 (只读 MCP)",
                                    palette = palette
                                ) {
                                    Text(
                                        text = if (longbridgeHasCredentials) {
                                            "授权状态：已连接长桥官方无状态只读通道 (${if (longbridgeSettings.normalizedRegion == "GLOBAL") "国际站 .com" else "大陆站 .cn"})"
                                        } else {
                                            "授权状态：尚未授权连接。支持 10 分钟只读授权码、OAuth 2.1 PKCE 或凭据 JSON 导入。"
                                        },
                                        color = if (longbridgeHasCredentials) palette.accent else palette.secondaryText,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 16.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (longbridgeViewModel != null) {
                                                    showLongbridgeAuthModal = true
                                                } else {
                                                    onOpenLongbridgeSettings()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = palette.accent,
                                                contentColor = palette.onAccent,
                                                disabledContainerColor = palette.border,
                                                disabledContentColor = palette.mutedText
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Key,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (longbridgeHasCredentials) "更换 / 重新授权" else "立即只读授权连接",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (longbridgeHasCredentials && longbridgeViewModel != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    longbridgeViewModel.disconnect()
                                                    Toast.makeText(context, "已清除长桥凭据与缓存", Toast.LENGTH_SHORT).show()
                                                },
                                                border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.55f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = palette.danger,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("断开", color = palette.danger, fontSize = 11.5.sp)
                                            }
                                        }
                                    }
                                }

                                SettingsGroupCard(
                                    title = "首页胶囊与资产展示偏好 (即时生效)",
                                    palette = palette
                                ) {
                                    SettingSwitchRow(
                                        title = "启用长桥独立全屏资产页",
                                        subtitle = "主界面右滑可查看完整净资产、现金余额与持仓明细",
                                        checked = longbridgeSettings.enabled,
                                        palette = palette,
                                        onCheckedChange = { v -> onUpdateLongbridgeSettings { it.copy(enabled = v) } }
                                    )

                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)

                                    SettingSwitchRow(
                                        title = "首页在 Model 3 旁展示当日盈亏胶囊",
                                        subtitle = "与天气胶囊严格对齐，仅展示官方 Logo 与当日盈亏额/涨跌幅（支持单独刷新）",
                                        checked = longbridgeSettings.showOnHome,
                                        palette = palette,
                                        onCheckedChange = { v -> onUpdateLongbridgeSettings { it.copy(showOnHome = v) } }
                                    )

                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)

                                    SettingSwitchRow(
                                        title = "含美股盘前 / 盘后 / 夜盘报价",
                                        subtitle = "美股非常规盘中时段优先取扩展报价计算当日盈亏",
                                        checked = longbridgeSettings.includeExtendedHours,
                                        palette = palette,
                                        onCheckedChange = { v -> onUpdateLongbridgeSettings { it.copy(includeExtendedHours = v) } }
                                    )

                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)

                                    SettingSwitchRow(
                                        title = "金额隐私脱敏 (****)",
                                        subtitle = "隐藏首页与全屏看板具体金额，保留百分比涨跌幅",
                                        checked = longbridgeSettings.privacyMaskAmounts,
                                        palette = palette,
                                        onCheckedChange = { v -> onUpdateLongbridgeSettings { it.copy(privacyMaskAmounts = v) } }
                                    )

                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)

                                    Text("展示币种", color = palette.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    SegmentedOptionRow(
                                        options = listOf(
                                            "HKD" to "港币 HKD",
                                            "USD" to "美元 USD",
                                            "CNY" to "人民币 CNY",
                                            "ACCOUNT" to "账户本位"
                                        ),
                                        selectedKey = longbridgeSettings.normalizedDisplayCurrency,
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { cur -> onUpdateLongbridgeSettings { it.copy(displayCurrency = cur) } }
                                    )

                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)

                                    Text("当日盈亏涨跌配色", color = palette.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    SegmentedOptionRow(
                                        options = listOf(
                                            "RED_UP_GREEN_DOWN" to "红涨绿跌",
                                            "GREEN_UP_RED_DOWN" to "绿涨红跌"
                                        ),
                                        selectedKey = if (longbridgeSettings.isRedUpGreenDown) "RED_UP_GREEN_DOWN" else "GREEN_UP_RED_DOWN",
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { scheme -> onUpdateLongbridgeSettings { it.copy(colorScheme = scheme) } }
                                    )

                                    HorizontalDivider(color = palette.divider, thickness = 0.8.dp)

                                    Text("接入节点与自动刷新间隔", color = palette.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    SegmentedOptionRow(
                                        options = listOf(
                                            "CN" to "大陆站 (.cn)",
                                            "GLOBAL" to "国际站 (.com)"
                                        ),
                                        selectedKey = longbridgeSettings.normalizedRegion,
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { reg -> onUpdateLongbridgeSettings { it.copy(region = reg) } }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SegmentedOptionRow(
                                        options = listOf(
                                            "30" to "30 秒",
                                            "60" to "60 秒",
                                            "120" to "2 分钟",
                                            "300" to "5 分钟"
                                        ),
                                        selectedKey = longbridgeSettings.refreshIntervalSec.toString(),
                                        palette = palette,
                                        isCompact = isCompact,
                                        onSelect = { sec ->
                                            onUpdateLongbridgeSettings { it.copy(refreshIntervalSec = sec.toIntOrNull() ?: 60) }
                                        }
                                    )
                                }
                            }

                            SettingsTab.VEHICLE_SYNC -> {
                                SettingsGroupCard(
                                    title = "车辆监控显示与车模外观",
                                    palette = palette
                                ) {
                                    if (teslaViewModel != null) {
                                        SettingSwitchRow(
                                            title = "启用车辆监控页",
                                            subtitle = "开启后主界面将增加横向滑屏车辆页展示车况、定位与今日看板",
                                            checked = teslaEnabled,
                                            palette = palette,
                                            onCheckedChange = { teslaEnabled = it }
                                        )
                                        HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                    }

                                    SettingSwitchRow(
                                        title = "首页在天气下方展示车辆胶囊",
                                        subtitle = "启用 TeslaMate 监控并完成配置后，与长桥胶囊同排对齐展示",
                                        checked = showTeslaOnHome,
                                        palette = palette,
                                        onCheckedChange = { showTeslaOnHome = it }
                                    )

                                    if (teslaViewModel != null) {
                                        HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                        Text(
                                            text = "车辆外观配色（3D 写实车模）",
                                            color = palette.primaryText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "AUTO" to "自动匹配",
                                                "RED" to "烈焰红",
                                                "RED_CLASSIC" to "经典红",
                                                "WHITE" to "珍珠白",
                                                "BLACK" to "纯黑",
                                                "SILVER" to "星空灰",
                                                "BLUE" to "深海蓝"
                                            ).forEach { (key, label) ->
                                                val isSel = teslaVehicleColor == key
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isSel) palette.accent else palette.chipBg)
                                                        .border(
                                                            1.dp,
                                                            if (isSel) palette.accent else palette.border,
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .clickable { teslaVehicleColor = key }
                                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isSel) palette.onAccent else palette.secondaryText,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(palette.chipBg)
                                                .border(1.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .clickable { onOpenTeslaSettings() }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsCar,
                                                    contentDescription = null,
                                                    tint = palette.accent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "TeslaMate / Grafana 数据源设置",
                                                        color = palette.primaryText,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = teslaStatusSummary.ifBlank { "配置本地或远程 Grafana API" },
                                                        color = palette.secondaryText,
                                                        fontSize = 10.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "打开 Tesla 设置",
                                                tint = palette.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                if (teslaViewModel != null) {
                                    SettingsGroupCard(
                                        title = "TeslaMate / Grafana 只读数据源",
                                        subtitle = "当前状态：${teslaStatusSummary.ifBlank { "未配置" }}",
                                        palette = palette
                                    ) {
                                        OutlinedTextField(
                                            value = teslaBaseUrl,
                                            onValueChange = { teslaBaseUrl = it },
                                            label = { Text("Grafana 根地址 (Base URL)", fontSize = 11.sp) },
                                            placeholder = { Text("https://grafana.example.com", fontSize = 11.5.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = palette.primaryText,
                                                unfocusedTextColor = palette.primaryText,
                                                focusedBorderColor = palette.accent,
                                                unfocusedBorderColor = palette.border
                                            )
                                        )

                                        OutlinedTextField(
                                            value = teslaToken,
                                            onValueChange = { teslaToken = it },
                                            label = { Text("Grafana 服务账号 Token (只读 SELECT)", fontSize = 11.sp) },
                                            placeholder = { Text("粘贴 Service Account Token", fontSize = 11.5.sp) },
                                            singleLine = true,
                                            visualTransformation = if (isTeslaTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                IconButton(onClick = { isTeslaTokenVisible = !isTeslaTokenVisible }) {
                                                    Icon(
                                                        imageVector = if (isTeslaTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                        contentDescription = if (isTeslaTokenVisible) "隐藏 Token" else "显示 Token",
                                                        tint = palette.secondaryText,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = palette.primaryText,
                                                unfocusedTextColor = palette.primaryText,
                                                focusedBorderColor = palette.accent,
                                                unfocusedBorderColor = palette.border
                                            )
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { teslaAllowHttp = !teslaAllowHttp }
                                        ) {
                                            Checkbox(
                                                checked = teslaAllowHttp,
                                                onCheckedChange = { teslaAllowHttp = it }
                                            )
                                            Text(
                                                text = "允许可信内网 HTTP（Token 将在局域网明文传输，禁止公网地址）",
                                                color = if (teslaAllowHttp) palette.warning else palette.secondaryText,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (teslaBaseUrl.isBlank() || teslaToken.isBlank()) {
                                                        Toast.makeText(context, "请先填写 Base URL 和 Token", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val temp = TeslaSettings(
                                                            enabled = teslaEnabled,
                                                            baseUrl = teslaBaseUrl,
                                                            dataSourceUid = teslaDataSourceUid,
                                                            dataSourceType = teslaDataSourceType,
                                                            carId = teslaCarId,
                                                            carName = teslaCarName,
                                                            refreshIntervalSec = teslaRefreshSec,
                                                            timeZone = teslaTimeZone,
                                                            allowHttp = teslaAllowHttp,
                                                            vehicleColor = teslaVehicleColor
                                                        )
                                                        teslaViewModel.testConnection(temp, teslaToken)
                                                    }
                                                },
                                                enabled = !isTeslaTesting,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = palette.accent,
                                                    contentColor = palette.onAccent,
                                                    disabledContainerColor = palette.border,
                                                    disabledContentColor = palette.mutedText
                                                )
                                            ) {
                                                if (isTeslaTesting) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(13.dp),
                                                        strokeWidth = 1.5.dp,
                                                        color = palette.onAccent
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("正在测试连接…", fontSize = 11.5.sp)
                                                } else {
                                                    Text("① 测试连接与发现车辆", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if (!teslaTestResult.isNullOrBlank()) {
                                                val ok = teslaTestResult.startsWith("连接成功")
                                                Text(
                                                    text = teslaTestResult,
                                                    color = if (ok) palette.positive else palette.danger,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    SettingsGroupCard(
                                        title = "数据源、目标车辆与同步频率",
                                        palette = palette
                                    ) {
                                        if (discoveredDatasources.isNotEmpty()) {
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                discoveredDatasources.forEach { ds ->
                                                    val isSel = teslaDataSourceUid == ds.uid
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSel) palette.accent else palette.chipBg)
                                                            .border(1.dp, if (isSel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                                                            .clickable {
                                                                teslaDataSourceUid = ds.uid
                                                                teslaDataSourceType = ds.type
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "${ds.name} (${ds.uid})",
                                                            color = if (isSel) palette.onAccent else palette.secondaryText,
                                                            fontSize = 10.5.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = teslaDataSourceUid,
                                            onValueChange = { teslaDataSourceUid = it },
                                            label = { Text("PostgreSQL 数据源 UID", fontSize = 11.sp) },
                                            placeholder = { Text("例如: teslamate", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = palette.primaryText,
                                                unfocusedTextColor = palette.primaryText,
                                                focusedBorderColor = palette.accent,
                                                unfocusedBorderColor = palette.border
                                            )
                                        )

                                        if (discoveredCars.isNotEmpty()) {
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                discoveredCars.forEach { car ->
                                                    val isSel = teslaCarId == car.id
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSel) palette.accent else palette.chipBg)
                                                            .border(1.dp, if (isSel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                                                            .clickable {
                                                                teslaCarId = car.id
                                                                teslaCarName = car.displayName
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "${car.displayName} (#${car.id})",
                                                            color = if (isSel) palette.onAccent else palette.secondaryText,
                                                            fontSize = 10.5.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = teslaCarName,
                                                onValueChange = { teslaCarName = it },
                                                label = { Text("车辆名称", fontSize = 11.sp) },
                                                modifier = Modifier.weight(1.5f),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = palette.primaryText,
                                                    unfocusedTextColor = palette.primaryText,
                                                    focusedBorderColor = palette.accent,
                                                    unfocusedBorderColor = palette.border
                                                )
                                            )
                                            OutlinedTextField(
                                                value = teslaCarId.toString(),
                                                onValueChange = { teslaCarId = it.toIntOrNull() ?: 1 },
                                                label = { Text("车辆 ID", fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = palette.primaryText,
                                                    unfocusedTextColor = palette.primaryText,
                                                    focusedBorderColor = palette.accent,
                                                    unfocusedBorderColor = palette.border
                                                )
                                            )
                                        }

                                        Text("轮询刷新频率", color = palette.primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        SegmentedOptionRow(
                                            options = listOf(
                                                "30" to "30 秒",
                                                "60" to "60 秒",
                                                "120" to "120 秒"
                                            ),
                                            selectedKey = teslaRefreshSec.toString(),
                                            palette = palette,
                                            isCompact = isCompact,
                                            onSelect = { teslaRefreshSec = it.toIntOrNull() ?: 60 }
                                        )

                                        OutlinedTextField(
                                            value = teslaTimeZone,
                                            onValueChange = { teslaTimeZone = it },
                                            label = { Text("统计时区", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = palette.primaryText,
                                                unfocusedTextColor = palette.primaryText,
                                                focusedBorderColor = palette.accent,
                                                unfocusedBorderColor = palette.border
                                            )
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                teslaViewModel.clearConfiguration()
                                                teslaEnabled = false
                                                teslaBaseUrl = ""
                                                teslaToken = ""
                                                teslaDataSourceUid = ""
                                                Toast.makeText(context, "已清除 TeslaMate 配置与缓存", Toast.LENGTH_SHORT).show()
                                            },
                                            border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = palette.danger,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("清除车辆配置与本地缓存", color = palette.danger, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }

                            SettingsTab.BACKUP -> {
                                SettingsGroupCard(
                                    title = "通用设置备份与迁移 (安全无凭据)",
                                    subtitle = "导出或导入时间制式、时钟缩放、排版占比、日夜模式等界面偏好（schema v1），绝不包含任何 API Key、Token 或账号邮箱。",
                                    palette = palette
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { exportLauncher.launch("chronodesk_settings_v1.json") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.55f))
                                        ) {
                                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp), tint = palette.accent)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("导出配置 JSON", fontSize = 12.sp, color = palette.accent, fontWeight = FontWeight.SemiBold)
                                        }
                                        OutlinedButton(
                                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, palette.positive.copy(alpha = 0.55f))
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp), tint = palette.positive)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("导入并校验", fontSize = 12.sp, color = palette.positive, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            SettingsTab.ABOUT -> {
                                AboutSettingsTabContent(
                                    palette = palette,
                                    isCompact = isCompact
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = palette.divider, thickness = 1.dp)

                // 3. 底部固定操作栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 42.dp else 54.dp)
                        .background(palette.sidebarBg)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val summaryLine = if (locationList.isEmpty()) {
                        "未选天气地点 · 时钟 ${(clockScale * 100).roundToInt()}%"
                    } else {
                        "${locationList.size} 处天气 (${locationList.first().name}) · 时钟 ${(clockScale * 100).roundToInt()}%"
                    }
                    Text(
                        text = summaryLine,
                        color = palette.secondaryText,
                        fontSize = if (isCompact) 10.sp else 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, palette.border),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("取消", color = palette.secondaryText, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val finalCityName = locationList.firstOrNull()?.name ?: ""
                                val finalLat = locationList.firstOrNull()?.lat ?: 0.0
                                val finalLng = locationList.firstOrNull()?.lng ?: 0.0
                                val finalAdditional = if (locationList.size > 1) {
                                    locationList.drop(1).joinToString(";") { "${it.name}:${it.lat}:${it.lng}" }
                                } else ""
                                val updated = currentSettings.copy(
                                    is24Hour = is24Hour,
                                    showSeconds = showSeconds,
                                    screenOnPolicy = screenOnPolicy,
                                    keepScreenOn = screenOnPolicy != "SYSTEM",
                                    nightModeOption = nightModeOption,
                                    dayThemeStyle = dayThemeStyle,
                                    autoNightDim = autoNightDim,
                                    allowNightSleep = allowNightSleep,
                                    nightBrightness = nightBrightness,
                                    autoRefreshIntervalMinutes = refreshInterval,
                                    autoLocation = autoLocation,
                                    cityName = finalCityName,
                                    latitude = finalLat,
                                    longitude = finalLng,
                                    additionalCities = finalAdditional,
                                    quotaWidthPercent = quotaWidthPercent,
                                    quotaCardDensity = quotaCardDensity,
                                    quotaDisplayMode = quotaDisplayMode,
                                    quotaProviderOrder = quotaProviderOrder.joinToString(","),
                                    clockScale = clockScale,
                                    clockOffsetX = clockOffsetX,
                                    clockOffsetY = clockOffsetY,
                                    showTeslaOnHome = showTeslaOnHome,
                                    useMockData = useMockData
                                )
                                onSave(updated)
                                if (teslaViewModel != null) {
                                    val updatedTesla = TeslaSettings(
                                        enabled = teslaEnabled,
                                        baseUrl = teslaBaseUrl.trim(),
                                        dataSourceUid = teslaDataSourceUid.trim(),
                                        dataSourceType = teslaDataSourceType.trim(),
                                        carId = teslaCarId,
                                        carName = teslaCarName.trim(),
                                        refreshIntervalSec = teslaRefreshSec,
                                        timeZone = teslaTimeZone.trim(),
                                        allowHttp = teslaAllowHttp,
                                        vehicleColor = teslaVehicleColor
                                    )
                                    val tokenChanged = teslaToken.isNotBlank() && teslaToken != initialTeslaToken
                                    if (updatedTesla != currentTeslaSettings || tokenChanged) {
                                        val ok = teslaViewModel.updateSettings(
                                            updatedTesla,
                                            teslaToken.takeIf { it.isNotBlank() }
                                        )
                                        if (!ok) {
                                            Toast.makeText(context, "设备安全存储不可用，车辆凭据未保存", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                        ) {
                            Text("保存生效", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSidebarItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isCompact: Boolean,
    palette: SettingsPalette,
    onClick: () -> Unit
) {
    val bg = if (isSelected) palette.accent.copy(alpha = 0.16f) else Color.Transparent
    val borderColor = if (isSelected) palette.accent.copy(alpha = 0.55f) else Color.Transparent
    val contentColor = if (isSelected) palette.accent else palette.secondaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = if (isCompact) 26.5.dp else 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(role = Role.Tab) { onClick() }
            .padding(horizontal = if (isCompact) 7.dp else 10.dp, vertical = if (isCompact) 2.5.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(if (isCompact) 14.dp else 17.dp)
        )
        Text(
            text = label,
            color = if (isSelected) palette.primaryText else palette.secondaryText,
            fontSize = if (isCompact) 10.5.sp else 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    palette: SettingsPalette,
    subtitle: String? = null,
    headerTrailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = palette.mutedText,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                }
            }
            headerTrailing?.invoke()
        }
        content()
    }
}

@Composable
private fun <T> SegmentedOptionRow(
    options: List<Pair<T, String>>,
    selectedKey: T,
    palette: SettingsPalette,
    isCompact: Boolean,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (key, label) ->
            val isSel = selectedKey == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) palette.accent else palette.chipBg)
                    .border(1.dp, if (isSel) palette.accent else palette.border, RoundedCornerShape(8.dp))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSel) palette.onAccent else palette.secondaryText,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    palette: SettingsPalette,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = palette.primaryText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = palette.mutedText, fontSize = 10.5.sp, lineHeight = 14.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onAccent,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.mutedText,
                uncheckedTrackColor = palette.chipBg
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountsSettingsTabContent(
    accounts: List<CodexAccount>,
    initialAccountId: String?,
    refreshInterval: Int,
    onRefreshIntervalChange: (Int) -> Unit,
    useMockData: Boolean,
    onUseMockDataChange: (Boolean) -> Unit,
    isNightMode: Boolean,
    isCompact: Boolean,
    palette: SettingsPalette,
    getDeepSeekApiKey: (accountId: String) -> String?,
    getAntigravityApiKey: (accountId: String) -> String?,
    onSaveCodexAccount: (slot: Int, account: CodexAccount, refreshToken: String, accessToken: String?) -> Boolean,
    onRemoveCodexAccount: (slot: Int) -> Unit,
    onUpdateCodexAlias: (slot: Int, newAlias: String) -> Unit,
    onSaveDeepSeekAccount: (accountId: String, apiKey: String, alias: String) -> Boolean,
    onAddDeepSeekAccount: () -> CodexAccount?,
    onRemoveDeepSeekAccount: (accountId: String) -> Unit,
    onSaveAntigravityAccount: (accountId: String, apiKey: String, alias: String) -> Boolean,
    onSaveAntigravityOAuthAccount: (
        accountId: String,
        alias: String?,
        email: String?,
        refreshToken: String?,
        accessToken: String?,
        expiresInSeconds: Long,
        idToken: String?,
        providerType: String
    ) -> Boolean,
    onUpdateAntigravityAlias: (accountId: String, newAlias: String) -> Unit,
    onAddAntigravityAccount: () -> CodexAccount?,
    onRemoveAntigravityAccount: (accountId: String) -> Unit,
    onUpdateAccountVisibility: (accountId: String, showOnMain: Boolean, showOnSecond: Boolean, isPinnedMain: Boolean) -> Unit
) {
    val context = LocalContext.current

    val chatGptAccounts = remember(accounts) {
        accounts.filter { it.modelProvider == "CHATGPT" }
    }
    var localDeepSeek by remember(accounts) {
        mutableStateOf(
            accounts.filter { it.modelProvider == "DEEPSEEK" }.ifEmpty {
                listOf(CodexAccount(id = "acc_deepseek", alias = "DeepSeek", modelProvider = "DEEPSEEK"))
            }
        )
    }
    var localGemini by remember(accounts) {
        mutableStateOf(
            accounts.filter { it.modelProvider == "ANTIGRAVITY" }.ifEmpty {
                listOf(CodexAccount(id = "acc_antigravity", alias = "Antigravity", modelProvider = "ANTIGRAVITY", providerType = "OAUTH_PKCE"))
            }
        )
    }

    val initialProvider = remember(initialAccountId, accounts) {
        val target = accounts.firstOrNull { it.id == initialAccountId }
        when (target?.modelProvider) {
            "DEEPSEEK" -> AiModelProvider.DEEPSEEK
            "ANTIGRAVITY" -> AiModelProvider.ANTIGRAVITY
            else -> AiModelProvider.CHATGPT
        }
    }
    var selectedProvider by remember(initialAccountId) { mutableStateOf(initialProvider) }
    var selectedChatGptSlot by remember(initialAccountId, chatGptAccounts) {
        val idx = chatGptAccounts.indexOfFirst { it.id == initialAccountId }
        mutableIntStateOf(if (idx == 1) 1 else 0)
    }
    var selectedDeepSeekIdx by remember(initialAccountId, localDeepSeek) {
        val idx = localDeepSeek.indexOfFirst { it.id == initialAccountId }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }
    var selectedGeminiIdx by remember(initialAccountId, localGemini) {
        val idx = localGemini.indexOfFirst { it.id == initialAccountId }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    var showDeviceAuthDialog by remember { mutableStateOf(false) }
    var showDirectTokenDialog by remember { mutableStateOf(false) }
    var showAgyOAuthDialog by remember { mutableStateOf(false) }
    var showAgyTokenDialog by remember { mutableStateOf(false) }

    val activeCodexAcc = chatGptAccounts.getOrNull(selectedChatGptSlot)
    if (showDeviceAuthDialog) {
        DeviceAuthDialog(
            slot = selectedChatGptSlot,
            isNightMode = isNightMode,
            onDismiss = { showDeviceAuthDialog = false },
            onSuccess = { acc, refresh, access ->
                val targetAcc = if (activeCodexAcc != null) acc.copy(id = activeCodexAcc.id) else acc
                val saved = onSaveCodexAccount(selectedChatGptSlot, targetAcc, refresh, access)
                if (saved) {
                    showDeviceAuthDialog = false
                    Toast.makeText(context, "账号授权成功并已保存", Toast.LENGTH_SHORT).show()
                }
                saved
            }
        )
    }

    if (showDirectTokenDialog) {
        DirectTokenDialog(
            slot = selectedChatGptSlot,
            currentAccount = activeCodexAcc,
            isNightMode = isNightMode,
            onDismiss = { showDirectTokenDialog = false },
            onSave = { acc, refresh, access ->
                val targetAcc = if (activeCodexAcc != null) acc.copy(id = activeCodexAcc.id) else acc
                val saved = onSaveCodexAccount(selectedChatGptSlot, targetAcc, refresh, access)
                if (saved) {
                    showDirectTokenDialog = false
                    Toast.makeText(context, "凭据已保存并触发同步", Toast.LENGTH_SHORT).show()
                }
                saved
            }
        )
    }

    val activeAgAcc = localGemini.getOrNull(selectedGeminiIdx.coerceIn(0, localGemini.lastIndex))
    if (showAgyOAuthDialog && activeAgAcc != null) {
        AntigravityOAuthDialog(
            currentAccount = activeAgAcc,
            isNightMode = isNightMode,
            onDismiss = { showAgyOAuthDialog = false },
            onSuccess = { email, refresh, access, expSec, idTok ->
                val saved = onSaveAntigravityOAuthAccount(
                    activeAgAcc.id,
                    null,
                    email,
                    refresh,
                    access,
                    expSec,
                    idTok,
                    "OAUTH_PKCE"
                )
                if (saved) {
                    localGemini = localGemini.map { item ->
                        if (item.id == activeAgAcc.id) item.copy(
                            email = email ?: item.email,
                            credentialStatus = CredentialStatus.VALID,
                            providerType = "OAUTH_PKCE"
                        ) else item
                    }
                    showAgyOAuthDialog = false
                    Toast.makeText(context, "Antigravity 授权成功并正在同步额度", Toast.LENGTH_SHORT).show()
                }
                saved
            }
        )
    }

    if (showAgyTokenDialog && activeAgAcc != null) {
        AntigravityTokenDialog(
            currentAccount = activeAgAcc,
            isNightMode = isNightMode,
            onDismiss = { showAgyTokenDialog = false },
            onSave = { alias, email, refresh, access, expSec, idTok ->
                val pType = if (!refresh.isNullOrBlank()) "OAUTH_PKCE" else "DIRECT_TOKEN"
                val saved = onSaveAntigravityOAuthAccount(
                    activeAgAcc.id,
                    alias,
                    email,
                    refresh,
                    access,
                    expSec,
                    idTok,
                    pType
                )
                if (saved) {
                    localGemini = localGemini.map { item ->
                        if (item.id == activeAgAcc.id) item.copy(
                            alias = alias.ifBlank { "Antigravity" },
                            email = email ?: item.email,
                            credentialStatus = CredentialStatus.VALID,
                            providerType = pType
                        ) else item
                    }
                    showAgyTokenDialog = false
                    Toast.makeText(context, "Antigravity 凭据已保存并开始查询额度", Toast.LENGTH_SHORT).show()
                }
                saved
            }
        )
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = palette.primaryText,
        unfocusedTextColor = palette.primaryText,
        focusedBorderColor = palette.accent,
        unfocusedBorderColor = palette.border,
        focusedContainerColor = palette.chipBg,
        unfocusedContainerColor = palette.chipBg
    )

    // 1. 平台切换 + 账号槽位选择器
    SettingsGroupCard(
        title = "AI 平台与账号管理",
        subtitle = "统一配置 ChatGPT (OAuth)、DeepSeek 余额及 Antigravity 订阅额度",
        palette = palette
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                AiModelProvider.CHATGPT to "ChatGPT",
                AiModelProvider.DEEPSEEK to "DeepSeek",
                AiModelProvider.ANTIGRAVITY to "Antigravity"
            ).forEach { (provider, label) ->
                val isSel = selectedProvider == provider
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) palette.accent.copy(alpha = 0.16f) else palette.chipBg)
                        .border(1.dp, if (isSel) palette.accent else palette.border, RoundedCornerShape(8.dp))
                        .clickable { selectedProvider = provider }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ModelProviderLogo(provider = provider, size = 16.dp, isNightMode = isNightMode)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        color = if (isSel) palette.primaryText else palette.secondaryText,
                        fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        // 对应平台下的账号槽位标签
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (selectedProvider) {
                AiModelProvider.CHATGPT -> {
                    listOf(0, 1).forEach { slot ->
                        val acc = chatGptAccounts.getOrNull(slot)
                        val isSel = selectedChatGptSlot == slot
                        val title = acc?.displayTitle ?: "槽位 ${slot + 1}"
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) palette.accent else palette.chipBg)
                                .border(1.dp, if (isSel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                                .clickable { selectedChatGptSlot = slot }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSel -> palette.onAccent
                                            acc?.credentialStatus == CredentialStatus.VALID -> palette.positive
                                            acc?.credentialStatus == CredentialStatus.EXPIRED -> palette.danger
                                            else -> palette.mutedText
                                        }
                                    )
                            )
                            Text(
                                text = "账号 ${slot + 1} · $title",
                                color = if (isSel) palette.onAccent else palette.secondaryText,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                AiModelProvider.DEEPSEEK -> {
                    localDeepSeek.forEachIndexed { idx, acc ->
                        val isSel = selectedDeepSeekIdx == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) palette.accent else palette.chipBg)
                                .border(1.dp, if (isSel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                                .clickable { selectedDeepSeekIdx = idx }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = acc.displayTitle,
                                color = if (isSel) palette.onAccent else palette.secondaryText,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                    if (localDeepSeek.size < 4) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.chipBg)
                                .border(1.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                .clickable {
                                    val added = onAddDeepSeekAccount()
                                    if (added != null) {
                                        localDeepSeek = localDeepSeek + added
                                        selectedDeepSeekIdx = localDeepSeek.lastIndex
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ 新增 DeepSeek",
                                color = palette.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                AiModelProvider.ANTIGRAVITY -> {
                    localGemini.forEachIndexed { idx, acc ->
                        val isSel = selectedGeminiIdx == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) palette.accent else palette.chipBg)
                                .border(1.dp, if (isSel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                                .clickable { selectedGeminiIdx = idx }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = acc.displayTitle,
                                color = if (isSel) palette.onAccent else palette.secondaryText,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                    if (localGemini.size < 4) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.chipBg)
                                .border(1.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                .clickable {
                                    val added = onAddAntigravityAccount()
                                    if (added != null) {
                                        localGemini = localGemini + added
                                        selectedGeminiIdx = localGemini.lastIndex
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ 新增 Antigravity",
                                color = palette.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // 2. 选中账号配置卡片
    when (selectedProvider) {
        AiModelProvider.CHATGPT -> {
            val acc = chatGptAccounts.getOrNull(selectedChatGptSlot)
            val statusLabel = when {
                acc == null || !acc.isConfigured -> "未授权" to palette.mutedText
                acc.credentialStatus == CredentialStatus.EXPIRED -> "凭据失效" to palette.danger
                else -> "正常在线" to palette.positive
            }
            var aliasEdit by remember(selectedChatGptSlot, acc?.id, acc?.alias) {
                mutableStateOf(
                    acc?.alias?.takeIf { it != acc.email && !it.contains("账号") } ?: ""
                )
            }
            var showMain by remember(acc?.id, acc?.isVisibleOnMain) { mutableStateOf(acc?.isVisibleOnMain ?: true) }
            var showSecond by remember(acc?.id, acc?.isVisibleOnSecond) { mutableStateOf(acc?.isVisibleOnSecond ?: true) }
            var isPinned by remember(acc?.id, acc?.isPinnedMain) { mutableStateOf(acc?.isPinnedMain ?: false) }

            SettingsGroupCard(
                title = "ChatGPT 槽位 ${selectedChatGptSlot + 1} 配置",
                subtitle = acc?.maskedEmail?.let { "绑定邮箱：$it · OAuth 2.0 PKCE" } ?: "支持浏览器设备码一键授权或导入 Token",
                palette = palette,
                headerTrailing = {
                    Text(
                        text = statusLabel.first,
                        color = statusLabel.second,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            ) {
                if (acc != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aliasEdit,
                            onValueChange = { aliasEdit = it },
                            placeholder = { Text("自定义显示名称 (避免邮箱暴露)", fontSize = 11.sp, color = palette.mutedText) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = fieldColors
                        )
                        Button(
                            onClick = {
                                onUpdateCodexAlias(selectedChatGptSlot, aliasEdit.trim())
                                Toast.makeText(context, "已更新显示名称", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent)
                        ) {
                            Text("保存名称", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AccountVisibilityChipsRow(
                        showOnMain = showMain,
                        showOnSecond = showSecond,
                        isPinnedMain = isPinned,
                        palette = palette,
                        onVisibilityChanged = { m, s, p ->
                            showMain = m; showSecond = s; isPinned = p
                            onUpdateAccountVisibility(acc.id, m, s, p)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showDeviceAuthDialog = true },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent)
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("官方设备码授权", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showDirectTokenDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = palette.primaryText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("导入 Token", fontSize = 11.5.sp, color = palette.primaryText)
                    }

                    if (acc != null && acc.isConfigured) {
                        OutlinedButton(
                            onClick = {
                                onRemoveCodexAccount(selectedChatGptSlot)
                                Toast.makeText(context, "已移除该账号凭据", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = palette.danger, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("清除", fontSize = 11.5.sp, color = palette.danger)
                        }
                    }
                }
            }
        }

        AiModelProvider.DEEPSEEK -> {
            val safeIdx = selectedDeepSeekIdx.coerceIn(0, localDeepSeek.lastIndex)
            val dsAcc = localDeepSeek[safeIdx]
            var dsKey by remember(dsAcc.id) { mutableStateOf(getDeepSeekApiKey(dsAcc.id) ?: "") }
            var dsAlias by remember(dsAcc.id, dsAcc.alias) {
                mutableStateOf(if (dsAcc.alias != "DeepSeek" && !dsAcc.alias.startsWith("DeepSeek ")) dsAcc.alias else "")
            }
            var showMain by remember(dsAcc.id, dsAcc.isVisibleOnMain) { mutableStateOf(dsAcc.isVisibleOnMain) }
            var showSecond by remember(dsAcc.id, dsAcc.isVisibleOnSecond) { mutableStateOf(dsAcc.isVisibleOnSecond) }
            var isPinned by remember(dsAcc.id, dsAcc.isPinnedMain) { mutableStateOf(dsAcc.isPinnedMain) }

            val statusLabel = when {
                dsAcc.credentialStatus == CredentialStatus.EXPIRED -> "Key 失效" to palette.danger
                dsAcc.credentialStatus == CredentialStatus.VALID && dsKey.isNotBlank() -> "已配置" to palette.positive
                else -> "待配置" to palette.mutedText
            }

            SettingsGroupCard(
                title = "DeepSeek API 余额配置",
                subtitle = "直连官方 /user/balance 查询实时可用余额",
                palette = palette,
                headerTrailing = {
                    Text(text = statusLabel.first, color = statusLabel.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            ) {
                OutlinedTextField(
                    value = dsAlias,
                    onValueChange = { dsAlias = it },
                    placeholder = { Text("自定义名称（例如：主力号 / 个人号）", fontSize = 11.sp, color = palette.mutedText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = dsKey,
                    onValueChange = { dsKey = it },
                    placeholder = { Text("DeepSeek API Key (sk-...)", fontSize = 11.sp, color = palette.mutedText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                AccountVisibilityChipsRow(
                    showOnMain = showMain,
                    showOnSecond = showSecond,
                    isPinnedMain = isPinned,
                    palette = palette,
                    onVisibilityChanged = { m, s, p ->
                        showMain = m; showSecond = s; isPinned = p
                        onUpdateAccountVisibility(dsAcc.id, m, s, p)
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val cleanAlias = dsAlias.trim()
                            val saved = onSaveDeepSeekAccount(dsAcc.id, dsKey.trim(), cleanAlias)
                            if (saved) {
                                localDeepSeek = localDeepSeek.mapIndexed { idx, item ->
                                    if (idx == safeIdx) item.copy(
                                        alias = cleanAlias.ifBlank { "DeepSeek" },
                                        credentialStatus = CredentialStatus.VALID
                                    ) else item
                                }
                                Toast.makeText(context, "DeepSeek API Key 已保存并触发查询", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "设备安全存储不可用，凭据未保存", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.positive, contentColor = palette.onAccent)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存并验证余额", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (localDeepSeek.size > 1) {
                        OutlinedButton(
                            onClick = {
                                onRemoveDeepSeekAccount(dsAcc.id)
                                localDeepSeek = localDeepSeek.filterNot { it.id == dsAcc.id }
                                selectedDeepSeekIdx = 0
                                Toast.makeText(context, "已删除 DeepSeek 账号", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = palette.danger, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("删除", fontSize = 12.sp, color = palette.danger)
                        }
                    }
                }
            }
        }

        AiModelProvider.ANTIGRAVITY -> {
            val safeIdx = selectedGeminiIdx.coerceIn(0, localGemini.lastIndex)
            val agAcc = localGemini[safeIdx]
            var agAlias by remember(agAcc.id, agAcc.alias) {
                mutableStateOf(
                    if (agAcc.alias != "Google Antigravity" &&
                        agAcc.alias != "Gemini API" &&
                        agAcc.alias != "Antigravity" &&
                        !agAcc.alias.startsWith("Gemini API ") &&
                        !agAcc.alias.startsWith("Antigravity ") &&
                        agAcc.alias != agAcc.email
                    ) agAcc.alias else ""
                )
            }
            var showMain by remember(agAcc.id, agAcc.isVisibleOnMain) { mutableStateOf(agAcc.isVisibleOnMain) }
            var showSecond by remember(agAcc.id, agAcc.isVisibleOnSecond) { mutableStateOf(agAcc.isVisibleOnSecond) }
            var isPinned by remember(agAcc.id, agAcc.isPinnedMain) { mutableStateOf(agAcc.isPinnedMain) }

            val statusLabel = when {
                agAcc.credentialStatus == CredentialStatus.EXPIRED -> "凭据失效" to palette.danger
                agAcc.isConfigured -> "已授权" to palette.positive
                else -> "未授权" to palette.mutedText
            }

            SettingsGroupCard(
                title = if (localGemini.size > 1) "Antigravity 槽位 ${safeIdx + 1} 配置" else "Antigravity 官方账号配置",
                subtitle = agAcc.maskedEmail?.let { "绑定邮箱：$it · Google OAuth 2.0 PKCE" }
                    ?: "使用 Google 账号授权或导入 CC Switch 凭据查询实时额度",
                palette = palette,
                headerTrailing = {
                    Text(text = statusLabel.first, color = statusLabel.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = agAlias,
                        onValueChange = { agAlias = it },
                        placeholder = { Text("自定义显示名称（留空展示脱敏邮箱）", fontSize = 11.sp, color = palette.mutedText) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = fieldColors
                    )
                    Button(
                        onClick = {
                            val cleanAlias = agAlias.trim()
                            onUpdateAntigravityAlias(agAcc.id, cleanAlias)
                            localGemini = localGemini.mapIndexed { idx, item ->
                                if (idx == safeIdx) item.copy(alias = cleanAlias.ifBlank { "Antigravity" }) else item
                            }
                            Toast.makeText(context, "已更新显示名称", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent)
                    ) {
                        Text("保存名称", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                AccountVisibilityChipsRow(
                    showOnMain = showMain,
                    showOnSecond = showSecond,
                    isPinnedMain = isPinned,
                    palette = palette,
                    onVisibilityChanged = { m, s, p ->
                        showMain = m; showSecond = s; isPinned = p
                        onUpdateAccountVisibility(agAcc.id, m, s, p)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAgyOAuthDialog = true },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent)
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google 账号授权", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showAgyTokenDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = palette.primaryText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("导入凭据", fontSize = 11.5.sp, color = palette.primaryText)
                    }

                    if (agAcc.isConfigured || localGemini.size > 1) {
                        OutlinedButton(
                            onClick = {
                                onRemoveAntigravityAccount(agAcc.id)
                                if (localGemini.size > 1) {
                                    localGemini = localGemini.filterNot { it.id == agAcc.id }
                                    selectedGeminiIdx = 0
                                    Toast.makeText(context, "已删除 Antigravity 账号", Toast.LENGTH_SHORT).show()
                                } else {
                                    localGemini = localGemini.map { item ->
                                        if (item.id == agAcc.id) item.copy(
                                            alias = "Antigravity",
                                            email = null,
                                            credentialStatus = CredentialStatus.NOT_LOGGED_IN
                                        ) else item
                                    }
                                    Toast.makeText(context, "已清除 Antigravity 账号凭据", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = palette.danger, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (localGemini.size > 1) "删除" else "清除", fontSize = 11.5.sp, color = palette.danger)
                        }
                    }
                }
            }
        }
        else -> {}
    }

    // 3. 同步频率与演示数据设置
    SettingsGroupCard(
        title = "轮询频率与演示模式",
        palette = palette
    ) {
        SegmentedOptionRow(
            options = listOf(5 to "5分钟", 10 to "10分钟", 15 to "15分钟", 30 to "30分钟", 60 to "60分钟"),
            selectedKey = refreshInterval,
            palette = palette,
            isCompact = isCompact,
            onSelect = onRefreshIntervalChange
        )
        HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
        SettingSwitchRow(
            title = "启用演示数据模式",
            subtitle = "开启后使用本地脱敏模拟额度用于预览排版（点击底部保存生效后切换）",
            checked = useMockData,
            palette = palette,
            onCheckedChange = onUseMockDataChange
        )
    }
}

@Composable
private fun AccountVisibilityChipsRow(
    showOnMain: Boolean,
    showOnSecond: Boolean,
    isPinnedMain: Boolean,
    palette: SettingsPalette,
    onVisibilityChanged: (showOnMain: Boolean, showOnSecond: Boolean, isPinnedMain: Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (showOnMain) palette.accent.copy(alpha = 0.16f) else palette.chipBg)
                .border(1.dp, if (showOnMain) palette.accent else palette.border, RoundedCornerShape(6.dp))
                .clickable {
                    val nextMain = !showOnMain
                    onVisibilityChanged(nextMain, showOnSecond, if (nextMain) isPinnedMain else false)
                }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showOnMain) "✓ 主屏显示" else "+ 主屏显示",
                color = if (showOnMain) palette.accent else palette.mutedText,
                fontSize = 10.5.sp,
                fontWeight = if (showOnMain) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (showOnSecond) palette.positive.copy(alpha = 0.16f) else palette.chipBg)
                .border(1.dp, if (showOnSecond) palette.positive else palette.border, RoundedCornerShape(6.dp))
                .clickable { onVisibilityChanged(showOnMain, !showOnSecond, isPinnedMain) }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showOnSecond) "✓ 扩展页显示" else "+ 扩展页显示",
                color = if (showOnSecond) palette.positive else palette.mutedText,
                fontSize = 10.5.sp,
                fontWeight = if (showOnSecond) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isPinnedMain) palette.warning.copy(alpha = 0.16f) else palette.chipBg)
                .border(1.dp, if (isPinnedMain) palette.warning else palette.border, RoundedCornerShape(6.dp))
                .clickable {
                    val nextPinned = !isPinnedMain
                    onVisibilityChanged(if (nextPinned) true else showOnMain, showOnSecond, nextPinned)
                }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPinnedMain) "★ 主屏置顶" else "☆ 主屏置顶",
                color = if (isPinnedMain) palette.warning else palette.mutedText,
                fontSize = 10.5.sp,
                fontWeight = if (isPinnedMain) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private data class AboutFeatureSpec(
    val icon: ImageVector,
    val title: String,
    val summary: String
)

/**
 * 关于项目页签：展示 ChronoDesk 版本、仓库开源地址、四大核心子系统、硬件加密安全架构与开源致谢
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutSettingsTabContent(
    palette: SettingsPalette,
    isCompact: Boolean
) {
    val context = LocalContext.current
    val repoUrl = "https://github.com/tzwdj01/ChronoDesk"
    val (versionName, versionCode) = remember(context) {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            (info.versionName ?: "2.0.0") to PackageInfoCompat.getLongVersionCode(info)
        } catch (_: Exception) {
            "2.0.0" to 2L
        }
    }

    val features = remember {
        listOf(
            AboutFeatureSpec(
                icon = Icons.Default.WatchLater,
                title = "机械翻牌与三态视觉",
                summary = "分体折页时钟 · 公历农历与多城气象 · 日间原彩 / 纯水墨(R=G=B) / AMOLED 夜间一键循环，轻触零跳变"
            ),
            AboutFeatureSpec(
                icon = Icons.Default.AutoAwesome,
                title = "AI 算力额度中枢",
                summary = "ChatGPT (5h/周双窗) · Google Antigravity (Gemini + Claude/GPT 双轨) · DeepSeek 余额 · 纯数字/进度条自由排序"
            ),
            AboutFeatureSpec(
                icon = Icons.Default.DirectionsCar,
                title = "TeslaMate 智驾座舱",
                summary = "Grafana PostgreSQL 只读直连（休眠零唤醒） · 多车漆超清车模与四轮胎压 · 今日行程能耗与国内离线纠偏地图"
            ),
            AboutFeatureSpec(
                icon = Icons.Default.AccountBalanceWallet,
                title = "长桥环球证券看板",
                summary = "OpenAPI 实时净资产与当日盈亏 · HKD/USD/CNY 汇率折算 · 美/港/A 股核心指数与市场温度计"
            )
        )
    }

    SettingsGroupCard(
        title = "ChronoDesk · 极客桌面遥测座舱",
        subtitle = "专为开发者、AI 重度用户与智驾车主打造的 Android 常亮桌面伴侣",
        palette = palette
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AboutTagBadge("v$versionName (Build $versionCode)", palette.accent)
            AboutTagBadge("MIT Open Source", palette.positive)
            AboutTagBadge("100% Kotlin · Compose", palette.secondaryText)
        }

        Text(
            text = "将闲置旗舰与经典小屏设备（如小米 6 / 小米 11）重塑为全天候桌面中控屏：兼顾温润水墨可读性与夜间 AMOLED 护眼，聚合高频个人工作流的关键状态，绝无广告与云端追踪。",
            color = palette.secondaryText,
            fontSize = if (isCompact) 10.5.sp else 11.5.sp,
            lineHeight = if (isCompact) 15.sp else 17.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val opened = runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.isSuccess
                    if (!opened) {
                        Toast.makeText(context, "未找到可用浏览器，请点击复制开源地址", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.55f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.accent)
                Spacer(modifier = Modifier.width(5.dp))
                Text("访问 GitHub 仓库", fontSize = if (isCompact) 11.sp else 12.sp, color = palette.accent, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("ChronoDesk GitHub", repoUrl))
                    Toast.makeText(context, "已复制 GitHub 仓库地址", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, palette.border),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.secondaryText)
                Spacer(modifier = Modifier.width(5.dp))
                Text("复制开源地址", fontSize = if (isCompact) 11.sp else 12.sp, color = palette.primaryText, fontWeight = FontWeight.Medium)
            }
        }
    }

    SettingsGroupCard(
        title = "四大核心模块",
        subtitle = "全链路真机调优，支持横屏分栏与 16:9 紧凑屏自适应排版",
        palette = palette
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)) {
            features.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.chipBg)
                        .border(0.8.dp, palette.border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = if (isCompact) 6.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.padding(top = 1.dp).size(15.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = item.title,
                            color = palette.primaryText,
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.summary,
                            color = palette.secondaryText,
                            fontSize = if (isCompact) 9.8.sp else 10.8.sp,
                            lineHeight = if (isCompact) 13.5.sp else 15.sp
                        )
                    }
                }
            }
        }
    }

    SettingsGroupCard(
        title = "安全架构与开源致谢",
        subtitle = "本地硬件密文存储 · 零中转服务器 · MIT 开源协议",
        palette = palette
    ) {
        Text(
            text = "• 硬件级加密：Android KeyStore (AES-256-GCM) 托管所有 API Key / Token；TokenRedactor 全链路日志自动脱敏，严防跨域名重定向泄露。\n" +
                "• 技术栈：Kotlin · Jetpack Compose · Material 3 · Clean MVVM + UDF · Coroutines / StateFlow · OkHttp / Retrofit。\n" +
                "• 致谢与许可：本项目遵循 MIT License 开源；多模型额度接口设计部分借鉴 CC Switch（Copyright (c) 2025 Jason Young）。\n" +
                "• 仓库地址：$repoUrl",
            color = palette.secondaryText,
            fontSize = if (isCompact) 10.sp else 11.sp,
            lineHeight = if (isCompact) 14.5.sp else 16.5.sp
        )
    }
}

@Composable
private fun AboutTagBadge(
    text: String,
    accent: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
