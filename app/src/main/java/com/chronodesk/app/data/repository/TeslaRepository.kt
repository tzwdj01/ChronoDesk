package com.chronodesk.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.chronodesk.app.data.model.TeslaConnectionState
import com.chronodesk.app.data.model.TeslaFullSnapshot
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.model.TeslaVehicle
import com.chronodesk.app.data.provider.TeslaDataProvider
import com.chronodesk.app.data.security.CredentialStorage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TeslaRepository(
    private val context: Context,
    private val provider: TeslaDataProvider = TeslaDataProvider(),
    private val credentialStorage: CredentialStorage = CredentialStorage(context),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val tag = "TeslaRepository"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chronodesk_tesla_settings", Context.MODE_PRIVATE)
    private val cachePrefs: SharedPreferences =
        context.getSharedPreferences("chronodesk_tesla_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val refreshMutex = Mutex()

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<TeslaSettings> = _settingsFlow.asStateFlow()

    private val _snapshotFlow = MutableStateFlow(loadCachedSnapshot())
    val snapshotFlow: StateFlow<TeslaFullSnapshot> = _snapshotFlow.asStateFlow()

    private var pollingJob: Job? = null
    private var isPageVisible: Boolean = false

    init {
        // 如果已配置且开启，加载后尝试刷新一次
        if (_settingsFlow.value.enabled && _settingsFlow.value.isConfigured) {
            scope.launch {
                refresh()
            }
        }
    }

    private fun loadSettings(): TeslaSettings {
        return TeslaSettings(
            enabled = prefs.getBoolean("enabled", false),
            baseUrl = prefs.getString("base_url", "") ?: "",
            dataSourceUid = prefs.getString("datasource_uid", "") ?: "",
            dataSourceType = prefs.getString("datasource_type", "postgres") ?: "postgres",
            carId = prefs.getInt("car_id", 1),
            carName = prefs.getString("car_name", "Tesla") ?: "Tesla",
            refreshIntervalSec = prefs.getInt("refresh_interval_sec", 60),
            timeZone = prefs.getString("timezone", "Asia/Shanghai") ?: "Asia/Shanghai",
            allowHttp = prefs.getBoolean("allow_http", false)
        )
    }

    fun updateSettings(newSettings: TeslaSettings, newToken: String? = null) {
        val oldSettings = _settingsFlow.value
        // 切换服务器或车辆时，清除旧缓存
        if (oldSettings.baseUrl != newSettings.baseUrl || oldSettings.carId != newSettings.carId || oldSettings.dataSourceUid != newSettings.dataSourceUid) {
            clearCachedSnapshot()
        }

        prefs.edit()
            .putBoolean("enabled", newSettings.enabled)
            .putString("base_url", newSettings.baseUrl)
            .putString("datasource_uid", newSettings.dataSourceUid)
            .putString("datasource_type", newSettings.dataSourceType)
            .putInt("car_id", newSettings.carId)
            .putString("car_name", newSettings.carName)
            .putInt("refresh_interval_sec", newSettings.refreshIntervalSec)
            .putString("timezone", newSettings.timeZone)
            .putBoolean("allow_http", newSettings.allowHttp)
            .apply()

        if (newToken != null) {
            credentialStorage.saveGrafanaToken(newToken)
        }

        _settingsFlow.value = newSettings

        // 重新调度或停止轮询
        if (newSettings.enabled && newSettings.isConfigured && isPageVisible) {
            startPolling()
        } else {
            stopPolling()
        }
    }

    fun getStoredToken(): String? {
        return credentialStorage.getGrafanaToken()
    }

    fun clearConfiguration() {
        prefs.edit().clear().apply()
        credentialStorage.clearGrafanaToken()
        clearCachedSnapshot()
        _settingsFlow.value = TeslaSettings()
        _snapshotFlow.value = TeslaFullSnapshot()
        stopPolling()
    }

    /**
     * 发现 PostgreSQL 数据源
     */
    suspend fun discoverDatasources(baseUrl: String, token: String, allowHttp: Boolean): Result<List<com.chronodesk.app.data.network.GrafanaDatasourceItem>> {
        return provider.discoverDatasources(baseUrl, token, allowHttp)
    }

    /**
     * 测试连接并发现车辆
     */
    suspend fun testConnection(testSettings: TeslaSettings, testToken: String): Result<List<TeslaVehicle>> {
        return provider.testConnectionAndDiscoverCars(testSettings, testToken)
    }

    /**
     * 执行单次数据刷新
     */
    suspend fun refresh(): Boolean {
        val currentSettings = _settingsFlow.value
        if (!currentSettings.enabled || !currentSettings.isConfigured) {
            return false
        }

        val token = credentialStorage.getGrafanaToken()
        if (token.isNullOrBlank()) {
            _snapshotFlow.value = _snapshotFlow.value.copy(
                connectionState = TeslaConnectionState(
                    isConfigured = false,
                    errorMessage = "未配置 Grafana 服务账号 Token"
                )
            )
            return false
        }

        return refreshMutex.withLock {
            try {
                _snapshotFlow.value = _snapshotFlow.value.copy(
                    connectionState = _snapshotFlow.value.connectionState.copy(isLoading = true, errorMessage = null)
                )

                val newSnapshot = provider.fetchVehicleSnapshot(
                    settings = currentSettings,
                    token = token,
                    lastSnapshot = _snapshotFlow.value
                )

                _snapshotFlow.value = newSnapshot
                saveCachedSnapshot(newSnapshot)
                true
            } catch (e: Exception) {
                Log.w(tag, "Tesla snapshot refresh failed: ${e.message}")
                _snapshotFlow.value = _snapshotFlow.value.copy(
                    connectionState = _snapshotFlow.value.connectionState.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "数据同步失败"
                    )
                )
                false
            }
        }
    }

    /**
     * 页面可见性变动通知：控制轮询开关
     */
    fun setPageVisible(visible: Boolean) {
        isPageVisible = visible
        if (visible && _settingsFlow.value.enabled && _settingsFlow.value.isConfigured) {
            startPolling()
            scope.launch {
                refresh()
            }
        } else {
            stopPolling()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (true) {
                val settings = _settingsFlow.value
                val currentState = _snapshotFlow.value.state

                // 遵循动态刷新间隔策略
                val intervalSec = when {
                    currentState.isCharging -> 30
                    currentState.isAsleep -> 180
                    else -> settings.refreshIntervalSec.coerceIn(15, 300)
                }

                delay(intervalSec * 1000L)
                if (isPageVisible && settings.enabled && settings.isConfigured) {
                    refresh()
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    // --- 缓存隔离存储 ---

    private fun cacheKey(): String {
        val s = _settingsFlow.value
        return "cache_${s.baseUrl.hashCode()}_${s.dataSourceUid}_${s.carId}"
    }

    private fun loadCachedSnapshot(): TeslaFullSnapshot {
        return try {
            val json = cachePrefs.getString(cacheKey(), null)
            if (!json.isNullOrBlank()) {
                gson.fromJson(json, TeslaFullSnapshot::class.java) ?: TeslaFullSnapshot()
            } else {
                TeslaFullSnapshot()
            }
        } catch (_: Exception) {
            TeslaFullSnapshot()
        }
    }

    private fun saveCachedSnapshot(snapshot: TeslaFullSnapshot) {
        try {
            val json = gson.toJson(snapshot)
            cachePrefs.edit().putString(cacheKey(), json).apply()
        } catch (e: Exception) {
            Log.w(tag, "Failed to cache snapshot: ${e.message}")
        }
    }

    private fun clearCachedSnapshot() {
        cachePrefs.edit().clear().apply()
    }
}
