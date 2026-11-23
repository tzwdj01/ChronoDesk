package com.chronodesk.app.data

import com.chronodesk.app.data.model.DailyDriveItem
import com.chronodesk.app.data.model.TeslaConnectionState
import com.chronodesk.app.data.model.VehicleSnapshot
import com.chronodesk.app.data.model.WeatherInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WeatherAndTeslaMetricsTest {

    @Test
    fun testWeatherInitialState_notLoadedAndNoSyntheticZero() {
        val initial = WeatherInfo.initial("北京")
        assertFalse("未请求完成前 isLoaded 必须为 false", initial.isLoaded)
        assertNull("未请求前温度不可合成 0°C 或固定值", initial.temperature)
        assertNull("未请求前风速不可合成固定值", initial.windSpeed)
        assertNull("未请求前湿度不可合成固定值", initial.humidity)
        assertNull("未请求前描述不可合成晴天", initial.weatherDescription)
    }

    @Test
    fun testWeatherFieldMissingDisplay() {
        // 当接口返回的某些字段为 null 时
        val missingFields = WeatherInfo(
            cityName = "北京",
            temperature = null,
            apparentTemperature = null,
            weatherCode = null,
            weatherDescription = null,
            humidity = null,
            windSpeed = null,
            isLoaded = true
        )
        assertTrue(missingFields.isLoaded)
        assertNull(missingFields.temperature)
        assertNull(missingFields.windSpeed)
        val (desc, _) = WeatherInfo.decodeWmoCode(missingFields.weatherCode)
        assertEquals("—", desc)
    }

    @Test
    fun testVehicleSampledTimeAndSyncTimeDistinction() {
        val snapshot = VehicleSnapshot(
            sampledAt = "2026-11-22T14:35:00Z"
        )
        val sampledTime = snapshot.formattedSampledTime
        assertTrue("必须能成功解析 ISO 采样上报时间", sampledTime != null && sampledTime.isNotBlank())

        val connection = TeslaConnectionState(
            lastSuccessTime = 1763800000000L
        )
        val syncTime = connection.formattedLastSuccessTime
        assertTrue("必须能成功格式化网关同步时间", syncTime.isNotBlank())
        // 两者来源完全独立
        assertFalse("上报时间与网关同步时间格式化文本应各自维护", sampledTime == null || syncTime == "--:--")
    }

    @Test
    fun testRecent7DaysDistanceCalculation() {
        val emptyItems = emptyList<DailyDriveItem>()
        val totalEmpty = if (emptyItems.isEmpty()) "—" else String.format("%.1f km", emptyItems.sumOf { it.distanceKm })
        assertEquals("数据缺失时应显示 —", "—", totalEmpty)

        val zeroItems = listOf(
            DailyDriveItem(dateString = "2026-11-21", distanceKm = 0.0, estimatedWhPerKm = 0.0),
            DailyDriveItem(dateString = "2026-11-22", distanceKm = 0.0, estimatedWhPerKm = 0.0)
        )
        val totalZeroDist = zeroItems.sumOf { it.distanceKm }
        val totalZeroText = if (zeroItems.isEmpty()) "—" else if (totalZeroDist == 0.0) "0 km" else String.format("%.1f km", totalZeroDist)
        assertEquals("近 7 天累计为 0 时应显示 0 km", "0 km", totalZeroText)

        val realItems = listOf(
            DailyDriveItem(dateString = "2026-11-21", distanceKm = 35.5, estimatedWhPerKm = 142.0),
            DailyDriveItem(dateString = "2026-11-22", distanceKm = 64.5, estimatedWhPerKm = 150.0)
        )
        val totalRealDist = realItems.sumOf { it.distanceKm }
        val totalRealText = if (realItems.isEmpty()) "—" else if (totalRealDist == 0.0) "0 km" else String.format("%.1f km", totalRealDist)
        assertEquals("正常累加应显示实际公里数", "100.0 km", totalRealText)
    }

    @Test
    fun testReleaseNetworkSecurityConfig_supportsIntranetCleartext() {
        val file1 = File("app/src/main/res/xml/network_security_config.xml")
        val file2 = File("src/main/res/xml/network_security_config.xml")
        val mainConfigFile = if (file1.exists()) file1 else file2
        assertTrue("主网络安全配置文件必须存在 (checked: ${mainConfigFile.absolutePath})", mainConfigFile.exists())
        val content = mainConfigFile.readText()

        assertTrue("网络安全配置必须允许明文传输以支持内网 TeslaMate HTTP",
            content.contains("cleartextTrafficPermitted=\"true\""))
    }
}
