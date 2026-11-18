package com.chronodesk.app.data

import com.chronodesk.app.data.model.ClockSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockSettingsTest {

    @Test
    fun testNightTimeAcrossMidnight() {
        // 22:00 至次日 07:00
        val settings = ClockSettings(
            autoNightDim = true,
            nightDimStartHour = 22,
            nightDimEndHour = 7
        )

        assertTrue("22点应处于夜间模式", settings.isNightTime(22))
        assertTrue("23点应处于夜间模式", settings.isNightTime(23))
        assertTrue("0点应处于夜间模式", settings.isNightTime(0))
        assertTrue("3点应处于夜间模式", settings.isNightTime(3))
        assertTrue("6点应处于夜间模式", settings.isNightTime(6))

        assertFalse("7点应退出夜间模式", settings.isNightTime(7))
        assertFalse("12点不应处于夜间模式", settings.isNightTime(12))
        assertFalse("21点不应处于夜间模式", settings.isNightTime(21))
    }

    @Test
    fun testNightTimeSameDay() {
        // 1:00 至 5:00
        val settings = ClockSettings(
            autoNightDim = true,
            nightDimStartHour = 1,
            nightDimEndHour = 5
        )

        assertTrue("2点应处于夜间模式", settings.isNightTime(2))
        assertFalse("0点不应处于夜间模式", settings.isNightTime(0))
        assertFalse("5点不应处于夜间模式", settings.isNightTime(5))
        assertFalse("14点不应处于夜间模式", settings.isNightTime(14))
    }

    @Test
    fun testDisabledNightDim() {
        val settings = ClockSettings(
            autoNightDim = false,
            nightDimStartHour = 22,
            nightDimEndHour = 7
        )
        assertFalse("关闭自动调暗后任何时刻都不应处于夜间模式", settings.isNightTime(23))
        assertFalse("关闭自动调暗后任何时刻都不应处于夜间模式", settings.isNightTime(3))
    }
}
