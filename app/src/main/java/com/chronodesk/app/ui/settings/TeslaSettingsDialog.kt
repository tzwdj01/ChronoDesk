package com.chronodesk.app.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.ui.tesla.TeslaViewModel
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkSurfaceElevated
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

@Composable
fun TeslaSettingsDialog(
    viewModel: TeslaViewModel,
    isNightMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentSettings by viewModel.settings.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val discoveredDatasources by viewModel.discoveredDatasources.collectAsState()
    val discoveredCars by viewModel.discoveredCars.collectAsState()

    var enabled by remember { mutableStateOf(currentSettings.enabled) }
    var baseUrl by remember { mutableStateOf(currentSettings.baseUrl) }
    var token by remember { mutableStateOf(viewModel.getStoredToken() ?: "") }
    var isTokenVisible by remember { mutableStateOf(false) }
    var dataSourceUid by remember { mutableStateOf(currentSettings.dataSourceUid) }
    var dataSourceType by remember { mutableStateOf(currentSettings.dataSourceType) }
    var carId by remember { mutableIntStateOf(currentSettings.carId) }
    var carName by remember { mutableStateOf(currentSettings.carName) }
    var refreshIntervalSec by remember { mutableIntStateOf(currentSettings.refreshIntervalSec) }
    var timeZone by remember { mutableStateOf(currentSettings.timeZone) }
    var allowHttp by remember { mutableStateOf(currentSettings.allowHttp) }

    LaunchedEffect(discoveredDatasources) {
        if (dataSourceUid.isBlank() && discoveredDatasources.isNotEmpty()) {
            dataSourceUid = discoveredDatasources.first().uid
        }
    }

    LaunchedEffect(discoveredCars) {
        if (discoveredCars.isNotEmpty()) {
            val exists = discoveredCars.any { it.id == carId }
            if (!exists) {
                carId = discoveredCars.first().id
                carName = discoveredCars.first().name ?: "Tesla"
            }
        }
    }

    val bg = if (isNightMode) DarkSurface else InkSurface
    val border = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 顶栏：标题与关闭按钮
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
                            tint = if (isNightMode) TechCyan else Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Tesla 车辆监控配置 (TeslaMate / Grafana)",
                            color = primaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. 启用开关
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "启用车辆监控页",
                            color = primaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "开启后主界面将增加第 3 页横向滑屏展示车辆实时状态",
                            color = secondaryText,
                            fontSize = 10.5.sp
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (isNightMode) TechCyan else Color(0xFF0284C7)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Grafana Base URL
                Text(
                    text = "Grafana 根地址 (Base URL)",
                    color = primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    placeholder = { Text("https://grafana.example.com", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedBorderColor = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        unfocusedBorderColor = border
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. 服务账号 Token
                Text(
                    text = "Grafana 服务账号 Token (只读 SELECT 权限)",
                    color = primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    placeholder = { Text("粘贴 Grafana Service Account Token", fontSize = 11.5.sp) },
                    singleLine = true,
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Icon(
                                imageVector = if (isTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isTokenVisible) "隐藏" else "显示",
                                tint = secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedBorderColor = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        unfocusedBorderColor = border
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 4. 允许可信内网 HTTP 勾选
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { allowHttp = !allowHttp }
                ) {
                    Checkbox(
                        checked = allowHttp,
                        onCheckedChange = { allowHttp = it }
                    )
                    Text(
                        text = "允许可信内网 HTTP（Token 将在网络明文传输，仅限个人可信局域网）",
                        color = if (allowHttp) (if (isNightMode) TechAmber else Color(0xFFB45309)) else secondaryText,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. 测试连接与车辆发现按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isNightMode) TechCyan.copy(alpha = 0.15f) else Color(0xFFE0F2FE))
                            .border(1.dp, if (isNightMode) TechCyan else Color(0xFF0284C7), RoundedCornerShape(8.dp))
                            .clickable(enabled = !isTesting) {
                                if (baseUrl.isBlank() || token.isBlank()) {
                                    Toast.makeText(context, "请先填写 Base URL 和 Token", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                val temp = TeslaSettings(
                                    enabled = enabled,
                                    baseUrl = baseUrl,
                                    dataSourceUid = dataSourceUid,
                                    dataSourceType = dataSourceType,
                                    carId = carId,
                                    carName = carName,
                                    refreshIntervalSec = refreshIntervalSec,
                                    timeZone = timeZone,
                                    allowHttp = allowHttp
                                )
                                viewModel.testConnection(temp, token)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 1.5.dp,
                                    color = if (isNightMode) TechCyan else Color(0xFF0284C7)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (isTesting) "正在测试连接…" else "① 测试连接与发现车辆",
                                color = if (isNightMode) TechCyan else Color(0xFF0284C7),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!testResult.isNullOrBlank()) {
                        val isSuccess = testResult!!.startsWith("连接成功")
                        Text(
                            text = testResult!!,
                            color = if (isSuccess) (if (isNightMode) TechEmerald else Color(0xFF16A34A)) else (if (isNightMode) TechRose else Color(0xFFDC2626)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6. 数据源 UID 选择 / 手动输入
                Text(
                    text = "PostgreSQL 数据源 UID",
                    color = primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (discoveredDatasources.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        discoveredDatasources.forEach { ds ->
                            val isSelected = dataSourceUid == ds.uid
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) (if (isNightMode) TechCyan.copy(alpha = 0.2f) else Color(0xFFE0F2FE)) else (if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated))
                                    .border(1.dp, if (isSelected) (if (isNightMode) TechCyan else Color(0xFF0284C7)) else border, RoundedCornerShape(6.dp))
                                    .clickable {
                                        dataSourceUid = ds.uid
                                        dataSourceType = ds.type
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${ds.name} (${ds.uid})",
                                    color = if (isSelected) (if (isNightMode) TechCyan else Color(0xFF0284C7)) else secondaryText,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                OutlinedTextField(
                    value = dataSourceUid,
                    onValueChange = { dataSourceUid = it },
                    placeholder = { Text("例如: teslamate 或从上面点击选择", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedBorderColor = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        unfocusedBorderColor = border
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 7. 车辆选择
                Text(
                    text = "目标车辆",
                    color = primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (discoveredCars.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        discoveredCars.forEach { car ->
                            val isSelected = carId == car.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) (if (isNightMode) TechCyan.copy(alpha = 0.2f) else Color(0xFFE0F2FE)) else (if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated))
                                    .border(1.dp, if (isSelected) (if (isNightMode) TechCyan else Color(0xFF0284C7)) else border, RoundedCornerShape(6.dp))
                                    .clickable {
                                        carId = car.id
                                        carName = car.displayName
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${car.displayName} (#${car.id})",
                                    color = if (isSelected) (if (isNightMode) TechCyan else Color(0xFF0284C7)) else secondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = carName,
                        onValueChange = { carName = it },
                        label = { Text("车辆名称", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = primaryText,
                            unfocusedTextColor = primaryText,
                            focusedBorderColor = if (isNightMode) TechCyan else Color(0xFF0284C7),
                            unfocusedBorderColor = border
                        )
                    )

                    OutlinedTextField(
                        value = carId.toString(),
                        onValueChange = { carId = it.toIntOrNull() ?: 1 },
                        label = { Text("车辆 ID", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = primaryText,
                            unfocusedTextColor = primaryText,
                            focusedBorderColor = if (isNightMode) TechCyan else Color(0xFF0284C7),
                            unfocusedBorderColor = border
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 8. 刷新间隔与时区设置
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "刷新频率", color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(30, 60, 120).forEach { sec ->
                                val isSel = refreshIntervalSec == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) (if (isNightMode) TechCyan else Color(0xFF0284C7)) else (if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated))
                                        .clickable { refreshIntervalSec = sec }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        color = if (isSel) Color.White else secondaryText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(text = "统计时区", color = primaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = timeZone,
                            onValueChange = { timeZone = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = primaryText,
                                unfocusedTextColor = primaryText,
                                focusedBorderColor = if (isNightMode) TechCyan else Color(0xFF0284C7),
                                unfocusedBorderColor = border
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 9. 底部操作按钮：清除连接 + 取消 + 保存生效
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 清除配置
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isNightMode) TechRose.copy(alpha = 0.15f) else Color(0xFFFEE2E2))
                            .clickable {
                                viewModel.clearConfiguration()
                                Toast.makeText(context, "已清除 TeslaMate 配置与本地缓存", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = if (isNightMode) TechRose else Color(0xFFDC2626),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "清除配置与缓存",
                                color = if (isNightMode) TechRose else Color(0xFFDC2626),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(text = "取消", color = secondaryText, fontSize = 12.sp)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isNightMode) TechCyan else Color(0xFF0284C7))
                                .clickable {
                                    val updated = TeslaSettings(
                                        enabled = enabled,
                                        baseUrl = baseUrl.trim(),
                                        dataSourceUid = dataSourceUid.trim(),
                                        dataSourceType = dataSourceType.trim(),
                                        carId = carId,
                                        carName = carName.trim(),
                                        refreshIntervalSec = refreshIntervalSec,
                                        timeZone = timeZone.trim(),
                                        allowHttp = allowHttp
                                    )
                                    val saved = viewModel.updateSettings(updated, token.takeIf { it.isNotBlank() })
                                    if (saved) {
                                        Toast.makeText(context, "TeslaMate 配置已保存", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "设备安全存储不可用，凭据未保存", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "保存并生效",
                                    color = Color.White,
                                    fontSize = 12.sp,
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
