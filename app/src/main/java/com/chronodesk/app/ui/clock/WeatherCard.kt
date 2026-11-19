package com.chronodesk.app.ui.clock

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
    isCompact: Boolean = false,
    onCardClick: () -> Unit = {}
) {
    val cardBg = if (isNightMode) AppleNightSurface else DarkSurface.copy(alpha = 0.9f)
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
            .clip(RoundedCornerShape(if (isCompact) 12.dp else 14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 12.dp else 14.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 12.dp else 16.dp,
                vertical = if (isCompact) 8.dp else 10.dp
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
        ) {
            // 第一行：主要天气信息 + 城市定位 + 空气质量
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：图标 + 温度 + 天气描述
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 10.dp)
                ) {
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = weather.weatherDescription,
                        tint = iconColor,
                        modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${weather.temperature.toInt()}°C",
                            color = primaryText,
                            fontSize = if (isCompact) 16.5.sp else 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(if (isCompact) 5.dp else 7.dp))
                        Text(
                            text = weather.weatherDescription,
                            color = if (isNightMode) AppleNightRedSecondary else TechCyan,
                            fontSize = if (isCompact) 12.sp else 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 右侧：城市定位胶囊 + AQI 优
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 城市胶囊
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
                            modifier = Modifier.size(if (isCompact) 10.dp else 12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = weather.cityName,
                            color = if (isNightMode) AppleNightRedPrimary else TechCyan,
                            fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 空气质量标签
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isNightMode) AppleNightSurfaceElevated else DarkSurfaceElevated)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AQI 32 优",
                            color = if (isNightMode) AppleNightRedSecondary else Color(0xFF10B981),
                            fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 分割线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(
                        if (isNightMode) AppleNightBorder.copy(alpha = 0.6f)
                        else DarkBorder.copy(alpha = 0.8f)
                    )
            )

            // 第二行：精细气象遥测指标（相对湿度、风向风速、体感温度）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 湿度
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = if (isNightMode) AppleNightRedSecondary else TechCyan.copy(alpha = 0.8f),
                        modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "相对湿度 ${weather.humidity}%",
                        color = if (isNightMode) AppleNightRedMuted else TextSecondary,
                        fontSize = if (isCompact) 10.sp else 11.sp
                    )
                }

                // 风速
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = if (isNightMode) AppleNightRedSecondary else TextSecondary,
                        modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "风速 ${weather.windSpeed}m/s",
                        color = if (isNightMode) AppleNightRedMuted else TextSecondary,
                        fontSize = if (isCompact) 10.sp else 11.sp
                    )
                }

                // 体感温度（估算 = 气温 - 风速修正）
                val apparentTemp = (weather.temperature - (weather.windSpeed * 0.15f)).toInt()
                Text(
                    text = "体感 $apparentTemp°C",
                    color = if (isNightMode) AppleNightRedMuted else TextTertiary,
                    fontSize = if (isCompact) 10.sp else 11.sp
                )
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
