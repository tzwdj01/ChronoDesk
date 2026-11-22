package com.chronodesk.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: ClockViewModel by viewModels()

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

        // 沉浸式全屏与隐藏系统栏（轻触边缘可呼出）
        applyFullScreenImmersive()

        // 剥离冷启动强制权限申请：未配置定位前直接按默认城市（北京）拉取天气，不弹窗

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isNightMode by viewModel.isNightMode.collectAsState()

            // 保持屏幕常亮
            if (settings.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                        onRequestLocationPermission = { requestLocationPermissionExplicitly() }
                    )
                }
            }
        }
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
        // 重新进入前台时刷新数据
        viewModel.refreshAll()
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
