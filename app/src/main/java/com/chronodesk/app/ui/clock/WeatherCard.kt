package com.chronodesk.app.ui.clock

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.LongbridgeFullSnapshot
import com.chronodesk.app.data.model.LongbridgeSettings
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.ui.longbridge.LongbridgeProviderLogo
import com.chronodesk.app.ui.longbridge.resolveLongbridgePnlColor
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkProgressTrack
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkProgressTrack
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkTextTertiary
import com.chronodesk.app.ui.theme.InkWarningAmber
import com.chronodesk.app.ui.theme.InkWarningRed
import com.chronodesk.app.ui.theme.InkStatusGreen
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary
import com.chronodesk.app.ui.theme.LocalPureInkMode
import com.chronodesk.app.ui.theme.resolveChronoPalette

/**
 * 气象单行内容组件（城市在首位，多行时垂直对齐更美观）
 */
@Composable
fun WeatherRowContent(
    weather: WeatherInfo,
    isNightMode: Boolean,
    isCompact: Boolean
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val dividerColor = theme.mutedText.copy(alpha = 0.55f)
    val iconColor = if (isPureInk) theme.primaryText else TechAmber
    val locationTint = when {
        isPureInk -> theme.primaryText
        isNightMode -> TechCyan
        else -> Color(0xFF16A34A)
    }
    val humidityTint = when {
        isPureInk -> theme.secondaryText
        isNightMode -> TechCyan
        else -> Color(0xFF0284C7)
    }
    val windTint = when {
        isPureInk -> theme.secondaryText
        isNightMode -> TechCyan
        else -> Color(0xFF64748B)
    }

    if (weather.cityName == "选择城市") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = locationTint,
                    modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                )
                Text(
                    text = "选择城市",
                    color = primaryText,
                    fontSize = if (isCompact) 11.sp else 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(text = "｜", color = dividerColor, fontSize = if (isCompact) 9.5.sp else 11.sp)
            Text(
                text = "点击配置天气地点",
                color = secondaryText,
                fontSize = if (isCompact) 10.5.sp else 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
        return
    }

    val (_, iconType) = WeatherInfo.decodeWmoCode(weather.weatherCode)
    val weatherIcon: ImageVector = when (iconType) {
        "sunny" -> Icons.Default.WbSunny
        "mostly_sunny" -> Icons.Default.WbCloudy
        "cloudy", "overcast" -> Icons.Default.Cloud
        "rain", "shower", "drizzle" -> Icons.Default.WaterDrop
        "thunderstorm" -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbCloudy
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 11.dp)
    ) {
        // 城市定位与名称
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = locationTint,
                modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
            )
            Text(
                text = weather.cityName,
                color = primaryText,
                fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = if (isCompact) 108.dp else 160.dp)
            )
        }

        Text(text = "｜", color = dividerColor, fontSize = if (isCompact) 9.5.sp else 11.sp)

        if (!weather.isLoaded) {
            Text(
                text = "气象获取中…",
                color = secondaryText,
                fontSize = if (isCompact) 10.5.sp else 12.sp,
                fontWeight = FontWeight.Normal
            )
        } else {
            // 天气图标 + 温度 + 状况
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 3.dp else 4.dp)
            ) {
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = weather.weatherDescription ?: "天气",
                    tint = iconColor,
                    modifier = Modifier.size(if (isCompact) 14.dp else 18.dp)
                )
                Text(
                    text = if (weather.temperature != null) "${weather.temperature.toInt()}°C" else "—",
                    color = primaryText,
                    fontSize = if (isCompact) 12.5.sp else 14.sp,
                    fontWeight = FontWeight.Bold
                )
                val statusText = buildString {
                    append(weather.weatherDescription ?: "—")
                    if (weather.isStale && weather.updatedAt > 0) {
                        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(weather.updatedAt))
                        append(" ($timeStr 更新)")
                    }
                }
                Text(
                    text = statusText,
                    color = if (weather.isStale && weather.updatedAt > 0 && !isPureInk) TechAmber else secondaryText,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(text = "｜", color = dividerColor, fontSize = if (isCompact) 9.5.sp else 11.sp)

            // 相对湿度
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = humidityTint,
                    modifier = Modifier.size(if (isCompact) 10.dp else 12.dp)
                )
                Text(
                    text = if (weather.humidity != null) "${weather.humidity}%" else "—",
                    color = secondaryText,
                    fontSize = if (isCompact) 10.sp else 11.5.sp
                )
            }

            Text(text = "｜", color = dividerColor, fontSize = if (isCompact) 9.5.sp else 11.sp)

            // 风速
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = windTint,
                    modifier = Modifier.size(if (isCompact) 10.dp else 12.dp)
                )
                Text(
                    text = if (weather.windSpeed != null) "${weather.windSpeed}m/s" else "—",
                    color = secondaryText,
                    fontSize = if (isCompact) 10.sp else 11.5.sp
                )
            }
        }
    }
}

/**
 * 支持多城市多行显示的天气胶囊卡片
 */
@Composable
fun MultiCityWeatherCapsule(
    weatherList: List<WeatherInfo>,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    isCompact: Boolean = false,
    onCardClick: () -> Unit = {}
) {
    if (weatherList.isEmpty()) {
        WeatherCapsule(
            weather = WeatherInfo(cityName = "选择城市", isLoaded = false),
            modifier = modifier,
            isNightMode = isNightMode,
            isCompact = isCompact,
            onCardClick = onCardClick
        )
        return
    }

    if (weatherList.size == 1) {
        WeatherCapsule(
            weather = weatherList[0],
            modifier = modifier,
            isNightMode = isNightMode,
            isCompact = isCompact,
            onCardClick = onCardClick
        )
        return
    }

    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = if (isNightMode) DarkSurface.copy(alpha = 0.9f) else theme.surface
    val cardBorder = theme.border
    val dividerColor = theme.border.copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 13.dp else 16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 13.dp else 16.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 10.dp else 16.dp,
                vertical = if (isCompact) 4.dp else 7.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.5.dp else 5.dp)
        ) {
            weatherList.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = dividerColor,
                        thickness = 0.6.dp
                    )
                }
                WeatherRowContent(
                    weather = item,
                    isNightMode = isNightMode,
                    isCompact = isCompact
                )
            }
        }
    }
}

/**
 * 极简单地区气象胶囊卡片
 */
@Composable
fun WeatherCapsule(
    weather: WeatherInfo,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    isCompact: Boolean = false,
    onCardClick: () -> Unit = {}
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = if (isNightMode) DarkSurface.copy(alpha = 0.9f) else theme.surface
    val cardBorder = theme.border

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 15.dp else 22.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 15.dp else 22.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 9.dp else 18.dp,
                vertical = if (isCompact) 3.5.dp else 7.5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        WeatherRowContent(
            weather = weather,
            isNightMode = isNightMode,
            isCompact = isCompact
        )
    }
}

private const val TESLA_T_SVG_PATH =
    "M12 5.362l2.475-3.026s4.245.09 8.471 2.054c-1.082 1.636-3.231 2.438-3.231 2.438" +
        "-.146-1.439-1.154-1.79-4.354-1.79L12 24 8.619 5.034c-3.18 0-4.188.354-4.335 1.792" +
        " 0 0-2.146-.795-3.229-2.43C5.28 2.431 9.525 2.34 9.525 2.34L12 5.362l-.004.002H12v-.002z" +
        "m0-3.899c3.415-.03 7.326.528 11.328 2.28.535-.968.672-1.395.672-1.395" +
        "C19.625.612 15.528.015 12 0 8.472.015 4.375.61 0 2.349c0 0 .195.525.672 1.396" +
        "C4.674 1.989 8.585 1.435 12 1.46v.003z"

/**
 * Tesla 官方 "T" 标识矢量 Logo
 */
@Composable
fun TeslaOfficialLogo(
    size: Dp = 15.dp,
    tint: Color = Color(0xFFE31937),
    modifier: Modifier = Modifier
) {
    val teslaPath = remember {
        PathParser().parsePathString(TESLA_T_SVG_PATH).toPath()
    }
    Canvas(modifier = modifier.size(size)) {
        withTransform({
            scale(
                scaleX = this@Canvas.size.width / 24f,
                scaleY = this@Canvas.size.height / 24f,
                pivot = Offset.Zero
            )
        }) {
            drawPath(path = teslaPath, color = tint)
        }
    }
}

@Composable
fun WeatherCard(
    weather: WeatherInfo,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    onCardClick: () -> Unit = {}
) {
    WeatherCapsule(
        weather = weather,
        modifier = modifier,
        isNightMode = isNightMode,
        onCardClick = onCardClick
    )
}

/**
 * 首页车辆剩余电量与续航状态胶囊卡片，点击可直达 Tesla 车辆详情页。
 */
@Composable
fun HomeTeslaCapsule(
    vehicleLabel: String,
    batteryLevel: Int?,
    ratedRangeKm: Double?,
    isCharging: Boolean,
    chargerPowerKw: Double?,
    isNightMode: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = if (isNightMode) DarkSurface.copy(alpha = 0.9f) else theme.surface
    val cardBorder = theme.border
    val primaryText = theme.primaryText
    val secondaryText = theme.secondaryText
    val dividerColor = theme.mutedText.copy(alpha = 0.55f)
    val trackColor = theme.progressTrack

    val accentColor = when {
        isPureInk -> theme.primaryText
        batteryLevel == null -> theme.accent
        batteryLevel <= 15 -> theme.danger
        batteryLevel <= 30 -> theme.warning
        else -> theme.positive
    }
    val teslaLogoTint = when {
        isPureInk -> theme.primaryText
        isNightMode -> Color(0xFFEF4444)
        else -> Color(0xFFE31937)
    }
    val chargingTint = if (isPureInk) theme.primaryText else theme.accent

    val shape = RoundedCornerShape(if (isCompact) 15.dp else 22.dp)
    val accessibilityDesc = if (batteryLevel != null) {
        val rangeText = ratedRangeKm?.let { "续航 ${it.toInt()} 公里" } ?: "续航未知"
        "$vehicleLabel 剩余电量 $batteryLevel%，$rangeText，点击查看车辆详情"
    } else {
        "$vehicleLabel 车辆数据待同步，点击查看车辆详情"
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = if (isCompact) 30.dp else 36.dp)
            .clip(shape)
            .background(cardBg)
            .border(1.dp, cardBorder, shape)
            .semantics { contentDescription = accessibilityDesc }
            .clickable(role = Role.Button, onClickLabel = "查看车辆状态") { onClick() }
            .padding(
                horizontal = if (isCompact) 8.dp else 13.dp,
                vertical = if (isCompact) 4.dp else 7.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 3.5.dp else 6.dp)
        ) {
            TeslaOfficialLogo(
                size = if (isCompact) 13.5.dp else 15.dp,
                tint = teslaLogoTint
            )
            Text(
                text = vehicleLabel,
                color = secondaryText,
                fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )

            if (batteryLevel != null) {
                Text(text = "｜", color = dividerColor, fontSize = if (isCompact) 9.5.sp else 11.sp)

                if (!isCompact) {
                    // 迷你水平电池指示条
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(6.5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(trackColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((batteryLevel / 100f).coerceIn(0.05f, 1f))
                                .height(6.5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(accentColor)
                        )
                    }
                }

                if (isCharging) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "充电中",
                        tint = chargingTint,
                        modifier = Modifier.size(if (isCompact) 11.5.dp else 13.5.dp)
                    )
                }

                Text(
                    text = "$batteryLevel%",
                    color = accentColor,
                    fontSize = if (isCompact) 11.5.sp else 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )

                Text(text = "·", color = dividerColor, fontSize = if (isCompact) 10.sp else 12.sp)

                val rangeDisplay = when {
                    ratedRangeKm != null -> if (isCompact) "${ratedRangeKm.toInt()}km" else "${ratedRangeKm.toInt()} km"
                    else -> if (isCompact) "—km" else "— km"
                }
                Text(
                    text = rangeDisplay,
                    color = primaryText,
                    fontSize = if (isCompact) 11.sp else 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )

                if (isCharging && chargerPowerKw != null && chargerPowerKw > 0.0) {
                    Text(
                        text = "${chargerPowerKw.toInt()}kW",
                        color = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        fontSize = if (isCompact) 9.sp else 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Text(text = "｜", color = dividerColor, fontSize = if (isCompact) 9.5.sp else 11.sp)
                Text(
                    text = "待同步",
                    color = secondaryText,
                    fontSize = if (isCompact) 10.sp else 11.5.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * 首页极简长桥当日盈亏胶囊（与 Model 3 同排并列并与天气对齐）：
 * 仅展示官方 Logo + 当日盈亏金额与百分比（正负号与红/绿配色自明），右侧支持单独刷新。
 */
@Composable
fun HomeLongbridgeCapsule(
    settings: LongbridgeSettings,
    snapshot: LongbridgeFullSnapshot,
    isNightMode: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {}
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val theme = resolveChronoPalette(isNightMode, isPureInk)
    val cardBg = if (isNightMode) DarkSurface.copy(alpha = 0.9f) else theme.surface
    val cardBorder = theme.border
    val secondaryText = theme.secondaryText
    val dividerColor = theme.mutedText.copy(alpha = 0.55f)

    val isLoading = snapshot.connectionState.isLoading
    val hasData = snapshot.hasValidAssetData || snapshot.isMock
    val pnlColor = resolveLongbridgePnlColor(
        value = snapshot.todayPl,
        isRedUpGreenDown = settings.isRedUpGreenDown,
        isNightMode = isNightMode,
        isPureInk = isPureInk
    )

    val infiniteTransition = rememberInfiniteTransition(label = "lbCapsuleSpinTransition")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lbCapsuleSpinAngle"
    )

    val shape = RoundedCornerShape(if (isCompact) 15.dp else 22.dp)
    val a11yDesc = if (hasData) {
        "长桥当日盈亏 ${snapshot.formattedCapsuleTodayPl(settings.privacyMaskAmounts, compact = false)} ${snapshot.formattedTodayPlRate()}"
    } else {
        "长桥证券，点击查看或授权"
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = if (isCompact) 30.dp else 36.dp)
            .clip(shape)
            .background(cardBg)
            .border(1.dp, cardBorder, shape)
            .semantics { contentDescription = a11yDesc }
            .clickable(role = Role.Button, onClickLabel = "查看长桥资产详情") { onClick() }
            .padding(
                start = if (isCompact) 8.dp else 12.dp,
                end = if (isCompact) 5.dp else 8.dp,
                top = if (isCompact) 4.dp else 7.dp,
                bottom = if (isCompact) 4.dp else 7.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 3.5.dp else 6.dp)
        ) {
            LongbridgeProviderLogo(
                size = if (isCompact) 14.dp else 16.5.dp,
                isNightMode = isNightMode
            )

            if (hasData) {
                Text(
                    text = snapshot.formattedCapsuleTodayPl(settings.privacyMaskAmounts, compact = isCompact),
                    color = pnlColor,
                    fontSize = if (isCompact) 11.sp else 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "·",
                    color = dividerColor,
                    fontSize = if (isCompact) 9.5.sp else 11.5.sp
                )
                Text(
                    text = snapshot.formattedTodayPlRate(),
                    color = pnlColor,
                    fontSize = if (isCompact) 10.5.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                val statusLabel = when {
                    isLoading -> "同步中…"
                    snapshot.connectionState.isAuthExpired -> "待重连"
                    snapshot.connectionState.isConfigured -> "待同步"
                    else -> "待授权"
                }
                Text(
                    text = statusLabel,
                    color = if (snapshot.connectionState.isAuthExpired) theme.danger else secondaryText,
                    fontSize = if (isCompact) 10.sp else 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                modifier = Modifier
                    .width(0.8.dp)
                    .height(if (isCompact) 11.dp else 13.dp)
                    .background(dividerColor.copy(alpha = 0.45f))
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = !isLoading, onClickLabel = "单独刷新长桥数据") {
                        onRefreshClick()
                    }
                    .padding(if (isCompact) 2.dp else 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "单独刷新股票数据",
                    tint = if (isLoading) theme.accent else secondaryText,
                    modifier = Modifier
                        .size(if (isCompact) 12.dp else 13.5.dp)
                        .rotate(if (isLoading) spinAngle else 0f)
                )
            }
        }
    }
}

/**
 * 将「天气胶囊」与下方「Model 3 / 长桥盈亏」胶囊组合按外边缘等宽对齐排版：
 * 上下外边界严格平齐，同时为下方各胶囊按自然内容宽度分配富余空间，避免等宽硬分导致截断。
 */
@Composable
fun AlignedWeatherAndAssetCapsules(
    isCompact: Boolean,
    weatherContent: @Composable () -> Unit,
    bottomCapsules: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val hGap = if (isCompact) 5.dp else 8.dp
    val vGap = if (isCompact) 4.dp else 7.dp

    Layout(
        content = {
            weatherContent()
            bottomCapsules()
        },
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = hGap.roundToPx()
        val vGapPx = vGap.roundToPx()
        val maxW = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val weatherMeasurable = measurables.first()
        val capMeasurables = measurables.drop(1)

        if (capMeasurables.isEmpty()) {
            val wPlaceable = weatherMeasurable.measure(constraints.copy(minWidth = 0))
            return@Layout layout(wPlaceable.width, wPlaceable.height) {
                wPlaceable.placeRelative(0, 0)
            }
        }

        val weatherNat = weatherMeasurable.maxIntrinsicWidth(constraints.maxHeight)
        val capNaturals = capMeasurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }
        val gapsTotal = hGapPx * (capMeasurables.size - 1)
        val bottomNatTotal = capNaturals.sum() + gapsTotal
        val targetW = maxOf(weatherNat, bottomNatTotal).coerceAtMost(maxW).coerceAtLeast(1)

        val weatherPlaceable = weatherMeasurable.measure(
            Constraints(
                minWidth = targetW,
                maxWidth = targetW,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
        )

        val availableForCaps = (targetW - gapsTotal).coerceAtLeast(capMeasurables.size)
        val capAllocations = if (capMeasurables.size == 1) {
            listOf(targetW)
        } else {
            val sumNat = capNaturals.sum().coerceAtLeast(1)
            val slack = availableForCaps - capNaturals.sum()
            if (slack >= 0) {
                val perExtra = slack / capMeasurables.size
                val rem = slack % capMeasurables.size
                capNaturals.mapIndexed { idx, nat ->
                    nat + perExtra + if (idx < rem) 1 else 0
                }
            } else {
                // 极窄屏按固有宽度比例缩放，确保所有项共同适配左栏总宽
                val list = MutableList(capMeasurables.size) { 0 }
                var used = 0
                for (i in 0 until capMeasurables.size - 1) {
                    val w = (availableForCaps * (capNaturals[i].toFloat() / sumNat.toFloat())).roundToInt()
                    list[i] = w.coerceAtLeast(1)
                    used += list[i]
                }
                list[capMeasurables.lastIndex] = (availableForCaps - used).coerceAtLeast(1)
                list
            }
        }

        val capPlaceables = capMeasurables.mapIndexed { index, m ->
            val w = capAllocations[index]
            m.measure(
                Constraints(
                    minWidth = w,
                    maxWidth = w,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight
                )
            )
        }

        val row2Height = capPlaceables.maxOfOrNull { it.height } ?: 0
        val totalHeight = weatherPlaceable.height + vGapPx + row2Height

        layout(targetW, totalHeight) {
            weatherPlaceable.placeRelative(0, 0)
            var x = 0
            val row2Y = weatherPlaceable.height + vGapPx
            capPlaceables.forEach { p ->
                val yOffset = (row2Height - p.height) / 2
                p.placeRelative(x, row2Y + yOffset)
                x += p.width + hGapPx
            }
        }
    }
}
