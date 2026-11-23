package com.chronodesk.app.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.chronodesk.app.data.model.ClockSettings
import com.chronodesk.app.data.provider.TeslaDataProvider
import com.chronodesk.app.data.repository.ClockSettingsRepository
import com.chronodesk.app.data.repository.CodexAccountRepository
import com.chronodesk.app.data.repository.SettingsValidationResult
import com.chronodesk.app.data.repository.TeslaRepository
import com.chronodesk.app.data.repository.WeatherRepository
import com.chronodesk.app.data.security.CredentialStorage
import com.chronodesk.app.ui.clock.ClockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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
        viewModel.onAppBackground()
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

    @Test
    fun testScreenOnMigration_legacyTrueToAlwaysAndRemoveKey() {
        val app = mock(Application::class.java)
        val sp = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(app.getSharedPreferences(anyString(), anyInt())).thenReturn(sp)
        `when`(sp.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.remove(anyString())).thenReturn(editor)

        `when`(sp.contains("screen_on_policy")).thenReturn(false)
        `when`(sp.contains("keep_screen_on")).thenReturn(true)
        `when`(sp.getBoolean("keep_screen_on", true)).thenReturn(true)
        `when`(sp.getInt(anyString(), anyInt())).thenAnswer { it.arguments[1] as Int }
        `when`(sp.getFloat(anyString(), anyFloat())).thenAnswer { it.arguments[1] as Float }
        `when`(sp.getString(anyString(), anyString())).thenAnswer { it.arguments[1] as String }

        val repo = ClockSettingsRepository(app)
        assertEquals("ALWAYS", repo.settingsFlow.value.screenOnPolicy)
        verify(editor).putString("screen_on_policy", "ALWAYS")
        verify(editor).remove("keep_screen_on")
    }

    @Test
    fun testScreenOnMigration_legacyFalseToSystemAndRemoveKey() {
        val app = mock(Application::class.java)
        val sp = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(app.getSharedPreferences(anyString(), anyInt())).thenReturn(sp)
        `when`(sp.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.remove(anyString())).thenReturn(editor)

        `when`(sp.contains("screen_on_policy")).thenReturn(false)
        `when`(sp.contains("keep_screen_on")).thenReturn(true)
        `when`(sp.getBoolean("keep_screen_on", true)).thenReturn(false)
        `when`(sp.getInt(anyString(), anyInt())).thenAnswer { it.arguments[1] as Int }
        `when`(sp.getFloat(anyString(), anyFloat())).thenAnswer { it.arguments[1] as Float }
        `when`(sp.getString(anyString(), anyString())).thenAnswer { it.arguments[1] as String }

        val repo = ClockSettingsRepository(app)
        assertEquals("SYSTEM", repo.settingsFlow.value.screenOnPolicy)
        verify(editor).putString("screen_on_policy", "SYSTEM")
        verify(editor).remove("keep_screen_on")
    }

    @Test
    fun testScreenOnMigration_newUserDefaultsToChargingOnly() {
        val app = mock(Application::class.java)
        val sp = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(app.getSharedPreferences(anyString(), anyInt())).thenReturn(sp)
        `when`(sp.edit()).thenReturn(editor)
        `when`(sp.contains("screen_on_policy")).thenReturn(false)
        `when`(sp.contains("keep_screen_on")).thenReturn(false)
        `when`(sp.getInt(anyString(), anyInt())).thenAnswer { it.arguments[1] as Int }
        `when`(sp.getFloat(anyString(), anyFloat())).thenAnswer { it.arguments[1] as Float }
        `when`(sp.getString(anyString(), anyString())).thenAnswer { it.arguments[1] as String }

        val repo = ClockSettingsRepository(app)
        assertEquals("CHARGING_ONLY", repo.settingsFlow.value.screenOnPolicy)
    }

    @Test
    fun testForegroundScheduling_deduplicatesAndStaleWindow() = runTest {
        val viewModel = ClockViewModel(
            application = mockApp,
            settingsRepo = settingsRepo,
            weatherRepo = weatherRepo,
            accountRepo = accountRepo
        )

        testDispatcher.scheduler.runCurrent()

        // 冷启动进入前台，触发初次同步
        verify(accountRepo, times(1)).refreshAll(false)
        verify(weatherRepo, times(1)).refreshAllWeather(anyList(), eq(false))

        // 模拟快速切后台再返回（时间未过刷新周期）
        viewModel.onAppBackground()
        viewModel.onAppForeground()
        testDispatcher.scheduler.runCurrent()

        // 额度与天气在有效期内，不应重复触发刷新请求
        verify(accountRepo, times(1)).refreshAll(false)
        verify(weatherRepo, times(1)).refreshAllWeather(anyList(), eq(false))

        viewModel.onAppBackground()
    }

    @Test
    fun testTeslaPollingLifecycle_strictlyRequiresForegroundAndPageVisibility() {
        val mockContext = mock(Context::class.java)
        val sp = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(sp)
        `when`(sp.edit()).thenReturn(editor)
        `when`(sp.getBoolean("enabled", false)).thenReturn(true)
        `when`(sp.getString("base_url", "")).thenReturn("http://192.168.1.100:3000")
        `when`(sp.getString("datasource_uid", "")).thenReturn("ds_pg")
        `when`(sp.getInt("car_id", 1)).thenReturn(1)
        `when`(sp.getInt("refresh_interval_sec", 60)).thenReturn(60)

        val credStorage = mock(CredentialStorage::class.java)
        `when`(credStorage.getGrafanaToken()).thenReturn("mock_token")

        val dataProvider = mock(TeslaDataProvider::class.java)
        val teslaRepo = TeslaRepository(
            context = mockContext,
            provider = dataProvider,
            credentialStorage = credStorage,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )

        // 初始状态：未进入前台且未进入车辆页，不轮询
        assertFalse("初始化时不应处于轮询状态", teslaRepo.isPollingActive())

        // 仅设为页面可见，但应用仍在后台：不轮询
        teslaRepo.setPageVisible(true)
        assertFalse("应用在后台时即使进入车辆页也不应轮询", teslaRepo.isPollingActive())

        // 应用进入前台且车辆页可见：开始轮询
        teslaRepo.onAppForeground()
        assertTrue("应用在前台且在车辆页时必须启动轮询", teslaRepo.isPollingActive())

        // 滑动离开车辆页（切换到时钟或详情）：立即停止轮询
        teslaRepo.setPageVisible(false)
        assertFalse("离开车辆页后必须立即停止轮询", teslaRepo.isPollingActive())

        // 滑回车辆页：恢复轮询
        teslaRepo.setPageVisible(true)
        assertTrue("滑回车辆页后恢复轮询", teslaRepo.isPollingActive())

        // 停留车辆页时按 Home 键（应用进入后台）：必须停止轮询
        teslaRepo.onAppBackground()
        assertFalse("退到后台必须立即停止轮询", teslaRepo.isPollingActive())
    }
}
