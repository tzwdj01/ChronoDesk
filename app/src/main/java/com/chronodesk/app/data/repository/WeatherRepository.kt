package com.chronodesk.app.data.repository

import android.util.Log
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.data.network.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherRepository {

    private val _weatherFlow = MutableStateFlow(WeatherInfo.default())
    val weatherFlow: StateFlow<WeatherInfo> = _weatherFlow.asStateFlow()

    private var lastFetchTime: Long = 0
    private val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

    suspend fun refreshWeather(cityName: String, lat: Double, lng: Double, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastFetchTime < CACHE_DURATION_MS)) {
            return
        }

        try {
            val response = NetworkClient.weatherApi.getCurrentWeather(latitude = lat, longitude = lng)
            if (response.isSuccessful && response.body()?.current != null) {
                val current = response.body()!!.current!!
                val (desc, _) = WeatherInfo.decodeWmoCode(current.weatherCode ?: 0)
                val info = WeatherInfo(
                    cityName = cityName,
                    temperature = current.temperature ?: 20.0,
                    apparentTemperature = current.apparentTemperature ?: current.temperature ?: 20.0,
                    weatherCode = current.weatherCode ?: 0,
                    weatherDescription = desc,
                    humidity = current.humidity ?: 50,
                    windSpeed = current.windSpeed ?: 0.0,
                    updatedAt = now
                )
                _weatherFlow.value = info
                lastFetchTime = now
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh weather: ${e.message}")
            // 网络异常时保留上一次成功数据，仅更新城市名
            _weatherFlow.value = _weatherFlow.value.copy(cityName = cityName)
        }
    }

    companion object {
        private const val TAG = "WeatherRepository"
    }
}
