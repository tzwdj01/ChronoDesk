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
import androidx.compose.material3.HorizontalDivider
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
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 11.dp)
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
                modifier = Modifier.size(if (isCompact) 13.dp else 14.dp)
            )
            Text(
                text = weather.cityName,
                color = primaryText,
                fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(text = "｜", color = dividerColor, fontSize = 11.sp)

        // 天气图标 + 温度 + 状况
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = weatherIcon,
                contentDescription = weather.weatherDescription,
                tint = iconColor,
                modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
            )
            Text(
                text = "${weather.temperature.toInt()}°C",
                color = primaryText,
                fontSize = if (isCompact) 12.5.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = weather.weatherDescription,
                color = secondaryText,
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

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
                modifier = Modifier.size(if (isCompact) 11.dp else 12.dp)
            )
            Text(
                text = "${weather.humidity}%",
                color = secondaryText,
                fontSize = if (isCompact) 10.5.sp else 11.5.sp
            )
        }

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
                modifier = Modifier.size(if (isCompact) 11.dp else 12.dp)
            )
            Text(
                text = "${weather.windSpeed}m/s",
                color = secondaryText,
                fontSize = if (isCompact) 10.5.sp else 11.5.sp
            )
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
            weather = WeatherInfo.default(),
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
            .clip(RoundedCornerShape(if (isCompact) 14.dp else 16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 14.dp else 16.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 12.dp else 16.dp,
                vertical = if (isCompact) 5.dp else 7.dp
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 3.5.dp else 5.dp)
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
            .clip(RoundedCornerShape(if (isCompact) 18.dp else 22.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(if (isCompact) 18.dp else 22.dp))
            .clickable { onCardClick() }
            .padding(
                horizontal = if (isCompact) 12.dp else 18.dp,
                vertical = if (isCompact) 6.dp else 7.5.dp
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
