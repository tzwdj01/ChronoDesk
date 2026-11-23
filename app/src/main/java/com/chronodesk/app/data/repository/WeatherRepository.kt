package com.chronodesk.app.data.repository

import android.util.Log
import com.chronodesk.app.data.model.ChinaCityDatabase
import com.chronodesk.app.data.model.WeatherCity
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.data.network.FallbackWeatherProvider
import com.chronodesk.app.data.network.GeocodingApi
import com.chronodesk.app.data.network.GeocodingItem
import com.chronodesk.app.data.network.MultiSourceWeatherFallback
import com.chronodesk.app.data.network.NetworkClient
import com.chronodesk.app.data.network.NominatimGeocodingFallback
import com.chronodesk.app.data.network.NoOpFallbackWeatherProvider
import com.chronodesk.app.data.network.NoOpOnlineGeocodingFallback
import com.chronodesk.app.data.network.OnlineGeocodingFallback
import com.chronodesk.app.data.network.WeatherApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import kotlin.math.abs

class WeatherRepository(
    private val weatherApi: WeatherApi = NetworkClient.weatherApi,
    private val geocodingApi: GeocodingApi = NetworkClient.geocodingApi,
    private val fallbackWeatherProvider: FallbackWeatherProvider =
        if (weatherApi === NetworkClient.weatherApi) MultiSourceWeatherFallback() else NoOpFallbackWeatherProvider,
    private val onlineGeocodingFallback: OnlineGeocodingFallback =
        if (geocodingApi === NetworkClient.geocodingApi) NominatimGeocodingFallback() else NoOpOnlineGeocodingFallback
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

        val offlineResults = ChinaCityDatabase.search(q, limit = 12)
        val merged = mutableListOf<GeocodingItem>()
        merged.addAll(offlineResults)

        var lastOnlineError: Exception? = null

        if (offlineResults.size < 8) {
            try {
                val response = geocodingApi.searchLocations(name = q)
                if (response.isSuccessful) {
                    val online = response.body()?.results ?: emptyList()
                    mergeUniqueLocations(merged, online)
                } else {
                    lastOnlineError = IOException("Geocoding HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                lastOnlineError = e
            }

            if (merged.size < 8) {
                try {
                    val fallbackList = onlineGeocodingFallback.searchLocations(q)
                    mergeUniqueLocations(merged, fallbackList)
                } catch (e: Exception) {
                    if (lastOnlineError == null) lastOnlineError = e
                }
            }
        }

        return if (merged.isNotEmpty() || lastOnlineError == null) {
            Result.success(merged.take(15))
        } else {
            Result.failure(lastOnlineError)
        }
    }

    private fun mergeUniqueLocations(
        target: MutableList<GeocodingItem>,
        candidates: List<GeocodingItem>
    ) {
        for (item in candidates) {
            if (!item.latitude.isFinite() || !item.longitude.isFinite()) continue
            val isDuplicate = target.any { existing ->
                abs(existing.latitude - item.latitude) <= 0.03 &&
                    abs(existing.longitude - item.longitude) <= 0.03
            }
            if (!isDuplicate) {
                target.add(item)
            }
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

        // 4. 逐一拉取天气（主源 Open-Meteo -> 备用多源 MET Norway / wttr.in -> 历史缓存/缺失降级）
        val resultList = mutableListOf<WeatherInfo>()
        for (city in cities) {
            var primaryErrorLabel = "获取失败"
            val primaryInfo = try {
                val response = weatherApi.getCurrentWeather(latitude = city.latitude, longitude = city.longitude)
                val current = if (response.isSuccessful) response.body()?.current else null
                if (current != null) {
                    val (desc, _) = WeatherInfo.decodeWmoCode(current.weatherCode)
                    WeatherInfo(
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
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh weather from primary for ${city.name}: ${e.message}")
                primaryErrorLabel = "网络异常"
                null
            }

            val resolvedInfo = primaryInfo ?: try {
                fallbackWeatherProvider.fetchWeather(city, now)
            } catch (e: Exception) {
                Log.w(TAG, "Fallback weather failed for ${city.name}: ${e.message}")
                null
            }

            if (resolvedInfo != null) {
                weatherCache[city.coordinateKey] = resolvedInfo
                resultList.add(resolvedInfo)
            } else {
                resultList.add(buildDegradedWeather(city, primaryErrorLabel))
            }
        }

        _weatherListFlow.value = resultList
        if (resultList.isNotEmpty()) {
            _weatherFlow.value = resultList.first()
        }
        lastFetchTime = now
    }

    private fun buildDegradedWeather(city: WeatherCity, errorLabel: String): WeatherInfo {
        val cached = weatherCache[city.coordinateKey]
        return if (cached != null && cached.isLoaded && cached.temperature != null) {
            cached.copy(cityName = city.name, isStale = true)
        } else {
            WeatherInfo(
                cityName = city.name,
                latitude = city.latitude,
                longitude = city.longitude,
                temperature = null,
                apparentTemperature = null,
                weatherCode = null,
                weatherDescription = errorLabel,
                isLoaded = true,
                isStale = true,
                updatedAt = 0L
            )
        }
    }

    companion object {
        private const val TAG = "WeatherRepository"
    }
}
