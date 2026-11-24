package com.chronodesk.app.data.provider

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.chronodesk.app.data.model.ChinaCityDatabase
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

    companion object {
        fun containsChinese(text: String?): Boolean =
            text?.any { it.code in 0x4E00..0x9FFF } == true

        fun isInGreaterChina(lat: Double, lng: Double): Boolean =
            lat in 17.5..54.0 && lng in 73.0..135.5
    }

    fun gridKey(lat: Double, lng: Double): String =
        String.format(Locale.US, "%.3f_%.3f", lat, lng)

    fun getCachedAddress(lat: Double, lng: Double): String? {
        if (!lat.isFinite() || !lng.isFinite()) return null
        val cached = gridCache[gridKey(lat, lng)] ?: return null
        if (isInGreaterChina(lat, lng) && !containsChinese(cached)) return null
        return cached
    }

    suspend fun resolveAddress(
        lat: Double,
        lng: Double,
        fallbackAddress: String? = null
    ): String? {
        if (!lat.isFinite() || !lng.isFinite()) return fallbackAddress
        val key = gridKey(lat, lng)
        val requireChinese = isInGreaterChina(lat, lng)
        gridCache[key]?.let { cached ->
            if (!requireChinese || containsChinese(cached)) {
                return cached
            }
        }

        val (androidAddr, nomAddr, bdcAddr) = withContext(Dispatchers.IO) {
            val a = tryAndroidGeocoder(lat, lng)
            if (!a.isNullOrBlank() && (!requireChinese || containsChinese(a))) {
                return@withContext Triple(a, null, null)
            }
            val n = tryNominatim(lat, lng)
            if (!n.isNullOrBlank() && (!requireChinese || containsChinese(n))) {
                return@withContext Triple(a, n, null)
            }
            val b = tryBigDataCloud(lat, lng)
            Triple(a, n, b)
        }

        val preferred = listOf(androidAddr, nomAddr, bdcAddr).firstOrNull { candidate ->
            !candidate.isNullOrBlank() && (!requireChinese || containsChinese(candidate))
        }
        if (!preferred.isNullOrBlank()) {
            if (!preferred.endsWith("附近")) {
                gridCache[key] = preferred
            }
            return preferred
        }

        if (requireChinese) {
            fallbackAddress?.takeIf { containsChinese(it) }?.let { return it }
            ChinaCityDatabase.findNearestChineseArea(lat, lng)?.let { return it }
        }

        val anyAvailable = androidAddr ?: nomAddr ?: bdcAddr
        if (!anyAvailable.isNullOrBlank()) {
            if (!requireChinese && !anyAvailable.endsWith("附近")) {
                gridCache[key] = anyAvailable
            }
            return anyAvailable
        }

        return fallbackAddress?.takeIf { it.isNotBlank() }
    }

    private suspend fun tryAndroidGeocoder(lat: Double, lng: Double): String? {
        val ctx = context ?: return null
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(ctx, Locale.SIMPLIFIED_CHINESE)
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
                "%s/reverse?format=jsonv2&lat=%.6f&lon=%.6f&zoom=16&accept-language=zh-CN,zh;q=0.9",
                base,
                lat,
                lng
            )
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "ChronoDesk-Android/1.0")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JsonParser.parseString(body).asJsonObject
                val addressObj = root.getAsJsonObject("address")
                val rawDisplayName = root.getStringOrNull("display_name")
                val formatted = addressObj?.let { extractNominatimParts(it, rawDisplayName) }
                if (!formatted.isNullOrBlank()) {
                    formatted
                } else {
                    rawDisplayName
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

    private fun extractNominatimParts(addressObj: JsonObject, displayName: String? = null): String? {
        val rawCity = firstNonBlank(addressObj, "city", "town", "village", "municipality", "state")
        val cityLooksLikeDistrict = rawCity != null && (rawCity.endsWith("区") || rawCity.endsWith("县"))
        val cityFromDisplay = if (cityLooksLikeDistrict && !displayName.isNullOrBlank()) {
            displayName.split(",", "，")
                .map { it.trim() }
                .firstOrNull { seg ->
                    containsChinese(seg) &&
                        seg.endsWith("市") &&
                        !seg.endsWith("超市") &&
                        !seg.endsWith("夜市")
                }
        } else null

        val city = cityFromDisplay ?: if (!cityLooksLikeDistrict) rawCity else firstNonBlank(addressObj, "state")
        val district = firstNonBlank(addressObj, "county", "district", "city_district")
            ?: if (cityLooksLikeDistrict) rawCity else firstNonBlank(addressObj, "suburb")
        val street = firstNonBlank(addressObj, "road", "neighbourhood", "city_block", "amenity", "pedestrian")
            ?: firstNonBlank(addressObj, "suburb")?.takeIf { it != district }
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
