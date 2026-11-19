package com.chronodesk.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentSettings: ClockSettings,
    onDismiss: () -> Unit,
    onSave: (ClockSettings) -> Unit
) {
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

                // 4. 天气与城市设置
                Column {
                    Text(
                        text = "天气与城市位置 (Open-Meteo 开放接口)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    SettingToggleItem(
                        title = "自动定位匹配",
                        subtitle = "依据设备 GPS 或网络自动获取所在城市",
                        checked = autoLocation,
                        onCheckedChange = { autoLocation = it }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "手动选择常驻城市：",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

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
                                    .background(if (isSel) TechCyan.copy(alpha = 0.2f) else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) TechCyan else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedCity = name
                                        lat = cLat
                                        lng = cLng
                                        autoLocation = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSel) TechCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

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
                                quotaWidthPercent = quotaWidthPercent,
                                quotaCardDensity = quotaCardDensity
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
