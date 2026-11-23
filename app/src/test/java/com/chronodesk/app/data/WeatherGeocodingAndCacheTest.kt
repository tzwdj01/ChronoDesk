package com.chronodesk.app.data

import com.chronodesk.app.data.model.ClockSettings
import com.chronodesk.app.data.model.WeatherCity
import com.chronodesk.app.data.network.GeocodingApi
import com.chronodesk.app.data.network.GeocodingItem
import com.chronodesk.app.data.network.GeocodingSearchResponse
import com.chronodesk.app.data.network.OpenMeteoCurrent
import com.chronodesk.app.data.network.OpenMeteoResponse
import com.chronodesk.app.data.network.WeatherApi
import com.chronodesk.app.data.repository.WeatherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import retrofit2.Response
import java.io.IOException

class WeatherGeocodingAndCacheTest {

    @Test
    fun testGeocodingDisplayName_countyAndOverseasCity() {
        val countyItem = GeocodingItem(
            id = 1809104,
            name = "海淀",
            latitude = 39.99064,
            longitude = 116.28868,
            country = "中国",
            admin1 = "北京市"
        )
        // 非省会县区级显示行政区加地名，避免重名歧义
        assertEquals("北京市 海淀", countyItem.displayName)
        assertTrue(countyItem.subtitle.contains("中国"))

        val overseasItem = GeocodingItem(
            id = 2643743,
            name = "倫敦",
            latitude = 51.50853,
            longitude = -0.12574,
            country = "英国",
            admin1 = "英格兰"
        )
        // 海外城市展示行政区加地名
        assertEquals("英格兰 倫敦", overseasItem.displayName)
        assertTrue(overseasItem.subtitle.contains("英国"))
    }

    @Test
    fun testDuplicateCityNameDifferentCoordinates() {
        // 同名不同坐标地点（例如北京朝阳区与长春朝阳区）
        val settings = ClockSettings(
            cityName = "朝阳区",
            latitude = 39.9219,
            longitude = 116.4431,
            additionalCities = "朝阳区:43.8322:125.2882"
        )
        val allCities = settings.getAllCities()
        assertEquals("按坐标去重，同名不同坐标的地点可同时添加", 2, allCities.size)
        assertEquals("朝阳区", allCities[0].name)
        assertEquals(39.9219, allCities[0].latitude, 0.0001)
        assertEquals("朝阳区", allCities[1].name)
        assertEquals(43.8322, allCities[1].latitude, 0.0001)
    }

    @Test
    fun testManualCoordinatesValidationLogic() {
        fun isValidCoordinate(latStr: String, lngStr: String): Pair<Double?, Double?>? {
            val lat = latStr.toDoubleOrNull() ?: return null
            val lng = lngStr.toDoubleOrNull() ?: return null
            if (lat !in -90.0..90.0) return null
            if (lng !in -180.0..180.0) return null
            return lat to lng
        }

        // 合法坐标
        val valid = isValidCoordinate("31.2304", "121.4737")
        assertNotNull(valid)
        assertEquals(31.2304, valid!!.first!!, 0.0001)

        // 越界坐标校验有效拦截
        assertNull("纬度超过90度拦截", isValidCoordinate("95.0", "120.0"))
        assertNull("纬度小于-90度拦截", isValidCoordinate("-95.0", "120.0"))
        assertNull("经度超过180度拦截", isValidCoordinate("30.0", "185.0"))
        assertNull("经度小于-180度拦截", isValidCoordinate("30.0", "-185.0"))
        assertNull("非数字字符串拦截", isValidCoordinate("abc", "120.0"))
    }

    @Test
    fun testNetworkFailureRetainsCacheWithUpdateTime() = runBlocking {
        val mockWeatherApi = mock(WeatherApi::class.java)
        val mockGeocodingApi = mock(GeocodingApi::class.java)
        val repo = WeatherRepository(mockWeatherApi, mockGeocodingApi)

        val city = WeatherCity("海淀", 39.99, 116.28)

        // 1. 首次网络成功，返回 23.5°C
        val successResponse = Response.success(
            OpenMeteoResponse(
                latitude = 39.99,
                longitude = 116.28,
                current = OpenMeteoCurrent(
                    time = "2026-11-23T12:00",
                    temperature = 23.5,
                    humidity = 45,
                    apparentTemperature = 22.0,
                    weatherCode = 0,
                    windSpeed = 3.2
                )
            )
        )
        `when`(mockWeatherApi.getCurrentWeather(39.99, 116.28)).thenReturn(successResponse)

        repo.refreshAllWeather(listOf(city), force = true)
        val weather1 = repo.weatherListFlow.value.first()
        assertEquals(23.5, weather1.temperature!!, 0.01)
        assertFalse(weather1.isStale)
        assertTrue(weather1.updatedAt > 0)
        val originalTimestamp = weather1.updatedAt

        // 2. 第二次网络异常（断网抛出网络异常）
        `when`(mockWeatherApi.getCurrentWeather(39.99, 116.28)).thenThrow(RuntimeException("No network connection"))

        repo.refreshAllWeather(listOf(city), force = true)
        val weather2 = repo.weatherListFlow.value.first()

        // 验证：刷新失败且有历史数据时，显示原温度并标注更新时间
        assertEquals("网络失败时保留原温度", 23.5, weather2.temperature!!, 0.01)
        assertTrue("网络失败标记为缓存数据", weather2.isStale)
        assertEquals("更新时间保留历史成功时间戳", originalTimestamp, weather2.updatedAt)
    }

    @Test
    fun testNetworkFailureWithNoHistoryShowsMissing() = runBlocking {
        val mockWeatherApi = mock(WeatherApi::class.java)
        val mockGeocodingApi = mock(GeocodingApi::class.java)
        val repo = WeatherRepository(mockWeatherApi, mockGeocodingApi)

        val city = WeatherCity("未知地点", 10.0, 20.0)
        `when`(mockWeatherApi.getCurrentWeather(10.0, 20.0)).thenThrow(RuntimeException("Connection timeout"))

        repo.refreshAllWeather(listOf(city), force = true)
        val weather = repo.weatherListFlow.value.first()

        // 从无数据时显示 —
        assertNull("从无数据时温度为 null，界面展示为 —", weather.temperature)
        assertTrue("标记为 stale", weather.isStale)
        assertEquals(0L, weather.updatedAt)
    }

    @Test
    fun testDeleteCityPurgesCacheImmediately() = runBlocking {
        val mockWeatherApi = mock(WeatherApi::class.java)
        val mockGeocodingApi = mock(GeocodingApi::class.java)
        val repo = WeatherRepository(mockWeatherApi, mockGeocodingApi)

        val city1 = WeatherCity("北京", 39.90, 116.40)
        val city2 = WeatherCity("上海", 31.23, 121.47)

        val resp1 = Response.success(
            OpenMeteoResponse(39.90, 116.40, OpenMeteoCurrent("2026-11-23T12:00", 15.0, 50, 14.0, 1, 2.0))
        )
        val resp2 = Response.success(
            OpenMeteoResponse(31.23, 121.47, OpenMeteoCurrent("2026-11-23T12:00", 20.0, 60, 19.0, 2, 3.0))
        )

        `when`(mockWeatherApi.getCurrentWeather(39.90, 116.40)).thenReturn(resp1)
        `when`(mockWeatherApi.getCurrentWeather(31.23, 121.47)).thenReturn(resp2)

        repo.refreshAllWeather(listOf(city1, city2), force = true)
        assertEquals(2, repo.weatherListFlow.value.size)

        // 删除 city2
        repo.refreshAllWeather(listOf(city1), force = true)
        assertEquals(1, repo.weatherListFlow.value.size)
        assertEquals("北京", repo.weatherListFlow.value.first().cityName)
    }

    @Test
    fun testDeleteAllCitiesShowsSelectCityAndDoesNotCrash() = runBlocking {
        val mockWeatherApi = mock(WeatherApi::class.java)
        val mockGeocodingApi = mock(GeocodingApi::class.java)
        val repo = WeatherRepository(mockWeatherApi, mockGeocodingApi)

        // 用户在设置中删除全部城市
        repo.refreshAllWeather(emptyList(), force = true)

        assertTrue(repo.weatherListFlow.value.isEmpty())
        assertEquals("选择城市", repo.weatherFlow.value.cityName)
        assertFalse(repo.weatherFlow.value.isLoaded)
        assertNull(repo.weatherFlow.value.temperature)
    }
}
