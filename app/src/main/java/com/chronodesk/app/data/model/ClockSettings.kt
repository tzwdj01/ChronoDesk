package com.chronodesk.app.data.model

data class ClockSettings(
    val is24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val keepScreenOn: Boolean = true,
    val nightModeOption: String = "AUTO", // "AUTO", "FORCE_NIGHT", "FORCE_DAY"
    val autoNightDim: Boolean = true,
    val nightDimStartHour: Int = 22,
    val nightDimEndHour: Int = 7,
    val nightBrightness: Float = 0.08f,
    val normalBrightness: Float = -1.0f, // -1.0f 遵循系统亮度
    val autoRefreshIntervalMinutes: Int = 10,
    val useMockData: Boolean = false,
    val autoLocation: Boolean = true,
    val cityName: String = "深圳",
    val latitude: Double = 22.5431,
    val longitude: Double = 114.0579,
    val quotaWidthPercent: Int = 38,       // 右侧额度看板宽度占比 (28% ~ 50%)
    val quotaCardDensity: String = "NORMAL" // COMPACT, NORMAL, SPACIOUS
) {
    /**
     * 判断当前是否处于夜间模式（支持自动时间段切换或手动强制切换）
     */
    fun isNightTime(currentHour: Int): Boolean {
        return when (nightModeOption) {
            "FORCE_NIGHT" -> true
            "FORCE_DAY" -> false
            else -> {
                if (!autoNightDim) return false
                if (nightDimStartHour > nightDimEndHour) {
                    // 跨天，如 22点 到 次日7点
                    currentHour >= nightDimStartHour || currentHour < nightDimEndHour
                } else {
                    // 同一天内，如 1点 到 6点
                    currentHour in nightDimStartHour until nightDimEndHour
                }
            }
        }
    }
}
