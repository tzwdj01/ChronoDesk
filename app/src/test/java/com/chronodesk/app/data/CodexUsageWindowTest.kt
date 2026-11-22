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
        assertEquals("5小时额度", window.windowLabel)
        assertEquals(25.5, window.usedPercent!!, 0.01)
        assertEquals(74.5, window.remainingPercent!!, 0.01)
        assertEquals("75%", window.formattedRemaining())
        assertEquals(resetAt, window.resetAtUnix)

        val countdown = window.formattedCountdown(nowSec)
        assertTrue("倒计时应包含2小时: $countdown", countdown.contains("2小时"))
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
        assertEquals("周额度", window.windowLabel)
        assertEquals(40.0, window.remainingPercent!!, 0.01)
        assertEquals("40%", window.formattedRemaining())
    }

    @Test
    fun testMissingOrUnknownWindowSeconds() {
        val missingWindow = CodexUsageWindow.fromRaw(
            usedPercent = 30.0,
            limitWindowSeconds = null,
            resetAtUnix = 1700000000L
        )
        assertNotNull(missingWindow)
        assertEquals("额度窗口", missingWindow!!.windowLabel)

        val customWindow = CodexUsageWindow.fromRaw(
            usedPercent = 30.0,
            limitWindowSeconds = 3600L,
            resetAtUnix = 1700000000L
        )
        assertNotNull(customWindow)
        assertEquals("额度窗口", customWindow!!.windowLabel)
    }

    @Test
    fun testOutOfBoundsAndNonFiniteUsedPercent() {
        // 越界值或非有限值不合成百分比，显示 —
        val overWindow = CodexUsageWindow.fromRaw(
            usedPercent = 120.0,
            limitWindowSeconds = 18000L,
            resetAtUnix = 1700000000L
        )
        assertNotNull(overWindow)
        assertNull(overWindow!!.remainingPercent)
        assertEquals("—", overWindow.formattedRemaining())

        val negativeWindow = CodexUsageWindow.fromRaw(
            usedPercent = -5.0,
            limitWindowSeconds = 18000L,
            resetAtUnix = 1700000000L
        )
        assertNotNull(negativeWindow)
        assertNull(negativeWindow!!.remainingPercent)
        assertEquals("—", negativeWindow.formattedRemaining())

        val nanWindow = CodexUsageWindow.fromRaw(
            usedPercent = Double.NaN,
            limitWindowSeconds = 18000L,
            resetAtUnix = 1700000000L
        )
        assertNotNull(nanWindow)
        assertNull(nanWindow!!.remainingPercent)
        assertEquals("—", nanWindow.formattedRemaining())
    }

    @Test
    fun testExhaustedAndSubOnePercent() {
        // 真实剩余 0% 显示耗尽
        val zeroRemaining = CodexUsageWindow.fromRaw(
            usedPercent = 100.0,
            limitWindowSeconds = 18000L,
            resetAtUnix = 1700018000L
        )
        assertNotNull(zeroRemaining)
        assertEquals(0.0, zeroRemaining!!.remainingPercent!!, 0.001)
        assertEquals("耗尽", zeroRemaining.formattedRemaining())

        // 真实剩余介于 0% 和 1% 之间显示 <1%
        val subOne = CodexUsageWindow.fromRaw(
            usedPercent = 99.4,
            limitWindowSeconds = 18000L,
            resetAtUnix = 1700018000L
        )
        assertNotNull(subOne)
        assertEquals(0.6, subOne!!.remainingPercent!!, 0.001)
        assertEquals("<1%", subOne.formattedRemaining())
    }

    @Test
    fun testResetTimeStates() {
        val nowSec = 1700000000L

        // 没有重置时间
        val noReset = CodexUsageWindow.fromRaw(
            usedPercent = 20.0,
            limitWindowSeconds = 18000L,
            resetAtUnix = null
        )
        assertNotNull(noReset)
        assertEquals("重置时间未知", noReset!!.formattedResetTime())
        assertEquals("重置时间未知", noReset.formattedCountdown(nowSec))

        // 重置时间在过去（设备时间超过重置时间且尚未同步）
        val pastReset = CodexUsageWindow.fromRaw(
            usedPercent = 20.0,
            limitWindowSeconds = 18000L,
            resetAtUnix = nowSec - 300L
        )
        assertNotNull(pastReset)
        assertEquals("等待更新", pastReset!!.formattedResetTime(nowSec))
        assertEquals("等待更新", pastReset.formattedCountdown(nowSec))
    }
}
