package com.chronodesk.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.chronodesk.app.data.model.ClockSettings
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GeneralSettingsBackup(
    val schemaVersion: Int = 1,
    val is24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val screenOnPolicy: String = "CHARGING_ONLY", // CHARGING_ONLY, ALWAYS, SYSTEM
    val nightModeOption: String = "AUTO", // AUTO, FORCE_NIGHT, FORCE_DAY
    val autoNightDim: Boolean = true,
    val nightDimStartHour: Int = 22,
    val nightDimEndHour: Int = 7,
    val nightBrightness: Float = 0.08f,
    val quotaWidthPercent: Int = 38,
    val quotaCardDensity: String = "NORMAL", // COMPACT, NORMAL, SPACIOUS
    val clockScale: Float = 1.0f,
    val clockOffsetX: Float = 0f,
    val clockOffsetY: Float = 0f
)

sealed class SettingsValidationResult {
    data class Valid(val backup: GeneralSettingsBackup, val previewSummary: String) : SettingsValidationResult()
    data class Invalid(val errorMessage: String) : SettingsValidationResult()
}

class ClockSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("chronodesk_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<ClockSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): ClockSettings {
        return ClockSettings(
            is24Hour = prefs.getBoolean("is_24_hour", true),
            showSeconds = prefs.getBoolean("show_seconds", true),
            keepScreenOn = prefs.getBoolean("keep_screen_on", true),
            screenOnPolicy = prefs.getString("screen_on_policy", "CHARGING_ONLY") ?: "CHARGING_ONLY",
            nightModeOption = prefs.getString("night_mode_option", "AUTO") ?: "AUTO",
            autoNightDim = prefs.getBoolean("auto_night_dim", true),
            nightDimStartHour = prefs.getInt("night_start_hour", 22),
            nightDimEndHour = prefs.getInt("night_end_hour", 7),
            nightBrightness = prefs.getFloat("night_brightness", 0.08f),
            normalBrightness = prefs.getFloat("normal_brightness", -1.0f),
            autoRefreshIntervalMinutes = prefs.getInt("refresh_interval_min", 10),
            useMockData = prefs.getBoolean("use_mock_data", false),
            autoLocation = prefs.getBoolean("auto_location", false),
            cityName = if (prefs.contains("city_name")) (prefs.getString("city_name", "") ?: "") else "北京",
            latitude = prefs.getFloat("city_lat", 39.9042f).toDouble(),
            longitude = prefs.getFloat("city_lng", 116.4074f).toDouble(),
            additionalCities = prefs.getString("additional_cities", "") ?: "",
            quotaWidthPercent = prefs.getInt("quota_width_percent", 38),
            quotaCardDensity = prefs.getString("quota_card_density", "NORMAL") ?: "NORMAL",
            clockScale = prefs.getFloat("clock_scale", 1.0f),
            clockOffsetX = prefs.getFloat("clock_offset_x", 0f),
            clockOffsetY = prefs.getFloat("clock_offset_y", 0f)
        )
    }

    fun updateSettings(newSettings: ClockSettings) {
        prefs.edit()
            .putBoolean("is_24_hour", newSettings.is24Hour)
            .putBoolean("show_seconds", newSettings.showSeconds)
            .putBoolean("keep_screen_on", newSettings.keepScreenOn)
            .putString("screen_on_policy", newSettings.screenOnPolicy)
            .putString("night_mode_option", newSettings.nightModeOption)
            .putBoolean("auto_night_dim", newSettings.autoNightDim)
            .putInt("night_start_hour", newSettings.nightDimStartHour)
            .putInt("night_end_hour", newSettings.nightDimEndHour)
            .putFloat("night_brightness", newSettings.nightBrightness)
            .putFloat("normal_brightness", newSettings.normalBrightness)
            .putInt("refresh_interval_min", newSettings.autoRefreshIntervalMinutes)
            .putBoolean("use_mock_data", newSettings.useMockData)
            .putBoolean("auto_location", newSettings.autoLocation)
            .putString("city_name", newSettings.cityName)
            .putFloat("city_lat", newSettings.latitude.toFloat())
            .putFloat("city_lng", newSettings.longitude.toFloat())
            .putString("additional_cities", newSettings.additionalCities)
            .putInt("quota_width_percent", newSettings.quotaWidthPercent)
            .putString("quota_card_density", newSettings.quotaCardDensity)
            .putFloat("clock_scale", newSettings.clockScale)
            .putFloat("clock_offset_x", newSettings.clockOffsetX)
            .putFloat("clock_offset_y", newSettings.clockOffsetY)
            .apply()
        _settingsFlow.value = newSettings
    }

    /**
     * 导出仅包含非敏感通用界面设置的 JSON（schemaVersion: 1）
     */
    fun exportSettingsJson(): String {
        val current = _settingsFlow.value
        val backup = GeneralSettingsBackup(
            schemaVersion = 1,
            is24Hour = current.is24Hour,
            showSeconds = current.showSeconds,
            screenOnPolicy = current.screenOnPolicy,
            nightModeOption = current.nightModeOption,
            autoNightDim = current.autoNightDim,
            nightDimStartHour = current.nightDimStartHour,
            nightDimEndHour = current.nightDimEndHour,
            nightBrightness = current.nightBrightness,
            quotaWidthPercent = current.quotaWidthPercent,
            quotaCardDensity = current.quotaCardDensity,
            clockScale = current.clockScale,
            clockOffsetX = current.clockOffsetX,
            clockOffsetY = current.clockOffsetY
        )
        return GsonBuilder().setPrettyPrinting().create().toJson(backup)
    }

    /**
     * 校验并解析通用设置 JSON。
     * 若包含凭据、旧迁移 payload 结构、未知版本或非法越界数值，立即拒绝。
     */
    fun validateAndParseSettingsJson(json: String): SettingsValidationResult {
        if (json.isBlank()) return SettingsValidationResult.Invalid("设置内容为空")
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            // 拒绝旧凭据迁移或账号元数据结构
            if (root.has("credentials") || root.has("accountsMeta") || root.has("exportTimestamp")) {
                return SettingsValidationResult.Invalid("已拒绝：包含凭据或旧迁移数据，仅支持通用配置导入")
            }
            if (!root.has("schemaVersion")) {
                return SettingsValidationResult.Invalid("格式错误：缺少 schemaVersion 字段")
            }
            val schemaVersion = root.get("schemaVersion").asInt
            if (schemaVersion != 1) {
                return SettingsValidationResult.Invalid("版本不兼容：当前仅支持 schemaVersion 1 (读取到: $schemaVersion)")
            }

            val is24Hour = if (root.has("is24Hour")) root.get("is24Hour").asBoolean else true
            val showSeconds = if (root.has("showSeconds")) root.get("showSeconds").asBoolean else true
            val screenOnPolicy = if (root.has("screenOnPolicy")) root.get("screenOnPolicy").asString else "CHARGING_ONLY"
            if (screenOnPolicy !in listOf("CHARGING_ONLY", "ALWAYS", "SYSTEM")) {
                return SettingsValidationResult.Invalid("屏幕常亮策略非法: $screenOnPolicy")
            }
            val nightModeOption = if (root.has("nightModeOption")) root.get("nightModeOption").asString else "AUTO"
            if (nightModeOption !in listOf("AUTO", "FORCE_NIGHT", "FORCE_DAY")) {
                return SettingsValidationResult.Invalid("夜间模式选项非法: $nightModeOption")
            }
            val autoNightDim = if (root.has("autoNightDim")) root.get("autoNightDim").asBoolean else true
            val startHour = if (root.has("nightDimStartHour")) root.get("nightDimStartHour").asInt else 22
            if (startHour !in 0..23) {
                return SettingsValidationResult.Invalid("夜间开始小时超出范围 (0-23): $startHour")
            }
            val endHour = if (root.has("nightDimEndHour")) root.get("nightDimEndHour").asInt else 7
            if (endHour !in 0..23) {
                return SettingsValidationResult.Invalid("夜间结束小时超出范围 (0-23): $endHour")
            }
            val brightness = if (root.has("nightBrightness")) root.get("nightBrightness").asFloat else 0.08f
            if (brightness !in 0.01f..1.0f) {
                return SettingsValidationResult.Invalid("夜间亮度超出范围 (0.01-1.0): $brightness")
            }
            val widthPercent = if (root.has("quotaWidthPercent")) root.get("quotaWidthPercent").asInt else 38
            if (widthPercent !in 20..60) {
                return SettingsValidationResult.Invalid("看板宽度超出范围 (20-60): $widthPercent")
            }
            val density = if (root.has("quotaCardDensity")) root.get("quotaCardDensity").asString else "NORMAL"
            if (density !in listOf("COMPACT", "NORMAL", "SPACIOUS")) {
                return SettingsValidationResult.Invalid("看板密度配置无效: $density")
            }
            val clockScale = if (root.has("clockScale")) root.get("clockScale").asFloat else 1.0f
            if (clockScale !in 0.5f..2.5f) {
                return SettingsValidationResult.Invalid("时钟缩放比例超出范围 (0.5-2.5): $clockScale")
            }
            val clockOffsetX = if (root.has("clockOffsetX")) root.get("clockOffsetX").asFloat else 0f
            if (clockOffsetX !in -500f..500f) {
                return SettingsValidationResult.Invalid("时钟水平偏移超出范围: $clockOffsetX")
            }
            val clockOffsetY = if (root.has("clockOffsetY")) root.get("clockOffsetY").asFloat else 0f
            if (clockOffsetY !in -500f..500f) {
                return SettingsValidationResult.Invalid("时钟垂直偏移超出范围: $clockOffsetY")
            }

            val backup = GeneralSettingsBackup(
                schemaVersion = 1,
                is24Hour = is24Hour,
                showSeconds = showSeconds,
                screenOnPolicy = screenOnPolicy,
                nightModeOption = nightModeOption,
                autoNightDim = autoNightDim,
                nightDimStartHour = startHour,
                nightDimEndHour = endHour,
                nightBrightness = brightness,
                quotaWidthPercent = widthPercent,
                quotaCardDensity = density,
                clockScale = clockScale,
                clockOffsetX = clockOffsetX,
                clockOffsetY = clockOffsetY
            )
            val preview = buildString {
                append("配置版本: v1\n")
                append("时间制式: ${if (is24Hour) "24小时" else "12小时"}\n")
                append("秒钟显示: ${if (showSeconds) "开启" else "关闭"}\n")
                append("常亮策略: $screenOnPolicy\n")
                append("主题模式: $nightModeOption\n")
                append("夜间时段: ${startHour}:00 - ${endHour}:00 (亮度 ${(brightness * 100).toInt()}%)\n")
                append("看板布局: 宽度 $widthPercent%, 密度 $density\n")
                append("时钟比例: ${(clockScale * 100).toInt()}%")
            }
            SettingsValidationResult.Valid(backup, preview)
        } catch (e: Exception) {
            SettingsValidationResult.Invalid("JSON 解析失败: ${e.message}")
        }
    }

    /**
     * 将校验通过的备份配置应用至当前设置
     */
    fun applySettingsBackup(backup: GeneralSettingsBackup) {
        val current = _settingsFlow.value
        val updated = current.copy(
            is24Hour = backup.is24Hour,
            showSeconds = backup.showSeconds,
            screenOnPolicy = backup.screenOnPolicy,
            keepScreenOn = backup.screenOnPolicy != "SYSTEM",
            nightModeOption = backup.nightModeOption,
            autoNightDim = backup.autoNightDim,
            nightDimStartHour = backup.nightDimStartHour,
            nightDimEndHour = backup.nightDimEndHour,
            nightBrightness = backup.nightBrightness,
            quotaWidthPercent = backup.quotaWidthPercent,
            quotaCardDensity = backup.quotaCardDensity,
            clockScale = backup.clockScale,
            clockOffsetX = backup.clockOffsetX,
            clockOffsetY = backup.clockOffsetY
        )
        updateSettings(updated)
    }
}
