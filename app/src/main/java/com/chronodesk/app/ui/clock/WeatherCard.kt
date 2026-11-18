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
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

@Composable
fun WeatherCard(
    weather: WeatherInfo,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    onCardClick: () -> Unit = {}
) {
    val cardBg = if (isNightMode) Color(0xFF0A0A0A) else DarkSurface
    val cardBorder = if (isNightMode) Color(0xFF141414) else DarkBorder
    val primaryText = if (isNightMode) Color(0xFF666666) else TextPrimary
    val secondaryText = if (isNightMode) Color(0xFF444444) else TextSecondary
    val iconColor = if (isNightMode) Color(0xFF555555) else TechCyan

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
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { onCardClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = weatherIcon,
                contentDescription = weather.weatherDescription,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )

            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${weather.temperature.toInt()}°C",
                        color = primaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = weather.weatherDescription,
                        color = secondaryText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = weather.cityName,
                        color = TechCyan.copy(alpha = if (isNightMode) 0.4f else 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "体感 ${weather.apparentTemperature.toInt()}°C",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${weather.humidity}%",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                    if (weather.windSpeed > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Air,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${weather.windSpeed}m/s",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
