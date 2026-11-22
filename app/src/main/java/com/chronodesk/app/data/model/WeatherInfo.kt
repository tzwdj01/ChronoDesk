package com.chronodesk.app.data.model

data class WeatherInfo(
    val cityName: String,
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val weatherCode: Int? = null,
    val weatherDescription: String? = null,
    val humidity: Int? = null,
    val windSpeed: Double? = null,
    val isLoaded: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun initial(cityName: String = "北京"): WeatherInfo {
            return WeatherInfo(
                cityName = cityName,
                temperature = null,
                apparentTemperature = null,
                weatherCode = null,
                weatherDescription = null,
                humidity = null,
                windSpeed = null,
                isLoaded = false,
                updatedAt = 0L
            )
        }

        fun default(): WeatherInfo = initial("北京")

        fun decodeWmoCode(code: Int?): Pair<String, String> {
            if (code == null) return "—" to "cloudy"
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
