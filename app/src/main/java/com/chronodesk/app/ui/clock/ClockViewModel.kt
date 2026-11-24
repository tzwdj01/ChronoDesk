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
    var lastForegroundRefreshTimeMs: Long = 0L
        private set
    var lastQuotaSuccessTimeMs: Long = 0L
        private set
    var lastWeatherSuccessTimeMs: Long = 0L
        private set

    private var quotaRefreshJob: Job? = null
    private var weatherRefreshJob: Job? = null

    init {
        onAppForeground()
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

        // 统一前台调度：按额度、天气各自的成功时间与过期时间决定补刷；未过期直接复用，避免频繁切后台带来的请求洪峰
        val now = System.currentTimeMillis()
        val quotaIntervalMs = settings.value.autoRefreshIntervalMinutes.coerceAtLeast(1) * 60 * 1000L
        if (now - lastQuotaSuccessTimeMs >= quotaIntervalMs) {
            refreshQuota()
        }

        val weatherIntervalMs = 30 * 60 * 1000L
        if (now - lastWeatherSuccessTimeMs >= weatherIntervalMs) {
            refreshWeather()
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

        // 退后台取消在途自动任务
        quotaRefreshJob?.cancel()
        quotaRefreshJob = null
        weatherRefreshJob?.cancel()
        weatherRefreshJob = null
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
                val intervalMin = settings.value.autoRefreshIntervalMinutes.coerceAtLeast(1)
                delay(intervalMin * 60 * 1000L)
                refreshQuota()
            }
        }
    }

    private fun startWeatherScheduler() {
        weatherSchedulerJob?.cancel()
        weatherSchedulerJob = viewModelScope.launch {
            while (isActive) {
                delay(30 * 60 * 1000L) // 30 分钟轮询天气
                refreshWeather()
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

    fun refreshQuota(force: Boolean = false): Job {
        quotaRefreshJob?.let {
            if (it.isActive) return it
        }
        val job = viewModelScope.launch {
            try {
                accountRepo.refreshAll(settings.value.useMockData)
                lastQuotaSuccessTimeMs = System.currentTimeMillis()
                lastForegroundRefreshTimeMs = lastQuotaSuccessTimeMs
            } catch (_: Exception) {}
        }
        quotaRefreshJob = job
        return job
    }

    fun refreshWeather(force: Boolean = false): Job {
        weatherRefreshJob?.let {
            if (it.isActive) return it
        }
        val job = viewModelScope.launch {
            try {
                val s = settings.value
                weatherRepo.refreshAllWeather(s.getAllCities(), force = force)
                lastWeatherSuccessTimeMs = System.currentTimeMillis()
            } catch (_: Exception) {}
        }
        weatherRefreshJob = job
        return job
    }

    fun refreshAll() {
        lastForegroundRefreshTimeMs = System.currentTimeMillis()
        refreshQuota(force = true)
        refreshWeather(force = true)
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
        if (old.useMockData && !newSettings.useMockData) {
            // 退出演示模式：清除演示快照，恢复真实缓存或待同步，再触发一次真实刷新
            accountRepo.clearMockSnapshots()
            refreshQuota(force = true)
        } else if (!old.useMockData && newSettings.useMockData) {
            // 开启演示模式：立即刷新为演示数据
            refreshQuota(force = true)
        }
        if (old.cityName != newSettings.cityName ||
            old.additionalCities != newSettings.additionalCities ||
            old.latitude != newSettings.latitude ||
            old.longitude != newSettings.longitude
        ) {
            refreshWeather(force = true)
        }
    }

    fun updateAccountVisibility(
        accountId: String,
        showOnMain: Boolean,
        showOnSecond: Boolean,
        isPinnedMain: Boolean = false
    ) {
        accountRepo.updateAccountVisibility(accountId, showOnMain, showOnSecond, isPinnedMain)
    }

    fun updateAccountVisibility(
        slot: Int,
        showOnMain: Boolean,
        showOnSecond: Boolean,
        isPinnedMain: Boolean = false
    ) {
        accountRepo.updateAccountVisibility(slot, showOnMain, showOnSecond, isPinnedMain)
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

    fun saveAntigravityOAuthAccount(
        accountId: String,
        alias: String?,
        email: String?,
        refreshToken: String?,
        accessToken: String?,
        expiresInSeconds: Long = 3599L,
        idToken: String? = null,
        providerType: String = "OAUTH_PKCE"
    ): Boolean {
        val saved = accountRepo.saveAntigravityOAuthAccount(
            accountId = accountId,
            alias = alias,
            email = email,
            refreshToken = refreshToken,
            accessToken = accessToken,
            expiresInSeconds = expiresInSeconds,
            idToken = idToken,
            providerType = providerType
        )
        if (saved && (!refreshToken.isNullOrBlank() || !accessToken.isNullOrBlank())) {
            refreshAccount(accountId)
        }
        return saved
    }

    fun updateAntigravityAlias(accountId: String, alias: String) {
        val clean = alias.trim().ifBlank { "Antigravity" }
        accountRepo.updateAccountAlias(accountId, clean)
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
