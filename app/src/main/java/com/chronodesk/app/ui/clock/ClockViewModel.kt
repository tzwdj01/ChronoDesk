package com.chronodesk.app.ui.clock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chronodesk.app.data.model.ClockSettings
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.model.WeatherInfo
import com.chronodesk.app.data.network.GeocodingItem
import com.chronodesk.app.data.repository.ClockSettingsRepository
import com.chronodesk.app.data.repository.CodexAccountRepository
import com.chronodesk.app.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.chronodesk.app.data.utils.LunarCalendarHelper
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
    val fullDateString: String = "",
    val dayOfWeek: String = "",
    val lunarDateString: String = ""
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
    val weatherList = weatherRepo.weatherListFlow

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

    private var timeTickerJob: Job? = null
    private var quotaSchedulerJob: Job? = null
    private var weatherSchedulerJob: Job? = null
    private var burnInShifterJob: Job? = null

    private var isForeground = false
    private var lastForegroundRefreshTimeMs: Long = 0L

    init {
        onAppForeground()
        refreshAll()
    }

    fun isPollingActive(): Boolean =
        timeTickerJob?.isActive == true && quotaSchedulerJob?.isActive == true

    fun onAppForeground() {
        if (isForeground) return
        isForeground = true
        startTimeTicker()
        startAutoRefreshScheduler()
        startWeatherScheduler()
        startBurnInShifter()

        // 重新回到前台时，若与上次静默刷新间隔超过刷新阈值，立即补齐一次最新数据
        val now = System.currentTimeMillis()
        val refreshIntervalMs = settings.value.autoRefreshIntervalMinutes.coerceAtLeast(5) * 60 * 1000L
        if (now - lastForegroundRefreshTimeMs >= refreshIntervalMs) {
            lastForegroundRefreshTimeMs = now
            refreshAll()
        }
    }

    fun onAppBackground() {
        isForeground = false
        timeTickerJob?.cancel()
        timeTickerJob = null
        quotaSchedulerJob?.cancel()
        quotaSchedulerJob = null
        weatherSchedulerJob?.cancel()
        weatherSchedulerJob = null
        burnInShifterJob?.cancel()
        burnInShifterJob = null
    }

    private fun startTimeTicker() {
        timeTickerJob?.cancel()
        timeTickerJob = viewModelScope.launch {
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
        quotaSchedulerJob?.cancel()
        quotaSchedulerJob = viewModelScope.launch {
            while (isActive) {
                val intervalMin = settings.value.autoRefreshIntervalMinutes.coerceAtLeast(5)
                delay(intervalMin * 60 * 1000L)
                accountRepo.refreshAll(settings.value.useMockData)
            }
        }
    }

    private fun startWeatherScheduler() {
        weatherSchedulerJob?.cancel()
        weatherSchedulerJob = viewModelScope.launch {
            while (isActive) {
                val s = settings.value
                weatherRepo.refreshAllWeather(s.getAllCities())
                delay(30 * 60 * 1000L) // 30 分钟轮询天气
            }
        }
    }

    private fun startBurnInShifter() {
        burnInShifterJob?.cancel()
        burnInShifterJob = viewModelScope.launch {
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
            weatherRepo.refreshAllWeather(s.getAllCities(), force = true)
        }
    }

    fun toggleNightMode(): String {
        val next = if (_isNightMode.value) {
            "FORCE_DAY"
        } else {
            "FORCE_NIGHT"
        }
        val updated = settings.value.copy(nightModeOption = next)
        updateSettings(updated)
        return next
    }

    suspend fun searchLocations(query: String): List<GeocodingItem> {
        return weatherRepo.searchLocations(query).getOrDefault(emptyList())
    }

    fun updateSettings(newSettings: ClockSettings) {
        val old = settings.value
        settingsRepo.updateSettings(newSettings)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        _isNightMode.value = newSettings.isNightTime(currentHour)
        if (old.cityName != newSettings.cityName ||
            old.additionalCities != newSettings.additionalCities ||
            old.latitude != newSettings.latitude ||
            old.longitude != newSettings.longitude
        ) {
            viewModelScope.launch {
                weatherRepo.refreshAllWeather(newSettings.getAllCities(), force = true)
            }
        }
    }

    fun updateAccountVisibility(accountId: String, showOnMain: Boolean, showOnSecond: Boolean) {
        accountRepo.updateAccountVisibility(accountId, showOnMain, showOnSecond)
    }

    fun updateAccountVisibility(slot: Int, showOnMain: Boolean, showOnSecond: Boolean) {
        accountRepo.updateAccountVisibility(slot, showOnMain, showOnSecond)
    }

    fun saveAccount(
        slot: Int,
        account: CodexAccount,
        refreshToken: String,
        accessToken: String?,
        expiresInSeconds: Long = 3600L,
        idToken: String? = null
    ): Boolean {
        val saved = accountRepo.saveAccount(slot, account, refreshToken, accessToken, expiresInSeconds, idToken)
        if (saved) {
            refreshSlot(slot)
        }
        return saved
    }

    fun removeAccount(slot: Int) {
        accountRepo.removeAccount(slot)
    }

    fun updateAccountAlias(slot: Int, newAlias: String) {
        accountRepo.updateAccountAlias(slot, newAlias)
    }

    fun updateAccountAliasById(accountId: String, newAlias: String) {
        accountRepo.updateAccountAlias(accountId, newAlias)
    }

    fun toggleShowSeconds(): Boolean {
        val newShow = !settings.value.showSeconds
        updateSettings(settings.value.copy(showSeconds = newShow))
        return newShow
    }

    fun saveDeepSeekApiKey(apiKey: String): Boolean {
        val saved = accountRepo.saveDeepSeekApiKey(apiKey)
        if (saved) {
            val deepSeekAcc = accounts.value.firstOrNull { it.modelProvider == "DEEPSEEK" }
            if (deepSeekAcc != null) {
                refreshAccount(deepSeekAcc.id)
            }
        }
        return saved
    }

    fun saveDeepSeekAccount(accountId: String, apiKey: String, alias: String): Boolean {
        val saved = accountRepo.saveDeepSeekApiKey(accountId, apiKey, alias)
        if (saved) {
            refreshAccount(accountId)
        }
        return saved
    }

    fun addDeepSeekAccount(alias: String = "", apiKey: String = ""): CodexAccount {
        val acc = accountRepo.addDeepSeekAccount(alias, apiKey)
        if (apiKey.isNotBlank() && acc.credentialStatus == CredentialStatus.VALID) {
            refreshAccount(acc.id)
        }
        return acc
    }

    fun removeDeepSeekAccount(accountId: String) {
        accountRepo.removeDeepSeekAccount(accountId)
    }

    fun getDeepSeekApiKey(accountId: String): String? {
        return accountRepo.getDeepSeekApiKey(accountId)
    }

    fun getDeepSeekApiKey(): String? {
        return accountRepo.getDeepSeekApiKey()
    }

    fun saveAntigravityAccount(accountId: String, apiKey: String, alias: String): Boolean {
        val saved = accountRepo.saveAntigravityApiKey(accountId, apiKey, alias)
        if (saved) {
            refreshAccount(accountId)
        }
        return saved
    }

    fun addAntigravityAccount(alias: String = "", apiKey: String = ""): CodexAccount {
        val acc = accountRepo.addAntigravityAccount(alias, apiKey)
        if (apiKey.isNotBlank() && acc.credentialStatus == CredentialStatus.VALID) {
            refreshAccount(acc.id)
        }
        return acc
    }

    fun removeAntigravityAccount(accountId: String) {
        accountRepo.removeAntigravityAccount(accountId)
    }

    fun getAntigravityApiKey(accountId: String): String? {
        return accountRepo.getAntigravityApiKey(accountId)
    }

    fun getAntigravityApiKey(): String? {
        return accountRepo.getAntigravityApiKey()
    }

    fun exportSettingsJson(): String {
        return settingsRepo.exportSettingsJson()
    }

    fun validateAndParseSettingsJson(json: String): com.chronodesk.app.data.repository.SettingsValidationResult {
        return settingsRepo.validateAndParseSettingsJson(json)
    }

    fun applySettingsBackup(backup: com.chronodesk.app.data.repository.GeneralSettingsBackup) {
        settingsRepo.applySettingsBackup(backup)
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
        val fullDateSdf = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE)
        val weekSdf = SimpleDateFormat("EEEE", Locale.CHINESE)
        val now = Date()
        val lunar = LunarCalendarHelper.getLunarString(cal)

        return TimeState(
            hour = hour,
            minute = minute,
            second = second,
            dateString = dateSdf.format(now),
            fullDateString = fullDateSdf.format(now),
            dayOfWeek = weekSdf.format(now),
            lunarDateString = lunar
        )
    }
}
