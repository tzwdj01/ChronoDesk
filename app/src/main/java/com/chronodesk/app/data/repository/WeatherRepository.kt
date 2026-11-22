package com.chronodesk.app.data.repository

import android.util.Log
import com.chronodesk.app.data.model.WeatherCity
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.data.network.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherRepository {

    private val _weatherFlow = MutableStateFlow(WeatherInfo.initial("北京"))
    val weatherFlow: StateFlow<WeatherInfo> = _weatherFlow.asStateFlow()

    private val _weatherListFlow = MutableStateFlow<List<WeatherInfo>>(listOf(WeatherInfo.initial("北京")))
    val weatherListFlow: StateFlow<List<WeatherInfo>> = _weatherListFlow.asStateFlow()

    private var lastFetchTime: Long = 0
    private val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

    suspend fun refreshWeather(cityName: String, lat: Double, lng: Double, force: Boolean = false) {
        refreshAllWeather(listOf(WeatherCity(cityName, lat, lng)), force)
    }

    suspend fun refreshAllWeather(cities: List<WeatherCity>, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastFetchTime < CACHE_DURATION_MS) && _weatherListFlow.value.size == cities.size) {
            return
        }

        val resultList = mutableListOf<WeatherInfo>()
        for (city in cities) {
            try {
                val response = NetworkClient.weatherApi.getCurrentWeather(latitude = city.latitude, longitude = city.longitude)
                if (response.isSuccessful && response.body()?.current != null) {
                    val current = response.body()!!.current!!
                    val (desc, _) = WeatherInfo.decodeWmoCode(current.weatherCode)
                    val info = WeatherInfo(
                        cityName = city.name,
                        temperature = current.temperature,
                        apparentTemperature = current.apparentTemperature ?: current.temperature,
                        weatherCode = current.weatherCode,
                        weatherDescription = if (current.weatherCode != null) desc else null,
                        humidity = current.humidity,
                        windSpeed = current.windSpeed,
                        isLoaded = true,
                        updatedAt = now
                    )
                    resultList.add(info)
                } else {
                    val fallback = _weatherListFlow.value.firstOrNull { it.cityName == city.name && it.isLoaded }
                        ?: WeatherInfo.initial(city.name)
                    resultList.add(fallback)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh weather for ${city.name}: ${e.message}")
                val fallback = _weatherListFlow.value.firstOrNull { it.cityName == city.name && it.isLoaded }
                    ?: WeatherInfo.initial(city.name)
                resultList.add(fallback)
            }
        }

        if (resultList.isNotEmpty()) {
            _weatherListFlow.value = resultList
            _weatherFlow.value = resultList.first()
            lastFetchTime = now
        }
    }

    companion object {
        private const val TAG = "WeatherRepository"
    }
}
