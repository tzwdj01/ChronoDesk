package com.chronodesk.app.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.WeatherInfo
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

/**
 * 气象单行内容组件（城市在首位，多行时垂直对齐更美观）
 */
@Composable
fun WeatherRowContent(
    weather: WeatherInfo,
    isNightMode: Boolean,
    isCompact: Boolean
) {
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val dividerColor = if (isNightMode) TextTertiary.copy(alpha = 0.5f) else InkTextTertiary.copy(alpha = 0.6f)
    val iconColor = TechAmber

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
                    tint = if (isNightMode) TechCyan else Color(0xFF16A34A),
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
                tint = if (isNightMode) TechCyan else Color(0xFF16A34A),
                modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
            )
            Text(
                text = weather.cityName,
                color = primaryText,
                fontSize = if (isCompact) 11.sp else 12.5.sp,
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
                    fontSize = if (isCompact) 12.sp else 14.sp,
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
                    color = if (weather.isStale && weather.updatedAt > 0) TechAmber else secondaryText,
                    fontSize = if (isCompact) 10.5.sp else 12.sp,
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
                    tint = if (isNightMode) TechCyan else Color(0xFF0284C7),
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
                    tint = if (isNightMode) TechCyan else Color(0xFF64748B),
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

    val cardBg = if (isNightMode) DarkSurface.copy(alpha = 0.9f) else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder
    val dividerColor = if (isNightMode) DarkBorder.copy(alpha = 0.6f) else InkBorder.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 13.dp else 16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 13.dp else 16.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 10.dp else 16.dp,
                vertical = if (isCompact) 4.dp else 7.dp
            )
    ) {
        Column(
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
    val cardBg = if (isNightMode) DarkSurface.copy(alpha = 0.9f) else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (isCompact) 16.dp else 22.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 16.dp else 22.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 10.dp else 18.dp,
                vertical = if (isCompact) 4.5.dp else 7.5.dp
            )
    ) {
        WeatherRowContent(
            weather = weather,
            isNightMode = isNightMode,
            isCompact = isCompact
        )
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
    val cardBg = if (isNightMode) DarkSurface.copy(alpha = 0.9f) else InkSurface
    val cardBorder = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val dividerColor = if (isNightMode) TextTertiary.copy(alpha = 0.5f) else InkTextTertiary.copy(alpha = 0.6f)
    val trackColor = if (isNightMode) DarkProgressTrack else InkProgressTrack

    val accentColor = when {
        batteryLevel == null -> if (isNightMode) TechCyan else Color(0xFF0284C7)
        batteryLevel <= 15 -> if (isNightMode) TechRose else InkWarningRed
        batteryLevel <= 30 -> if (isNightMode) TechAmber else InkWarningAmber
        else -> if (isNightMode) TechEmerald else InkStatusGreen
    }

    val shape = RoundedCornerShape(if (isCompact) 16.dp else 22.dp)
    val accessibilityDesc = if (batteryLevel != null) {
        val rangeText = ratedRangeKm?.let { "续航 ${it.toInt()} 公里" } ?: "续航未知"
        "$vehicleLabel 剩余电量 $batteryLevel%，$rangeText，点击查看车辆详情"
    } else {
        "$vehicleLabel 车辆数据待同步，点击查看车辆详情"
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 36.dp)
            .clip(shape)
            .background(cardBg)
            .border(1.dp, cardBorder, shape)
            .semantics { contentDescription = accessibilityDesc }
            .clickable(role = Role.Button, onClickLabel = "查看车辆状态") { onClick() }
            .padding(
                horizontal = if (isCompact) 10.dp else 15.dp,
                vertical = if (isCompact) 4.5.dp else 7.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 5.dp else 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = if (isNightMode) TechCyan else Color(0xFF0284C7),
                modifier = Modifier.size(if (isCompact) 13.dp else 15.dp)
            )
            Text(
                text = vehicleLabel,
                color = secondaryText,
                fontSize = if (isCompact) 10.sp else 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )

            if (batteryLevel != null) {
                Text(text = "｜", color = dividerColor, fontSize = if (isCompact) 9.5.sp else 11.sp)

                // 迷你水平电池指示条
                Box(
                    modifier = Modifier
                        .width(if (isCompact) 18.dp else 24.dp)
                        .height(if (isCompact) 6.dp else 7.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(trackColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((batteryLevel / 100f).coerceIn(0.05f, 1f))
                            .height(if (isCompact) 6.dp else 7.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accentColor)
                    )
                }

                if (isCharging) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "充电中",
                        tint = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                    )
                }

                Text(
                    text = "$batteryLevel%",
                    color = accentColor,
                    fontSize = if (isCompact) 11.5.sp else 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )

                Text(text = "·", color = dividerColor, fontSize = if (isCompact) 10.sp else 12.sp)

                val rangeDisplay = when {
                    ratedRangeKm != null -> "${ratedRangeKm.toInt()} km"
                    else -> "— km"
                }
                Text(
                    text = rangeDisplay,
                    color = primaryText,
                    fontSize = if (isCompact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )

                if (isCharging && chargerPowerKw != null && chargerPowerKw > 0.0) {
                    Text(
                        text = "${chargerPowerKw.toInt()}kW",
                        color = if (isNightMode) TechCyan else Color(0xFF0284C7),
                        fontSize = if (isCompact) 9.5.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Text(text = "｜", color = dividerColor, fontSize = if (isCompact) 9.5.sp else 11.sp)
                Text(
                    text = "车辆待同步",
                    color = secondaryText,
                    fontSize = if (isCompact) 10.sp else 11.5.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
