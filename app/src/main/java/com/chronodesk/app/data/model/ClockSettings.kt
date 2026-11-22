package com.chronodesk.app.data.model

data class WeatherCity(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class ClockSettings(
    val is24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val keepScreenOn: Boolean = true,
    val screenOnPolicy: String = "CHARGING_ONLY", // "SYSTEM", "CHARGING_ONLY", "ALWAYS"
    val nightModeOption: String = "AUTO", // "AUTO", "FORCE_NIGHT", "FORCE_DAY"
    val autoNightDim: Boolean = true,
    val nightDimStartHour: Int = 22,
    val nightDimEndHour: Int = 7,
    val nightBrightness: Float = 0.08f,
    val normalBrightness: Float = -1.0f, // -1.0f 遵循系统亮度
    val autoRefreshIntervalMinutes: Int = 10,
    val useMockData: Boolean = false,
    val autoLocation: Boolean = false,
    val cityName: String = "北京",
    val latitude: Double = 39.9042,
    val longitude: Double = 116.4074,
    val additionalCities: String = "",     // 额外城市列表，格式: "城市:纬度:经度;城市:纬度:经度"
    val quotaWidthPercent: Int = 38,       // 右侧额度看板宽度占比 (28% ~ 50%)
    val quotaCardDensity: String = "NORMAL", // COMPACT, NORMAL, SPACIOUS
    val clockScale: Float = 1.0f,          // 时钟缩放比例 (0.6f ~ 1.8f)
    val clockOffsetX: Float = 0f,          // 时钟水平偏移 (-300f ~ 300f)
    val clockOffsetY: Float = 0f           // 时钟垂直偏移 (-200f ~ 200f)
) {
    /**
     * 判断当前是否处于设定的夜间熄屏/调暗时段
     */
    fun isNightTimeWindow(currentHour: Int): Boolean {
        if (!autoNightDim) return false
        return if (nightDimStartHour > nightDimEndHour) {
            currentHour >= nightDimStartHour || currentHour < nightDimEndHour
        } else if (nightDimStartHour < nightDimEndHour) {
            currentHour in nightDimStartHour until nightDimEndHour
        } else {
            false
        }
    }

    /**
     * 根据充电状态、时间段及常亮策略计算是否保持屏幕常亮
     * 1. 处于夜间时段时，不再强制常亮，交由系统熄屏休眠
     * 2. 非夜间时段按 screenOnPolicy 判定
     */
    fun shouldKeepScreenOn(isCharging: Boolean, currentHour: Int): Boolean {
        if (isNightTimeWindow(currentHour)) {
            return false
        }
        return when (screenOnPolicy) {
            "ALWAYS" -> true
            "CHARGING_ONLY" -> isCharging
            "SYSTEM" -> false
            else -> isCharging
        }
    }
    /**
     * 获取所有配置的天气城市（主城市 + 额外城市）
     */
    fun getAllCities(): List<WeatherCity> {
        val list = mutableListOf(WeatherCity(cityName, latitude, longitude))
        if (additionalCities.isNotBlank()) {
            additionalCities.split(";").forEach { item ->
                val parts = item.split(":")
                if (parts.size >= 3) {
                    val n = parts[0].trim()
                    val la = parts[1].toDoubleOrNull()
                    val lo = parts[2].toDoubleOrNull()
                    if (n.isNotBlank() && la != null && lo != null && list.none { it.name == n }) {
                        list.add(WeatherCity(n, la, lo))
                    }
                }
            }
        }
        return list
    }

    /**
     * 判断当前是否处于夜间模式（支持自动时间段切换或手动强制切换）
     */
    fun isNightTime(currentHour: Int): Boolean {
        return when (nightModeOption) {
            "FORCE_NIGHT" -> true
            "FORCE_DAY" -> false
            else -> isNightTimeWindow(currentHour)
        }
    }
}
