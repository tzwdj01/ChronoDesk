package com.chronodesk.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext
import com.chronodesk.app.data.network.GeocodingItem
import com.chronodesk.app.data.repository.GeneralSettingsBackup
import com.chronodesk.app.data.repository.SettingsValidationResult
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chronodesk.app.data.model.ClockSettings
import kotlin.math.roundToInt
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentSettings: ClockSettings,
    onDismiss: () -> Unit,
    onSave: (ClockSettings) -> Unit,
    onOpenTeslaSettings: () -> Unit = {},
    onExportSettings: () -> String = { "" },
    onValidateSettings: (String) -> SettingsValidationResult = { SettingsValidationResult.Invalid("未实现") },
    onApplySettingsBackup: (GeneralSettingsBackup) -> Unit = {},
    onRequestLocationPermission: () -> Unit = {},
    onSearchLocations: suspend (String) -> List<GeocodingItem> = { emptyList() }
) {
    val context = LocalContext.current
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
            containerColor = DarkSurfaceElevated,
            title = {
                Text("确认导入设置", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("检测到合法的通用设置备份文件，内容预览如下：", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = previewSummary ?: "",
                            color = TechCyan,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "提示：此操作仅更新界面、时钟与显示偏好，不含账号与凭据数据。",
                        color = TextTertiary,
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
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black)
                ) {
                    Text("确认应用", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        previewBackup = null
                        previewSummary = null
                    }
                ) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    if (importErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { importErrorMessage = null },
            containerColor = DarkSurfaceElevated,
            title = {
                Text("导入失败", color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(importErrorMessage ?: "", color = TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = { importErrorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black)
                ) {
                    Text("确定", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
    var is24Hour by remember { mutableStateOf(currentSettings.is24Hour) }
    var showSeconds by remember { mutableStateOf(currentSettings.showSeconds) }
    var screenOnPolicy by remember { mutableStateOf(currentSettings.screenOnPolicy) }
    var nightModeOption by remember { mutableStateOf(currentSettings.nightModeOption) }
    var autoNightDim by remember { mutableStateOf(currentSettings.autoNightDim) }
    var nightBrightness by remember { mutableFloatStateOf(currentSettings.nightBrightness) }
    var refreshInterval by remember { mutableIntStateOf(currentSettings.autoRefreshIntervalMinutes) }
    var autoLocation by remember { mutableStateOf(currentSettings.autoLocation) }
    var clockScale by remember { mutableFloatStateOf(currentSettings.clockScale) }
    var clockOffsetX by remember { mutableFloatStateOf(currentSettings.clockOffsetX) }
    var clockOffsetY by remember { mutableFloatStateOf(currentSettings.clockOffsetY) }
    var quotaWidthPercent by remember { mutableIntStateOf(currentSettings.quotaWidthPercent) }
    var quotaCardDensity by remember { mutableStateOf(currentSettings.quotaCardDensity) }

    data class LocationItem(val name: String, val lat: Double, val lng: Double)

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

    var weatherAddMode by remember { mutableStateOf(0) } // 0: 搜索地名, 1: 经纬度录入, 2: 常用预设
    var weatherSearchQuery by remember { mutableStateOf("") }
    var weatherSearchResults by remember { mutableStateOf<List<GeocodingItem>>(emptyList()) }
    var isWeatherSearching by remember { mutableStateOf(false) }
    var weatherSearchMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var manualLat by remember { mutableStateOf("") }
    var manualLng by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }

    val cityPresets = listOf(
        Triple("深圳", 22.5431, 114.0579),
        Triple("北京", 39.9042, 116.4074),
        Triple("上海", 31.2304, 121.4737),
        Triple("广州", 23.1291, 113.2644),
        Triple("杭州", 30.2741, 120.1551),
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

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "系统与显示设置",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // 1. 日夜模式选择
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "显示与日夜模式",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "AUTO" to "自动 (22:00-07:00)",
                            "FORCE_DAY" to "强制日间",
                            "FORCE_NIGHT" to "强制夜间"
                        ).forEach { (opt, label) ->
                            val isSel = nightModeOption == opt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) TechCyan else DarkSurfaceElevated)
                                    .clickable { nightModeOption = opt }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.Black else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 夜间窗口暗度
                if (nightModeOption != "FORCE_DAY") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "夜间窗口暗度: ${(nightBrightness * 100).toInt()}% (超暗省电且防刺眼)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Slider(
                            value = nightBrightness,
                            onValueChange = { nightBrightness = it },
                            valueRange = 0.02f..0.30f,
                            colors = SliderDefaults.colors(
                                thumbColor = TechCyan,
                                activeTrackColor = TechCyan,
                                inactiveTrackColor = DarkBorder
                            )
                        )
                    }
                }

                // 2. 时钟设置
                SettingToggleItem(
                    title = "24小时制",
                    subtitle = "关闭则采用 12 小时制",
                    checked = is24Hour,
                    onCheckedChange = { is24Hour = it }
                )

                SettingToggleItem(
                    title = "显示秒钟",
                    subtitle = "主时钟右侧显示秒钟计数",
                    checked = showSeconds,
                    onCheckedChange = { showSeconds = it }
                )

                // 屏幕常亮策略
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "屏幕常亮策略",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "夜间时段自动熄屏休眠，清晨充电中恢复常亮",
                        color = TextSecondary,
                        fontSize = 10.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "CHARGING_ONLY" to "仅充电时常亮",
                            "ALWAYS" to "前台常亮",
                            "SYSTEM" to "跟随系统"
                        ).forEach { (opt, label) ->
                            val isSel = screenOnPolicy == opt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .sizeIn(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) TechCyan else DarkSurface)
                                    .clickable { screenOnPolicy = opt }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.Black else TextSecondary,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 2.1 时钟尺寸缩放与位置微调
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "时钟尺寸与位置调节",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                clockScale = 1.0f
                                clockOffsetX = 0f
                                clockOffsetY = 0f
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "一键复位居中 (100%)", color = TechCyan, fontSize = 11.sp)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 缩放比例
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "时钟大小比例", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "${(clockScale * 100).roundToInt()}%",
                                color = TechCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = clockScale,
                            onValueChange = { clockScale = (it * 20).roundToInt() / 20f },
                            valueRange = 0.6f..1.8f,
                            steps = 23,
                            colors = SliderDefaults.colors(
                                thumbColor = TechCyan,
                                activeTrackColor = TechCyan,
                                inactiveTrackColor = DarkBorder
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0.8f to "缩小 80%", 1.0f to "标准 100%", 1.25f to "放大 125%", 1.5f to "特大 150%").forEach { (sc, label) ->
                                val isSel = kotlin.math.abs(clockScale - sc) < 0.05f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) TechCyan.copy(alpha = 0.2f) else DarkSurface)
                                        .border(1.dp, if (isSel) TechCyan else DarkBorder, RoundedCornerShape(6.dp))
                                        .clickable { clockScale = sc }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) TechCyan else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // 水平位置偏移 (X)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "水平位置偏移 (左右移动)", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = if (clockOffsetX.toInt() == 0) "水平居中" else "${if (clockOffsetX > 0) "+" else ""}${clockOffsetX.toInt()} px",
                                color = if (clockOffsetX.toInt() == 0) TextSecondary else TechCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = clockOffsetX,
                            onValueChange = { clockOffsetX = it.roundToInt().toFloat() },
                            valueRange = -250f..250f,
                            colors = SliderDefaults.colors(
                                thumbColor = TechCyan,
                                activeTrackColor = TechCyan,
                                inactiveTrackColor = DarkBorder
                            )
                        )

                        // 垂直位置偏移 (Y)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "垂直位置偏移 (上下移动)", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = if (clockOffsetY.toInt() == 0) "垂直居中" else "${if (clockOffsetY > 0) "+" else ""}${clockOffsetY.toInt()} px",
                                color = if (clockOffsetY.toInt() == 0) TextSecondary else TechCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = clockOffsetY,
                            onValueChange = { clockOffsetY = it.roundToInt().toFloat() },
                            valueRange = -180f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = TechCyan,
                                activeTrackColor = TechCyan,
                                inactiveTrackColor = DarkBorder
                            )
                        )
                    }
                }

                // 3. 界面布局与右侧额度区域比例
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "界面布局与比例 (横屏)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "右侧额度宽度占比: $quotaWidthPercent%",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "时钟区域: ${100 - quotaWidthPercent}%",
                                color = TechCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = quotaWidthPercent.toFloat(),
                            onValueChange = { quotaWidthPercent = it.roundToInt() },
                            valueRange = 28f..50f,
                            steps = 21,
                            colors = SliderDefaults.colors(
                                thumbColor = TechCyan,
                                activeTrackColor = TechCyan,
                                inactiveTrackColor = DarkBorder
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                32 to "紧凑 32%",
                                38 to "标准 38%",
                                44 to "宽体 44%",
                                50 to "对等 50%"
                            ).forEach { (pct, label) ->
                                val isSel = quotaWidthPercent == pct
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) TechCyan.copy(alpha = 0.25f) else DarkSurface)
                                        .border(1.dp, if (isSel) TechCyan else Color.Transparent, RoundedCornerShape(4.dp))
                                        .clickable { quotaWidthPercent = pct }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) TechCyan else TextSecondary,
                                        fontSize = 9.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. 额度自动刷新间隔
                Column {
                    Text(
                        text = "Codex 额度自动刷新频率",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 30, 60).forEach { mins ->
                            val isSelected = refreshInterval == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) TechCyan else DarkSurfaceElevated)
                                    .clickable { refreshInterval = mins }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}分钟",
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 4. 天气与城市设置 (地名搜索、经纬度录入、多地点并列)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "天气与城市位置 (地名搜索与经纬度支持)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    SettingToggleItem(
                        title = "开启自动定位",
                        subtitle = "依据设备 GPS 或网络自动获取首选城市天气（关闭则固定使用列表地点）",
                        checked = autoLocation,
                        onCheckedChange = { checked ->
                            autoLocation = checked
                            if (checked) {
                                onRequestLocationPermission()
                            }
                        }
                    )

                    // 1. 已配置天气地点列表
                    Text(
                        text = "1. 已配置天气地点 (${locationList.size} 处)：",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    if (locationList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, TechAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "未配置任何天气地点，主屏天气卡片将显示「选择城市」",
                                color = TechAmber,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            locationList.forEachIndexed { index, loc ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (index == 0) TechCyan.copy(alpha = 0.35f) else DarkBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
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
                                                        if (index == 0) TechCyan.copy(alpha = 0.2f)
                                                        else DarkBorder.copy(alpha = 0.5f)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (index == 0) "主地点 (第1行)" else "地点 ${index + 1}",
                                                    color = if (index == 0) TechCyan else TextTertiary,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = loc.name,
                                                color = TextPrimary,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = String.format(Locale.US, "(%.2f, %.2f)", loc.lat, loc.lng),
                                                color = TextTertiary,
                                                fontSize = 10.sp
                                            )
                                        }

                                        // 删除按钮
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
                                                contentDescription = "删除该地点",
                                                tint = TechRose.copy(alpha = 0.8f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. 添加新地点操作区
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "2. 录入或添加新地点：",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    // 录入方式切换标签
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "地名搜索", 1 to "经纬度录入", 2 to "常用预设").forEach { (mode, label) ->
                            val isSel = weatherAddMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) TechCyan.copy(alpha = 0.2f) else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) TechCyan else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable { weatherAddMode = mode }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) TechCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // 模式 0: Geocoding 地名搜索
                    if (weatherAddMode == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        Text(text = "输入地名，如：海淀、延庆、London", fontSize = 11.sp, color = TextTertiary)
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceElevated,
                                        unfocusedContainerColor = DarkSurfaceElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedIndicatorColor = TechCyan,
                                        unfocusedIndicatorColor = DarkBorder
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (weatherSearchQuery.isBlank()) {
                                            weatherSearchMessage = "请输入地名关键词"
                                            return@Button
                                        }
                                        isWeatherSearching = true
                                        weatherSearchMessage = null
                                        coroutineScope.launch {
                                            try {
                                                val results = onSearchLocations(weatherSearchQuery)
                                                weatherSearchResults = results
                                                if (results.isEmpty()) {
                                                    weatherSearchMessage = "未找到匹配地点，请尝试其他关键词或使用经纬度录入"
                                                }
                                            } catch (e: Exception) {
                                                weatherSearchMessage = "搜索异常: ${e.message}"
                                            } finally {
                                                isWeatherSearching = false
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    if (isWeatherSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("搜索", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (weatherSearchMessage != null) {
                                Text(
                                    text = weatherSearchMessage!!,
                                    color = TechAmber,
                                    fontSize = 10.5.sp
                                )
                            }

                            // 搜索结果列表
                            if (weatherSearchResults.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    weatherSearchResults.take(5).forEach { item ->
                                        val key = String.format(Locale.US, "%.4f_%.4f", item.latitude, item.longitude)
                                        val isAlreadyAdded = locationList.any {
                                            String.format(Locale.US, "%.4f_%.4f", it.lat, it.lng) == key
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(DarkSurfaceElevated)
                                                .border(1.dp, if (isAlreadyAdded) TechEmerald.copy(alpha = 0.3f) else DarkBorder, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.displayName,
                                                        color = TextPrimary,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = item.subtitle,
                                                        color = TextTertiary,
                                                        fontSize = 9.5.sp
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (isAlreadyAdded) DarkBorder else TechCyan.copy(alpha = 0.18f))
                                                        .border(1.dp, if (isAlreadyAdded) Color.Transparent else TechCyan, RoundedCornerShape(4.dp))
                                                        .clickable(enabled = !isAlreadyAdded) {
                                                            locationList = locationList + LocationItem(item.displayName, item.latitude, item.longitude)
                                                            Toast.makeText(context, "已添加 ${item.displayName}", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (isAlreadyAdded) "已添加" else "+ 添加",
                                                        color = if (isAlreadyAdded) TextTertiary else TechCyan,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 模式 1: 经纬度录入
                    if (weatherAddMode == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    placeholder = { Text("纬度 (-90 ~ 90)", fontSize = 10.5.sp, color = TextTertiary) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceElevated,
                                        unfocusedContainerColor = DarkSurfaceElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedIndicatorColor = TechCyan,
                                        unfocusedIndicatorColor = DarkBorder
                                    )
                                )
                                OutlinedTextField(
                                    value = manualLng,
                                    onValueChange = {
                                        manualLng = it
                                        manualError = null
                                    },
                                    placeholder = { Text("经度 (-180 ~ 180)", fontSize = 10.5.sp, color = TextTertiary) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceElevated,
                                        unfocusedContainerColor = DarkSurfaceElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedIndicatorColor = TechCyan,
                                        unfocusedIndicatorColor = DarkBorder
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
                                    placeholder = { Text("地点自定义名称 (可选)", fontSize = 10.5.sp, color = TextTertiary) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceElevated,
                                        unfocusedContainerColor = DarkSurfaceElevated,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedIndicatorColor = TechCyan,
                                        unfocusedIndicatorColor = DarkBorder
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
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TechEmerald, contentColor = Color.Black),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("+ 添加坐标", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (manualError != null) {
                                Text(
                                    text = manualError!!,
                                    color = TechRose,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    // 模式 2: 常用预设
                    if (weatherAddMode == 2) {
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
                                        .background(if (isAdded) TechEmerald.copy(alpha = 0.22f) else DarkSurfaceElevated)
                                        .border(1.dp, if (isAdded) TechEmerald else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable {
                                            if (isAdded) {
                                                locationList = locationList.filter {
                                                    String.format(Locale.US, "%.4f_%.4f", it.lat, it.lng) != key
                                                }
                                            } else {
                                                locationList = locationList + LocationItem(name, cLat, cLng)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isAdded) "✓ $name" else "+ $name",
                                        color = if (isAdded) TechEmerald else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // 天气行概览标签
                    val totalWeatherCount = locationList.size
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (totalWeatherCount == 0) "天气栏预览：未配置地点 (主屏显示「选择城市」)"
                            else "天气栏预览：共 $totalWeatherCount 行 (${locationList.joinToString(" ｜ ") { it.name }})",
                            color = TechCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tesla 车辆监控设置快捷入口
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, TechCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onOpenTeslaSettings() }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = TechCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Tesla 车辆监控配置",
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "连接本地 TeslaMate / Grafana API 采集车辆与能耗",
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = "配置 >",
                            color = TechCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 通用设置导入/导出卡片 (schemaVersion: 1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "普通设置迁移 (安全无凭据)",
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "导出/导入时间制式、时钟尺寸、夜间模式等偏好 (schema v1)",
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { exportLauncher.launch("chronodesk_settings_v1.json") },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(13.dp), tint = TechCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("导出", fontSize = 11.sp, color = TechCyan)
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(13.dp), tint = TechEmerald)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("导入", fontSize = 11.sp, color = TechEmerald)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 底部操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "取消", color = TextSecondary)
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
                                autoNightDim = autoNightDim,
                                nightBrightness = nightBrightness,
                                autoRefreshIntervalMinutes = refreshInterval,
                                autoLocation = autoLocation,
                                cityName = finalCityName,
                                latitude = finalLat,
                                longitude = finalLng,
                                additionalCities = finalAdditional,
                                quotaWidthPercent = quotaWidthPercent,
                                quotaCardDensity = quotaCardDensity,
                                clockScale = clockScale,
                                clockOffsetX = clockOffsetX,
                                clockOffsetY = clockOffsetY
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black)
                    ) {
                        Text(text = "保存生效", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = TextTertiary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TechCyan,
                checkedTrackColor = TechCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = DarkSurfaceElevated
            )
        )
    }
}
