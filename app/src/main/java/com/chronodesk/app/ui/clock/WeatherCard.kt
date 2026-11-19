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
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.ui.theme.AppleNightBorder
import com.chronodesk.app.ui.theme.AppleNightRedMuted
import com.chronodesk.app.ui.theme.AppleNightRedPrimary
import com.chronodesk.app.ui.theme.AppleNightRedSecondary
import com.chronodesk.app.ui.theme.AppleNightSurface
import com.chronodesk.app.ui.theme.AppleNightSurfaceElevated
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

@Composable
fun WeatherCapsule(
    weather: WeatherInfo,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    onCardClick: () -> Unit = {}
) {
    val cardBg = if (isNightMode) AppleNightSurface else DarkSurface.copy(alpha = 0.85f)
    val cardBorder = if (isNightMode) AppleNightBorder else DarkBorder
    val primaryText = if (isNightMode) AppleNightRedPrimary else TextPrimary
    val secondaryText = if (isNightMode) AppleNightRedSecondary else TextSecondary
    val iconColor = if (isNightMode) AppleNightRedPrimary else TechAmber

    val (_, iconType) = WeatherInfo.decodeWmoCode(weather.weatherCode)
    val weatherIcon: ImageVector = when (iconType) {
        "sunny" -> Icons.Default.WbSunny
        "mostly_sunny" -> Icons.Default.WbCloudy
        "cloudy", "overcast" -> Icons.Default.Cloud
        "rain", "shower", "drizzle" -> Icons.Default.WaterDrop
        "thunderstorm" -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbCloudy
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .clickable { onCardClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 天气图标
            Icon(
                imageVector = weatherIcon,
                contentDescription = weather.weatherDescription,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            // 温度与天气状况
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${weather.temperature.toInt()}°C",
                    color = primaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = weather.weatherDescription,
                    color = secondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 城市标签
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isNightMode) AppleNightSurfaceElevated else DarkSurfaceElevated)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isNightMode) AppleNightRedPrimary else TechCyan.copy(alpha = 0.85f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = weather.cityName,
                    color = if (isNightMode) AppleNightRedPrimary else TechCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 湿度 & 风速辅助指标
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = if (isNightMode) AppleNightRedSecondary else TechCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${weather.humidity}%",
                        color = if (isNightMode) AppleNightRedMuted else TextTertiary,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = if (isNightMode) AppleNightRedSecondary else TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${weather.windSpeed}m/s",
                        color = if (isNightMode) AppleNightRedMuted else TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
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
