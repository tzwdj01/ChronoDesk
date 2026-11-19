package com.chronodesk.app.ui.clock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chronodesk.app.data.model.ClockSettings
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.data.repository.ClockSettingsRepository
import com.chronodesk.app.data.repository.CodexAccountRepository
import com.chronodesk.app.data.repository.WeatherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class TimeState(
    val hour: Int = 12,
    val minute: Int = 0,
    val second: Int = 0,
    val dateString: String = "",
    val dayOfWeek: String = ""
)

class ClockViewModel @JvmOverloads constructor(
    application: Application,
    val settingsRepo: ClockSettingsRepository = ClockSettingsRepository(application),
    val weatherRepo: WeatherRepository = WeatherRepository(),
    val accountRepo: CodexAccountRepository = CodexAccountRepository(application)
) : AndroidViewModel(application) {

    private val _timeState = MutableStateFlow(getCurrentTimeState(true))
    val timeState: StateFlow<TimeState> = _timeState.asStateFlow()

    val settings = settingsRepo.settingsFlow
    val weather = weatherRepo.weatherFlow

    // 动态多账号体系
    val accounts = accountRepo.accounts
    val snapshots = accountRepo.snapshots
    val loadingMap = accountRepo.loadingMap
    val isRefreshingAll = accountRepo.isRefreshingAll

    // 兼容传统槽位接口
    val accountA = accountRepo.accountA
    val accountB = accountRepo.accountB
    val snapshotA = accountRepo.snapshotA
    val snapshotB = accountRepo.snapshotB
    val isLoadingA = accountRepo.isLoadingA
    val isLoadingB = accountRepo.isLoadingB

    private val _isNightMode = MutableStateFlow(false)
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    // 防 OLED 烧屏微幅位移 (像素)
    private val _burnInShift = MutableStateFlow(Pair(0f, 0f))
    val burnInShift: StateFlow<Pair<Float, Float>> = _burnInShift.asStateFlow()

    init {
        startTimeTicker()
        startAutoRefreshScheduler()
        startWeatherScheduler()
        startBurnInShifter()
    }

    private fun startTimeTicker() {
        viewModelScope.launch {
            while (isActive) {
                val is24 = settings.value.is24Hour
                val newState = getCurrentTimeState(is24)
                _timeState.value = newState

                // 检测是否处于夜间模式（根据当前模式及小时）
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                _isNightMode.value = settings.value.isNightTime(currentHour)

                delay(1000)
            }
        }
    }

    private fun startAutoRefreshScheduler() {
        viewModelScope.launch {
            while (isActive) {
                val intervalMin = settings.value.autoRefreshIntervalMinutes.coerceAtLeast(5)
                delay(intervalMin * 60 * 1000L)
                accountRepo.refreshAll(settings.value.useMockData)
            }
        }
    }

    private fun startWeatherScheduler() {
        viewModelScope.launch {
            while (isActive) {
                val s = settings.value
                weatherRepo.refreshWeather(s.cityName, s.latitude, s.longitude)
                delay(30 * 60 * 1000L) // 30 分钟轮询天气
            }
        }
    }

    private fun startBurnInShifter() {
        viewModelScope.launch {
            while (isActive) {
                delay(3 * 60 * 1000L) // 每 3 分钟轻微偏移 1~2 像素
                val dx = (Random.nextFloat() * 4f) - 2f
                val dy = (Random.nextFloat() * 4f) - 2f
                _burnInShift.value = Pair(dx, dy)
            }
        }
    }

    fun addAccount() {
        accountRepo.addAccount()
    }

    fun removeAccountById(accountId: String) {
        accountRepo.removeAccountById(accountId)
    }

    fun refreshAccount(accountId: String) {
        viewModelScope.launch {
            accountRepo.refreshAccount(accountId, settings.value.useMockData)
        }
    }

    fun refreshSlot(slot: Int) {
        viewModelScope.launch {
            accountRepo.refreshSlot(slot, settings.value.useMockData)
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            accountRepo.refreshAll(settings.value.useMockData)
            val s = settings.value
            weatherRepo.refreshWeather(s.cityName, s.latitude, s.longitude, force = true)
        }
    }

    fun toggleNightMode(): String {
        val next = when (settings.value.nightModeOption) {
            "AUTO" -> "FORCE_DAY"
            "FORCE_DAY" -> "FORCE_NIGHT"
            else -> "AUTO"
        }
        val updated = settings.value.copy(nightModeOption = next)
        updateSettings(updated)
        return next
    }

    fun updateSettings(newSettings: ClockSettings) {
        settingsRepo.updateSettings(newSettings)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        _isNightMode.value = newSettings.isNightTime(currentHour)
    }

    fun saveAccount(
        slot: Int,
        account: CodexAccount,
        refreshToken: String,
        accessToken: String?,
        expiresInSeconds: Long = 3600L
    ) {
        accountRepo.saveAccount(slot, account, refreshToken, accessToken, expiresInSeconds)
        refreshSlot(slot)
    }

    fun removeAccount(slot: Int) {
        accountRepo.removeAccount(slot)
    }

    fun updateAccountAlias(slot: Int, newAlias: String) {
        accountRepo.updateAccountAlias(slot, newAlias)
    }

    private fun getCurrentTimeState(is24Hour: Boolean): TimeState {
        val cal = Calendar.getInstance()
        val hour = if (is24Hour) {
            cal.get(Calendar.HOUR_OF_DAY)
        } else {
            val h = cal.get(Calendar.HOUR)
            if (h == 0) 12 else h
        }
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)

        val dateSdf = SimpleDateFormat("M月d日", Locale.CHINESE)
        val weekSdf = SimpleDateFormat("EEEE", Locale.CHINESE)
        val now = Date()

        return TimeState(
            hour = hour,
            minute = minute,
            second = second,
            dateString = dateSdf.format(now),
            dayOfWeek = weekSdf.format(now)
        )
    }
}
