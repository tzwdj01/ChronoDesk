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
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
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
