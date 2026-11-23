package com.chronodesk.app.ui.tesla

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chronodesk.app.data.model.TeslaFullSnapshot
import com.chronodesk.app.data.model.TeslaSettings
import com.chronodesk.app.data.model.TeslaVehicle
import com.chronodesk.app.data.network.GrafanaDatasourceItem
import com.chronodesk.app.data.repository.TeslaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeslaViewModel @JvmOverloads constructor(
    application: Application,
    val repository: TeslaRepository = TeslaRepository(application)
) : AndroidViewModel(application) {

    val settings: StateFlow<TeslaSettings> = repository.settingsFlow
    val snapshot: StateFlow<TeslaFullSnapshot> = repository.snapshotFlow

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    private val _discoveredDatasources = MutableStateFlow<List<GrafanaDatasourceItem>>(emptyList())
    val discoveredDatasources: StateFlow<List<GrafanaDatasourceItem>> = _discoveredDatasources.asStateFlow()

    private val _discoveredCars = MutableStateFlow<List<TeslaVehicle>>(emptyList())
    val discoveredCars: StateFlow<List<TeslaVehicle>> = _discoveredCars.asStateFlow()

    fun updateSettings(newSettings: TeslaSettings, newToken: String? = null): Boolean {
        return repository.updateSettings(newSettings, newToken)
    }

    fun getStoredToken(): String? {
        return repository.getStoredToken()
    }

    fun clearConfiguration() {
        repository.clearConfiguration()
        _testResult.value = null
        _discoveredDatasources.value = emptyList()
        _discoveredCars.value = emptyList()
    }

    fun refreshNow() {
        viewModelScope.launch {
            repository.refresh()
        }
    }

    fun onAppForeground() {
        repository.onAppForeground()
    }

    fun onAppBackground() {
        repository.onAppBackground()
    }

    fun isPollingActive(): Boolean = repository.isPollingActive()

    fun setPageVisible(visible: Boolean) {
        repository.setPageVisible(visible)
    }

    fun testConnection(
        testSettings: TeslaSettings,
        testToken: String
    ) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _testResult.value = null
            try {
                // 1. 发现数据源
                val dsResult = repository.discoverDatasources(
                    baseUrl = testSettings.baseUrl,
                    token = testToken,
                    allowHttp = testSettings.allowHttp
                )
                val sources = if (dsResult.isSuccess) {
                    val list = dsResult.getOrNull() ?: emptyList()
                    _discoveredDatasources.value = list
                    list
                } else {
                    emptyList()
                }

                // 确定目标数据源 UID：如果用户已在设置框中输入则使用用户指定的；
                // 否则若自动发现中包含有效数据源，则采用首个数据源 UID
                val targetUid = if (testSettings.dataSourceUid.isNotBlank()) {
                    testSettings.dataSourceUid.trim()
                } else {
                    sources.firstOrNull()?.uid?.trim() ?: ""
                }

                if (targetUid.isBlank()) {
                    if (dsResult.isFailure) {
                        val errMsg = dsResult.exceptionOrNull()?.message ?: "未知异常"
                        _testResult.value = "数据源发现失败: $errMsg。若 Token 无数据源列表权限，请手动在下方填入 PostgreSQL 数据源 UID 后重试。"
                    } else {
                        _testResult.value = "未发现 PostgreSQL 类型数据源，请在下方手动填写数据源 UID 后重试。"
                    }
                    return@launch
                }

                // 2. 验证数据源连通性并发现车辆
                val effectiveSettings = testSettings.copy(dataSourceUid = targetUid)
                val carsResult = repository.testConnection(effectiveSettings, testToken)
                if (carsResult.isSuccess) {
                    val cars = carsResult.getOrNull() ?: emptyList()
                    _discoveredCars.value = cars
                    _testResult.value = "连接成功！找到 ${cars.size} 辆车"
                } else {
                    val err = carsResult.exceptionOrNull()?.message ?: "连接失败"
                    _testResult.value = err
                }
            } catch (e: Exception) {
                _testResult.value = e.message ?: "连接异常"
            } finally {
                _isTestingConnection.value = false
            }
        }
    }
}
