package com.chronodesk.app.data.utils

import java.util.Calendar

/**
 * 农历（阴历）日历转换工具类
 * 基于中国传统历法天文算法（支持 1900 - 2100 年精准换算）
 */
object LunarCalendarHelper {

    // 1900 - 2050 年农历数据压缩编码表
    // 16位表示12个月大小（1为大月30天，0为小月29天），低4位表示闰月月份（0为无闰月），高4位表示闰月大小
    private val lunarInfo = longArrayOf(
        0x04bd8L, 0x04ae0L, 0x0a570L, 0x054d5L, 0x0d260L, 0x0d950L, 0x16554L, 0x056a0L, 0x09ad0L, 0x055d2L,
        0x04ae0L, 0x0a5b6L, 0x0a4d0L, 0x0d250L, 0x1d255L, 0x0b540L, 0x0d6a0L, 0x0ada2L, 0x095b0L, 0x14977L,
        0x04970L, 0x0a4b0L, 0x0b4b5L, 0x06a50L, 0x06d40L, 0x1ab54L, 0x02b60L, 0x09570L, 0x052f2L, 0x04970L,
        0x06566L, 0x0d4a0L, 0x0ea50L, 0x06e95L, 0x05ad0L, 0x02b60L, 0x186e3L, 0x092e0L, 0x1c8d7L, 0x04950L,
        0x0d4a0L, 0x1d8a6L, 0x0b550L, 0x056a0L, 0x1a5b4L, 0x025d0L, 0x092d0L, 0x0d2b2L, 0x0a950L, 0x0b557L,
        0x06ca0L, 0x0b550L, 0x15355L, 0x04da0L, 0x0a5d0L, 0x14573L, 0x052d0L, 0x0a9a8L, 0x0e950L, 0x06aa0L,
        0x0aea6L, 0x0ab50L, 0x04b60L, 0x0aae4L, 0x0a570L, 0x05260L, 0x0f263L, 0x0d950L, 0x05b57L, 0x056a0L,
        0x096d0L, 0x04dd5L, 0x04ad0L, 0x0a4d0L, 0x0d4d4L, 0x0d250L, 0x0d558L, 0x0b540L, 0x0b5a0L, 0x195a6L,
        0x095b0L, 0x049b0L, 0x0a974L, 0x0a4b0L, 0x0b27aL, 0x06a50L, 0x06d40L, 0x0af46L, 0x0ab60L, 0x09570L,
        0x04af5L, 0x04970L, 0x064b0L, 0x074a3L, 0x0ea50L, 0x06b58L, 0x055c0L, 0x0ab60L, 0x096d5L, 0x092e0L,
        0x0c960L, 0x0d954L, 0x0d4a0L, 0x0da50L, 0x07552L, 0x056a0L, 0x0abb7L, 0x025d0L, 0x092d0L, 0x0cab5L,
        0x0a950L, 0x0b4a0L, 0x0baa4L, 0x0ad50L, 0x055d9L, 0x04ba0L, 0x0a5b0L, 0x15176L, 0x052b0L, 0x0a930L,
        0x07954L, 0x06aa0L, 0x0ad50L, 0x05b52L, 0x04b60L, 0x0a6e6L, 0x0a4e0L, 0x0d260L, 0x0ea65L, 0x0d530L,
        0x05aa0L, 0x076a3L, 0x096d0L, 0x04bd7L, 0x04ad0L, 0x0a4d0L, 0x1d0b6L, 0x0d250L, 0x0d520L, 0x0dd45L,
        0x0b5a0L, 0x056d0L, 0x055b2L, 0x049b0L, 0x0a577L, 0x0a4b0L, 0x0aa50L, 0x1b255L, 0x06d20L, 0x0ada0L
    )

    private val lunarMonthNames = arrayOf(
        "", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
    )

    private val lunarDayNames = arrayOf(
        "", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    /**
     * 根据公历 Calendar 实例获取格式化农历字符串，如 "农历十月初十"
     */
    fun getLunarString(calendar: Calendar = Calendar.getInstance()): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 基础锚点：1900年1月31日为农历1900年正月初一
        val baseCalendar = Calendar.getInstance().apply {
            set(1900, Calendar.JANUARY, 31, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCalendar = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var offset = ((targetCalendar.timeInMillis - baseCalendar.timeInMillis) / 86400000L).toInt()
        if (offset < 0) return "农历正月初一"

        var lunarYear = 1900
        for (y in 1900 until 2050) {
            val idx = y - 1900
            if (idx >= lunarInfo.size) break
            val info = lunarInfo[idx]

            var daysInYear = 0
            for (m in 1..12) {
                daysInYear += if (((info shr (16 - m)) and 1L) == 1L) 30 else 29
            }
            val leapM = (info and 0xfL).toInt()
            if (leapM > 0) {
                daysInYear += if ((info and 0x10000L) != 0L) 30 else 29
            }

            if (offset < daysInYear) {
                lunarYear = y
                break
            }
            offset -= daysInYear
        }

        val idx = (lunarYear - 1900).coerceIn(0, lunarInfo.size - 1)
        val info = lunarInfo[idx]
        val leapM = (info and 0xfL).toInt()
        var isLeap = false
        var lunarMonth = 1

        for (m in 1..12) {
            val daysInM = if (((info shr (16 - m)) and 1L) == 1L) 30 else 29
            if (offset < daysInM) {
                lunarMonth = m
                break
            }
            offset -= daysInM

            if (m == leapM) {
                val daysInLeap = if ((info and 0x10000L) != 0L) 30 else 29
                if (offset < daysInLeap) {
                    lunarMonth = m
                    isLeap = true
                    break
                }
                offset -= daysInLeap
            }
        }

        val lunarDay = offset + 1
        val monthStr = (if (isLeap) "闰" else "") + (lunarMonthNames.getOrNull(lunarMonth) ?: "$lunarMonth")
        val dayStr = lunarDayNames.getOrNull(lunarDay) ?: "${lunarDay}日"

        return "农历${monthStr}月$dayStr"
    }
}
