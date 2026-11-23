package com.chronodesk.app.data.network

import android.util.Log
import com.chronodesk.app.data.model.WeatherCity
import com.chronodesk.app.data.model.WeatherInfo
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 备用实时气象拉取提供方（纯 HTTPS）
 */
fun interface FallbackWeatherProvider {
    suspend fun fetchWeather(city: WeatherCity, nowMs: Long): WeatherInfo?
}

/**
 * 备用在线地理编码提供方（纯 HTTPS）
 */
fun interface OnlineGeocodingFallback {
    suspend fun searchLocations(query: String): List<GeocodingItem>
}

object NoOpFallbackWeatherProvider : FallbackWeatherProvider {
    override suspend fun fetchWeather(city: WeatherCity, nowMs: Long): WeatherInfo? = null
}

object NoOpOnlineGeocodingFallback : OnlineGeocodingFallback {
    override suspend fun searchLocations(query: String): List<GeocodingItem> = emptyList()
}

/**
 * OpenStreetMap Nominatim 备用地理编码搜索（支持中文县市与全球地名）
 */
class NominatimGeocodingFallback(
    private val httpClient: OkHttpClient = NetworkClient.weatherFallbackHttpClient
) : OnlineGeocodingFallback {

    override suspend fun searchLocations(query: String): List<GeocodingItem> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=jsonv2&addressdetails=1&accept-language=zh-CN&limit=8"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "ChronoDesk-Android/1.0")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                parseNominatimJson(body)
            }
        } catch (e: Exception) {
            Log.d("NominatimGeoFallback", "Nominatim search failed: ${e.message}")
            emptyList()
        }
    }

    companion object {
        private fun JsonObject.optString(key: String): String? {
            val el = get(key) ?: return null
            if (el.isJsonNull || !el.isJsonPrimitive) return null
            return try { el.asString } catch (_: Exception) { null }
        }

        private fun JsonObject.optDouble(key: String): Double? {
            val el = get(key) ?: return null
            if (el.isJsonNull || !el.isJsonPrimitive) return null
            return try { el.asDouble } catch (_: Exception) { el.asString.toDoubleOrNull() }
        }

        private fun JsonObject.optLong(key: String): Long? {
            val el = get(key) ?: return null
            if (el.isJsonNull || !el.isJsonPrimitive) return null
            return try { el.asLong } catch (_: Exception) { el.asString.toLongOrNull() }
        }

        private fun JsonObject.optObj(key: String): JsonObject? {
            val el = get(key) ?: return null
            return if (el.isJsonObject) el.asJsonObject else null
        }

        fun parseNominatimJson(json: String): List<GeocodingItem> {
            return try {
                val parsed = JsonParser.parseString(json)
                if (!parsed.isJsonArray) return emptyList()
                val arr = parsed.asJsonArray
                val list = mutableListOf<GeocodingItem>()
                for (el in arr) {
                    if (!el.isJsonObject) continue
                    try {
                        val obj = el.asJsonObject
                        val lat = obj.optString("lat")?.toDoubleOrNull() ?: obj.optDouble("lat") ?: continue
                        val lon = obj.optString("lon")?.toDoubleOrNull() ?: obj.optDouble("lon") ?: continue
                        if (!lat.isFinite() || !lon.isFinite()) continue
                        val rawName = obj.optString("name")?.takeIf { it.isNotBlank() }
                            ?: obj.optString("display_name")?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() }
                            ?: continue
                        val address = obj.optObj("address")
                        val country = address?.optString("country")
                        val countryCode = address?.optString("country_code")?.uppercase(Locale.US)
                        val admin1 = address?.optString("state") ?: address?.optString("province")
                        val admin2 = address?.optString("city")
                            ?: address?.optString("town")
                            ?: address?.optString("county")
                            ?: address?.optString("state_district")
                        val placeId = obj.optLong("place_id") ?: rawName.hashCode().toLong()
                        list.add(
                            GeocodingItem(
                                id = placeId,
                                name = rawName,
                                latitude = lat,
                                longitude = lon,
                                country = country,
                                countryCode = countryCode,
                                admin1 = admin1,
                                admin2 = admin2
                            )
                        )
                    } catch (_: Exception) {
                        // 跳过单条畸形记录，避免整批搜索结果丢失
                    }
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

/**
 * 多源 HTTPS 天气兜底提供方：依次尝试 MET Norway Locationforecast 2.0 与 wttr.in JSON。
 */
class MultiSourceWeatherFallback(
    private val httpClient: OkHttpClient = NetworkClient.weatherFallbackHttpClient
) : FallbackWeatherProvider {

    override suspend fun fetchWeather(city: WeatherCity, nowMs: Long): WeatherInfo? = withContext(Dispatchers.IO) {
        fetchFromMetNorway(city, nowMs) ?: fetchFromWttr(city, nowMs)
    }

    private fun fetchFromMetNorway(city: WeatherCity, nowMs: Long): WeatherInfo? {
        return try {
            val url = String.format(
                Locale.US,
                "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.4f&lon=%.4f",
                city.latitude,
                city.longitude
            )
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "ChronoDesk-Android/1.0 https://github.com/tzwdj01/ChronoDesk")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                parseMetNorwayJson(body, city, nowMs)
            }
        } catch (e: Exception) {
            Log.d("WeatherFallback", "MET Norway failed for ${city.name}: ${e.message}")
            null
        }
    }

    private fun fetchFromWttr(city: WeatherCity, nowMs: Long): WeatherInfo? {
        return try {
            val url = String.format(
                Locale.US,
                "https://wttr.in/%.4f,%.4f?format=j1&lang=zh",
                city.latitude,
                city.longitude
            )
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "ChronoDesk-Android/1.0")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                parseWttrJson(body, city, nowMs)
            }
        } catch (e: Exception) {
            Log.d("WeatherFallback", "wttr.in failed for ${city.name}: ${e.message}")
            null
        }
    }

    companion object {
        private fun JsonObject.optString(key: String): String? {
            val el = get(key) ?: return null
            if (el.isJsonNull || !el.isJsonPrimitive) return null
            return try { el.asString } catch (_: Exception) { null }
        }

        private fun JsonObject.optDouble(key: String): Double? {
            val el = get(key) ?: return null
            if (el.isJsonNull || !el.isJsonPrimitive) return null
            return try { el.asDouble } catch (_: Exception) { el.asString.toDoubleOrNull() }
        }

        private fun JsonObject.optObj(key: String): JsonObject? {
            val el = get(key) ?: return null
            return if (el.isJsonObject) el.asJsonObject else null
        }

        private fun JsonObject.optArr(key: String): JsonArray? {
            val el = get(key) ?: return null
            return if (el.isJsonArray) el.asJsonArray else null
        }

        fun parseMetNorwayJson(json: String, city: WeatherCity, nowMs: Long): WeatherInfo? {
            return try {
                val parsed = JsonParser.parseString(json)
                if (!parsed.isJsonObject) return null
                val root = parsed.asJsonObject
                val series = root.optObj("properties")?.optArr("timeseries") ?: return null
                if (series.size() == 0 || !series[0].isJsonObject) return null
                val first = series[0].asJsonObject
                val data = first.optObj("data") ?: return null
                val details = data.optObj("instant")?.optObj("details") ?: return null

                val temp = details.optDouble("air_temperature")?.takeIf { it.isFinite() } ?: return null
                val humidity = details.optDouble("relative_humidity")?.takeIf { it.isFinite() }?.roundToInt()
                val wind = details.optDouble("wind_speed")?.takeIf { it.isFinite() }
                    ?.let { (it * 10.0).roundToInt() / 10.0 }

                val symbol = data.optObj("next_1_hours")
                    ?.optObj("summary")
                    ?.optString("symbol_code")
                    ?: data.optObj("next_6_hours")
                        ?.optObj("summary")
                        ?.optString("symbol_code")
                val wmoCode = mapMetNorwaySymbolToWmo(symbol)
                val (desc, _) = WeatherInfo.decodeWmoCode(wmoCode)

                WeatherInfo(
                    cityName = city.name,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    temperature = temp,
                    apparentTemperature = temp,
                    weatherCode = wmoCode,
                    weatherDescription = if (wmoCode != null) desc else null,
                    humidity = humidity,
                    windSpeed = wind,
                    isLoaded = true,
                    isStale = false,
                    updatedAt = nowMs
                )
            } catch (_: Exception) {
                null
            }
        }

        fun mapMetNorwaySymbolToWmo(symbol: String?): Int? {
            val base = symbol?.lowercase(Locale.US)
                ?.removeSuffix("_day")
                ?.removeSuffix("_night")
                ?.removeSuffix("_polartwilight")
                ?.trim()
                ?: return null
            if (base.isBlank()) return null
            return when {
                base.contains("thunder") -> 95
                base.contains("sleet") -> 66
                base.contains("snow") -> 71
                base.contains("rainshowers") -> 80
                base.contains("heavyrain") -> 65
                base.contains("lightrain") || base.contains("drizzle") -> 61
                base.contains("rain") -> 63
                base.contains("fog") -> 45
                base == "clearsky" -> 0
                base == "fair" -> 1
                base == "partlycloudy" -> 2
                base == "cloudy" -> 3
                else -> 2
            }
        }

        fun parseWttrJson(json: String, city: WeatherCity, nowMs: Long): WeatherInfo? {
            return try {
                val parsed = JsonParser.parseString(json)
                if (!parsed.isJsonObject) return null
                val currentArr = parsed.asJsonObject.optArr("current_condition") ?: return null
                if (currentArr.size() == 0 || !currentArr[0].isJsonObject) return null
                val current = currentArr[0].asJsonObject

                val temp = current.optString("temp_C")?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
                val apparent = current.optString("FeelsLikeC")?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: temp
                val humidity = current.optString("humidity")?.toIntOrNull()
                val windKmph = current.optString("windspeedKmph")?.toDoubleOrNull()?.takeIf { it.isFinite() }
                val windMs = windKmph?.let { ((it / 3.6) * 10.0).roundToInt() / 10.0 }
                val wwoCode = current.optString("weatherCode")?.toIntOrNull()
                val wmoCode = mapWttrWwoToWmo(wwoCode)
                val (desc, _) = WeatherInfo.decodeWmoCode(wmoCode)

                WeatherInfo(
                    cityName = city.name,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    temperature = temp,
                    apparentTemperature = apparent,
                    weatherCode = wmoCode,
                    weatherDescription = if (wmoCode != null) desc else null,
                    humidity = humidity,
                    windSpeed = windMs,
                    isLoaded = true,
                    isStale = false,
                    updatedAt = nowMs
                )
            } catch (_: Exception) {
                null
            }
        }

        fun mapWttrWwoToWmo(wwoCode: Int?): Int? {
            return when (wwoCode) {
                null -> null
                113 -> 0
                116 -> 2
                119, 122 -> 3
                143, 248, 260 -> 45
                176, 263, 266, 293, 296, 353 -> 61
                299, 302 -> 63
                305, 308, 356, 359 -> 65
                179, 182, 185, 311, 314, 317, 320 -> 66
                227, 230, 323, 326, 329, 332, 335, 338, 368, 371 -> 71
                200, 386, 389, 392, 395 -> 95
                else -> 2
            }
        }
    }
}
