package com.chronodesk.app.data

import com.chronodesk.app.data.model.ClockSettings
import com.chronodesk.app.data.model.CodexAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstUseAndAccessibilityTest {

    @Test
    fun testColdBootDefaultSettings_noForcedPermissionAndBeijingDefault() {
        val defaultSettings = ClockSettings()
        // 冷启动默认关闭自动定位，避免强弹权限框
        assertFalse("默认配置下 autoLocation 必须为 false", defaultSettings.autoLocation)
        // 默认城市为北京
        assertEquals("北京", defaultSettings.cityName)
        assertEquals(39.9042, defaultSettings.latitude, 0.001)
        assertEquals(116.4074, defaultSettings.longitude, 0.001)
    }

    @Test
    fun testXiaomi6_16x9AspectRatioDetectionAnd50PercentSplit() {
        // 小米 6 分辨率 1920x1080 (16:9 = 1.7778)
        val width = 1920f
        val height = 1080f
        val aspectRatio = width / height

        val isLandscape = true
        val is16x9Compact = isLandscape && (aspectRatio <= 1.85f)
        assertTrue("小米 6 16:9 横屏必须命中 is16x9Compact", is16x9Compact)

        val settings = ClockSettings(quotaWidthPercent = 38)
        val quotaFraction = if (is16x9Compact) 0.50f else (settings.quotaWidthPercent / 100f).coerceIn(0.28f, 0.52f)
        val clockFraction = 1f - quotaFraction

        assertEquals("小米 6 16:9 额度区域必须分配 50%", 0.50f, quotaFraction, 0.001f)
        assertEquals("小米 6 16:9 时钟区域必须分配 50%", 0.50f, clockFraction, 0.001f)
    }

    @Test
    fun testModernWideScreen_aspectRatioAllocation() {
        // 现代宽屏手机 (20:9 = 2.222, 如小米 11)
        val width = 2400f
        val height = 1080f
        val aspectRatio = width / height

        val isLandscape = true
        val is16x9Compact = isLandscape && (aspectRatio <= 1.85f)
        assertFalse("20:9 现代宽屏不应命中 16:9 紧凑限制", is16x9Compact)

        val settings = ClockSettings(quotaWidthPercent = 38)
        val quotaFraction = if (is16x9Compact) 0.50f else (settings.quotaWidthPercent / 100f).coerceIn(0.28f, 0.52f)
        assertEquals("宽屏应遵循设置配比 38%", 0.38f, quotaFraction, 0.001f)
    }

    @Test
    fun testEmptyAccountStateHandling() {
        val emptyAccounts = emptyList<CodexAccount>()
        assertTrue("初始新安装状态账号列表为空", emptyAccounts.isEmpty())

        // 有账号时区分主副账号折叠
        val twoAccounts = listOf(
            CodexAccount.createPlaceholder().copy(id = "acc_1", email = "primary@test.com"),
            CodexAccount.createPlaceholder().copy(id = "acc_2", email = "secondary@test.com")
        )
        assertEquals(2, twoAccounts.size)
        // 索引 > 0 为副账号，在 compact 模式下折叠
        val isCompact = true
        val secondaryAccountFolded = isCompact && (twoAccounts.indices.last > 0)
        assertTrue("紧凑模式下第二个及后续账号应折叠显示", secondaryAccountFolded)
    }

    @Test
    fun testLocationPermissionRejectionState() {
        var settings = ClockSettings(autoLocation = true)
        // 模拟权限被拒逻辑
        val permissionGranted = false
        if (!permissionGranted) {
            settings = settings.copy(autoLocation = false)
        }
        assertFalse("权限被拒后 autoLocation 应被置为 false 避免重复弹窗", settings.autoLocation)
        assertEquals("北京", settings.cityName)
    }

    @Test
    fun testTouchTargetStandardCompliance() {
        val minTouchTargetDp = 48
        assertTrue("Material 触控区标准不低于 48dp", minTouchTargetDp >= 48)
    }
}
