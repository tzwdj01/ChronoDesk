package com.chronodesk.app.data.utils

import java.net.URLEncoder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class SlippyTilePosition(
    val tileX: Int,
    val tileY: Int,
    val offsetX: Float, // 车辆位置在当前瓦片内部的 X 归一化偏移 [0f, 1f)
    val offsetY: Float  // 车辆位置在当前瓦片内部的 Y 归一化偏移 [0f, 1f)
)

data class MapLaunchUris(
    val amapAppUri: String,
    val baiduAppUri: String,
    val geoUri: String,
    val webFallbackUri: String
)

/**
 * 坐标系纠偏与地图瓦片/URI 构造工具（纯 Kotlin，无 Android 依赖）
 * - TeslaMate 数据库原始定位为 WGS-84 坐标
 * - 中国大陆高德地图 / 国内地理视图需转换为 GCJ-02，百度地图需转换为 BD-09
 * - 海外坐标不做偏移
 */
object CoordinateConverter {

    private const val EARTH_AXIS = 6378245.0
    private const val ECCENTRICITY_SQ = 0.00669342162296594323
    private const val X_PI = PI * 3000.0 / 180.0

    fun isInChinaMainland(lat: Double, lng: Double): Boolean {
        if (!lat.isFinite() || !lng.isFinite()) return false
        return lng in 72.004..137.8347 && lat in 0.8293..55.8271
    }

    fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (!isInChinaMainland(lat, lng)) {
            return lat to lng
        }
        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1.0 - ECCENTRICITY_SQ * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((EARTH_AXIS * (1.0 - ECCENTRICITY_SQ)) / (magic * sqrtMagic) * PI)
        dLng = (dLng * 180.0) / (EARTH_AXIS / sqrtMagic * cos(radLat) * PI)
        return (lat + dLat) to (lng + dLng)
    }

    fun gcj02ToBd09(gcjLat: Double, gcjLng: Double): Pair<Double, Double> {
        val x = gcjLng
        val y = gcjLat
        val z = sqrt(x * x + y * y) + 0.00002 * sin(y * X_PI)
        val theta = atan2(y, x) + 0.000003 * cos(x * X_PI)
        val bdLng = z * cos(theta) + 0.0065
        val bdLat = z * sin(theta) + 0.006
        return bdLat to bdLng
    }

    fun wgs84ToBd09(lat: Double, lng: Double): Pair<Double, Double> {
        if (!isInChinaMainland(lat, lng)) {
            return lat to lng
        }
        val (gcjLat, gcjLng) = wgs84ToGcj02(lat, lng)
        return gcj02ToBd09(gcjLat, gcjLng)
    }

    fun latLngToSlippyTile(lat: Double, lng: Double, zoom: Int): SlippyTilePosition {
        val safeZoom = zoom.coerceIn(0, 20)
        val n = 1 shl safeZoom
        val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
        val normLng = ((lng + 180.0) % 360.0 + 360.0) % 360.0
        val xFloat = normLng / 360.0 * n
        val latRad = Math.toRadians(clampedLat)
        val yFloat = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n

        val tileX = floor(xFloat).toInt().coerceIn(0, n - 1)
        val tileY = floor(yFloat).toInt().coerceIn(0, n - 1)
        val offsetX = (xFloat - tileX).toFloat().coerceIn(0f, 1f)
        val offsetY = (yFloat - tileY).toFloat().coerceIn(0f, 1f)
        return SlippyTilePosition(tileX = tileX, tileY = tileY, offsetX = offsetX, offsetY = offsetY)
    }

    fun buildMapLaunchUris(lat: Double, lng: Double, label: String): MapLaunchUris {
        val safeLabel = label.ifBlank { "车辆位置" }
        val encodedLabel = URLEncoder.encode(safeLabel, "UTF-8").replace("+", "%20")
        val inChina = isInChinaMainland(lat, lng)
        val (gcjLat, gcjLng) = wgs84ToGcj02(lat, lng)
        val (bdLat, bdLng) = wgs84ToBd09(lat, lng)
        val (tLat, tLng) = if (inChina) gcjLat to gcjLng else lat to lng

        val amapAppUri =
            "androidamap://viewMap?sourceApplication=ChronoDesk&poiname=$encodedLabel&lat=${formatCoord(gcjLat)}&lon=${formatCoord(gcjLng)}&dev=0"
        val baiduAppUri =
            "baidumap://map/marker?location=${formatCoord(bdLat)},${formatCoord(bdLng)}&title=$encodedLabel&coord_type=bd09ll&src=com.chronodesk.app"
        val geoUri =
            "geo:${formatCoord(tLat)},${formatCoord(tLng)}?q=${formatCoord(tLat)},${formatCoord(tLng)}($encodedLabel)"
        val webFallbackUri = if (inChina) {
            "https://uri.amap.com/marker?position=${formatCoord(gcjLng)},${formatCoord(gcjLat)}&name=$encodedLabel"
        } else {
            "https://www.openstreetmap.org/?mlat=${formatCoord(lat)}&mlon=${formatCoord(lng)}#map=16/${formatCoord(lat)}/${formatCoord(lng)}"
        }
        return MapLaunchUris(
            amapAppUri = amapAppUri,
            baiduAppUri = baiduAppUri,
            geoUri = geoUri,
            webFallbackUri = webFallbackUri
        )
    }

    private fun formatCoord(value: Double): String = String.format(Locale.US, "%.6f", value)

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(kotlin.math.abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(kotlin.math.abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }
}
