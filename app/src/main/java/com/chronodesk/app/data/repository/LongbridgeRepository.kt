package com.chronodesk.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.chronodesk.app.data.model.LongbridgeCashItem
import com.chronodesk.app.data.model.LongbridgeConnectionState
import com.chronodesk.app.data.model.LongbridgeFullSnapshot
import com.chronodesk.app.data.model.LongbridgeIndexQuote
import com.chronodesk.app.data.model.LongbridgeMarketTemp
import com.chronodesk.app.data.model.LongbridgePositionItem
import com.chronodesk.app.data.model.LongbridgeRatePair
import com.chronodesk.app.data.model.LongbridgeSettings
import com.chronodesk.app.data.provider.LongbridgeAuthResult
import com.chronodesk.app.data.provider.LongbridgeDataProvider
import com.chronodesk.app.data.provider.LongbridgeFetchResult
import com.chronodesk.app.data.provider.LongbridgePkcePair
import com.chronodesk.app.data.security.CredentialStorage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LongbridgeOAuthLaunch(
    val clientId: String,
    val authorizeUrl: String,
    val redirectUri: String,
    val pkce: LongbridgePkcePair
)

/**
 * 长桥证券 (Longbridge) 仓库：
 * - 管理配置持久化、独立加密凭据、本地快照缓存与多币种即时重投影；
 * - 统一处理 10 分钟只读授权码兑换、OAuth 2.1 PKCE 浏览器授权、令牌无感续期与单次 401 重试；
 * - 遵循全局 Mock 演示开关：开启时零网络请求直接返回本地仿真持仓快照。
 */
class LongbridgeRepository(
    context: Context,
    private val dataProvider: LongbridgeDataProvider = LongbridgeDataProvider(),
    private val credentialStorage: CredentialStorage = CredentialStorage(context.applicationContext),
    private val externalScope: CoroutineScope? = null
) {
    private val tag = "LongbridgeRepo"
    private val appContext = context.applicationContext
    private val gson = Gson()
    private val settingsPrefs: SharedPreferences =
        appContext.getSharedPreferences("chronodesk_longbridge_settings", Context.MODE_PRIVATE)
    private val cachePrefs: SharedPreferences =
        appContext.getSharedPreferences("chronodesk_longbridge_cache", Context.MODE_PRIVATE)
    private val ownedScope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val refreshMutex = Mutex()
    private var pollingJob: Job? = null
    private var isAppForeground = false
    private var isHomePageVisible = false
    private var isLongbridgePageVisible = false
    private var lastSuccessFetchTime = 0L

    @Volatile
    private var isMockMode = false

    @Volatile
    private var lastRegisteredRedirectUri: String? = null

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<LongbridgeSettings> = _settingsFlow.asStateFlow()

    private val _snapshotFlow = MutableStateFlow(initialSnapshot())
    val snapshotFlow: StateFlow<LongbridgeFullSnapshot> = _snapshotFlow.asStateFlow()

    private fun initialSnapshot(): LongbridgeFullSnapshot {
        val cached = loadCachedSnapshot()
        val hasCred = credentialStorage.hasLongbridgeCredentials()
        return cached.reprojectToCurrency(_settingsFlow.value.displayCurrency).copy(
            connectionState = cached.connectionState.copy(
                isConfigured = hasCred,
                isLoading = false
            )
        )
    }

    private fun loadSettings(): LongbridgeSettings {
        return LongbridgeSettings(
            enabled = settingsPrefs.getBoolean("enabled", false),
            showOnHome = settingsPrefs.getBoolean("show_on_home", true),
            displayCurrency = settingsPrefs.getString("display_currency", "HKD") ?: "HKD",
            colorScheme = settingsPrefs.getString("color_scheme", "RED_UP_GREEN_DOWN") ?: "RED_UP_GREEN_DOWN",
            includeExtendedHours = settingsPrefs.getBoolean("include_extended_hours", true),
            privacyMaskAmounts = settingsPrefs.getBoolean("privacy_mask_amounts", false),
            region = settingsPrefs.getString("region", "CN") ?: "CN",
            refreshIntervalSec = settingsPrefs.getInt("refresh_interval_sec", 60).coerceIn(30, 300),
            accountAlias = settingsPrefs.getString("account_alias", "长桥证券") ?: "长桥证券",
            clientId = settingsPrefs.getString("client_id", "") ?: "",
            callbackPort = settingsPrefs.getInt("callback_port", 0)
        )
    }

    private fun saveSettings(settings: LongbridgeSettings) {
        settingsPrefs.edit()
            .putBoolean("enabled", settings.enabled)
            .putBoolean("show_on_home", settings.showOnHome)
            .putString("display_currency", settings.normalizedDisplayCurrency)
            .putString("color_scheme", if (settings.isRedUpGreenDown) "RED_UP_GREEN_DOWN" else "GREEN_UP_RED_DOWN")
            .putBoolean("include_extended_hours", settings.includeExtendedHours)
            .putBoolean("privacy_mask_amounts", settings.privacyMaskAmounts)
            .putString("region", settings.normalizedRegion)
            .putInt("refresh_interval_sec", settings.refreshIntervalSec.coerceIn(30, 300))
            .putString("account_alias", settings.accountAlias.ifBlank { "长桥证券" })
            .putString("client_id", settings.clientId)
            .putInt("callback_port", settings.callbackPort)
            .apply()
    }

    fun hasCredentials(): Boolean = isMockMode || credentialStorage.hasLongbridgeCredentials()

    fun setMockMode(enabled: Boolean) {
        if (isMockMode == enabled) return
        isMockMode = enabled
        if (enabled) {
            stopPolling()
            _snapshotFlow.value = createMockSnapshot(_settingsFlow.value)
        } else {
            _snapshotFlow.value = initialSnapshot()
            lastSuccessFetchTime = 0L
            evaluatePolling()
        }
    }

    fun updateSettings(transform: (LongbridgeSettings) -> LongbridgeSettings) {
        val old = _settingsFlow.value
        val updated = transform(old)
        _settingsFlow.value = updated
        saveSettings(updated)

        if (isMockMode) {
            _snapshotFlow.value = createMockSnapshot(updated)
            return
        }

        val current = _snapshotFlow.value
        if (old.displayCurrency != updated.displayCurrency && current.hasValidAssetData) {
            val reprojected = current.reprojectToCurrency(updated.displayCurrency)
            _snapshotFlow.value = reprojected
            saveCachedSnapshot(reprojected)
        }

        if (old.region != updated.region && !updated.enabled) {
            lastRegisteredRedirectUri = null
        }

        if (old.includeExtendedHours != updated.includeExtendedHours &&
            updated.enabled &&
            credentialStorage.hasLongbridgeCredentials()
        ) {
            ownedScope.launch {
                refresh(force = true)
            }
        }

        evaluatePolling()
    }

    fun togglePrivacyMask() {
        updateSettings { it.copy(privacyMaskAmounts = !it.privacyMaskAmounts) }
    }

    fun cycleDisplayCurrency() {
        val order = listOf("HKD", "USD", "CNY")
        updateSettings { s ->
            val idx = order.indexOf(s.normalizedDisplayCurrency)
            val next = order[(idx + 1).mod(order.size)]
            s.copy(displayCurrency = next)
        }
    }

    fun resolveConnectUrl(): String = dataProvider.resolveConnectUrl(_settingsFlow.value.normalizedRegion)

    /**
     * 方案 1：使用长桥开放平台 10 分钟一次性只读授权码兑换 Bearer Token 并启用长桥看板
     */
    suspend fun authenticateWithOneTimeCode(rawAuthCode: String): LongbridgeAuthResult {
        val region = _settingsFlow.value.normalizedRegion
        val authResult = dataProvider.exchangeOneTimeAuthCode(region, rawAuthCode)
        if (authResult is LongbridgeAuthResult.Success) {
            credentialStorage.saveLongbridgeCredentials(
                accessToken = authResult.accessToken,
                refreshToken = authResult.refreshToken,
                expiresAtMs = authResult.expiresAtMs,
                clientId = authResult.clientId
            )
            lastSuccessFetchTime = 0L
            updateSettings { it.copy(enabled = true) }
            _snapshotFlow.value = _snapshotFlow.value.copy(
                connectionState = _snapshotFlow.value.connectionState.copy(
                    isConfigured = true,
                    isAuthExpired = false,
                    errorMessage = null
                )
            )
            refresh(force = true, bypassMock = true)
        }
        return authResult
    }

    /**
     * 方案 2：动态注册公开 OAuth 2.1 PKCE 客户端并构造系统浏览器授权链接
     */
    suspend fun registerAndBuildOAuthRequest(redirectUri: String): Result<LongbridgeOAuthLaunch> {
        val settings = _settingsFlow.value
        val region = settings.normalizedRegion
        val savedClientId = credentialStorage.getLongbridgeClientId()?.takeIf { it.isNotBlank() }
            ?: settings.clientId.takeIf { it.isNotBlank() }

        val clientId = if (savedClientId != null && lastRegisteredRedirectUri == redirectUri) {
            savedClientId
        } else {
            val reg = dataProvider.registerOAuthClient(region, listOf(redirectUri))
            val resolved = reg.getOrElse { return Result.failure(it) }
            lastRegisteredRedirectUri = redirectUri
            updateSettings { it.copy(clientId = resolved) }
            resolved
        }

        val pkce = dataProvider.generatePkcePair()
        val authUrl = dataProvider.buildAuthorizationUrl(
            region = region,
            clientId = clientId,
            redirectUri = redirectUri,
            pkce = pkce
        )
        return Result.success(
            LongbridgeOAuthLaunch(
                clientId = clientId,
                authorizeUrl = authUrl,
                redirectUri = redirectUri,
                pkce = pkce
            )
        )
    }

    suspend fun completeOAuthAuthorization(
        launch: LongbridgeOAuthLaunch,
        rawCallbackOrCode: String
    ): LongbridgeAuthResult {
        val codeResult = dataProvider.extractCodeFromCallbackInput(
            rawInput = rawCallbackOrCode,
            expectedState = launch.pkce.state
        )
        val code = codeResult.getOrElse {
            return LongbridgeAuthResult.AuthError(it.message ?: "授权回调解析失败")
        }

        val region = _settingsFlow.value.normalizedRegion
        val authResult = dataProvider.exchangeCodeForToken(
            region = region,
            clientId = launch.clientId,
            code = code,
            redirectUri = launch.redirectUri,
            codeVerifier = launch.pkce.codeVerifier
        )
        if (authResult is LongbridgeAuthResult.Success) {
            credentialStorage.saveLongbridgeCredentials(
                accessToken = authResult.accessToken,
                refreshToken = authResult.refreshToken,
                expiresAtMs = authResult.expiresAtMs,
                clientId = authResult.clientId ?: launch.clientId
            )
            lastSuccessFetchTime = 0L
            updateSettings { it.copy(enabled = true, clientId = authResult.clientId ?: launch.clientId) }
            _snapshotFlow.value = _snapshotFlow.value.copy(
                connectionState = _snapshotFlow.value.connectionState.copy(
                    isConfigured = true,
                    isAuthExpired = false,
                    errorMessage = null
                )
            )
            refresh(force = true, bypassMock = true)
        }
        return authResult
    }

    /**
     * 方案 3：导入本地长桥 CLI ~/.longbridge/credentials.json 或 Bearer Token 并立即校验
     */
    suspend fun importCredentialText(rawInput: String): LongbridgeAuthResult {
        val parsed = LongbridgeDataProvider.parseCredentialInput(rawInput)
            ?: return LongbridgeAuthResult.AuthError("未识别到有效 Token 或 credentials.json 格式")

        credentialStorage.saveLongbridgeCredentials(
            accessToken = parsed.accessToken,
            refreshToken = parsed.refreshToken,
            expiresAtMs = parsed.expiresAtMs,
            clientId = parsed.clientId ?: _settingsFlow.value.clientId.ifBlank { null }
        )
        lastSuccessFetchTime = 0L
        updateSettings {
            it.copy(
                enabled = true,
                clientId = parsed.clientId ?: it.clientId
            )
        }
        _snapshotFlow.value = _snapshotFlow.value.copy(
            connectionState = _snapshotFlow.value.connectionState.copy(
                isConfigured = true,
                isAuthExpired = false,
                errorMessage = null
            )
        )

        val ok = refresh(force = true, bypassMock = true)
        val latestState = _snapshotFlow.value.connectionState
        return if (ok) {
            LongbridgeAuthResult.Success(
                accessToken = parsed.accessToken,
                refreshToken = parsed.refreshToken,
                expiresAtMs = parsed.expiresAtMs,
                clientId = parsed.clientId
            )
        } else if (latestState.isAuthExpired) {
            LongbridgeAuthResult.AuthError(latestState.errorMessage ?: "导入的凭据已失效，请重新获取")
        } else {
            LongbridgeAuthResult.NetworkError(latestState.errorMessage ?: "凭据已保存，但暂时无法连接长桥服务")
        }
    }

    fun disconnect() {
        stopPolling()
        credentialStorage.clearLongbridgeCredentials()
        clearCachedSnapshot()
        lastSuccessFetchTime = 0L
        lastRegisteredRedirectUri = null
        if (isMockMode) {
            _snapshotFlow.value = createMockSnapshot(_settingsFlow.value)
        } else {
            _snapshotFlow.value = LongbridgeFullSnapshot(
                displayCurrency = _settingsFlow.value.normalizedDisplayCurrency,
                connectionState = LongbridgeConnectionState(isConfigured = false)
            )
        }
    }

    suspend fun refresh(force: Boolean = false, bypassMock: Boolean = false): Boolean {
        val settings = _settingsFlow.value
        if (isMockMode && !bypassMock) {
            _snapshotFlow.value = createMockSnapshot(settings)
            return true
        }

        if (!credentialStorage.hasLongbridgeCredentials()) {
            _snapshotFlow.value = _snapshotFlow.value.copy(
                connectionState = _snapshotFlow.value.connectionState.copy(
                    isConfigured = false,
                    isLoading = false,
                    errorMessage = if (settings.enabled) "尚未完成长桥授权连接" else null
                )
            )
            return false
        }

        val now = System.currentTimeMillis()
        if (!force && now - lastSuccessFetchTime < 5_000L && _snapshotFlow.value.hasValidAssetData) {
            return true
        }

        return refreshMutex.withLock {
            if (isMockMode && !bypassMock) {
                _snapshotFlow.value = createMockSnapshot(_settingsFlow.value)
                return@withLock true
            }

            val latestSettings = _settingsFlow.value
            val prev = _snapshotFlow.value
            _snapshotFlow.value = prev.copy(
                connectionState = prev.connectionState.copy(
                    isConfigured = true,
                    isLoading = true,
                    errorMessage = null
                )
            )

            var accessToken = credentialStorage.getLongbridgeAccessToken()
            if (accessToken == null) {
                when (val refreshed = tryRefreshStoredToken(latestSettings)) {
                    is LongbridgeAuthResult.Success -> accessToken = refreshed.accessToken
                    is LongbridgeAuthResult.AuthError -> {
                        markAuthExpired(refreshed.message)
                        return@withLock false
                    }
                    is LongbridgeAuthResult.NetworkError -> {
                        markTransientError(refreshed.message)
                        return@withLock false
                    }
                }
            }

            val activeToken = accessToken

            when (val result = dataProvider.fetchSnapshot(activeToken, latestSettings)) {
                is LongbridgeFetchResult.Success -> {
                    val finalSnap = result.snapshot
                        .reprojectToCurrency(latestSettings.displayCurrency)
                    _snapshotFlow.value = finalSnap
                    lastSuccessFetchTime = System.currentTimeMillis()
                    saveCachedSnapshot(finalSnap)
                    true
                }

                is LongbridgeFetchResult.AuthExpired -> {
                    val hasRefresh = !credentialStorage.getLongbridgeRefreshToken().isNullOrBlank() &&
                        !(credentialStorage.getLongbridgeClientId()?.takeIf { it.isNotBlank() }
                            ?: latestSettings.clientId.takeIf { it.isNotBlank() }).isNullOrBlank()

                    if (!hasRefresh) {
                        // 若用户在误选区域（如 .cn 授权却选了国际站）触发 401，自动尝试另一节点一次
                        val altRegion = if (latestSettings.normalizedRegion == "CN") "GLOBAL" else "CN"
                        val altSettings = latestSettings.copy(region = altRegion)
                        when (val altRes = dataProvider.fetchSnapshot(activeToken, altSettings)) {
                            is LongbridgeFetchResult.Success -> {
                                _settingsFlow.value = altSettings
                                saveSettings(altSettings)
                                val finalSnap = altRes.snapshot.reprojectToCurrency(altSettings.displayCurrency)
                                _snapshotFlow.value = finalSnap
                                lastSuccessFetchTime = System.currentTimeMillis()
                                saveCachedSnapshot(finalSnap)
                                return@withLock true
                            }
                            else -> {
                                markAuthExpired(result.message)
                                return@withLock false
                            }
                        }
                    }

                    // 单次 401 无感 Refresh 重试
                    when (val refreshed = tryRefreshStoredToken(latestSettings)) {
                        is LongbridgeAuthResult.Success -> {
                            when (val retry = dataProvider.fetchSnapshot(refreshed.accessToken, latestSettings)) {
                                is LongbridgeFetchResult.Success -> {
                                    val finalSnap = retry.snapshot
                                        .reprojectToCurrency(latestSettings.displayCurrency)
                                    _snapshotFlow.value = finalSnap
                                    lastSuccessFetchTime = System.currentTimeMillis()
                                    saveCachedSnapshot(finalSnap)
                                    true
                                }

                                is LongbridgeFetchResult.AuthExpired -> {
                                    markAuthExpired(retry.message)
                                    false
                                }

                                is LongbridgeFetchResult.TransientError -> {
                                    markTransientError(retry.message)
                                    false
                                }
                            }
                        }

                        is LongbridgeAuthResult.AuthError -> {
                            markAuthExpired(result.message)
                            false
                        }

                        is LongbridgeAuthResult.NetworkError -> {
                            markTransientError(refreshed.message)
                            false
                        }
                    }
                }

                is LongbridgeFetchResult.TransientError -> {
                    markTransientError(result.message)
                    false
                }
            }
        }
    }

    private suspend fun tryRefreshStoredToken(settings: LongbridgeSettings): LongbridgeAuthResult {
        val refreshToken = credentialStorage.getLongbridgeRefreshToken()
        val clientId = credentialStorage.getLongbridgeClientId()?.takeIf { it.isNotBlank() }
            ?: settings.clientId.takeIf { it.isNotBlank() }
        if (refreshToken.isNullOrBlank() || clientId.isNullOrBlank()) {
            return LongbridgeAuthResult.AuthError("长桥访问令牌已过期，请重新授权")
        }
        val res = dataProvider.refreshAccessToken(
            region = settings.normalizedRegion,
            clientId = clientId,
            refreshToken = refreshToken
        )
        if (res is LongbridgeAuthResult.Success) {
            credentialStorage.saveLongbridgeCredentials(
                accessToken = res.accessToken,
                refreshToken = res.refreshToken ?: refreshToken,
                expiresAtMs = res.expiresAtMs,
                clientId = res.clientId ?: clientId
            )
        }
        return res
    }

    private fun markAuthExpired(message: String) {
        val current = _snapshotFlow.value
        _snapshotFlow.value = current.copy(
            connectionState = current.connectionState.copy(
                isConfigured = true,
                isLoading = false,
                isAuthExpired = true,
                errorMessage = message
            )
        )
    }

    private fun markTransientError(message: String) {
        Log.w(tag, "Longbridge refresh transient error: $message")
        val current = _snapshotFlow.value
        _snapshotFlow.value = current.copy(
            connectionState = current.connectionState.copy(
                isConfigured = credentialStorage.hasLongbridgeCredentials(),
                isLoading = false,
                errorMessage = message
            )
        )
    }

    fun onAppForeground() {
        isAppForeground = true
        evaluatePolling()
    }

    fun onAppBackground() {
        isAppForeground = false
        stopPolling()
    }

    fun setActiveVisibility(isHomePage: Boolean, isLongbridgePage: Boolean) {
        if (isHomePageVisible == isHomePage && isLongbridgePageVisible == isLongbridgePage) return
        isHomePageVisible = isHomePage
        isLongbridgePageVisible = isLongbridgePage
        evaluatePolling()
    }

    fun isPollingActive(): Boolean = pollingJob?.isActive == true

    private fun evaluatePolling() {
        if (isMockMode) {
            stopPolling()
            return
        }
        val settings = _settingsFlow.value
        val activeSurfaceVisible = isLongbridgePageVisible || (isHomePageVisible && settings.showOnHome)
        val shouldPoll = isAppForeground &&
            activeSurfaceVisible &&
            settings.enabled &&
            credentialStorage.hasLongbridgeCredentials()

        if (shouldPoll) {
            if (pollingJob == null || pollingJob?.isActive != true) {
                startPolling()
                val stale = System.currentTimeMillis() - lastSuccessFetchTime >= 15_000L
                if (stale || !_snapshotFlow.value.hasValidAssetData) {
                    ownedScope.launch {
                        refresh()
                    }
                }
            }
        } else {
            stopPolling()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = ownedScope.launch {
            while (isActive) {
                val intervalSec = _settingsFlow.value.refreshIntervalSec.coerceIn(30, 300)
                delay(intervalSec * 1000L)
                val settings = _settingsFlow.value
                val activeSurfaceVisible = isLongbridgePageVisible || (isHomePageVisible && settings.showOnHome)
                val shouldPoll = isAppForeground &&
                    !isMockMode &&
                    activeSurfaceVisible &&
                    settings.enabled &&
                    credentialStorage.hasLongbridgeCredentials()
                if (shouldPoll) {
                    refresh()
                } else {
                    break
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun cacheKey(): String {
        val settings = _settingsFlow.value
        return "lb_cache_${settings.normalizedRegion}"
    }

    private fun loadCachedSnapshot(): LongbridgeFullSnapshot {
        return try {
            val json = cachePrefs.getString(cacheKey(), null)
            if (!json.isNullOrBlank()) {
                gson.fromJson(json, LongbridgeFullSnapshot::class.java) ?: LongbridgeFullSnapshot()
            } else {
                LongbridgeFullSnapshot()
            }
        } catch (_: Exception) {
            LongbridgeFullSnapshot()
        }
    }

    private fun saveCachedSnapshot(snapshot: LongbridgeFullSnapshot) {
        try {
            val clean = snapshot.copy(
                connectionState = snapshot.connectionState.copy(isLoading = false)
            )
            cachePrefs.edit().putString(cacheKey(), gson.toJson(clean)).apply()
        } catch (e: Exception) {
            Log.w(tag, "Failed to save Longbridge cache: ${e.message}")
        }
    }

    private fun clearCachedSnapshot() {
        cachePrefs.edit().clear().apply()
    }

    private fun createMockSnapshot(settings: LongbridgeSettings): LongbridgeFullSnapshot =
        createDemoSnapshot(settings = settings, isMock = true)

    companion object {
        fun createDemoSnapshot(
            settings: LongbridgeSettings,
            isMock: Boolean = true
        ): LongbridgeFullSnapshot {
            val now = System.currentTimeMillis()
            val rates = listOf(
                LongbridgeRatePair(baseCurrency = "HKD", otherCurrency = "USD", averageRate = 7.78),
                LongbridgeRatePair(baseCurrency = "HKD", otherCurrency = "CNY", averageRate = 1.08),
                LongbridgeRatePair(baseCurrency = "USD", otherCurrency = "CNY", averageRate = 0.1388)
            )
            val positions = listOf(
                LongbridgePositionItem(
                    symbol = "0700.HK",
                    symbolName = "腾讯控股",
                    market = "HK",
                    currency = "HKD",
                    quantity = 300.0,
                    initQuantity = 300.0,
                    costPrice = 358.20,
                    lastDone = 412.60,
                    prevClose = 404.00,
                    marketValue = 123_780.0,
                    todayPl = 2_580.0,
                    todayPlRatePercent = 2.13,
                    totalPl = 16_320.0,
                    sessionTag = null
                ),
                LongbridgePositionItem(
                    symbol = "NVDA.US",
                    symbolName = "英伟达",
                    market = "US",
                    currency = "USD",
                    quantity = 90.0,
                    initQuantity = 90.0,
                    costPrice = 114.50,
                    lastDone = 142.80,
                    prevClose = 139.60,
                    marketValue = 12_852.0,
                    todayPl = 288.0,
                    todayPlRatePercent = 2.29,
                    totalPl = 2_547.0,
                    sessionTag = if (settings.includeExtendedHours) "盘后" else null
                ),
                LongbridgePositionItem(
                    symbol = "9988.HK",
                    symbolName = "阿里巴巴-W",
                    market = "HK",
                    currency = "HKD",
                    quantity = 600.0,
                    initQuantity = 600.0,
                    costPrice = 89.40,
                    lastDone = 86.25,
                    prevClose = 87.90,
                    marketValue = 51_750.0,
                    todayPl = -990.0,
                    todayPlRatePercent = -1.88,
                    totalPl = -1_890.0,
                    sessionTag = null
                ),
                LongbridgePositionItem(
                    symbol = "AAPL.US",
                    symbolName = "苹果",
                    market = "US",
                    currency = "USD",
                    quantity = 40.0,
                    initQuantity = 40.0,
                    costPrice = 201.00,
                    lastDone = 229.50,
                    prevClose = 227.10,
                    marketValue = 9_180.0,
                    todayPl = 96.0,
                    todayPlRatePercent = 1.06,
                    totalPl = 1_140.0,
                    sessionTag = if (settings.includeExtendedHours) "夜盘" else null
                ),
                LongbridgePositionItem(
                    symbol = "0388.HK",
                    symbolName = "香港交易所",
                    market = "HK",
                    currency = "HKD",
                    quantity = 100.0,
                    initQuantity = 100.0,
                    costPrice = 285.00,
                    lastDone = 312.40,
                    prevClose = 310.00,
                    marketValue = 31_240.0,
                    todayPl = 240.0,
                    todayPlRatePercent = 0.77,
                    totalPl = 2_740.0,
                    sessionTag = null
                )
            )

            val baseTodayPl = positions.sumOf { p ->
                LongbridgeFullSnapshot.convertAmount(p.todayPl ?: 0.0, p.currency, "HKD", rates)
            }
            val baseMarketValue = positions.sumOf { p ->
                LongbridgeFullSnapshot.convertAmount(p.marketValue ?: 0.0, p.currency, "HKD", rates)
            }
            val baseCash = 92_380.0
            val baseNetAssets = baseMarketValue + baseCash
            val basePrevEquity = baseNetAssets - baseTodayPl
            val basePlRate = if (basePrevEquity > 0.0) (baseTodayPl / basePrevEquity) * 100.0 else 0.0

            val baseSnapshot = LongbridgeFullSnapshot(
                accountId = "LB-DEMO-01",
                accountName = settings.accountAlias.ifBlank { "Longbridge Global" },
                dcRegion = settings.normalizedRegion,
                baseCurrency = "HKD",
                displayCurrency = "HKD",
                netAssets = baseNetAssets,
                totalCash = baseCash,
                buyPower = 198_400.0,
                marketValueTotal = baseMarketValue,
                todayPl = baseTodayPl,
                todayPlRatePercent = basePlRate,
                cumulativePl = 46_820.0,
                riskLevel = 0,
                marginCall = 0.0,
                cashDetails = listOf(
                    LongbridgeCashItem(currency = "HKD", availableCash = 48_620.0, withdrawCash = 45_000.0),
                    LongbridgeCashItem(currency = "USD", availableCash = 4_850.0, withdrawCash = 4_850.0),
                    LongbridgeCashItem(currency = "CNY", availableCash = 5_580.0, withdrawCash = 5_580.0)
                ),
                positions = positions,
                marketIndices = listOf(
                    LongbridgeIndexQuote(".IXIC.US", "纳指", "US", 19_003.65, 18_849.10, 154.55, 0.82),
                    LongbridgeIndexQuote(".SPX.US", "标普500", "US", 5_987.37, 5_966.49, 20.88, 0.35),
                    LongbridgeIndexQuote(".DJI.US", "道指", "US", 44_296.51, 43_862.30, 434.21, 0.99),
                    LongbridgeIndexQuote("HSI.HK", "恒指", "HK", 19_229.97, 19_309.14, -79.17, -0.41),
                    LongbridgeIndexQuote("HSTECH.HK", "恒科", "HK", 4_246.20, 4_283.90, -37.70, -0.88),
                    LongbridgeIndexQuote("000001.SH", "上证", "CN", 3_267.19, 3_246.41, 20.78, 0.64),
                    LongbridgeIndexQuote("000300.SH", "沪深300", "CN", 3_865.70, 3_845.70, 20.00, 0.52)
                ),
                marketTemperatures = listOf(
                    LongbridgeMarketTemp("US", 70, "Warm & rising", valuation = 59, sentiment = 82),
                    LongbridgeMarketTemp("HK", 46, "Moderate", valuation = 38, sentiment = 52),
                    LongbridgeMarketTemp("CN", 58, "Mild warm", valuation = 49, sentiment = 66)
                ),
                rawNetAssetsInBase = baseNetAssets,
                rawTotalCashInBase = baseCash,
                rawBuyPowerInBase = 198_400.0,
                rawMarketValueInBase = baseMarketValue,
                rawTodayPlInBase = baseTodayPl,
                rawCumulativePlInBase = 46_820.0,
                rawCumulativeCurrency = "HKD",
                exchangeRates = rates,
                isMock = isMock,
                connectionState = LongbridgeConnectionState(
                    isConfigured = true,
                    isLoading = false,
                    isAuthExpired = false,
                    lastSuccessTime = now
                ),
                queriedAt = now
            )
            return baseSnapshot.reprojectToCurrency(settings.displayCurrency)
        }
    }
}
