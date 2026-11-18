package com.chronodesk.app.data.model

data class ClockSettings(
    val is24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val keepScreenOn: Boolean = true,
    val autoNightDim: Boolean = true,
    val nightDimStartHour: Int = 22,
    val nightDimEndHour: Int = 7,
    val nightBrightness: Float = 0.08f,
    val normalBrightness: Float = -1.0f, // -1.0f 遵循系统亮度
    val autoRefreshIntervalMinutes: Int = 10,
    val useMockData: Boolean = false,
    val cityName: String = "北京",
    val latitude: Double = 39.9042,
    val longitude: Double = 116.4074
) {
    /**
     * 判断当前小时是否处于夜间模式时间段
     */
    fun isNightTime(currentHour: Int): Boolean {
        if (!autoNightDim) return false
        return if (nightDimStartHour > nightDimEndHour) {
            // 跨天，如 22点 到 次日7点
            currentHour >= nightDimStartHour || currentHour < nightDimEndHour
        } else {
            // 同一天内，如 1点 到 6点
            currentHour in nightDimStartHour until nightDimEndHour
        }
    }
}
