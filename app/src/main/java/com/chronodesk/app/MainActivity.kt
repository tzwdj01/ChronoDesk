package com.chronodesk.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chronodesk.app.ui.clock.ClockScreen
import com.chronodesk.app.ui.clock.ClockViewModel
import com.chronodesk.app.ui.theme.ChronoDeskTheme
import com.chronodesk.app.ui.theme.PureBlack

class MainActivity : ComponentActivity() {

    private val viewModel: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 沉浸式全屏与隐藏系统栏（轻触可呼出）
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

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
                    ClockScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 重新进入前台时刷新数据
        viewModel.refreshAll()
    }
}
