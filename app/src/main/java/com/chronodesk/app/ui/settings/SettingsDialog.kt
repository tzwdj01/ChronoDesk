package com.chronodesk.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

@Composable
fun SettingsDialog(
    currentSettings: ClockSettings,
    onDismiss: () -> Unit,
    onSave: (ClockSettings) -> Unit
) {
    var is24Hour by remember { mutableStateOf(currentSettings.is24Hour) }
    var showSeconds by remember { mutableStateOf(currentSettings.showSeconds) }
    var keepScreenOn by remember { mutableStateOf(currentSettings.keepScreenOn) }
    var autoNightDim by remember { mutableStateOf(currentSettings.autoNightDim) }
    var nightBrightness by remember { mutableFloatStateOf(currentSettings.nightBrightness) }
    var refreshInterval by remember { mutableIntStateOf(currentSettings.autoRefreshIntervalMinutes) }
    var selectedCity by remember { mutableStateOf(currentSettings.cityName) }
    var lat by remember { mutableStateOf(currentSettings.latitude) }
    var lng by remember { mutableStateOf(currentSettings.longitude) }

    val cityPresets = listOf(
        Triple("北京", 39.9042, 116.4074),
        Triple("上海", 31.2304, 121.4737),
        Triple("广州", 23.1291, 113.2644),
        Triple("深圳", 22.5431, 114.0579),
        Triple("成都", 30.5728, 104.0668),
        Triple("杭州", 30.2741, 120.1551),
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
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "时钟与系统设置",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // 24小时制
                SettingToggleItem(
                    title = "24小时制",
                    subtitle = "关闭则采用12小时制",
                    checked = is24Hour,
                    onCheckedChange = { is24Hour = it }
                )

                // 显示秒钟
                SettingToggleItem(
                    title = "显示秒钟",
                    subtitle = "主时钟右侧显示秒钟计数",
                    checked = showSeconds,
                    onCheckedChange = { showSeconds = it }
                )

                // 屏幕常亮
                SettingToggleItem(
                    title = "保持屏幕常亮",
                    subtitle = "放置桌面时不自动休眠息屏",
                    checked = keepScreenOn,
                    onCheckedChange = { keepScreenOn = it }
                )

                // 夜间自动调暗
                SettingToggleItem(
                    title = "夜间自动暗光模式",
                    subtitle = "22:00 至次日 07:00 自动调低窗口亮度并纯黑背景",
                    checked = autoNightDim,
                    onCheckedChange = { autoNightDim = it }
                )

                if (autoNightDim) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "夜间窗口亮度: ${(nightBrightness * 100).toInt()}%",
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

                // 额度自动刷新间隔
                Column {
                    Text(
                        text = "额度自动刷新间隔",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
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
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}分",
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 城市选择
                Column {
                    Text(
                        text = "当前天气城市",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cityPresets.take(4).forEach { (name, cLat, cLng) ->
                            val isSel = selectedCity == name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) TechCyan.copy(alpha = 0.2f) else DarkSurfaceElevated)
                                    .border(1.dp, if (isSel) TechCyan else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedCity = name
                                        lat = cLat
                                        lng = cLng
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSel) TechCyan else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

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
                                autoNightDim = autoNightDim,
                                nightBrightness = nightBrightness,
                                autoRefreshIntervalMinutes = refreshInterval,
                                cityName = selectedCity,
                                latitude = lat,
                                longitude = lng
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
