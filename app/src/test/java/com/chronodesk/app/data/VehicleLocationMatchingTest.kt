package com.chronodesk.app.data

import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.model.VehicleLocation
import com.chronodesk.app.data.network.GrafanaApiClient
import com.chronodesk.app.data.provider.TeslaDataProvider
import com.chronodesk.app.data.provider.VehicleLocationResolver
import com.chronodesk.app.data.utils.CoordinateConverter
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class VehicleLocationMatchingTest {

    @Test
    fun testCoordinateConverter_chinaOffsetsAndOverseasUnchanged() {
        // 南通/上海附近坐标：国内必须识别为大陆且 GCJ-02/BD-09 有真实纠偏偏移
        val lat = 31.9367
        val lng = 120.9431
        assertTrue(CoordinateConverter.isInChinaMainland(lat, lng))

        val (gcjLat, gcjLng) = CoordinateConverter.wgs84ToGcj02(lat, lng)
        assertTrue("GCJ-02 纬度应产生明显纠偏", abs(gcjLat - lat) in 0.0005..0.01)
        assertTrue("GCJ-02 经度应产生明显纠偏", abs(gcjLng - lng) in 0.0005..0.01)

        val (bdLat, bdLng) = CoordinateConverter.wgs84ToBd09(lat, lng)
        assertTrue("BD-09 相对 GCJ-02 应继续偏移", abs(bdLat - gcjLat) > 0.001 && abs(bdLng - gcjLng) > 0.001)

        // 海外坐标（旧金山、东京）应原样返回，禁止偏移
        listOf(37.7749 to -122.4194, 35.6762 to 139.6503).forEach { (oLat, oLng) ->
            assertFalse("海外坐标不应判为国内", CoordinateConverter.isInChinaMainland(oLat, oLng))
            val (gLat, gLng) = CoordinateConverter.wgs84ToGcj02(oLat, oLng)
            val (bLat, bLng) = CoordinateConverter.wgs84ToBd09(oLat, oLng)
            assertEquals(oLat, gLat, 0.0)
            assertEquals(oLng, gLng, 0.0)
            assertEquals(oLat, bLat, 0.0)
            assertEquals(oLng, bLng, 0.0)
        }
    }

    @Test
    fun testCoordinateConverter_slippyTileAndMapLaunchUris() {
        val tile = CoordinateConverter.latLngToSlippyTile(31.9367, 120.9431, 15)
        val maxTiles = 1 shl 15
        assertTrue(tile.tileX in 0 until maxTiles)
        assertTrue(tile.tileY in 0 until maxTiles)
        assertTrue(tile.offsetX in 0f..1f)
        assertTrue(tile.offsetY in 0f..1f)

        val cnUris = CoordinateConverter.buildMapLaunchUris(31.9367, 120.9431, "测试围栏")
        assertTrue(cnUris.amapAppUri.startsWith("androidamap://viewMap"))
        assertTrue(cnUris.baiduAppUri.startsWith("baidumap://map/marker"))
        assertTrue(cnUris.geoUri.startsWith("geo:"))
        assertTrue(cnUris.webFallbackUri.startsWith("https://uri.amap.com/marker"))

        val usUris = CoordinateConverter.buildMapLaunchUris(37.7749, -122.4194, "SFO")
        assertTrue(usUris.geoUri.startsWith("geo:37.774900,-122.419400"))
        assertTrue(usUris.webFallbackUri.startsWith("https://www.openstreetmap.org/"))
    }

    @Test
    fun testVehicleLocation_displayPriority() {
        val withBoth = VehicleLocation(
            latitude = 31.9367,
            longitude = 120.9431,
            geofenceName = "公司地库",
            resolvedAddress = "南通市 · 崇川区 · 工农路"
        )
        assertEquals("公司地库", withBoth.primaryPlaceName)
        assertEquals("南通市 · 崇川区 · 工农路", withBoth.secondaryPlaceDetail)

        val addressOnly = VehicleLocation(
            latitude = 31.9367,
            longitude = 120.9431,
            geofenceName = null,
            resolvedAddress = "南通市 · 崇川区附近"
        )
        assertEquals("南通市 · 崇川区附近", addressOnly.primaryPlaceName)
        assertEquals("31.9367, 120.9431", addressOnly.secondaryPlaceDetail)

        val coordsOnly = VehicleLocation(latitude = 31.9367, longitude = 120.9431)
        assertEquals("已记录停车点", coordsOnly.primaryPlaceName)
        assertEquals("31.9367, 120.9431", coordsOnly.secondaryPlaceDetail)

        val invalid = VehicleLocation()
        assertEquals("位置未记录", invalid.primaryPlaceName)
        assertEquals("—", invalid.secondaryPlaceDetail)
    }

    private fun grafanaFrameJson(fieldsJson: String, valuesJson: String): MockResponse {
        val body = """
            {
              "results": {
                "A": {
                  "frames": [
                    {
                      "schema": { "fields": $fieldsJson },
                      "data": { "values": $valuesJson }
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        return MockResponse().setResponseCode(200).setBody(body)
    }

    private fun createProviderDispatcher(addressesHandler: () -> MockResponse): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val sql = request.body.readUtf8()
                return when {
                    sql.contains("FROM addresses") -> addressesHandler()
                    sql.contains("FROM geofences") -> grafanaFrameJson("[]", "[]")
                    sql.contains("SELECT date, latitude, longitude") -> grafanaFrameJson(
                        """[{"name":"date"},{"name":"latitude"},{"name":"longitude"}]""",
                        """[[1795415440951],[31.9367],[120.9431]]"""
                    )
                    sql.contains("FROM positions") -> grafanaFrameJson(
                        """[{"name":"date"},{"name":"battery_level"}]""",
                        """[[1795415440951],[47]]"""
                    )
                    sql.contains("FROM states") -> grafanaFrameJson(
                        """[{"name":"state"},{"name":"start_date"}]""",
                        """[["online"],["2026-11-23T08:00:00Z"]]"""
                    )
                    else -> grafanaFrameJson("[]", "[]")
                }
            }
        }
    }

    @Test
    fun testTeslaDataProvider_addressesExactNearbyAndErrorFallback() = runBlocking {
        // 1. 500m 内返回精确地名
        val serverExact = MockWebServer()
        serverExact.dispatcher = createProviderDispatcher {
            grafanaFrameJson(
                """[{"name":"name"},{"name":"road"},{"name":"neighbourhood"},{"name":"city"},{"name":"county"},{"name":"state"},{"name":"display_name"},{"name":"latitude"},{"name":"longitude"}]""",
                """[["科技园"],["工农南路"],["新城"],["南通市"],["崇川区"],["江苏省"],["江苏省南通市崇川区工农南路"],[31.9369],[120.9433]]"""
            )
        }
        serverExact.start()
        try {
            val provider = TeslaDataProvider(GrafanaApiClient())
            val settings = TeslaSettings(true, serverExact.url("/").toString(), "ds", carId = 1, allowHttp = true)
            val snap = provider.fetchVehicleSnapshot(settings, "token")
            assertEquals("南通市 · 崇川区 · 工农南路", snap.location.resolvedAddress)
        } finally {
            serverExact.shutdown()
        }

        // 2. 500m~3000m 返回「xx附近」大概地点（~1.1km 偏移）
        val serverNearby = MockWebServer()
        serverNearby.dispatcher = createProviderDispatcher {
            grafanaFrameJson(
                """[{"name":"name"},{"name":"road"},{"name":"neighbourhood"},{"name":"city"},{"name":"county"},{"name":"state"},{"name":"display_name"},{"name":"latitude"},{"name":"longitude"}]""",
                """[["体育公园"],["世纪大道"],["新城区"],["南通市"],["崇川区"],["江苏省"],["江苏省南通市崇川区世纪大道"],[31.9467],[120.9431]]"""
            )
        }
        serverNearby.start()
        try {
            val provider = TeslaDataProvider(GrafanaApiClient())
            val settings = TeslaSettings(true, serverNearby.url("/").toString(), "ds", carId = 1, allowHttp = true)
            val snap = provider.fetchVehicleSnapshot(settings, "token")
            assertEquals("南通市 · 崇川区附近", snap.location.resolvedAddress)
        } finally {
            serverNearby.shutdown()
        }

        // 3. addresses 查询报 400 不影响基础位置与坐标
        val serverErr = MockWebServer()
        serverErr.dispatcher = createProviderDispatcher {
            MockResponse().setResponseCode(400).setBody("""{"message":"relation addresses error"}""")
        }
        serverErr.start()
        try {
            val provider = TeslaDataProvider(GrafanaApiClient())
            val settings = TeslaSettings(true, serverErr.url("/").toString(), "ds", carId = 1, allowHttp = true)
            val snap = provider.fetchVehicleSnapshot(settings, "token")
            assertTrue(snap.location.isValid)
            assertNull(snap.location.resolvedAddress)
        } finally {
            serverErr.shutdown()
        }
    }

    @Test
    fun testVehicleLocationResolver_nominatimFallbackBigDataCloudAndGridCache() = runBlocking {
        // 1. Nominatim 返回中文地名 + 同一网格缓存命中不再发请求
        val nomServer = MockWebServer()
        nomServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"address":{"city":"南通市","district":"崇川区","road":"工农路"}}"""
            )
        )
        nomServer.start()
        val unusedBdcServer = MockWebServer()
        unusedBdcServer.start()
        try {
            val resolver = VehicleLocationResolver(
                context = null,
                nominatimBaseUrl = nomServer.url("/").toString(),
                bigDataCloudBaseUrl = unusedBdcServer.url("/").toString()
            )
            val first = resolver.resolveAddress(31.9367, 120.9431)
            assertEquals("南通市 · 崇川区 · 工农路", first)
            assertEquals(1, nomServer.requestCount)
            assertEquals(0, unusedBdcServer.requestCount)

            // 同一 0.001° 网格内 (%.3f_%.3f 均为 31.937_120.943) 第二次查询命中缓存，请求数不变
            val second = resolver.resolveAddress(31.9369, 120.9433)
            assertEquals("南通市 · 崇川区 · 工农路", second)
            assertEquals("命中网格缓存后不应产生新请求", 1, nomServer.requestCount)
            assertEquals(0, unusedBdcServer.requestCount)
        } finally {
            nomServer.shutdown()
            unusedBdcServer.shutdown()
        }

        // 2. Nominatim 失败自动降级到 BigDataCloud 返回省市区大概地点
        val failingNom = MockWebServer()
        failingNom.enqueue(MockResponse().setResponseCode(503))
        failingNom.start()
        val bdcServer = MockWebServer()
        bdcServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"principalSubdivision":"江苏省","city":"南通市","locality":"崇川区"}"""
            )
        )
        bdcServer.start()
        try {
            val resolver = VehicleLocationResolver(
                context = null,
                nominatimBaseUrl = failingNom.url("/").toString(),
                bigDataCloudBaseUrl = bdcServer.url("/").toString()
            )
            val resolved = resolver.resolveAddress(31.9367, 120.9431, fallbackAddress = "默认兜底")
            assertEquals("江苏省 · 南通市 · 崇川区", resolved)
            assertEquals(1, failingNom.requestCount)
            assertEquals(1, bdcServer.requestCount)
        } finally {
            failingNom.shutdown()
            bdcServer.shutdown()
        }
    }
}
