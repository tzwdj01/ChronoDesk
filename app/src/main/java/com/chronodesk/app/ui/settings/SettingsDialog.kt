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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext
import com.chronodesk.app.data.repository.GeneralSettingsBackup
import com.chronodesk.app.data.repository.SettingsValidationResult
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
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
    onRequestLocationPermission: () -> Unit = {}
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
    var keepScreenOn by remember { mutableStateOf(currentSettings.keepScreenOn) }
    var nightModeOption by remember { mutableStateOf(currentSettings.nightModeOption) }
    var autoNightDim by remember { mutableStateOf(currentSettings.autoNightDim) }
    var nightBrightness by remember { mutableFloatStateOf(currentSettings.nightBrightness) }
    var refreshInterval by remember { mutableIntStateOf(currentSettings.autoRefreshIntervalMinutes) }
    var autoLocation by remember { mutableStateOf(currentSettings.autoLocation) }
    var selectedCity by remember { mutableStateOf(currentSettings.cityName) }
    var lat by remember { mutableStateOf(currentSettings.latitude) }
    var lng by remember { mutableStateOf(currentSettings.longitude) }
    var additionalCities by remember { mutableStateOf(currentSettings.additionalCities) }
    var clockScale by remember { mutableFloatStateOf(currentSettings.clockScale) }
    var clockOffsetX by remember { mutableFloatStateOf(currentSettings.clockOffsetX) }
    var clockOffsetY by remember { mutableFloatStateOf(currentSettings.clockOffsetY) }
    var quotaWidthPercent by remember { mutableIntStateOf(currentSettings.quotaWidthPercent) }
    var quotaCardDensity by remember { mutableStateOf(currentSettings.quotaCardDensity) }

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

                SettingToggleItem(
                    title = "保持屏幕常亮",
                    subtitle = "桌面放置时不自动熄屏 (FLAG_KEEP_SCREEN_ON)",
                    checked = keepScreenOn,
                    onCheckedChange = { keepScreenOn = it }
                )

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

                // 4. 天气与城市设置 (多城市与多行天气支持)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "天气与城市位置 (支持多地区多行并列显示)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    SettingToggleItem(
                        title = "开启自动定位",
                        subtitle = "依据设备 GPS 或网络自动获取首选城市天气（关闭则固定使用选中城市）",
                        checked = autoLocation,
                        onCheckedChange = { checked ->
                            autoLocation = checked
                            if (checked) {
                                onRequestLocationPermission()
                            }
                        }
                    )

                    val extraList = remember(additionalCities) {
                        if (additionalCities.isBlank()) emptyList<String>()
                        else additionalCities.split(";").mapNotNull { it.split(":").firstOrNull()?.trim() }.filter { it.isNotBlank() }
                    }

                    // 1. 首选主城市 (第1行天气)
                    Text(
                        text = "1. 首选主城市（天气第 1 行）：",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cityPresets.forEach { (name, cLat, cLng) ->
                            val isSel = selectedCity == name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) TechCyan.copy(alpha = 0.22f) else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) TechCyan else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedCity = name
                                        lat = cLat
                                        lng = cLng
                                        autoLocation = false
                                        if (extraList.contains(name)) {
                                            additionalCities = additionalCities.split(";")
                                                .filter { !it.startsWith("$name:") && it.isNotBlank() }
                                                .joinToString(";")
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isSel) "★ $name" else name,
                                    color = if (isSel) TechCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // 2. 添加额外地区 (开启多行并列显示)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "2. 额外地区（点击开启第 2、3 行多地天气并列展示）：",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cityPresets.filter { it.first != selectedCity }.forEach { (name, cLat, cLng) ->
                            val isExtra = extraList.contains(name)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isExtra) TechEmerald.copy(alpha = 0.22f) else DarkSurfaceElevated)
                                    .border(1.dp, if (isExtra) TechEmerald else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (isExtra) {
                                            additionalCities = additionalCities.split(";")
                                                .filter { !it.startsWith("$name:") && it.isNotBlank() }
                                                .joinToString(";")
                                        } else {
                                            val entry = "$name:$cLat:$cLng"
                                            additionalCities = if (additionalCities.isBlank()) entry else "$additionalCities;$entry"
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isExtra) "✓ $name" else "+ $name",
                                    color = if (isExtra) TechEmerald else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isExtra) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // 天气行概览标签
                    val totalWeatherCount = 1 + extraList.size
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "天气栏预览：共 $totalWeatherCount 行 ($selectedCity${if (extraList.isNotEmpty()) " ｜ " + extraList.joinToString(" ｜ ") else ""})",
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
                            val updated = currentSettings.copy(
                                is24Hour = is24Hour,
                                showSeconds = showSeconds,
                                keepScreenOn = keepScreenOn,
                                nightModeOption = nightModeOption,
                                autoNightDim = autoNightDim,
                                nightBrightness = nightBrightness,
                                autoRefreshIntervalMinutes = refreshInterval,
                                autoLocation = autoLocation,
                                cityName = selectedCity,
                                latitude = lat,
                                longitude = lng,
                                additionalCities = additionalCities,
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
