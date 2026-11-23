package com.chronodesk.app.ui.tesla

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.DailyDriveItem
import com.chronodesk.app.data.model.TeslaFullSnapshot
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.model.VehicleLocation
import com.chronodesk.app.data.model.VehicleState
import com.chronodesk.app.data.utils.TeslaPhotoManager
import com.chronodesk.app.data.utils.TeslaVehicleRenderLoader
import androidx.compose.runtime.LaunchedEffect
import java.util.Locale
import com.chronodesk.app.ui.theme.DarkBackground
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBackground
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkBorderSubtle
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkSurfaceElevated
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkTextTertiary
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

@Composable
fun TeslaScreen(
    snapshot: TeslaFullSnapshot,
    settings: TeslaSettings,
    isNightMode: Boolean,
    currentTimeString: String,
    isCompact: Boolean = false,
    onRefresh: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 监听页面进入与离开，按需管理轮询生命周期
    DisposableEffect(Unit) {
        onVisibilityChanged(true)
        onDispose {
            onVisibilityChanged(false)
        }
    }

    val bgColor = if (isNightMode) DarkBackground else InkBackground
    val cardBg = if (isNightMode) DarkSurface else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary

    val context = LocalContext.current
    var showTirePressureDetail by remember { mutableStateOf(false) } // 默认展示写实实车模型，点击切换四轮胎压俯视详情
    var renderView by remember { mutableStateOf(TeslaVehicleRenderLoader.VIEW_STUD_3QTR) }
    var renderReloadTrigger by remember { mutableIntStateOf(0) }
    var showLocationMapDialog by remember { mutableStateOf(false) }
    var photoReloadTrigger by remember { mutableIntStateOf(0) }
    val customPhotoBitmap = remember(settings.dataSourceUid, settings.carId, photoReloadTrigger) {
        TeslaPhotoManager.loadScaledBitmap(context, settings.dataSourceUid, settings.carId)
    }

    val resolvedModel = snapshot.car?.normalizedModel ?: "Model 3"
    val resolvedColor = if (settings.vehicleColor == "AUTO") {
        snapshot.car?.normalizedExteriorColor ?: "RED"
    } else {
        settings.vehicleColor
    }
    val colorDisplayLabel = when (resolvedColor) {
        "RED" -> "烈焰红"
        "RED_CLASSIC" -> "经典红"
        "WHITE" -> "珍珠白"
        "BLACK" -> "纯黑"
        "SILVER" -> "星空灰"
        "BLUE" -> "深海蓝"
        else -> "烈焰红"
    }
    val paintDotColor = when (resolvedColor) {
        "RED", "RED_CLASSIC" -> Color(0xFFDC2626)
        "WHITE" -> Color(0xFFE2E8F0)
        "BLACK" -> Color(0xFF334155)
        "SILVER" -> Color(0xFF64748B)
        "BLUE" -> Color(0xFF2563EB)
        else -> Color(0xFFDC2626)
    }

    var vehicleRenderBitmap by remember(resolvedModel, resolvedColor, renderView) {
        mutableStateOf(
            TeslaVehicleRenderLoader.loadCachedBitmap(context, resolvedModel, resolvedColor, renderView)
        )
    }
    var isLoadingRender by remember { mutableStateOf(false) }
    var renderLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(resolvedModel, resolvedColor, renderView, renderReloadTrigger) {
        val cached = TeslaVehicleRenderLoader.loadCachedBitmap(context, resolvedModel, resolvedColor, renderView)
        if (cached != null) {
            vehicleRenderBitmap = cached
            isLoadingRender = false
            renderLoadFailed = false
        } else {
            isLoadingRender = true
            renderLoadFailed = false
            val fetched = TeslaVehicleRenderLoader.fetchOrLoadBitmap(context, resolvedModel, resolvedColor, renderView)
            vehicleRenderBitmap = fetched
            isLoadingRender = false
            renderLoadFailed = (fetched == null)
        }
    }

    if (showLocationMapDialog && snapshot.location.isValid) {
        VehicleLocationMapDialog(
            location = snapshot.location,
            vehicleName = snapshot.car?.displayName ?: settings.carName.takeIf { it.isNotBlank() } ?: "Tesla",
            vehicleState = snapshot.state,
            isNightMode = isNightMode,
            onDismiss = { showLocationMapDialog = false }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    TeslaPhotoManager.savePhoto(context, settings.dataSourceUid, settings.carId, stream)
                }
                photoReloadTrigger++
                showTirePressureDetail = false
                Toast.makeText(context, "车辆照片已更新", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "照片保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(
                start = if (isCompact) 14.dp else 20.dp,
                end = if (isCompact) 14.dp else 20.dp,
                top = if (isCompact) 6.dp else 10.dp,
                bottom = if (isCompact) 16.dp else 20.dp
            )
    ) {
        // ================= 顶栏：车辆名称 + 状态徽标 + 小时钟 + 同步时间与控制胶囊 =================
        TeslaTopBar(
            carName = snapshot.car?.displayName ?: settings.carName.takeIf { it.isNotBlank() } ?: "Tesla",
            state = snapshot.state.displayState,
            isCharging = snapshot.state.isCharging,
            isAsleep = snapshot.state.isAsleep,
            isLoading = snapshot.connectionState.isLoading,
            sampledTime = snapshot.snapshot.formattedSampledTime,
            lastSyncTime = snapshot.connectionState.formattedLastSuccessTime,
            currentTimeString = currentTimeString,
            isNightMode = isNightMode,
            isCompact = isCompact,
            onRefresh = onRefresh,
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

        // ================= 未配置时的优雅指引卡片 =================
        if (!settings.isConfigured) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "未配置 TeslaMate 车辆监控",
                        color = primaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "在设置中绑定 TeslaMate 即可查看车辆状态\n通过 Grafana API 直连本地数据源，只读采集车辆状态（不唤醒车辆）",
                        color = secondaryText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isNightMode) TechCyan.copy(alpha = 0.15f) else Color(0xFFE0F2FE))
                            .border(1.dp, if (isNightMode) TechCyan else Color(0xFF0284C7), RoundedCornerShape(8.dp))
                            .clickable { onOpenSettings() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "前往设置配置连接",
                            color = if (isNightMode) TechCyan else Color(0xFF0369A1),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            return
        }

        // ================= 主体区域：横向三列排版 (~60% 高度) =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.35f),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左列：电量 SOC 大字 + 续航 + 充电功率与充入电量
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(if (isCompact) 10.dp else 14.dp)
            ) {
                TeslaBatteryCardContent(
                    snapshot = snapshot,
                    isNightMode = isNightMode,
                    isCompact = isCompact
                )
            }

            // 中列：默认写实 3D 实车模型 / 自定义照片，支持一键切换独立无遮挡的四轮胎压俯视图
            Box(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(if (isCompact) 7.dp else 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val actionBg = if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
                val actionBorder = if (isNightMode) DarkBorder else InkBorder
                val accentTint = if (isNightMode) TechCyan else Color(0xFF0284C7)

                if (!showTirePressureDetail) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 顶部独立操作行：左侧车型+车漆徽标，右侧零遮挡小胶囊
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(3.5.dp))
                                        .background(paintDotColor)
                                        .border(0.6.dp, cardBorder, RoundedCornerShape(3.5.dp))
                                )
                                Text(
                                    text = "$resolvedModel · $colorDisplayLabel",
                                    color = primaryText,
                                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (customPhotoBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(actionBg)
                                            .border(0.8.dp, actionBorder, RoundedCornerShape(4.dp))
                                            .clickable {
                                                TeslaPhotoManager.deletePhoto(context, settings.dataSourceUid, settings.carId)
                                                photoReloadTrigger++
                                                Toast.makeText(context, "已恢复 3D 实车模型", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 5.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = if (isCompact) "实车" else "恢复实车",
                                            color = secondaryText,
                                            fontSize = if (isCompact) 8.5.sp else 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(actionBg)
                                            .border(0.8.dp, actionBorder, RoundedCornerShape(4.dp))
                                            .clickable {
                                                renderView = if (renderView == TeslaVehicleRenderLoader.VIEW_STUD_3QTR) {
                                                    TeslaVehicleRenderLoader.VIEW_SIDE
                                                } else {
                                                    TeslaVehicleRenderLoader.VIEW_STUD_3QTR
                                                }
                                            }
                                            .padding(horizontal = 5.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = if (renderView == TeslaVehicleRenderLoader.VIEW_STUD_3QTR) "侧面" else "3/4",
                                            color = secondaryText,
                                            fontSize = if (isCompact) 8.5.sp else 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(actionBg)
                                        .border(0.8.dp, actionBorder, RoundedCornerShape(4.dp))
                                        .clickable { showTirePressureDetail = true }
                                        .padding(horizontal = 5.dp, vertical = 2.5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "查看四轮胎压",
                                            tint = accentTint,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "胎压",
                                            color = primaryText,
                                            fontSize = if (isCompact) 8.5.sp else 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(actionBg)
                                        .border(0.8.dp, actionBorder, RoundedCornerShape(4.dp))
                                        .clickable { photoPickerLauncher.launch("image/*") }
                                        .padding(horizontal = 5.dp, vertical = 2.5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "自定义车辆照片",
                                            tint = accentTint,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = if (isCompact) "照片" else "换照片",
                                            color = primaryText,
                                            fontSize = if (isCompact) 8.5.sp else 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // 中央实车渲染展示区
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = if (isCompact) 2.dp else 4.dp)
                                .then(
                                    if (isNightMode) {
                                        Modifier.background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF1E3A5F).copy(alpha = 0.55f),
                                                    Color(0xFF0F223D).copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                customPhotoBitmap != null -> {
                                    Image(
                                        bitmap = customPhotoBitmap,
                                        contentDescription = "车辆自定义照片",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showTirePressureDetail = true }
                                    )
                                }
                                vehicleRenderBitmap != null -> {
                                    Image(
                                        bitmap = vehicleRenderBitmap!!,
                                        contentDescription = "$resolvedModel $colorDisplayLabel 实车模型",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                renderView = if (renderView == TeslaVehicleRenderLoader.VIEW_STUD_3QTR) {
                                                    TeslaVehicleRenderLoader.VIEW_SIDE
                                                } else {
                                                    TeslaVehicleRenderLoader.VIEW_STUD_3QTR
                                                }
                                            }
                                    )
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        VehicleSilhouetteCanvas(
                                            model = snapshot.car?.model,
                                            isNightMode = isNightMode,
                                            isCompact = isCompact,
                                            accentBodyColor = paintDotColor
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(actionBg.copy(alpha = 0.9f))
                                                .clickable(enabled = renderLoadFailed) { renderReloadTrigger++ }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isLoadingRender) "正在加载实车模型…" else "离线轮廓 · 点击重试加载实车",
                                                color = if (renderLoadFailed) accentTint else secondaryText,
                                                fontSize = 8.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 底部紧凑四轮胎压摘要条
                        CompactTpmsSummaryBar(
                            fl = snapshot.snapshot.tpmsPressureFl,
                            fr = snapshot.snapshot.tpmsPressureFr,
                            rl = snapshot.snapshot.tpmsPressureRl,
                            rr = snapshot.snapshot.tpmsPressureRr,
                            isNightMode = isNightMode,
                            isCompact = isCompact,
                            onClick = { showTirePressureDetail = true }
                        )
                    }
                } else {
                    // 俯视四轮胎压图：顶部独立操作条，完全消除旧版按钮遮挡右前胎压的缺陷
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(actionBg)
                                    .border(0.8.dp, actionBorder, RoundedCornerShape(4.dp))
                                    .clickable { showTirePressureDetail = false }
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "返回实车模型",
                                        tint = accentTint,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (customPhotoBitmap != null) "返回照片" else "返回实车",
                                        color = primaryText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(actionBg)
                                    .border(0.8.dp, actionBorder, RoundedCornerShape(4.dp))
                                    .clickable { photoPickerLauncher.launch("image/*") }
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "自定义照片",
                                        tint = accentTint,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (customPhotoBitmap != null) "换照片" else "自定义照片",
                                        color = primaryText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TirePressureDiagram(
                                snapshot = snapshot.snapshot,
                                isNightMode = isNightMode,
                                isCompact = isCompact
                            )
                        }
                    }
                }
            }

            // 右列：总里程 + 今日行驶 + 今日平均能耗 + 今日行程数
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(if (isCompact) 10.dp else 14.dp)
            ) {
                TeslaOdometerAndTodayContent(
                    snapshot = snapshot,
                    isNightMode = isNightMode,
                    isCompact = isCompact
                )
            }
        }

        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 9.dp))

        // ================= 底部辅助区域：4 块数据统计与位置卡片 (~40% 高度) =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.9f),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
        ) {
            // 底部 1：近 7 天能耗趋势
            Box(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(if (isCompact) 7.dp else 10.dp)
            ) {
                Recent7DaysEnergyCard(
                    items = snapshot.drives.recent7DaysDrives,
                    isNightMode = isNightMode,
                    isCompact = isCompact
                )
            }

            // 底部 2：本月行驶与充入电量
            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(if (isCompact) 7.dp else 10.dp)
            ) {
                MonthlySummaryCard(
                    monthDistance = snapshot.drives.monthDistanceKm,
                    monthTrips = snapshot.drives.monthTrips,
                    monthCharge = snapshot.charging.monthCharge,
                    isNightMode = isNightMode,
                    isCompact = isCompact
                )
            }

            // 底部 3：车内车外温控环境
            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .padding(if (isCompact) 7.dp else 10.dp)
            ) {
                ClimateSummaryCard(
                    insideTemp = snapshot.snapshot.insideTemp,
                    outsideTemp = snapshot.snapshot.outsideTemp,
                    isNightMode = isNightMode,
                    isCompact = isCompact
                )
            }

            // 底部 4：最后有效位置与地图导航
            Box(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                    .clickable(enabled = snapshot.location.isValid) {
                        showLocationMapDialog = true
                    }
                    .padding(if (isCompact) 7.dp else 10.dp)
            ) {
                TeslaLocationCard(
                    location = snapshot.location,
                    vehicleState = snapshot.state,
                    isNightMode = isNightMode,
                    isCompact = isCompact,
                    onOpenMap = { showLocationMapDialog = true }
                )
            }
        }
    }
}

/**
 * 屏幕顶栏：车辆名称 + 状态 + 小时钟 + 同步时间与控制胶囊
 */
@Composable
private fun TeslaTopBar(
    carName: String,
    state: String,
    isCharging: Boolean,
    isAsleep: Boolean,
    isLoading: Boolean,
    sampledTime: String?,
    lastSyncTime: String,
    currentTimeString: String,
    isNightMode: Boolean,
    isCompact: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val buttonBg = if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
    val buttonBorder = if (isNightMode) DarkBorder else InkBorder

    val (badgeText, badgeColor) = when {
        isCharging -> "充电中" to (if (isNightMode) TechCyan else Color(0xFF0284C7))
        isAsleep -> "休眠" to (if (isNightMode) TextTertiary else InkTextTertiary)
        state == "行驶中" -> "行驶中" to (if (isNightMode) TechEmerald else Color(0xFF16A34A))
        else -> state to (if (isNightMode) TechEmerald else Color(0xFF16A34A))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：车辆名称与当前状态
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = if (isNightMode) TechCyan else InkTextPrimary,
                modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
            )
            Text(
                text = carName,
                color = primaryText,
                fontSize = if (isCompact) 14.5.sp else 17.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(0.8.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 中间：小尺寸数字时钟
        Text(
            text = currentTimeString,
            color = if (isNightMode) TechCyan else InkTextPrimary,
            fontSize = if (isCompact) 13.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )

        // 右侧：明确区分「车辆上报于」与「同步于」及操作按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "车辆上报于 ${sampledTime ?: "—"}",
                    color = secondaryText,
                    fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "同步于 $lastSyncTime",
                    color = secondaryText.copy(alpha = 0.85f),
                    fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 刷新按钮
            val infiniteTransition = rememberInfiniteTransition(label = "spin")
            val spinAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "spinAngle"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(buttonBg)
                    .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                    .clickable { onRefresh() }
                    .padding(horizontal = if (isCompact) 7.dp else 9.dp, vertical = if (isCompact) 3.dp else 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = if (isNightMode) TechCyan else InkTextPrimary,
                        modifier = Modifier
                            .size(if (isCompact) 11.dp else 12.dp)
                            .then(if (isLoading) Modifier.rotate(spinAngle) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "刷新",
                        color = primaryText,
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 设置按钮
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(buttonBg)
                    .border(1.dp, buttonBorder, RoundedCornerShape(6.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = if (isCompact) 7.dp else 9.dp, vertical = if (isCompact) 3.dp else 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = if (isNightMode) TechCyan else InkTextPrimary,
                        modifier = Modifier.size(if (isCompact) 11.dp else 12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "设置",
                        color = primaryText,
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 左列：电量 SOC 大字 + 续航 + 充电功率
 */
@Composable
private fun TeslaBatteryCardContent(
    snapshot: TeslaFullSnapshot,
    isNightMode: Boolean,
    isCompact: Boolean
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val battery = snapshot.snapshot.batteryLevel
    val ratedKm = snapshot.snapshot.ratedBatteryRangeKm
    val estKm = snapshot.snapshot.estBatteryRangeKm
    val isCharging = snapshot.state.isCharging
    val chargerPower = snapshot.charging.chargerPower
    val energyAdded = snapshot.charging.chargeEnergyAdded

    val progressColor = if (isNightMode) {
        when {
            battery == null -> TechCyan
            battery <= 15 -> TechRose
            battery <= 30 -> TechAmber
            else -> TechEmerald
        }
    } else {
        when {
            battery == null -> Color(0xFF0284C7)
            battery <= 15 -> Color(0xFFDC2626)
            battery <= 30 -> Color(0xFFD97706)
            else -> Color(0xFF16A34A)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "剩余电量",
                color = secondaryText,
                fontSize = if (isCompact) 11.sp else 12.5.sp,
                fontWeight = FontWeight.Medium
            )
            if (isCharging) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (chargerPower != null) "${chargerPower.toInt()} kW" else "充电中",
                        color = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 大号 SOC 数字
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (battery != null) "$battery" else "—",
                color = primaryText,
                fontSize = if (isCompact) 42.sp else 50.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                lineHeight = if (isCompact) 42.sp else 50.sp
            )
            Text(
                text = "%",
                color = secondaryText,
                fontSize = if (isCompact) 18.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // 电量水平进度条
        LinearProgressIndicator(
            progress = { ((battery ?: 0) / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 6.dp else 8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
        )

        // 额定续航与预估续航
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "额定续航",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp
                )
                Text(
                    text = if (ratedKm != null) "${ratedKm.toInt()} km" else "— km",
                    color = primaryText,
                    fontSize = if (isCompact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (isCharging && energyAdded != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "充入电量",
                        color = secondaryText,
                        fontSize = if (isCompact) 9.5.sp else 10.5.sp
                    )
                    Text(
                        text = String.format("+%.1f kWh", energyAdded),
                        color = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        fontSize = if (isCompact) 13.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else if (estKm != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "预估续航",
                        color = secondaryText,
                        fontSize = if (isCompact) 9.5.sp else 10.5.sp
                    )
                    Text(
                        text = "${estKm.toInt()} km",
                        color = secondaryText,
                        fontSize = if (isCompact) 13.sp else 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * 右列：总里程 + 今日已完成行驶 + 今日平均能耗
 */
@Composable
private fun TeslaOdometerAndTodayContent(
    snapshot: TeslaFullSnapshot,
    isNightMode: Boolean,
    isCompact: Boolean
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val odometer = snapshot.snapshot.odometer
    val todayDist = snapshot.drives.todayDistanceKm
    val todayWhKm = snapshot.drives.todayEstimatedWhPerKm
    val todayTrips = snapshot.drives.todayTrips ?: 0

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 总里程
        Column {
            Text(
                text = "累计总里程",
                color = secondaryText,
                fontSize = if (isCompact) 10.5.sp else 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (odometer != null) String.format("%,.1f km", odometer) else "— km",
                color = primaryText,
                fontSize = if (isCompact) 17.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // 今日已完成行程
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "今日行驶",
                    color = secondaryText,
                    fontSize = if (isCompact) 10.sp else 11.5.sp
                )
                Text(
                    text = "${todayTrips}次行程",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (todayDist != null) String.format("%.1f km", todayDist) else "0.0 km",
                color = if (isNightMode) TechCyan else Color(0xFF0284C7),
                fontSize = if (isCompact) 17.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // 今日平均能耗 (估算)
        Column {
            Text(
                text = "今日平均能耗 (估算)",
                color = secondaryText,
                fontSize = if (isCompact) 10.sp else 11.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (todayWhKm != null && todayWhKm > 0) "${todayWhKm.toInt()} Wh/km" else "— Wh/km",
                color = primaryText,
                fontSize = if (isCompact) 14.sp else 16.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * 底部卡片 1：近 7 天能耗趋势
 */
@Composable
private fun Recent7DaysEnergyCard(
    items: List<DailyDriveItem>,
    isNightMode: Boolean,
    isCompact: Boolean
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "近 7 天行驶里程",
                color = primaryText,
                fontSize = if (isCompact) 10.5.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
            val total7Dist = items.sumOf { it.distanceKm }
            val totalText = if (items.isEmpty()) "—" else if (total7Dist == 0.0) "0 km" else String.format("%.1f km", total7Dist)
            Text(
                text = "累计 $totalText",
                color = secondaryText,
                fontSize = if (isCompact) 9.5.sp else 10.5.sp
            )
        }

        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "近 7 天行驶里程: —",
                    color = secondaryText,
                    fontSize = 11.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxDist = (items.maxOfOrNull { it.distanceKm } ?: 1.0).coerceAtLeast(10.0)
                items.takeLast(7).forEach { item ->
                    val ratio = (item.distanceKm / maxDist).toFloat().coerceIn(0.08f, 1f)
                    val barColor = if (isNightMode) TechCyan else Color(0xFF0284C7)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (item.distanceKm > 0) "${item.distanceKm.toInt()}" else "0",
                            color = secondaryText,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(if (isCompact) 8.dp else 12.dp)
                                .fillMaxHeight(ratio * 0.7f)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.dateString.takeLast(2), // 仅显示日期 22, 23
                            color = secondaryText,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * 底部卡片 2：本月行驶与充入电量
 */
@Composable
private fun MonthlySummaryCard(
    monthDistance: Double?,
    monthTrips: Int?,
    monthCharge: Double?,
    isNightMode: Boolean,
    isCompact: Boolean
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "本月汇总",
            color = primaryText,
            fontSize = if (isCompact) 10.5.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "本月里程",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp
                )
                Text(
                    text = if (monthDistance != null) String.format("%.1f km", monthDistance) else "0.0 km",
                    color = primaryText,
                    fontSize = if (isCompact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "本月充入",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp
                )
                Text(
                    text = if (monthCharge != null) String.format("%.1f kWh", monthCharge) else "0.0 kWh",
                    color = if (isNightMode) TechEmerald else Color(0xFF16A34A),
                    fontSize = if (isCompact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = "累计完成 ${monthTrips ?: 0} 次行程",
            color = secondaryText,
            fontSize = if (isCompact) 9.sp else 10.sp
        )
    }
}

/**
 * 底部卡片 3：车内车外温控环境
 */
@Composable
private fun ClimateSummaryCard(
    insideTemp: Double?,
    outsideTemp: Double?,
    isNightMode: Boolean,
    isCompact: Boolean
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "车辆温控",
                color = primaryText,
                fontSize = if (isCompact) 10.5.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.Thermostat,
                contentDescription = null,
                tint = if (isNightMode) TechCyan else Color(0xFF0284C7),
                modifier = Modifier.size(14.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "车内温度",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp
                )
                Text(
                    text = if (insideTemp != null) String.format("%.1f°C", insideTemp) else "—°C",
                    color = primaryText,
                    fontSize = if (isCompact) 13.5.sp else 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "车外温度",
                    color = secondaryText,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp
                )
                Text(
                    text = if (outsideTemp != null) String.format("%.1f°C", outsideTemp) else "—°C",
                    color = secondaryText,
                    fontSize = if (isCompact) 13.5.sp else 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = "由 positions 表最近采样",
            color = secondaryText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp
        )
    }
}

/**
 * 底部卡片 4：最后有效位置与地图导航
 */
@Composable
private fun TeslaLocationCard(
    location: VehicleLocation,
    vehicleState: VehicleState,
    isNightMode: Boolean,
    isCompact: Boolean,
    onOpenMap: () -> Unit
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary

    val (statusLabel, statusColor) = when {
        vehicleState.isDriving -> "行驶中" to (if (isNightMode) TechEmerald else Color(0xFF16A34A))
        vehicleState.isAsleep -> "休眠停靠" to (if (isNightMode) TextTertiary else InkTextTertiary)
        vehicleState.isCharging -> "充电中" to (if (isNightMode) TechCyan else Color(0xFF0284C7))
        else -> "停泊点" to secondaryText
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isNightMode) TechCyan else Color(0xFF0284C7),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "最后上报位置",
                    color = primaryText,
                    fontSize = if (isCompact) 10.5.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(statusColor.copy(alpha = 0.18f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 围栏名称 / 匹配地名与坐标副标题
        Column {
            Text(
                text = location.primaryPlaceName,
                color = primaryText,
                fontSize = if (isCompact) 11.5.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = location.secondaryPlaceDetail,
                color = secondaryText,
                fontSize = if (isCompact) 9.sp else 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 采样时间与查看地图预览弹窗入口
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (location.formattedSampledTime != null) "采样于 ${location.formattedSampledTime}" else "时间未知",
                color = secondaryText,
                fontSize = if (isCompact) 8.5.sp else 9.5.sp
            )

            if (location.isValid) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onOpenMap() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "查看地图 >",
                        color = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        fontSize = if (isCompact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTpmsSummaryBar(
    fl: Double?,
    fr: Double?,
    rl: Double?,
    rr: Double?,
    isNightMode: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val hasAny = fl != null || fr != null || rl != null || rr != null
    val bg = if (isNightMode) DarkSurfaceElevated.copy(alpha = 0.72f) else InkSurfaceElevated.copy(alpha = 0.8f)
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary

    fun fmt(v: Double?): String = if (v != null) String.format(Locale.US, "%.2f", v) else "—"
    fun tone(v: Double?): Color = when {
        v == null -> secondaryText
        v < 2.4 -> if (isNightMode) TechRose else Color(0xFFDC2626)
        v > 3.2 -> if (isNightMode) TechAmber else Color(0xFFD97706)
        else -> if (isNightMode) TechEmerald else Color(0xFF16A34A)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = if (isCompact) 3.dp else 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!hasAny) {
            Text(
                text = "点击查看四轮胎压 (暂无采样)",
                color = secondaryText,
                fontSize = if (isCompact) 8.5.sp else 9.5.sp
            )
        } else {
            val items = listOf(
                (if (isCompact) "前左" else "左前") to fl,
                (if (isCompact) "前右" else "右前") to fr,
                (if (isCompact) "后左" else "左后") to rl,
                (if (isCompact) "后右" else "右后") to rr
            )
            items.forEach { (label, valP) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        color = secondaryText,
                        fontSize = if (isCompact) 8.sp else 9.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = fmt(valP),
                        color = tone(valP),
                        fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
