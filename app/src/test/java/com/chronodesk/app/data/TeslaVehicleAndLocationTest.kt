package com.chronodesk.app.data

import android.content.Context
import com.chronodesk.app.data.model.TeslaFullSnapshot
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.model.TeslaVehicle
import com.chronodesk.app.data.model.VehicleLocation
import com.chronodesk.app.data.model.VehicleSnapshot
import com.chronodesk.app.data.model.VehicleState
import com.chronodesk.app.data.network.GrafanaApiClient
import com.chronodesk.app.data.provider.TeslaDataProvider
import com.chronodesk.app.data.security.CredentialStorage
import com.chronodesk.app.data.repository.TeslaRepository
import com.chronodesk.app.data.utils.TeslaPhotoManager
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.io.File

class TeslaVehicleAndLocationTest {

    @Test
    fun testTwoVehiclesIsolation_modelsAndPhotos() {
        val car1 = TeslaVehicle(id = 1, name = "白色标续", model = "3", trimBadging = "RWD")
        val car2 = TeslaVehicle(id = 2, name = "黑色长续", model = "Y", trimBadging = "AWD")

        assertEquals("Model 3", car1.normalizedModel)
        assertEquals("Model Y", car2.normalizedModel)

        val mockContext = mock(Context::class.java)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "chronodesk_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(tempDir)

        try {
            val dsUid = "grafana_pg_ds"
            // 为车辆 1 保存自定义照片
            val stream1 = ByteArrayInputStream("fake_photo_car_1".toByteArray())
            val saved1 = TeslaPhotoManager.savePhoto(mockContext, dsUid, car1.id, stream1)
            assertTrue("车辆 1 照片保存成功", saved1)

            // 验证照片隔离：车辆 1 有照片，车辆 2 无照片，互不串联
            assertTrue("车辆 1 应当存在自定义照片", TeslaPhotoManager.hasCustomPhoto(mockContext, dsUid, car1.id))
            assertFalse("车辆 2 绝对不应串联显示车辆 1 的照片", TeslaPhotoManager.hasCustomPhoto(mockContext, dsUid, car2.id))

            // 删除车辆 1 照片
            TeslaPhotoManager.deletePhoto(mockContext, dsUid, car1.id)
            assertFalse("车辆 1 照片删除后恢复无照片状态", TeslaPhotoManager.hasCustomPhoto(mockContext, dsUid, car1.id))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testMissingCoordinatesAndOutdatedTimestamp_noCrash() {
        // 坐标缺失场景
        val missingCoords = VehicleLocation(
            latitude = null,
            longitude = null,
            geofenceName = null,
            sampledAt = null
        )
        assertFalse("坐标为空时 isValid 必须为 false", missingCoords.isValid)
        assertNull("采样时间为空时不崩溃返回 null", missingCoords.formattedSampledTime)

        // 仅有 0.0 坐标或损坏时间戳
        val zeroCoords = VehicleLocation(
            latitude = 0.0,
            longitude = 0.0,
            geofenceName = "地下车库",
            sampledAt = "invalid-date-string"
        )
        assertFalse("0.0 坐标不可视为合法定位", zeroCoords.isValid)
        assertEquals("无法解析的日期字符串优雅回退原文本而不崩溃", "invalid-date-string", zeroCoords.formattedSampledTime)

        // 正常时间戳与地理围栏
        val validLoc = VehicleLocation(
            latitude = 39.9042,
            longitude = 116.4074,
            geofenceName = "公司超级充电站",
            sampledAt = "2026-11-23T12:30:00Z"
        )
        assertTrue(validLoc.isValid)
        assertEquals("公司超级充电站", validLoc.geofenceName)
        assertNotNull(validLoc.formattedSampledTime)
    }

    @Test
    fun testVehicleAsleepShowsLastParkingLocationWithoutWakeup() {
        val asleepState = VehicleState(
            state = "asleep",
            startDate = "2026-11-23T02:00:00Z"
        )
        assertTrue("休眠判定有效", asleepState.isAsleep)
        assertFalse("休眠中不是行驶状态", asleepState.isDriving)
        assertEquals("休眠", asleepState.displayState)

        // 车辆休眠时，快照保存最后停靠位置，不发起任何唤醒操作
        val parkingSnapshot = TeslaFullSnapshot(
            car = TeslaVehicle(1, "我的 Model Y", "Y"),
            state = asleepState,
            location = VehicleLocation(
                latitude = 31.2304,
                longitude = 121.4737,
                geofenceName = "住宅地下车位",
                sampledAt = "2026-11-23T02:05:00Z"
            )
        )
        assertTrue(parkingSnapshot.location.isValid)
        assertEquals("住宅地下车位", parkingSnapshot.location.geofenceName)
        assertEquals("Model Y", parkingSnapshot.car?.normalizedModel)
    }

    @Test
    fun testSwitchingVehicleOrDatasourceResetsSnapshotImmediately() {
        val mockContext = mock(Context::class.java)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "chronodesk_repo_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(tempDir)
        val mockPrefs = mock(android.content.SharedPreferences::class.java)
        val mockEditor = mock(android.content.SharedPreferences.Editor::class.java)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.clear()).thenReturn(mockEditor)
        `when`(mockContext.getSharedPreferences(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(mockPrefs)

        val mockStorage = mock(CredentialStorage::class.java)
        `when`(mockStorage.getGrafanaToken()).thenReturn("mock_token")

        val repo = TeslaRepository(
            context = mockContext,
            provider = mock(TeslaDataProvider::class.java),
            credentialStorage = mockStorage
        )

        // 切换到新车辆 (carId = 2)
        val newSettings = TeslaSettings(
            enabled = true,
            baseUrl = "https://grafana.example.com",
            dataSourceUid = "ds_1",
            carId = 2,
            carName = "第二辆车"
        )
        repo.updateSettings(newSettings)

        // 验证切换后快照被立即重置为 loading 状态，不会残留第一辆车的数据
        val currentSnap = repo.snapshotFlow.value
        assertNull("切换后车辆对象应重置", currentSnap.car)
        assertTrue("切换后应立即进入加载状态", currentSnap.connectionState.isLoading)
        assertFalse("位置应重置", currentSnap.location.isValid)

        tempDir.deleteRecursively()
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

    private fun createLocationDispatcher(geofenceHandler: () -> MockResponse): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val sql = request.body.readUtf8()
                return when {
                    sql.contains("FROM geofences") -> geofenceHandler()
                    sql.contains("SELECT date, latitude, longitude") -> grafanaFrameJson(
                        """[{"name":"date"},{"name":"latitude"},{"name":"longitude"}]""",
                        """[[1795415440951],[32.0268],[121.06508]]"""
                    )
                    sql.contains("FROM positions") -> grafanaFrameJson(
                        """[{"name":"date"},{"name":"battery_level"},{"name":"rated_battery_range_km"},{"name":"est_battery_range_km"},{"name":"odometer"},{"name":"inside_temp"},{"name":"outside_temp"}]""",
                        """[[1795415440951],[53],[210.0],[195.0],[127580.7],[21.0],[17.0]]"""
                    )
                    sql.contains("FROM states") -> grafanaFrameJson(
                        """[{"name":"state"},{"name":"start_date"}]""",
                        """[["online"],["2026-11-23T08:00:00Z"]]"""
                    )
                    sql.contains("trim_badging") -> MockResponse().setResponseCode(400).setBody("""{"message":"column trim_badging does not exist"}""")
                    sql.contains("FROM cars") -> grafanaFrameJson(
                        """[{"name":"id"},{"name":"name"},{"name":"model"}]""",
                        """[[1],["No.1 Lucky"],["3"]]"""
                    )
                    else -> grafanaFrameJson("[]", "[]")
                }
            }
        }
    }

    @Test
    fun testFetchVehicleSnapshot_locationStillWorksWhenGeofencesFailsAndFormatsNumericDate() = runBlocking {
        val server = MockWebServer()
        server.dispatcher = createLocationDispatcher {
            MockResponse().setResponseCode(400).setBody("""{"message":"permission denied for relation geofences"}""")
        }
        server.start()
        try {
            val provider = TeslaDataProvider(GrafanaApiClient())
            val settings = TeslaSettings(
                enabled = true,
                baseUrl = server.url("/").toString(),
                dataSourceUid = "ds_test",
                carId = 1,
                allowHttp = true
            )
            val snap = provider.fetchVehicleSnapshot(settings, "test_token")
            assertTrue("即使 geofences 返回 400，经纬度位置仍必须有效", snap.location.isValid)
            assertEquals(32.0268, snap.location.latitude ?: 0.0, 0.0001)
            assertEquals(121.06508, snap.location.longitude ?: 0.0, 0.0001)
            assertNull("geofences 失败时围栏名应为 null 而非阻断坐标", snap.location.geofenceName)

            val formattedLocTime = snap.location.formattedSampledTime
            assertNotNull("科学计数法纪元时间应成功格式化", formattedLocTime)
            assertTrue("位置时间应为 HH:mm 格式: $formattedLocTime", formattedLocTime!!.matches(Regex("^\\d{2}:\\d{2}$")))

            val formattedSnapTime = snap.snapshot.formattedSampledTime
            assertNotNull(formattedSnapTime)
            assertTrue("快照时间应为 HH:mm 格式: $formattedSnapTime", formattedSnapTime!!.matches(Regex("^\\d{2}:\\d{2}$")))

            assertEquals("cars 缺 trim_badging 列时降级查询仍应识别车型", "Model 3", snap.car?.normalizedModel)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun testFetchVehicleSnapshot_matchesClosestGeofenceByHaversineRadius() = runBlocking {
        val server = MockWebServer()
        server.dispatcher = createLocationDispatcher {
            grafanaFrameJson(
                """[{"name":"name"},{"name":"latitude"},{"name":"longitude"},{"name":"radius"}]""",
                """[["公司充电站","家 · 地下车库"],[32.1000,32.02685],[121.1000,121.06510],[100.0,80.0]]"""
            )
        }
        server.start()
        try {
            val provider = TeslaDataProvider(GrafanaApiClient())
            val settings = TeslaSettings(
                enabled = true,
                baseUrl = server.url("/").toString(),
                dataSourceUid = "ds_test",
                carId = 1,
                allowHttp = true
            )
            val snap = provider.fetchVehicleSnapshot(settings, "test_token")
            assertTrue(snap.location.isValid)
            assertEquals("Haversine 距离命中半径内围栏", "家 · 地下车库", snap.location.geofenceName)
        } finally {
            server.shutdown()
        }
    }
}
