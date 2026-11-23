package com.chronodesk.app.data.repository

import android.util.Log
import com.chronodesk.app.data.model.WeatherCity
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.data.network.GeocodingApi
import com.chronodesk.app.data.network.GeocodingItem
import com.chronodesk.app.data.network.NetworkClient
import com.chronodesk.app.data.network.WeatherApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class WeatherRepository(
    private val weatherApi: WeatherApi = NetworkClient.weatherApi,
    private val geocodingApi: GeocodingApi = NetworkClient.geocodingApi
) {

    private val _weatherFlow = MutableStateFlow(WeatherInfo.initial("北京"))
    val weatherFlow: StateFlow<WeatherInfo> = _weatherFlow.asStateFlow()

    private val _weatherListFlow = MutableStateFlow<List<WeatherInfo>>(listOf(WeatherInfo.initial("北京")))
    val weatherListFlow: StateFlow<List<WeatherInfo>> = _weatherListFlow.asStateFlow()

    // 坐标缓存映射：key 为 coordinateKey
    private val weatherCache = mutableMapOf<String, WeatherInfo>()
    private var lastFetchTime: Long = 0
    private val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

    suspend fun searchLocations(query: String): Result<List<GeocodingItem>> {
        val q = query.trim()
        if (q.isBlank()) return Result.success(emptyList())
        return try {
            val response = geocodingApi.searchLocations(name = q)
            if (response.isSuccessful) {
                val results = response.body()?.results ?: emptyList()
                Result.success(results)
            } else {
                Result.failure(IOException("Geocoding HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshWeather(cityName: String, lat: Double, lng: Double, force: Boolean = false) {
        refreshAllWeather(listOf(WeatherCity(cityName, lat, lng)), force)
    }

    suspend fun refreshAllWeather(cities: List<WeatherCity>, force: Boolean = false) {
        val now = System.currentTimeMillis()

        // 1. 如果没有配置任何城市
        if (cities.isEmpty()) {
            weatherCache.clear()
            _weatherListFlow.value = emptyList()
            _weatherFlow.value = WeatherInfo(
                cityName = "选择城市",
                isLoaded = false,
                updatedAt = 0L
            )
            return
        }

        // 2. 清理已被用户移除的地点的缓存
        val activeKeys = cities.map { it.coordinateKey }.toSet()
        weatherCache.keys.retainAll(activeKeys)

        // 3. 检查缓存是否完全有效可用（所有配置的城市均在缓存且未过期）
        val allCachedFresh = !force && (now - lastFetchTime < CACHE_DURATION_MS) && cities.all { city ->
            val cached = weatherCache[city.coordinateKey]
            cached != null && cached.isLoaded && !cached.isStale
        }
        if (allCachedFresh && _weatherListFlow.value.size == cities.size) {
            val cachedList = cities.mapNotNull { weatherCache[it.coordinateKey]?.copy(cityName = it.name) }
            if (cachedList.size == cities.size) {
                _weatherListFlow.value = cachedList
                _weatherFlow.value = cachedList.first()
                return
            }
        }

        // 4. 逐一拉取天气
        val resultList = mutableListOf<WeatherInfo>()
        for (city in cities) {
            try {
                val response = weatherApi.getCurrentWeather(latitude = city.latitude, longitude = city.longitude)
                if (response.isSuccessful && response.body()?.current != null) {
                    val current = response.body()!!.current!!
                    val (desc, _) = WeatherInfo.decodeWmoCode(current.weatherCode)
                    val info = WeatherInfo(
                        cityName = city.name,
                        latitude = city.latitude,
                        longitude = city.longitude,
                        temperature = current.temperature,
                        apparentTemperature = current.apparentTemperature ?: current.temperature,
                        weatherCode = current.weatherCode,
                        weatherDescription = if (current.weatherCode != null) desc else null,
                        humidity = current.humidity,
                        windSpeed = current.windSpeed,
                        isLoaded = true,
                        isStale = false,
                        updatedAt = now
                    )
                    weatherCache[city.coordinateKey] = info
                    resultList.add(info)
                } else {
                    // 请求失败：若有历史缓存保留温度并标注更新时间，若无则显示为缺失
                    val cached = weatherCache[city.coordinateKey]
                    if (cached != null && cached.isLoaded && cached.temperature != null) {
                        val fallback = cached.copy(cityName = city.name, isStale = true)
                        resultList.add(fallback)
                    } else {
                        val noData = WeatherInfo(
                            cityName = city.name,
                            latitude = city.latitude,
                            longitude = city.longitude,
                            temperature = null,
                            apparentTemperature = null,
                            weatherCode = null,
                            weatherDescription = "获取失败",
                            isLoaded = true,
                            isStale = true,
                            updatedAt = 0L
                        )
                        resultList.add(noData)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh weather for ${city.name}: ${e.message}")
                val cached = weatherCache[city.coordinateKey]
                if (cached != null && cached.isLoaded && cached.temperature != null) {
                    val fallback = cached.copy(cityName = city.name, isStale = true)
                    resultList.add(fallback)
                } else {
                    val noData = WeatherInfo(
                        cityName = city.name,
                        latitude = city.latitude,
                        longitude = city.longitude,
                        temperature = null,
                        apparentTemperature = null,
                        weatherCode = null,
                        weatherDescription = "网络异常",
                        isLoaded = true,
                        isStale = true,
                        updatedAt = 0L
                    )
                    resultList.add(noData)
                }
            }
        }

        _weatherListFlow.value = resultList
        if (resultList.isNotEmpty()) {
            _weatherFlow.value = resultList.first()
        }
        lastFetchTime = now
    }

    companion object {
        private const val TAG = "WeatherRepository"
    }
}
