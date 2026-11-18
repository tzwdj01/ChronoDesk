package com.chronodesk.app.data.model

data class WeatherInfo(
    val cityName: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val weatherCode: Int,
    val weatherDescription: String,
    val humidity: Int,
    val windSpeed: Double,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun default(): WeatherInfo {
            return WeatherInfo(
                cityName = "北京",
                temperature = 18.0,
                apparentTemperature = 17.5,
                weatherCode = 0,
                weatherDescription = "晴",
                humidity = 45,
                windSpeed = 2.4,
                updatedAt = System.currentTimeMillis()
            )
        }

        fun decodeWmoCode(code: Int): Pair<String, String> {
            // 返回 (天气简述, 对应图标分类)
            return when (code) {
                0 -> "晴朗" to "sunny"
                1 -> "大致晴朗" to "mostly_sunny"
                2 -> "多云" to "cloudy"
                3 -> "阴天" to "overcast"
                45, 48 -> "雾" to "fog"
                51, 53, 55 -> "毛毛雨" to "drizzle"
                61, 63, 65 -> "小雨" to "rain"
                66, 67 -> "冻雨" to "sleet"
                71, 73, 75 -> "雪" to "snow"
                77 -> "雪粒" to "snow"
                80, 81, 82 -> "阵雨" to "shower"
                85, 86 -> "阵雪" to "snow"
                95, 96, 99 -> "雷暴" to "thunderstorm"
                else -> "多云" to "cloudy"
            }
        }
    }
}
