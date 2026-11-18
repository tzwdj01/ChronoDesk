package com.chronodesk.app.data

import com.chronodesk.app.data.model.CodexUsageWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexUsageWindowTest {

    @Test
    fun testWindowParsingAndRemainingCalculation() {
        val nowSec = 1700000000L
        val resetAt = nowSec + 7200L // 2 hours later

        val window = CodexUsageWindow.fromRaw(
            usedPercent = 25.5,
            limitWindowSeconds = 18000L,
            resetAtUnix = resetAt
        )

        assertNotNull(window)
        assertEquals("five_hour", window!!.name)
        assertEquals("5小时额度", window.label)
        assertEquals(25.5, window.usedPercent, 0.01)
        assertEquals(74.5, window.remainingPercent, 0.01)
        assertEquals(resetAt, window.resetAtUnix)

        val countdown = window.formattedCountdown(nowSec)
        assertTrue("倒计时应包含2小时: $countdown", countdown.contains("2小时"))
    }

    @Test
    fun testZeroUsedSuppressesResetTime() {
        // 验证 CC Switch 逻辑：已用额度为 0 时屏蔽虚假重置时间
        val window = CodexUsageWindow.fromRaw(
            usedPercent = 0.0,
            limitWindowSeconds = 18000L,
            resetAtUnix = 1700018000L
        )

        assertNotNull(window)
        assertEquals(0.0, window!!.usedPercent, 0.001)
        assertEquals(100.0, window.remainingPercent, 0.001)
        assertNull("已用额度为0时不应展示虚假重置时间", window.resetAtUnix)
        assertEquals("已就绪", window.formattedCountdown())
    }

    @Test
    fun testWeeklyWindow() {
        val window = CodexUsageWindow.fromRaw(
            usedPercent = 60.0,
            limitWindowSeconds = 604800L,
            resetAtUnix = 1700500000L
        )

        assertNotNull(window)
        assertEquals("seven_day", window!!.name)
        assertEquals("周额度", window.label)
        assertEquals(40.0, window.remainingPercent, 0.01)
    }

    @Test
    fun testClamping() {
        val overWindow = CodexUsageWindow.fromRaw(
            usedPercent = 120.0,
            limitWindowSeconds = 18000L,
            resetAtUnix = 1700000000L
        )
        assertNotNull(overWindow)
        assertEquals(100.0, overWindow!!.usedPercent, 0.001)
        assertEquals(0.0, overWindow.remainingPercent, 0.001)

        val negativeWindow = CodexUsageWindow.fromRaw(
            usedPercent = -5.0,
            limitWindowSeconds = 18000L,
            resetAtUnix = 1700000000L
        )
        assertNotNull(negativeWindow)
        assertEquals(0.0, negativeWindow!!.usedPercent, 0.001)
        assertEquals(100.0, negativeWindow.remainingPercent, 0.001)
    }
}
