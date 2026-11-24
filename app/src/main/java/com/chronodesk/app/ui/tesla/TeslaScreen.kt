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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.DailyDriveItem
import com.chronodesk.app.data.model.LastDriveInfo
import com.chronodesk.app.data.model.TeslaFullSnapshot
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.model.VehicleLocation
import com.chronodesk.app.data.model.VehicleState
import com.chronodesk.app.data.utils.TeslaPhotoManager
import com.chronodesk.app.data.utils.TeslaVehicleRenderLoader
import com.chronodesk.app.ui.theme.DarkBackground
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkProgressTrack
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBackground
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkProgressTrack
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
import java.util.Locale

private data class TeslaCockpitPalette(
    val isNight: Boolean,
    val bg: Color,
    val cardBg: Color,
    val innerBg: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val positive: Color,
    val warning: Color,
    val danger: Color,
    val trackBg: Color
) {
    companion object {
        fun from(isNightMode: Boolean): TeslaCockpitPalette = if (isNightMode) {
            TeslaCockpitPalette(
                isNight = true,
                bg = DarkBackground,
                cardBg = DarkSurface,
                innerBg = DarkSurfaceElevated,
                border = DarkBorder,
                primaryText = TextPrimary,
                secondaryText = TextSecondary,
                mutedText = TextTertiary,
                accent = TechCyan,
                positive = TechEmerald,
                warning = TechAmber,
                danger = TechRose,
                trackBg = DarkProgressTrack
            )
        } else {
            TeslaCockpitPalette(
                isNight = false,
                bg = InkBackground,
                cardBg = InkSurface,
                innerBg = InkSurfaceElevated,
                border = InkBorder,
                primaryText = InkTextPrimary,
                secondaryText = InkTextSecondary,
                mutedText = InkTextTertiary,
                accent = Color(0xFF0284C7),
                positive = InkStatusGreen,
                warning = InkWarningAmber,
                danger = InkWarningRed,
                trackBg = InkProgressTrack
            )
        }
    }
}

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
    DisposableEffect(Unit) {
        onVisibilityChanged(true)
        onDispose {
            onVisibilityChanged(false)
        }
    }

    val palette = remember(isNightMode) { TeslaCockpitPalette.from(isNightMode) }
    val context = LocalContext.current

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

    val renderView = TeslaVehicleRenderLoader.VIEW_STUD_3QTR
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
                Toast.makeText(context, "车辆实拍已更新", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "照片保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(
                start = if (isCompact) 12.dp else 18.dp,
                end = if (isCompact) 12.dp else 18.dp,
                top = if (isCompact) 6.dp else 10.dp,
                bottom = if (isCompact) 16.dp else 20.dp
            )
    ) {
        TeslaCockpitTopBar(
            carName = snapshot.car?.displayName ?: settings.carName.takeIf { it.isNotBlank() } ?: "Tesla",
            modelAndColor = "$resolvedModel · $colorDisplayLabel",
            paintDotColor = paintDotColor,
            state = snapshot.state,
            chargerPowerKw = snapshot.charging.chargerPower,
            isLoading = snapshot.connectionState.isLoading,
            sampledTime = snapshot.snapshot.formattedSampledTime,
            lastSyncTime = snapshot.connectionState.formattedLastSuccessTime,
            currentTimeString = currentTimeString,
            palette = palette,
            isCompact = isCompact,
            onRefresh = onRefresh,
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

        if (!settings.isConfigured) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.cardBg)
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = "尚未连接 TeslaMate 数据源",
                        color = palette.primaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "只读接入 Grafana PostgreSQL，实时展示车型车况、中文定位与最近行程",
                        color = palette.secondaryText,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.accent)
                            .clickable { onOpenSettings() }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "前往配置 TeslaMate / Grafana",
                            color = if (isNightMode) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            return
        }

        // 双栏非对称驾驶舱主体：左展台车模 + 续航电量 + 四轮数值，右可扩展模块化信息网格
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
        ) {
            // ================= 左栏：展台级车模 + 电量/续航 + 四轮数字胎压 =================
            TeslaShowroomHeroCard(
                customPhotoBitmap = customPhotoBitmap,
                vehicleRenderBitmap = vehicleRenderBitmap,
                isLoadingRender = isLoadingRender,
                renderLoadFailed = renderLoadFailed,
                resolvedModel = resolvedModel,
                batteryLevel = snapshot.snapshot.batteryLevel,
                ratedRangeKm = snapshot.snapshot.ratedBatteryRangeKm,
                estRangeKm = snapshot.snapshot.estBatteryRangeKm,
                isCharging = snapshot.state.isCharging,
                chargerPower = snapshot.charging.chargerPower,
                chargeAdded = snapshot.charging.chargeEnergyAdded,
                fl = snapshot.snapshot.tpmsPressureFl,
                fr = snapshot.snapshot.tpmsPressureFr,
                rl = snapshot.snapshot.tpmsPressureRl,
                rr = snapshot.snapshot.tpmsPressureRr,
                palette = palette,
                isCompact = isCompact,
                onPickPhoto = { photoPickerLauncher.launch("image/*") },
                onRestoreModel = {
                    TeslaPhotoManager.deletePhoto(context, settings.dataSourceUid, settings.carId)
                    photoReloadTrigger++
                    Toast.makeText(context, "已恢复 3D 车辆模型", Toast.LENGTH_SHORT).show()
                },
                onRetryRender = { renderReloadTrigger++ },
                modifier = Modifier
                    .weight(if (isCompact) 0.43f else 0.42f)
                    .fillMaxHeight()
            )

            // ================= 右栏：可扩展模块化 Bento 指标矩阵 =================
            Column(
                modifier = Modifier
                    .weight(if (isCompact) 0.57f else 0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
            ) {
                // 第一行：最后上报位置（中文） + 最后行驶里程（新增）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.04f),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
                ) {
                    TeslaModuleCard(
                        palette = palette,
                        isCompact = isCompact,
                        modifier = Modifier
                            .weight(1.12f)
                            .fillMaxHeight()
                            .clickable(enabled = snapshot.location.isValid) {
                                showLocationMapDialog = true
                            }
                    ) {
                        LastLocationModuleContent(
                            location = snapshot.location,
                            vehicleState = snapshot.state,
                            palette = palette,
                            isCompact = isCompact
                        )
                    }

                    TeslaModuleCard(
                        palette = palette,
                        isCompact = isCompact,
                        modifier = Modifier
                            .weight(0.88f)
                            .fillMaxHeight()
                    ) {
                        LastDriveModuleContent(
                            lastDrive = snapshot.drives.lastDrive,
                            palette = palette,
                            isCompact = isCompact
                        )
                    }
                }

                // 第二行：今日行驶与均耗 + 累计里程与座舱温控
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.92f),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
                ) {
                    TeslaModuleCard(
                        palette = palette,
                        isCompact = isCompact,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        TodayDriveModuleContent(
                            todayDist = snapshot.drives.todayDistanceKm,
                            todayTrips = snapshot.drives.todayTrips,
                            todayWhKm = snapshot.drives.todayEstimatedWhPerKm,
                            todayChargeKwh = snapshot.charging.todayCharge,
                            palette = palette,
                            isCompact = isCompact
                        )
                    }

                    TeslaModuleCard(
                        palette = palette,
                        isCompact = isCompact,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        OdometerAndClimateModuleContent(
                            odometer = snapshot.snapshot.odometer,
                            insideTemp = snapshot.snapshot.insideTemp,
                            outsideTemp = snapshot.snapshot.outsideTemp,
                            palette = palette,
                            isCompact = isCompact
                        )
                    }
                }

                // 第三行：近 7 天里程趋势 + 本月汇总
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.04f),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
                ) {
                    TeslaModuleCard(
                        palette = palette,
                        isCompact = isCompact,
                        modifier = Modifier
                            .weight(1.12f)
                            .fillMaxHeight()
                    ) {
                        Recent7DaysTrendModuleContent(
                            items = snapshot.drives.recent7DaysDrives,
                            palette = palette,
                            isCompact = isCompact
                        )
                    }

                    TeslaModuleCard(
                        palette = palette,
                        isCompact = isCompact,
                        modifier = Modifier
                            .weight(0.88f)
                            .fillMaxHeight()
                    ) {
                        MonthlySummaryModuleContent(
                            monthDistance = snapshot.drives.monthDistanceKm,
                            monthTrips = snapshot.drives.monthTrips,
                            monthCharge = snapshot.charging.monthCharge,
                            palette = palette,
                            isCompact = isCompact
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeslaModuleCard(
    palette: TeslaCockpitPalette,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 12.dp else 14.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.border, RoundedCornerShape(if (isCompact) 12.dp else 14.dp))
            .padding(horizontal = if (isCompact) 9.dp else 12.dp, vertical = if (isCompact) 7.dp else 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@Composable
private fun TeslaCockpitTopBar(
    carName: String,
    modelAndColor: String,
    paintDotColor: Color,
    state: VehicleState,
    chargerPowerKw: Double?,
    isLoading: Boolean,
    sampledTime: String?,
    lastSyncTime: String,
    currentTimeString: String,
    palette: TeslaCockpitPalette,
    isCompact: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val (badgeText, badgeColor) = when {
        state.isCharging -> {
            val kw = chargerPowerKw?.takeIf { it > 0 }?.let { " ${String.format(Locale.US, "%.1f", it)}kW" } ?: ""
            "充电中$kw" to palette.accent
        }
        state.isDriving -> "行驶中" to palette.positive
        state.isAsleep -> "休眠停靠" to palette.mutedText
        else -> state.displayState to palette.positive
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
            modifier = Modifier.weight(1.2f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 8.dp else 10.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Text(
                text = carName,
                color = palette.primaryText,
                fontSize = if (isCompact) 15.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(badgeColor.copy(alpha = 0.14f))
                    .border(0.8.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = if (isCompact) 9.sp else 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            if (!isCompact) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(palette.innerBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(paintDotColor)
                    )
                    Text(
                        text = modelAndColor,
                        color = palette.secondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = currentTimeString,
            color = palette.accent,
            fontSize = if (isCompact) 12.5.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 5.dp else 8.dp)
        ) {
            Text(
                text = if (sampledTime != null) "上报 $sampledTime" else "同步 $lastSyncTime",
                color = palette.mutedText,
                fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                fontFamily = FontFamily.Monospace
            )

            val infiniteTransition = rememberInfiniteTransition(label = "teslaRefresh")
            val spin by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
                label = "teslaRefreshAngle"
            )

            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 42.dp, minHeight = 36.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(palette.innerBg)
                    .border(1.dp, palette.border, RoundedCornerShape(7.dp))
                    .clickable(enabled = !isLoading) { onRefresh() }
                    .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = if (isCompact) 4.dp else 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新车辆",
                        tint = palette.accent,
                        modifier = Modifier
                            .size(if (isCompact) 11.dp else 13.dp)
                            .rotate(if (isLoading) spin else 0f)
                    )
                    Text(
                        text = if (isLoading) "同步中" else "刷新",
                        color = palette.primaryText,
                        fontSize = if (isCompact) 9.5.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 42.dp, minHeight = 36.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(palette.innerBg)
                    .border(1.dp, palette.border, RoundedCornerShape(7.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = if (isCompact) 4.dp else 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "车辆设置",
                        tint = palette.secondaryText,
                        modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
                    )
                    Text(
                        text = "设置",
                        color = palette.primaryText,
                        fontSize = if (isCompact) 9.5.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TeslaShowroomHeroCard(
    customPhotoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    vehicleRenderBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    isLoadingRender: Boolean,
    renderLoadFailed: Boolean,
    resolvedModel: String,
    batteryLevel: Int?,
    ratedRangeKm: Double?,
    estRangeKm: Double?,
    isCharging: Boolean,
    chargerPower: Double?,
    chargeAdded: Double?,
    fl: Double?,
    fr: Double?,
    rl: Double?,
    rr: Double?,
    palette: TeslaCockpitPalette,
    isCompact: Boolean,
    onPickPhoto: () -> Unit,
    onRestoreModel: () -> Unit,
    onRetryRender: () -> Unit,
    modifier: Modifier = Modifier
) {
    val soc = batteryLevel ?: 0
    val socColor = when {
        isCharging -> palette.accent
        batteryLevel == null -> palette.secondaryText
        soc <= 10 -> palette.danger
        soc <= 20 -> palette.warning
        else -> palette.positive
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 13.dp else 16.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.border, RoundedCornerShape(if (isCompact) 13.dp else 16.dp))
            .padding(horizontal = if (isCompact) 10.dp else 14.dp, vertical = if (isCompact) 8.dp else 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 顶部精简操作行（无 SVG 切换按钮，仅保留自定义实拍更换入口）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (customPhotoBitmap != null) "$resolvedModel · 实拍照片" else "$resolvedModel · 数字车展",
                color = palette.mutedText,
                fontSize = if (isCompact) 9.sp else 10.5.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (customPhotoBitmap != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(palette.innerBg)
                            .border(0.6.dp, palette.border, RoundedCornerShape(5.dp))
                            .clickable { onRestoreModel() }
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = "恢复车模",
                            color = palette.secondaryText,
                            fontSize = if (isCompact) 8.5.sp else 9.5.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(palette.innerBg)
                        .border(0.6.dp, palette.border, RoundedCornerShape(5.dp))
                        .clickable { onPickPhoto() }
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(if (isCompact) 9.dp else 11.dp)
                        )
                        Text(
                            text = if (customPhotoBitmap != null) "更换" else "换实拍",
                            color = palette.primaryText,
                            fontSize = if (isCompact) 8.5.sp else 9.5.sp
                        )
                    }
                }
            }
        }

        // 中部：高清 3D 车辆模型或实拍图（带地台光影，完全移除 SVG）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = if (isCompact) 2.dp else 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (customPhotoBitmap != null) {
                Image(
                    bitmap = customPhotoBitmap,
                    contentDescription = "车辆实拍照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                // 地台光影氛围
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.62f)
                    if (palette.isNight) {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF1D4ED8).copy(alpha = 0.22f), Color.Transparent),
                                center = center,
                                radius = size.width * 0.56f
                            )
                        )
                    } else {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF0F172A).copy(alpha = 0.08f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.78f),
                                radius = size.width * 0.45f
                            )
                        )
                    }
                }

                when {
                    vehicleRenderBitmap != null -> {
                        Image(
                            bitmap = vehicleRenderBitmap,
                            contentDescription = "$resolvedModel 3D 车模",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (isCompact) 4.dp else 6.dp)
                        )
                    }
                    isLoadingRender -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = palette.accent
                            )
                            Text(
                                text = "正在加载高清车模…",
                                color = palette.mutedText,
                                fontSize = 10.sp
                            )
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.innerBg)
                                .clickable { onRetryRender() }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = if (renderLoadFailed) "车模加载失败 · 点击重试" else "点击加载车模",
                                color = palette.secondaryText,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // 车模下方核心焦点：剩余电量 % + 额定续航 km + 进度条
        Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // 左侧大号电量百分比
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (batteryLevel != null) "$batteryLevel" else "—",
                        color = socColor,
                        fontSize = if (isCompact) 26.sp else 33.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = if (isCompact) 28.sp else 35.sp
                    )
                    Column(modifier = Modifier.padding(bottom = 3.dp)) {
                        Text(
                            text = "%",
                            color = socColor,
                            fontSize = if (isCompact) 11.sp else 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "剩余电量",
                            color = palette.mutedText,
                            fontSize = if (isCompact) 8.5.sp else 9.5.sp
                        )
                    }
                }

                // 右侧合理大号额定续航里程
                Column(horizontalAlignment = Alignment.End) {
                    if (isCharging && chargerPower != null && chargerPower > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(11.dp)
                            )
                            val addedStr = chargeAdded?.let { " · +${String.format(Locale.US, "%.1f", it)}kWh" } ?: ""
                            Text(
                                text = "${String.format(Locale.US, "%.1f", chargerPower)}kW$addedStr",
                                color = palette.accent,
                                fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = if (ratedRangeKm != null) "${ratedRangeKm.toInt()} km" else "— km",
                        color = palette.primaryText,
                        fontSize = if (isCompact) 19.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    val estLabel = estRangeKm?.let { "预估 ${it.toInt()} km" } ?: "额定续航"
                    Text(
                        text = estLabel,
                        color = palette.mutedText,
                        fontSize = if (isCompact) 8.5.sp else 9.5.sp
                    )
                }
            }

            // 电量填充条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 5.5.dp else 7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.trackBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((soc / 100f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(socColor)
                )
            }

            // 底部精简四轮胎压纯数字条（bar，无 SVG 示意图）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(palette.innerBg)
                    .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = if (isCompact) 4.dp else 5.5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("左前" to fl, "右前" to fr, "左后" to rl, "右后" to rr).forEach { (wheel, value) ->
                    val abnormal = value != null && (value < 2.50 || value > 3.30)
                    val vColor = when {
                        value == null -> palette.mutedText
                        abnormal -> palette.warning
                        else -> palette.positive
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = wheel,
                            color = palette.secondaryText,
                            fontSize = if (isCompact) 8.sp else 9.5.sp
                        )
                        Text(
                            text = if (value != null) String.format(Locale.US, "%.2f", value) else "--",
                            color = vColor,
                            fontSize = if (isCompact) 8.5.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.LastLocationModuleContent(
    location: VehicleLocation,
    vehicleState: VehicleState,
    palette: TeslaCockpitPalette,
    isCompact: Boolean
) {
    val (statusLabel, statusColor) = when {
        vehicleState.isDriving -> "行驶中" to palette.positive
        vehicleState.isCharging -> "充电中" to palette.accent
        vehicleState.isAsleep -> "休眠" to palette.mutedText
        else -> "停泊" to palette.secondaryText
    }

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
                tint = palette.accent,
                modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
            )
            Text(
                text = "最后上报位置",
                color = palette.secondaryText,
                fontSize = if (isCompact) 10.sp else 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor.copy(alpha = 0.14f))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = statusLabel,
                color = statusColor,
                fontSize = if (isCompact) 8.sp else 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Column {
        Text(
            text = location.primaryPlaceName,
            color = palette.primaryText,
            fontSize = if (isCompact) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = if (isCompact) 15.sp else 18.sp
        )
        Text(
            text = location.secondaryPlaceDetail,
            color = palette.mutedText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = location.formattedSampledTime?.let { "采样于 $it" } ?: "采样时间未知",
            color = palette.mutedText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp
        )
        if (location.isValid) {
            Text(
                text = "查看地图 >",
                color = palette.accent,
                fontSize = if (isCompact) 9.sp else 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ColumnScope.LastDriveModuleContent(
    lastDrive: LastDriveInfo?,
    palette: TeslaCockpitPalette,
    isCompact: Boolean
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
                imageVector = Icons.Default.Route,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
            )
            Text(
                text = "最后行驶里程",
                color = palette.secondaryText,
                fontSize = if (isCompact) 10.sp else 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        val tagText = when {
            lastDrive == null -> "暂无"
            lastDrive.isOngoing -> "行驶中"
            else -> lastDrive.formattedTime ?: "最近"
        }
        Text(
            text = tagText,
            color = if (lastDrive?.isOngoing == true) palette.positive else palette.mutedText,
            fontSize = if (isCompact) 8.sp else 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = lastDrive?.formattedDistanceKm ?: "—",
            color = if (lastDrive != null) palette.accent else palette.mutedText,
            fontSize = if (isCompact) 18.sp else 23.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "km",
            color = palette.secondaryText,
            fontSize = if (isCompact) 9.5.sp else 11.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val dur = lastDrive?.formattedDuration ?: "—"
        val whKm = lastDrive?.estimatedWhPerKm?.takeIf { it > 0 }?.let { "${it.toInt()} Wh/km" } ?: "— Wh/km"
        Text(
            text = "耗时 $dur",
            color = palette.mutedText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp
        )
        Text(
            text = whKm,
            color = palette.secondaryText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ColumnScope.TodayDriveModuleContent(
    todayDist: Double?,
    todayTrips: Int?,
    todayWhKm: Double?,
    todayChargeKwh: Double?,
    palette: TeslaCockpitPalette,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "今日行驶",
            color = palette.secondaryText,
            fontSize = if (isCompact) 10.sp else 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${todayTrips ?: 0} 次行程",
            color = palette.mutedText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp
        )
    }

    Text(
        text = if (todayDist != null) String.format(Locale.US, "%.1f km", todayDist) else "0.0 km",
        color = palette.primaryText,
        fontSize = if (isCompact) 16.5.sp else 20.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val whStr = if (todayWhKm != null && todayWhKm > 0) "${todayWhKm.toInt()} Wh/km" else "— Wh/km"
        Text(
            text = "均耗 $whStr",
            color = palette.secondaryText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp,
            fontFamily = FontFamily.Monospace
        )
        if (todayChargeKwh != null && todayChargeKwh > 0.05) {
            Text(
                text = "+${String.format(Locale.US, "%.1f", todayChargeKwh)} kWh",
                color = palette.positive,
                fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ColumnScope.OdometerAndClimateModuleContent(
    odometer: Double?,
    insideTemp: Double?,
    outsideTemp: Double?,
    palette: TeslaCockpitPalette,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "总里程与温控",
            color = palette.secondaryText,
            fontSize = if (isCompact) 10.sp else 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Icons.Default.Thermostat,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
        )
    }

    Text(
        text = if (odometer != null) String.format(Locale.US, "%,.1f km", odometer) else "— km",
        color = palette.primaryText,
        fontSize = if (isCompact) 15.5.sp else 19.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "车内 ${insideTemp?.let { String.format(Locale.US, "%.1f°C", it) } ?: "—"}",
            color = palette.secondaryText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "车外 ${outsideTemp?.let { String.format(Locale.US, "%.1f°C", it) } ?: "—"}",
            color = palette.mutedText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ColumnScope.Recent7DaysTrendModuleContent(
    items: List<DailyDriveItem>,
    palette: TeslaCockpitPalette,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "近 7 天里程趋势",
            color = palette.secondaryText,
            fontSize = if (isCompact) 10.sp else 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        val total7Dist = items.sumOf { it.distanceKm }
        val totalText = if (items.isEmpty()) "—" else String.format(Locale.US, "累计 %.1f km", total7Dist)
        Text(
            text = totalText,
            color = palette.mutedText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp
        )
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "近 7 天暂无行程记录",
                color = palette.mutedText,
                fontSize = 10.sp
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val maxDist = (items.maxOfOrNull { it.distanceKm } ?: 1.0).coerceAtLeast(10.0)
            items.takeLast(7).forEach { item ->
                val ratio = (item.distanceKm / maxDist).toFloat().coerceIn(0.08f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (item.distanceKm > 0) "${item.distanceKm.toInt()}" else "0",
                        color = palette.mutedText,
                        fontSize = if (isCompact) 7.5.sp else 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .width(if (isCompact) 7.dp else 11.dp)
                            .fillMaxHeight(ratio * 0.62f)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(palette.accent)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = item.dateString.takeLast(2),
                        color = palette.secondaryText,
                        fontSize = if (isCompact) 7.5.sp else 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.MonthlySummaryModuleContent(
    monthDistance: Double?,
    monthTrips: Int?,
    monthCharge: Double?,
    palette: TeslaCockpitPalette,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "本月汇总",
            color = palette.secondaryText,
            fontSize = if (isCompact) 10.sp else 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${monthTrips ?: 0} 次行程",
            color = palette.mutedText,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = "行驶里程",
                color = palette.mutedText,
                fontSize = if (isCompact) 8.5.sp else 9.5.sp
            )
            Text(
                text = if (monthDistance != null) String.format(Locale.US, "%.0f km", monthDistance) else "0 km",
                color = palette.primaryText,
                fontSize = if (isCompact) 13.5.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "本月充入",
                color = palette.mutedText,
                fontSize = if (isCompact) 8.5.sp else 9.5.sp
            )
            Text(
                text = if (monthCharge != null) "+${String.format(Locale.US, "%.1f", monthCharge)} kWh" else "0.0 kWh",
                color = palette.positive,
                fontSize = if (isCompact) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }

    val avgTripStr = if (monthTrips != null && monthTrips > 0 && monthDistance != null && monthDistance > 0) {
        String.format(Locale.US, "单程均值 %.1f km/次", monthDistance / monthTrips)
    } else {
        "按配置时区统计当月行程与补能"
    }
    Text(
        text = avgTripStr,
        color = palette.mutedText,
        fontSize = if (isCompact) 7.5.sp else 8.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
