package com.chronodesk.app.data.model

data class WeatherInfo(
    val cityName: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val weatherCode: Int? = null,
    val weatherDescription: String? = null,
    val humidity: Int? = null,
    val windSpeed: Double? = null,
    val isLoaded: Boolean = true,
    val isStale: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val coordinateKey: String?
        get() = if (latitude != null && longitude != null) {
            String.format(java.util.Locale.US, "%.4f_%.4f", latitude, longitude)
        } else null

    companion object {
        fun initial(cityName: String = "北京", lat: Double? = null, lng: Double? = null): WeatherInfo {
            return WeatherInfo(
                cityName = cityName,
                latitude = lat,
                longitude = lng,
                temperature = null,
                apparentTemperature = null,
                weatherCode = null,
                weatherDescription = null,
                humidity = null,
                windSpeed = null,
                isLoaded = false,
                isStale = false,
                updatedAt = 0L
            )
        }

        fun default(): WeatherInfo = initial("北京", 39.9042, 116.4074)

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
