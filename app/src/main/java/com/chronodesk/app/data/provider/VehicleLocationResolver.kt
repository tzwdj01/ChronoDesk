package com.chronodesk.app.data.provider

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * 车辆定位逆地理编码解析器（按 0.001° ~110m 网格缓存，支持多源级联兜底，全部外部请求仅走 HTTPS）
 * 1. 内存网格缓存（命中即零网络开销）
 * 2. Android 系统 Geocoder（国内手机内置高德/系统定位服务）
 * 3. OpenStreetMap Nominatim HTTPS
 * 4. BigDataCloud HTTPS（国内 CDN 免代理直连兜底省市区大概地点）
 */
class VehicleLocationResolver @JvmOverloads constructor(
    private val context: Context? = null,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build(),
    private val nominatimBaseUrl: String = "https://nominatim.openstreetmap.org/",
    private val bigDataCloudBaseUrl: String = "https://api.bigdatacloud.net/"
) {

    private val tag = "VehicleLocationResolver"
    private val gridCache = ConcurrentHashMap<String, String>()

    fun gridKey(lat: Double, lng: Double): String =
        String.format(Locale.US, "%.3f_%.3f", lat, lng)

    fun getCachedAddress(lat: Double, lng: Double): String? {
        if (!lat.isFinite() || !lng.isFinite()) return null
        return gridCache[gridKey(lat, lng)]
    }

    suspend fun resolveAddress(
        lat: Double,
        lng: Double,
        fallbackAddress: String? = null
    ): String? {
        if (!lat.isFinite() || !lng.isFinite()) return fallbackAddress
        val key = gridKey(lat, lng)
        gridCache[key]?.let { return it }

        val resolved = withContext(Dispatchers.IO) {
            tryAndroidGeocoder(lat, lng)
                ?: tryNominatim(lat, lng)
                ?: tryBigDataCloud(lat, lng)
        }

        if (!resolved.isNullOrBlank()) {
            if (!resolved.endsWith("附近")) {
                gridCache[key] = resolved
            }
            return resolved
        }

        return fallbackAddress?.takeIf { it.isNotBlank() }
    }

    private suspend fun tryAndroidGeocoder(lat: Double, lng: Double): String? {
        val ctx = context ?: return null
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(ctx, Locale.CHINESE)
            val addr: Address? = withTimeoutOrNull(4000L) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        try {
                            geocoder.getFromLocation(lat, lng, 1) { addresses ->
                                if (cont.isActive) {
                                    cont.resume(addresses.firstOrNull())
                                }
                            }
                        } catch (_: Throwable) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
                }
            }
            addr?.let { formatAndroidAddress(it) }
        } catch (t: Throwable) {
            Log.d(tag, "Android Geocoder skipped: ${t.message}")
            null
        }
    }

    private fun formatAndroidAddress(addr: Address): String? {
        val city = addr.locality?.takeIf { it.isNotBlank() }
            ?: addr.adminArea?.takeIf { it.isNotBlank() }
        val district = addr.subLocality?.takeIf { it.isNotBlank() }
        val street = addr.thoroughfare?.takeIf { it.isNotBlank() }
            ?: addr.featureName?.takeIf { it.isNotBlank() && !it.all(Char::isDigit) }
        val parts = listOfNotNull(city, district, street)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return if (parts.isNotEmpty()) {
            parts.joinToString(" · ")
        } else {
            addr.getAddressLine(0)?.trim()?.takeIf { it.isNotBlank() }
        }
    }

    private fun tryNominatim(lat: Double, lng: Double): String? {
        return try {
            val base = nominatimBaseUrl.trimEnd('/')
            val url = String.format(
                Locale.US,
                "%s/reverse?format=jsonv2&lat=%.6f&lon=%.6f&zoom=16&accept-language=zh-CN",
                base,
                lat,
                lng
            )
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "ChronoDesk-Android/1.0")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JsonParser.parseString(body).asJsonObject
                val addressObj = root.getAsJsonObject("address")
                val formatted = addressObj?.let { extractNominatimParts(it) }
                if (!formatted.isNullOrBlank()) {
                    formatted
                } else {
                    root.getStringOrNull("display_name")
                        ?.split(",", "，")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.take(2)
                        ?.distinct()
                        ?.joinToString(" · ")
                        ?.takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Nominatim reverse geocode failed: ${e.message}")
            null
        }
    }

    private fun extractNominatimParts(addressObj: JsonObject): String? {
        val city = firstNonBlank(addressObj, "city", "town", "village", "municipality", "state")
        val district = firstNonBlank(addressObj, "county", "district", "city_district", "suburb")
        val street = firstNonBlank(addressObj, "road", "neighbourhood", "amenity", "pedestrian")
        val parts = listOfNotNull(city, district, street)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return if (parts.isNotEmpty()) parts.joinToString(" · ") else null
    }

    private fun tryBigDataCloud(lat: Double, lng: Double): String? {
        return try {
            val base = bigDataCloudBaseUrl.trimEnd('/')
            val url = String.format(
                Locale.US,
                "%s/data/reverse-geocode-client?latitude=%.6f&longitude=%.6f&localityLanguage=zh",
                base,
                lat,
                lng
            )
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "ChronoDesk-Android/1.0")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JsonParser.parseString(body).asJsonObject
                val subdivision = root.getStringOrNull("principalSubdivision")
                val city = root.getStringOrNull("city")
                val locality = root.getStringOrNull("locality")
                val parts = listOfNotNull(subdivision, city, locality)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                if (parts.isNotEmpty()) parts.joinToString(" · ") else null
            }
        } catch (e: Exception) {
            Log.d(tag, "BigDataCloud reverse geocode failed: ${e.message}")
            null
        }
    }

    private fun firstNonBlank(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val v = obj.getStringOrNull(key)
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        val elem = get(key) ?: return null
        if (elem.isJsonNull || !elem.isJsonPrimitive) return null
        return elem.asString
    }
}
