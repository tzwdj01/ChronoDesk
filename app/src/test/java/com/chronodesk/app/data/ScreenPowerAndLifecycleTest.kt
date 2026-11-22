package com.chronodesk.app.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.chronodesk.app.data.model.ClockSettings
import com.chronodesk.app.data.repository.ClockSettingsRepository
import com.chronodesk.app.data.repository.CodexAccountRepository
import com.chronodesk.app.data.repository.SettingsValidationResult
import com.chronodesk.app.data.repository.WeatherRepository
import com.chronodesk.app.ui.clock.ClockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenPowerAndLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApp: Application
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var settingsRepo: ClockSettingsRepository
    private lateinit var weatherRepo: WeatherRepository
    private lateinit var accountRepo: CodexAccountRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApp = mock(Application::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockApp.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockPrefs.getBoolean(anyString(), anyInt() == 1)).thenReturn(true)
        `when`(mockPrefs.getInt(anyString(), anyInt())).thenAnswer { it.arguments[1] as Int }
        `when`(mockPrefs.getFloat(anyString(), anyFloat())).thenAnswer { it.arguments[1] as Float }
        `when`(mockPrefs.getString(anyString(), anyString())).thenAnswer { it.arguments[1] as String }

        settingsRepo = ClockSettingsRepository(mockApp)
        weatherRepo = mock(WeatherRepository::class.java)
        accountRepo = mock(CodexAccountRepository::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testScreenOnPolicy_chargingOnlyDefault() {
        val settings = ClockSettings(
            screenOnPolicy = "CHARGING_ONLY",
            autoNightDim = false // 排除夜间影响
        )
        assertTrue("充电中应保持常亮", settings.shouldKeepScreenOn(isCharging = true, currentHour = 14))
        assertFalse("未充电时不应保持常亮", settings.shouldKeepScreenOn(isCharging = false, currentHour = 14))
    }

    @Test
    fun testScreenOnPolicy_alwaysAndSystem() {
        val always = ClockSettings(screenOnPolicy = "ALWAYS", autoNightDim = false)
        assertTrue("前台常亮策略即使未充电也应常亮", always.shouldKeepScreenOn(isCharging = false, currentHour = 14))
        assertTrue("前台常亮策略充电中保持常亮", always.shouldKeepScreenOn(isCharging = true, currentHour = 14))

        val system = ClockSettings(screenOnPolicy = "SYSTEM", autoNightDim = false)
        assertFalse("跟随系统策略充电中也不强制常亮", system.shouldKeepScreenOn(isCharging = true, currentHour = 14))
        assertFalse("跟随系统策略未充电不强制常亮", system.shouldKeepScreenOn(isCharging = false, currentHour = 14))
    }

    @Test
    fun testNightWindow_overridesScreenOn() {
        val settings = ClockSettings(
            screenOnPolicy = "CHARGING_ONLY",
            autoNightDim = true,
            nightDimStartHour = 22,
            nightDimEndHour = 7
        )

        // 白天 14点，充电中 -> 常亮
        assertTrue("白天充电中保持常亮", settings.shouldKeepScreenOn(isCharging = true, currentHour = 14))

        // 夜间 23点与凌晨 3点 -> 即使充电中也释放常亮以熄屏休眠
        assertFalse("夜间23点即使充电中也不应强制常亮", settings.shouldKeepScreenOn(isCharging = true, currentHour = 23))
        assertFalse("凌晨3点即使充电中也不应强制常亮", settings.shouldKeepScreenOn(isCharging = true, currentHour = 3))

        // 清晨到达 7点结束时间，若仍在充电则恢复常亮
        assertTrue("清晨7点夜间时段结束若在充电恢复常亮", settings.shouldKeepScreenOn(isCharging = true, currentHour = 7))
    }

    @Test
    fun testLifecycleBoundPolling_pauseOnBackgroundAndResumeOnForeground() {
        val viewModel = ClockViewModel(
            application = mockApp,
            settingsRepo = settingsRepo,
            weatherRepo = weatherRepo,
            accountRepo = accountRepo
        )

        assertTrue("创建并进入前台后轮询任务必须处于活动状态", viewModel.isPollingActive())

        // 模拟应用进入后台 (onStop)
        viewModel.onAppBackground()
        assertFalse("应用进入后台后轮询任务必须立即暂停", viewModel.isPollingActive())

        // 模拟应用重新回到前台 (onStart)
        viewModel.onAppForeground()
        assertTrue("应用重回前台后轮询任务必须恢复运行", viewModel.isPollingActive())
    }

    @Test
    fun testSettingsBackup_roundtripAndValidationForScreenOnPolicy() {
        val validJson = """
            {
                "schemaVersion": 1,
                "screenOnPolicy": "ALWAYS",
                "is24Hour": true
            }
        """.trimIndent()
        val result = settingsRepo.validateAndParseSettingsJson(validJson)
        assertTrue("合法的 screenOnPolicy 必须通过校验", result is SettingsValidationResult.Valid)
        val valid = result as SettingsValidationResult.Valid
        assertEquals("ALWAYS", valid.backup.screenOnPolicy)

        val invalidJson = """
            {
                "schemaVersion": 1,
                "screenOnPolicy": "INVALID_POLICY"
            }
        """.trimIndent()
        val invalidResult = settingsRepo.validateAndParseSettingsJson(invalidJson)
        assertTrue("非法的 screenOnPolicy 必须被拒绝", invalidResult is SettingsValidationResult.Invalid)
    }
}
