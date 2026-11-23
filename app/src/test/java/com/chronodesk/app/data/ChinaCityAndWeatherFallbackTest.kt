package com.chronodesk.app.data

import com.chronodesk.app.data.model.ChinaCityDatabase
import com.chronodesk.app.data.model.WeatherCity
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.data.network.FallbackWeatherProvider
import com.chronodesk.app.data.network.GeocodingApi
import com.chronodesk.app.data.network.GeocodingItem
import com.chronodesk.app.data.network.GeocodingSearchResponse
import com.chronodesk.app.data.network.MultiSourceWeatherFallback
import com.chronodesk.app.data.network.NominatimGeocodingFallback
import com.chronodesk.app.data.network.OnlineGeocodingFallback
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

class ChinaCityAndWeatherFallbackTest {

    @Test
    fun testOfflineDatabase_findsWenzhouYanjiNantongYiwuAndPinyin() {
        val wenzhou = ChinaCityDatabase.search("温州")
        assertTrue("离线库必须命中温州", wenzhou.isNotEmpty())
        assertEquals("温州", wenzhou.first().name)
        assertEquals("浙江省", wenzhou.first().admin1)
        assertEquals(28.0006, wenzhou.first().latitude, 0.01)
        assertEquals(120.6994, wenzhou.first().longitude, 0.01)

        val wenzhouSuffix = ChinaCityDatabase.search("温州市")
        assertTrue("输入带市后缀也能命中温州", wenzhouSuffix.any { it.name == "温州" })

        val wenzhouPinyin = ChinaCityDatabase.search("wenzhou")
        assertTrue("拼音全拼能命中温州", wenzhouPinyin.any { it.name == "温州" })

        val wenzhouInitial = ChinaCityDatabase.search("wz")
        assertTrue("拼音首字母能命中温州", wenzhouInitial.any { it.name == "温州" })

        val yanji = ChinaCityDatabase.search("延吉")
        assertTrue("离线库必须命中延吉", yanji.isNotEmpty())
        assertEquals("延吉", yanji.first().name)
        assertEquals("吉林省", yanji.first().admin1)
        assertEquals("延边朝鲜族自治州", yanji.first().admin2)

        val yanjiCity = ChinaCityDatabase.search("延吉市")
        assertTrue(yanjiCity.any { it.name == "延吉" })

        val yanjiPinyin = ChinaCityDatabase.search("yanji")
        assertTrue(yanjiPinyin.any { it.name == "延吉" })

        val yanbian = ChinaCityDatabase.search("延边")
        assertTrue("搜索延边州名可匹配州内县市", yanbian.size >= 3)

        val tongzhou = ChinaCityDatabase.search("通州")
        assertTrue("同时区分北京通州与南通通州", tongzhou.count { it.name == "通州" } >= 2)

        assertTrue(ChinaCityDatabase.search("义乌").any { it.name == "义乌" })
        assertTrue(ChinaCityDatabase.search("昆山").any { it.name == "昆山" })
        assertTrue(ChinaCityDatabase.search("拉萨").any { it.name == "拉萨" })
    }

    @Test
    fun testSearchLocations_mergesDeduplicatesAndResilientWhenOnlineFails() = runBlocking {
        val mockWeatherApi = mock(WeatherApi::class.java)
        val mockGeocodingApi = mock(GeocodingApi::class.java)

        // 模拟 Open-Meteo 抛出网络异常，但备用 Nominatim 返回两个地点（一个与离线重复、一个为新地点）
        `when`(mockGeocodingApi.searchLocations("温州")).thenThrow(RuntimeException("timeout"))
        val onlineFallback = OnlineGeocodingFallback {
            listOf(
                GeocodingItem(1001L, "温州市鹿城区", 28.005, 120.700, "中国", "CN", "浙江省"), // 坐标 < 0.03° 应去重
                GeocodingItem(1002L, "温州湾新区", 27.850, 120.850, "中国", "CN", "浙江省")    // 新坐标应保留
            )
        }
        val repo = WeatherRepository(
            weatherApi = mockWeatherApi,
            geocodingApi = mockGeocodingApi,
            onlineGeocodingFallback = onlineFallback
        )

        val res = repo.searchLocations("温州")
        assertTrue("即使主在线源异常，离线与备用合并必须成功", res.isSuccess)
        val items = res.getOrThrow()
        assertTrue("首选结果仍为精确匹配的温州", items.first().name == "温州")
        assertTrue("应去重鹿城区但保留新坐标温州湾新区", items.any { it.name == "温州湾新区" })
        assertFalse("接近 0.03° 的重复坐标不应重复出现", items.any { it.name == "温州市鹿城区" })
    }

    @Test
    fun testRefreshAllWeather_switchesToFallbackProviderWhenPrimaryFails() = runBlocking {
        val mockWeatherApi = mock(WeatherApi::class.java)
        val mockGeocodingApi = mock(GeocodingApi::class.java)
        val city = WeatherCity("温州", 28.0006, 120.6994)

        // Open-Meteo 抛异常
        `when`(mockWeatherApi.getCurrentWeather(28.0006, 120.6994))
            .thenThrow(RuntimeException("open-meteo unreachable"))

        val fallback = FallbackWeatherProvider { target, nowMs ->
            WeatherInfo(
                cityName = target.name,
                latitude = target.latitude,
                longitude = target.longitude,
                temperature = 18.6,
                apparentTemperature = 18.6,
                weatherCode = 2,
                weatherDescription = "多云",
                humidity = 68,
                windSpeed = 2.4,
                isLoaded = true,
                isStale = false,
                updatedAt = nowMs
            )
        }

        val repo = WeatherRepository(
            weatherApi = mockWeatherApi,
            geocodingApi = mockGeocodingApi,
            fallbackWeatherProvider = fallback
        )

        repo.refreshAllWeather(listOf(city), force = true)
        val info = repo.weatherListFlow.value.first()
        assertTrue(info.isLoaded)
        assertFalse("备用源成功时不应标记为 stale", info.isStale)
        assertEquals(18.6, info.temperature!!, 0.01)
        assertEquals("多云", info.weatherDescription)
        assertEquals(68, info.humidity)
        assertEquals(2.4, info.windSpeed!!, 0.01)
    }

    @Test
    fun testRefreshAllWeather_keepsStaleCacheWhenPrimaryAndFallbackBothFail() = runBlocking {
        val mockWeatherApi = mock(WeatherApi::class.java)
        val mockGeocodingApi = mock(GeocodingApi::class.java)
        val city = WeatherCity("延吉", 42.8913, 129.5089)

        `when`(mockWeatherApi.getCurrentWeather(42.8913, 129.5089)).thenReturn(
            Response.success(
                OpenMeteoResponse(
                    42.8913,
                    129.5089,
                    OpenMeteoCurrent("2026-11-24T01:00", -4.5, 52, -8.0, 71, 3.1)
                )
            )
        )
        val failingFallback = FallbackWeatherProvider { _, _ -> null }
        val repo = WeatherRepository(
            weatherApi = mockWeatherApi,
            geocodingApi = mockGeocodingApi,
            fallbackWeatherProvider = failingFallback
        )

        repo.refreshAllWeather(listOf(city), force = true)
        val first = repo.weatherListFlow.value.first()
        assertEquals(-4.5, first.temperature!!, 0.01)
        assertFalse(first.isStale)

        // 第二次主源返回 HTTP 502 且备用源也失败
        `when`(mockWeatherApi.getCurrentWeather(42.8913, 129.5089))
            .thenThrow(RuntimeException("offline"))
        repo.refreshAllWeather(listOf(city), force = true)

        val second = repo.weatherListFlow.value.first()
        assertEquals(-4.5, second.temperature!!, 0.01)
        assertTrue("主备双源失败时应保留缓存并标为 stale", second.isStale)
    }

    @Test
    fun testMetNorwayWttrAndNominatimParsers() {
        assertEquals(0, MultiSourceWeatherFallback.mapMetNorwaySymbolToWmo("clearsky_day"))
        assertEquals(2, MultiSourceWeatherFallback.mapMetNorwaySymbolToWmo("partlycloudy_night"))
        assertEquals(61, MultiSourceWeatherFallback.mapMetNorwaySymbolToWmo("lightrain_day"))
        assertEquals(95, MultiSourceWeatherFallback.mapMetNorwaySymbolToWmo("heavyrainshowersandthunder_day"))
        assertEquals(71, MultiSourceWeatherFallback.mapMetNorwaySymbolToWmo("snow"))
        assertNull(MultiSourceWeatherFallback.mapMetNorwaySymbolToWmo(""))

        val metJson = """
            {
              "properties": {
                "timeseries": [
                  {
                    "data": {
                      "instant": {
                        "details": {
                          "air_temperature": 14.2,
                          "relative_humidity": 71.4,
                          "wind_speed": 3.54
                        }
                      },
                      "next_1_hours": {
                        "summary": { "symbol_code": "partlycloudy_day" }
                      }
                    }
                  }
                ]
              }
            }
        """.trimIndent()
        val metParsed = MultiSourceWeatherFallback.parseMetNorwayJson(metJson, WeatherCity("温州", 28.0, 120.7), 123456L)
        assertNotNull(metParsed)
        assertEquals(14.2, metParsed!!.temperature!!, 0.01)
        assertEquals(71, metParsed.humidity)
        assertEquals(3.5, metParsed.windSpeed!!, 0.01)
        assertEquals("多云", metParsed.weatherDescription)

        val wttrJson = """
            {
              "current_condition": [
                {
                  "temp_C": "11",
                  "FeelsLikeC": "9",
                  "humidity": "65",
                  "windspeedKmph": "18",
                  "weatherCode": "113"
                }
              ]
            }
        """.trimIndent()
        val wttrParsed = MultiSourceWeatherFallback.parseWttrJson(wttrJson, WeatherCity("延吉", 42.89, 129.51), 654321L)
        assertNotNull(wttrParsed)
        assertEquals(11.0, wttrParsed!!.temperature!!, 0.01)
        assertEquals(65, wttrParsed.humidity)
        assertEquals(5.0, wttrParsed.windSpeed!!, 0.01) // 18 km/h == 5.0 m/s
        assertEquals("晴朗", wttrParsed.weatherDescription)

        val nominatimJson = """
            [
              {
                "place_id": 8899,
                "name": "延吉市",
                "lat": "42.89127",
                "lon": "129.50894",
                "address": {
                  "country": "中国",
                  "country_code": "cn",
                  "state": "吉林省",
                  "city": "延边朝鲜族自治州"
                }
              }
            ]
        """.trimIndent()
        val geoList = NominatimGeocodingFallback.parseNominatimJson(nominatimJson)
        assertEquals(1, geoList.size)
        assertEquals("吉林省 延吉市", geoList.first().displayName)
    }

    @Test
    fun testFallbackParsers_handleJsonNullFieldsGracefully() {
        val metJsonWithNulls = """
            {
              "properties": {
                "timeseries": [
                  {
                    "data": {
                      "instant": {
                        "details": {
                          "air_temperature": 19.5,
                          "relative_humidity": null,
                          "wind_speed": null
                        }
                      },
                      "next_1_hours": {
                        "summary": { "symbol_code": null }
                      },
                      "next_6_hours": null
                    }
                  }
                ]
              }
            }
        """.trimIndent()
        val metParsed = MultiSourceWeatherFallback.parseMetNorwayJson(metJsonWithNulls, WeatherCity("温州", 28.0, 120.7), 123456L)
        assertNotNull(metParsed)
        assertEquals(19.5, metParsed!!.temperature!!, 0.01)
        assertNull(metParsed.humidity)
        assertNull(metParsed.windSpeed)

        val wttrJsonWithNulls = """
            {
              "current_condition": [
                {
                  "temp_C": "12",
                  "FeelsLikeC": null,
                  "humidity": null,
                  "windspeedKmph": null,
                  "weatherCode": null
                }
              ]
            }
        """.trimIndent()
        val wttrParsed = MultiSourceWeatherFallback.parseWttrJson(wttrJsonWithNulls, WeatherCity("延吉", 42.89, 129.51), 654321L)
        assertNotNull(wttrParsed)
        assertEquals(12.0, wttrParsed!!.temperature!!, 0.01)
        assertNull(wttrParsed.humidity)
        assertNull(wttrParsed.windSpeed)

        val nominatimWithNulls = """
            [
              {
                "place_id": null,
                "name": null,
                "display_name": "温州市, 浙江省, 中国",
                "lat": "28.0006",
                "lon": "120.6994",
                "address": {
                  "country": null,
                  "country_code": null,
                  "state": "浙江省",
                  "city": "温州市"
                }
              },
              {
                "place_id": 1002,
                "name": "鹿城区",
                "lat": null,
                "lon": "120.65"
              },
              {
                "place_id": 1003,
                "name": null,
                "display_name": null,
                "lat": "28.01",
                "lon": "120.70"
              }
            ]
        """.trimIndent()
        val geoList = NominatimGeocodingFallback.parseNominatimJson(nominatimWithNulls)
        assertEquals(1, geoList.size)
        assertEquals("浙江省 温州市", geoList.first().displayName)
    }
}
