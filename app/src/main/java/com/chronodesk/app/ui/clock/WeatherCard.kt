package com.chronodesk.app.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkTextTertiary
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

/**
 * 极简气象胶囊卡片（与参考图完全一致的一体式横条胶囊）
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
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val dividerColor = if (isNightMode) TextTertiary.copy(alpha = 0.5f) else InkTextTertiary.copy(alpha = 0.6f)
    val iconColor = TechAmber

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
            .clip(RoundedCornerShape(if (isCompact) 18.dp else 22.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 18.dp else 22.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 12.dp else 18.dp,
                vertical = if (isCompact) 6.dp else 7.5.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
        ) {
            // 天气图标 + 温度 + 状况
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = weather.weatherDescription,
                    tint = iconColor,
                    modifier = Modifier.size(if (isCompact) 17.dp else 20.dp)
                )
                Text(
                    text = "${weather.temperature.toInt()}°C",
                    color = primaryText,
                    fontSize = if (isCompact) 13.5.sp else 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = weather.weatherDescription,
                    color = secondaryText,
                    fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 分隔符
            Text(text = "｜", color = dividerColor, fontSize = 11.sp)

            // 城市定位
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isNightMode) TechCyan else Color(0xFF16A34A),
                    modifier = Modifier.size(if (isCompact) 13.dp else 14.dp)
                )
                Text(
                    text = weather.cityName,
                    color = primaryText,
                    fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 分隔符
            Text(text = "｜", color = dividerColor, fontSize = 11.sp)

            // 相对湿度
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = if (isNightMode) TechCyan else Color(0xFF0284C7),
                    modifier = Modifier.size(if (isCompact) 12.dp else 13.dp)
                )
                Text(
                    text = "湿度 ${weather.humidity}%",
                    color = secondaryText,
                    fontSize = if (isCompact) 11.sp else 12.sp
                )
            }

            // 分隔符
            Text(text = "｜", color = dividerColor, fontSize = 11.sp)

            // 风速
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = if (isNightMode) TechCyan else Color(0xFF64748B),
                    modifier = Modifier.size(if (isCompact) 12.dp else 13.dp)
                )
                Text(
                    text = "风速 ${weather.windSpeed} m/s",
                    color = secondaryText,
                    fontSize = if (isCompact) 11.sp else 12.sp
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
