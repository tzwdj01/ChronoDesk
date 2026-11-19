package com.chronodesk.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.chronodesk.app.data.model.ClockSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
            nightModeOption = prefs.getString("night_mode_option", "AUTO") ?: "AUTO",
            autoNightDim = prefs.getBoolean("auto_night_dim", true),
            nightDimStartHour = prefs.getInt("night_start_hour", 22),
            nightDimEndHour = prefs.getInt("night_end_hour", 7),
            nightBrightness = prefs.getFloat("night_brightness", 0.08f),
            autoRefreshIntervalMinutes = prefs.getInt("refresh_interval_min", 10),
            useMockData = prefs.getBoolean("use_mock_data", false),
            autoLocation = prefs.getBoolean("auto_location", true),
            cityName = prefs.getString("city_name", "深圳") ?: "深圳",
            latitude = prefs.getFloat("city_lat", 22.5431f).toDouble(),
            longitude = prefs.getFloat("city_lng", 114.0579f).toDouble()
        )
    }

    fun updateSettings(newSettings: ClockSettings) {
        prefs.edit()
            .putBoolean("is_24_hour", newSettings.is24Hour)
            .putBoolean("show_seconds", newSettings.showSeconds)
            .putBoolean("keep_screen_on", newSettings.keepScreenOn)
            .putString("night_mode_option", newSettings.nightModeOption)
            .putBoolean("auto_night_dim", newSettings.autoNightDim)
            .putInt("night_start_hour", newSettings.nightDimStartHour)
            .putInt("night_end_hour", newSettings.nightDimEndHour)
            .putFloat("night_brightness", newSettings.nightBrightness)
            .putInt("refresh_interval_min", newSettings.autoRefreshIntervalMinutes)
            .putBoolean("use_mock_data", newSettings.useMockData)
            .putBoolean("auto_location", newSettings.autoLocation)
            .putString("city_name", newSettings.cityName)
            .putFloat("city_lat", newSettings.latitude.toFloat())
            .putFloat("city_lng", newSettings.longitude.toFloat())
            .apply()
        _settingsFlow.value = newSettings
    }
}
