package com.chronodesk.app.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
        @Query("wind_speed_unit") windSpeedUnit: String = "ms"
    ): Response<OpenMeteoResponse>
}

data class OpenMeteoResponse(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("current") val current: OpenMeteoCurrent?
)

data class OpenMeteoCurrent(
    @SerializedName("time") val time: String?,
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("relative_humidity_2m") val humidity: Int?,
    @SerializedName("apparent_temperature") val apparentTemperature: Double?,
    @SerializedName("weather_code") val weatherCode: Int?,
    @SerializedName("wind_speed_10m") val windSpeed: Double?
)

interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchLocations(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json"
    ): Response<GeocodingSearchResponse>
}

data class GeocodingSearchResponse(
    @SerializedName("results") val results: List<GeocodingItem>? = null
)

data class GeocodingItem(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("country") val country: String? = null,
    @SerializedName("country_code") val countryCode: String? = null,
    @SerializedName("admin1") val admin1: String? = null,
    @SerializedName("admin2") val admin2: String? = null,
    @SerializedName("admin3") val admin3: String? = null,
    @SerializedName("admin4") val admin4: String? = null
) {
    /**
     * 展示名称默认使用行政区加地名，避免同名歧义。
     */
    val displayName: String
        get() {
            val admin = admin1 ?: admin2 ?: country
            return if (!admin.isNullOrBlank() && !admin.equals(name, ignoreCase = true) && !admin.equals("${name}市", ignoreCase = true)) {
                "$admin $name"
            } else if (!admin.isNullOrBlank()) {
                admin
            } else {
                name
            }
        }

    val subtitle: String
        get() {
            val parts = listOfNotNull(
                country,
                admin1?.takeIf { it != country },
                admin2?.takeIf { it != admin1 && it != country }
            )
            val loc = if (parts.isNotEmpty()) parts.joinToString(" · ") else ""
            val coords = String.format(java.util.Locale.US, "%.4f, %.4f", latitude, longitude)
            return if (loc.isNotBlank()) "$loc ($coords)" else coords
        }
}
