package com.chronodesk.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.chronodesk.app.ui.clock.ClockScreen
import com.chronodesk.app.ui.clock.ClockViewModel
import com.chronodesk.app.ui.theme.ChronoDeskTheme
import com.chronodesk.app.ui.theme.PureBlack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

data class DebugNavCommand(
    val tick: Int = 0,
    val page: String? = null,
    val settingsTab: String? = null,
    val mockMode: Boolean? = null,
    val previewLongbridge: Boolean? = null,
    val displayMode: String? = null,
    val providerOrder: String? = null,
    val richPreview: Boolean? = null,
    val themeMode: String? = null,
    val controlsVisible: Boolean? = null
)

class MainActivity : ComponentActivity() {

    private val viewModel: ClockViewModel by viewModels()
    private var isCharging by mutableStateOf(false)
    private var debugNavCommand by mutableStateOf(DebugNavCommand())

    private fun handleDebugIntent(intent: Intent?) {
        if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) return
        if (intent == null) return
        val page = intent.getStringExtra("debug_page")
        val tab = intent.getStringExtra("debug_settings_tab")
        val mock = intent.getStringExtra("debug_mock")?.toBooleanStrictOrNull()
        val lbPreview = intent.getStringExtra("debug_lb_preview")?.toBooleanStrictOrNull()
        val displayMode = intent.getStringExtra("debug_display_mode")
        val providerOrder = intent.getStringExtra("debug_provider_order")
        val richPreview = intent.getStringExtra("debug_rich_preview")?.toBooleanStrictOrNull()
        val themeMode = intent.getStringExtra("debug_theme")
        val controlsVisible = intent.getStringExtra("debug_controls")?.toBooleanStrictOrNull()
        if (page != null || tab != null || mock != null || lbPreview != null ||
            displayMode != null || providerOrder != null || richPreview != null ||
            themeMode != null || controlsVisible != null
        ) {
            debugNavCommand = DebugNavCommand(
                tick = debugNavCommand.tick + 1,
                page = page,
                settingsTab = tab,
                mockMode = mock,
                previewLongbridge = lbPreview,
                displayMode = displayMode,
                providerOrder = providerOrder,
                richPreview = richPreview,
                themeMode = themeMode,
                controlsVisible = controlsVisible
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDebugIntent(intent)
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    updateScreenOnState()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    updateScreenOnState()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    if (charging != isCharging) {
                        isCharging = charging
                        updateScreenOnState()
                    }
                }
            }
        }
    }

    private fun checkInitialChargingState() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = registerReceiver(null, filter)
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {}
    }

    private fun updateScreenOnState() {
        val settings = viewModel.settings.value
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val shouldKeepOn = settings.shouldKeepScreenOn(isCharging, currentHour)
        if (shouldKeepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            detectCurrentLocation()
        } else {
            // 用户拒绝时保持默认城市或已有手工城市，关闭自动定位标记，不再反复弹窗
            val current = viewModel.settings.value
            viewModel.updateSettings(current.copy(autoLocation = false))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDebugIntent(intent)

        // 沉浸式全屏与隐藏系统栏（轻触边缘可呼出）
        applyFullScreenImmersive()

        // 剥离冷启动强制权限申请：未配置定位前直接按默认城市（北京）拉取天气，不弹窗

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isNightMode by viewModel.isNightMode.collectAsState()
            val timeState by viewModel.timeState.collectAsState()

            // 动态响应屏幕常亮策略、充电状态与夜间时段
            LaunchedEffect(
                settings.screenOnPolicy,
                settings.autoNightDim,
                settings.allowNightSleep,
                settings.nightDimStartHour,
                settings.nightDimEndHour,
                isCharging,
                timeState.hour
            ) {
                updateScreenOnState()
            }

            // 夜间窗口亮度控制（仅调节本 App 窗口亮度属性，不影响系统全局设置）
            val layoutParams = window.attributes
            if (isNightMode) {
                layoutParams.screenBrightness = settings.nightBrightness.coerceIn(0.01f, 1.0f)
            } else {
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            window.attributes = layoutParams

            ChronoDeskTheme(isNightMode = isNightMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureBlack
                ) {
                    ClockScreen(
                        viewModel = viewModel,
                        debugNavCommand = debugNavCommand,
                        onRequestLocationPermission = { requestLocationPermissionExplicitly() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkInitialChargingState()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(powerReceiver, filter)
        viewModel.onAppForeground()
        updateScreenOnState()
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(powerReceiver)
        } catch (_: Exception) {}
        viewModel.onAppBackground()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyFullScreenImmersive()
        }
    }

    override fun onResume() {
        super.onResume()
        applyFullScreenImmersive()
        updateScreenOnState()
        if (viewModel.settings.value.autoLocation) {
            val hasCoarse = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCoarse) {
                detectCurrentLocation()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyFullScreenImmersive() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // 针对 Android 9 (API 28, 如小米 6) 强效沉浸式全屏
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    fun requestLocationPermissionExplicitly() {
        val hasCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            detectCurrentLocation()
        }
    }

    private fun detectCurrentLocation() {
        if (!viewModel.settings.value.autoLocation) return

        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val lastLoc: Location? = try {
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } catch (e: SecurityException) {
                null
            }

            if (lastLoc != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    var detectedCity: String? = null
                    try {
                        val geocoder = Geocoder(this@MainActivity, Locale.CHINESE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1) { addrs ->
                                val city = addrs.firstOrNull()?.locality
                                    ?: addrs.firstOrNull()?.subAdminArea
                                    ?: addrs.firstOrNull()?.adminArea
                                if (!city.isNullOrBlank()) {
                                    val cityName = city.removeSuffix("市").removeSuffix("省")
                                    lifecycleScope.launch {
                                        applyLocation(lastLoc.latitude, lastLoc.longitude, cityName)
                                    }
                                }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addrs = geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1)
                            val city = addrs?.firstOrNull()?.locality
                                ?: addrs?.firstOrNull()?.subAdminArea
                                ?: addrs?.firstOrNull()?.adminArea
                            if (!city.isNullOrBlank()) {
                                detectedCity = city.removeSuffix("市").removeSuffix("省")
                            }
                        }
                    } catch (_: Exception) {
                    }

                    if (detectedCity != null) {
                        applyLocation(lastLoc.latitude, lastLoc.longitude, detectedCity)
                    } else {
                        applyLocation(lastLoc.latitude, lastLoc.longitude, viewModel.settings.value.cityName)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun applyLocation(lat: Double, lng: Double, cityName: String) {
        withContext(Dispatchers.Main) {
            val current = viewModel.settings.value
            val updated = current.copy(latitude = lat, longitude = lng, cityName = cityName)
            viewModel.updateSettings(updated)
            viewModel.weatherRepo.refreshWeather(cityName, lat, lng, force = true)
        }
    }
}
