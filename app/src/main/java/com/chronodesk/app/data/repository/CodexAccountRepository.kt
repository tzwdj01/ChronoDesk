package com.chronodesk.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.provider.CodexUsageProvider
import com.chronodesk.app.data.provider.MockCodexProvider
import com.chronodesk.app.data.provider.RealCodexUsageProvider
import com.chronodesk.app.data.provider.TokenResult
import com.chronodesk.app.data.provider.UsageFetchResult
import com.chronodesk.app.data.security.CredentialStorage
import com.chronodesk.app.data.security.JwtUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class CodexAccountRepository(
    private val context: Context,
    private val credentialStorage: CredentialStorage = CredentialStorage(context),
    private val realProvider: CodexUsageProvider = RealCodexUsageProvider()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chronodesk_accounts_meta", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val accountMutexMap = ConcurrentHashMap<String, Mutex>()

    private val _accounts = MutableStateFlow<List<CodexAccount>>(emptyList())
    val accounts: StateFlow<List<CodexAccount>> = _accounts.asStateFlow()

    private val _snapshots = MutableStateFlow<Map<String, CodexUsageSnapshot>>(emptyMap())
    val snapshots: StateFlow<Map<String, CodexUsageSnapshot>> = _snapshots.asStateFlow()

    private val _loadingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val loadingMap: StateFlow<Map<String, Boolean>> = _loadingMap.asStateFlow()

    private val _isRefreshingAll = MutableStateFlow(false)
    val isRefreshingAll: StateFlow<Boolean> = _isRefreshingAll.asStateFlow()

    // 兼容旧接口：Slot A 与 Slot B
    val accountA: StateFlow<CodexAccount?> = _accounts.map { it.getOrNull(0) }
        .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, null)
    val accountB: StateFlow<CodexAccount?> = _accounts.map { it.getOrNull(1) }
        .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, null)

    val snapshotA: StateFlow<CodexUsageSnapshot?> = _snapshots.map { map ->
        _accounts.value.getOrNull(0)?.id?.let { map[it] }
    }.stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, null)

    val snapshotB: StateFlow<CodexUsageSnapshot?> = _snapshots.map { map ->
        _accounts.value.getOrNull(1)?.id?.let { map[it] }
    }.stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, null)

    val isLoadingA: StateFlow<Boolean> = _loadingMap.map { map ->
        _accounts.value.getOrNull(0)?.id?.let { map[it] == true } ?: false
    }.stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, false)

    val isLoadingB: StateFlow<Boolean> = _loadingMap.map { map ->
        _accounts.value.getOrNull(1)?.id?.let { map[it] == true } ?: false
    }.stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, false)

    init {
        loadAccountsFromStorage()
    }

    private fun loadAccountsFromStorage() {
        val listJson = prefs.getString("account_list", null)
        val loadedList = mutableListOf<CodexAccount>()

        if (!listJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<CodexAccount>>() {}.type
                val parsed: List<CodexAccount>? = gson.fromJson(listJson, type)
                if (parsed != null) {
                    loadedList.addAll(parsed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse account_list json", e)
            }
        } else {
            // 从老旧的 slot_0 和 slot_1 迁移数据
            val jsonA = prefs.getString("account_slot_0", null)
            val jsonB = prefs.getString("account_slot_1", null)
            val accA = jsonA?.let { parseAccountJson(it) }
            val accB = jsonB?.let { parseAccountJson(it) }
            if (accA != null) loadedList.add(accA)
            if (accB != null) loadedList.add(accB)
        }

        // 默认确保至少有两个账号槽位供展示与绑定
        while (loadedList.size < 2) {
            val defaultEmail = if (loadedList.isEmpty()) "alex19900223@gmail.com" else "wangxinyu297@jd.com"
            loadedList.add(CodexAccount(
                alias = defaultEmail,
                email = defaultEmail,
                credentialStatus = CredentialStatus.VALID,
                providerType = "MOCK"
            ))
        }

        // 自动发现邮箱与清洗旧数据（兼容从 cc-switch 或旧版迁移过来的账号）
        for (i in loadedList.indices) {
            val acc = loadedList[i]
            if (acc.email.isNullOrBlank()) {
                val emailFromId = credentialStorage.getIdToken(acc.id)?.let { JwtUtils.extractEmail(it) }
                val emailFromAccess = credentialStorage.getAccessToken(acc.id)?.let { JwtUtils.extractEmail(it) }
                val detected = emailFromId ?: emailFromAccess
                if (detected != null) {
                    loadedList[i] = acc.copy(email = detected, alias = detected)
                } else if (acc.providerType == "MOCK" || acc.alias.contains("账号") || acc.alias.isBlank()) {
                    val fallback = if (i == 0) "alex19900223@gmail.com" else "wangxinyu297@jd.com"
                    loadedList[i] = acc.copy(email = fallback, alias = fallback, providerType = "MOCK", credentialStatus = CredentialStatus.VALID)
                }
            }
        }

        _accounts.value = loadedList
        saveAccountListToStorage(loadedList)

        // 初始化 Snapshots
        val initialSnaps = mutableMapOf<String, CodexUsageSnapshot>()
        loadedList.forEach { acc ->
            if (acc.isConfigured) {
                initialSnaps[acc.id] = CodexUsageSnapshot.empty(acc.id, acc.displayTitle)
            }
        }
        _snapshots.value = initialSnaps
    }

    private fun parseAccountJson(json: String): CodexAccount? {
        return try {
            gson.fromJson(json, CodexAccount::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveAccountListToStorage(list: List<CodexAccount>) {
        val json = gson.toJson(list)
        prefs.edit().putString("account_list", json).apply()
    }

    private fun getMutex(accountId: String): Mutex {
        return accountMutexMap.computeIfAbsent(accountId) { Mutex() }
    }

    fun addAccount(account: CodexAccount = CodexAccount.createPlaceholder()) {
        val current = _accounts.value.toMutableList()
        current.add(account)
        _accounts.value = current
        saveAccountListToStorage(current)
    }

    fun removeAccountById(accountId: String) {
        credentialStorage.removeAccountCredentials(accountId)
        val current = _accounts.value.toMutableList()
        current.removeAll { it.id == accountId }

        // 如果账号清空，保留至少一个占位槽位
        if (current.isEmpty()) {
            current.add(CodexAccount.createPlaceholder())
        }
        _accounts.value = current
        saveAccountListToStorage(current)

        val snapMap = _snapshots.value.toMutableMap()
        snapMap.remove(accountId)
        _snapshots.value = snapMap
    }

    fun removeAccount(slot: Int) {
        val acc = _accounts.value.getOrNull(slot) ?: return
        // 如果列表中有多于 2 个账号，直接移除该槽位；如果只有 2 个，重置为空占位
        if (_accounts.value.size > 2) {
            removeAccountById(acc.id)
        } else {
            credentialStorage.removeAccountCredentials(acc.id)
            val current = _accounts.value.toMutableList()
            current[slot] = CodexAccount.createPlaceholder()
            _accounts.value = current
            saveAccountListToStorage(current)

            val snapMap = _snapshots.value.toMutableMap()
            snapMap.remove(acc.id)
            _snapshots.value = snapMap
        }
    }

    fun updateAccount(account: CodexAccount) {
        val current = _accounts.value.toMutableList()
        val index = current.indexOfFirst { it.id == account.id }
        if (index != -1) {
            current[index] = account
            _accounts.value = current
            saveAccountListToStorage(current)

            val currentSnap = _snapshots.value[account.id]
            if (currentSnap != null) {
                val updatedSnap = currentSnap.copy(accountAlias = account.displayTitle)
                val newMap = _snapshots.value.toMutableMap()
                newMap[account.id] = updatedSnap
                _snapshots.value = newMap
            }
        }
    }

    fun updateAccountAlias(slot: Int, newAlias: String) {
        val current = _accounts.value.getOrNull(slot) ?: return
        val updated = current.copy(alias = newAlias)
        updateAccount(updated)
    }

    fun saveAccountAtSlot(
        slot: Int,
        account: CodexAccount,
        refreshToken: String,
        accessToken: String?,
        expiresInSeconds: Long = 3600L
    ) {
        credentialStorage.saveRefreshToken(account.id, refreshToken)
        if (!accessToken.isNullOrBlank()) {
            val expiresAtMs = System.currentTimeMillis() + (expiresInSeconds * 1000)
            credentialStorage.saveAccessToken(account.id, accessToken, expiresAtMs)
        }

        // 自动发现邮箱绑定
        val detectedEmail = account.email
            ?: JwtUtils.extractEmail(accessToken)
            ?: JwtUtils.extractEmail(refreshToken)
        val finalAccount = if (detectedEmail != null) {
            account.copy(
                email = detectedEmail,
                alias = if (account.alias.isBlank() || account.alias.contains("账号")) detectedEmail else account.alias
            )
        } else {
            account
        }

        val current = _accounts.value.toMutableList()
        if (slot in current.indices) {
            current[slot] = finalAccount
        } else {
            current.add(finalAccount)
        }
        _accounts.value = current
        saveAccountListToStorage(current)

        val newMap = _snapshots.value.toMutableMap()
        newMap[finalAccount.id] = CodexUsageSnapshot.empty(finalAccount.id, finalAccount.displayTitle)
        _snapshots.value = newMap
    }

    fun saveAccount(
        slot: Int,
        account: CodexAccount,
        refreshToken: String,
        accessToken: String?,
        expiresInSeconds: Long = 3600L
    ) {
        saveAccountAtSlot(slot, account, refreshToken, accessToken, expiresInSeconds)
    }

    private fun getProviderForAccount(account: CodexAccount, isMockGlobal: Boolean): CodexUsageProvider {
        return if (isMockGlobal || account.providerType == "MOCK") {
            MockCodexProvider(account.id.hashCode())
        } else {
            realProvider
        }
    }

    suspend fun refreshAccount(accountId: String, isMockGlobal: Boolean = false) {
        val mutex = getMutex(accountId)
        mutex.withLock {
            val account = _accounts.value.firstOrNull { it.id == accountId } ?: return
            if (!account.isConfigured && !isMockGlobal && account.providerType != "MOCK") {
                return
            }

            setLoading(accountId, true)
            val provider = getProviderForAccount(account, isMockGlobal)

            try {
                // 1. 获取有效 Access Token
                var accessToken = credentialStorage.getAccessToken(account.id)
                if (accessToken.isNullOrBlank()) {
                    val refreshToken = credentialStorage.getRefreshToken(account.id)
                    if (refreshToken.isNullOrBlank()) {
                        val failedSnap = CodexUsageSnapshot.error(
                            accountId = account.id,
                            alias = account.displayTitle,
                            status = CredentialStatus.NOT_LOGGED_IN,
                            message = "未找到有效登录凭据，请重新授权",
                            lastGood = _snapshots.value[account.id]
                        )
                        putSnapshot(accountId, failedSnap)
                        updateAccount(account.copy(credentialStatus = CredentialStatus.NOT_LOGGED_IN))
                        return
                    }

                    when (val refreshResult = provider.refreshToken(refreshToken)) {
                        is TokenResult.Success -> {
                            accessToken = refreshResult.accessToken
                            credentialStorage.saveAccessToken(
                                account.id,
                                refreshResult.accessToken,
                                System.currentTimeMillis() + (refreshResult.expiresInSeconds * 1000)
                            )
                            if (!refreshResult.refreshToken.isNullOrBlank()) {
                                credentialStorage.saveRefreshToken(account.id, refreshResult.refreshToken)
                            }
                            updateAccount(account.copy(credentialStatus = CredentialStatus.VALID))
                        }
                        is TokenResult.ExpiredOrInvalid -> {
                            val failedSnap = CodexUsageSnapshot.error(
                                accountId = account.id,
                                alias = account.displayTitle,
                                status = CredentialStatus.EXPIRED,
                                message = refreshResult.message,
                                lastGood = null
                            )
                            putSnapshot(accountId, failedSnap)
                            updateAccount(account.copy(credentialStatus = CredentialStatus.EXPIRED))
                            return
                        }
                        is TokenResult.TransientError -> {
                            val failedSnap = CodexUsageSnapshot.error(
                                accountId = account.id,
                                alias = account.displayTitle,
                                status = account.credentialStatus,
                                message = refreshResult.message,
                                lastGood = _snapshots.value[account.id]
                            )
                            putSnapshot(accountId, failedSnap)
                            return
                        }
                    }
                }

                // 2. 使用 Access Token 查询额度
                val usageResult = provider.fetchUsage(
                    accountId = account.id,
                    accountAlias = account.displayTitle,
                    accessToken = accessToken,
                    chatgptAccountId = account.chatgptAccountId
                )

                when (usageResult) {
                    is UsageFetchResult.Success -> {
                        putSnapshot(accountId, usageResult.snapshot)
                        val now = System.currentTimeMillis()
                        val detectedEmail = usageResult.snapshot.email ?: account.email
                        val updatedAlias = if (account.alias.isBlank() || account.alias.contains("账号")) {
                            (detectedEmail ?: account.alias)
                        } else {
                            account.alias
                        }
                        updateAccount(account.copy(
                            email = detectedEmail,
                            alias = updatedAlias,
                            credentialStatus = CredentialStatus.VALID,
                            lastSyncTime = now
                        ))
                    }
                    is UsageFetchResult.AuthExpired -> {
                        credentialStorage.saveAccessToken(account.id, "", 0L)
                        val failedSnap = CodexUsageSnapshot.error(
                            accountId = account.id,
                            alias = account.displayTitle,
                            status = CredentialStatus.EXPIRED,
                            message = usageResult.message,
                            lastGood = null
                        )
                        putSnapshot(accountId, failedSnap)
                        updateAccount(account.copy(credentialStatus = CredentialStatus.EXPIRED))
                    }
                    is UsageFetchResult.TransientError -> {
                        val failedSnap = CodexUsageSnapshot.error(
                            accountId = account.id,
                            alias = account.displayTitle,
                            status = account.credentialStatus,
                            message = usageResult.message,
                            lastGood = _snapshots.value[account.id]
                        )
                        putSnapshot(accountId, failedSnap)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Account $accountId refresh error", e)
                val failedSnap = CodexUsageSnapshot.error(
                    accountId = account.id,
                    alias = account.displayTitle,
                    status = account.credentialStatus,
                    message = "刷新异常: ${e.message}",
                    lastGood = _snapshots.value[account.id]
                )
                putSnapshot(accountId, failedSnap)
            } finally {
                setLoading(accountId, false)
            }
        }
    }

    suspend fun refreshSlot(slot: Int, isMockGlobal: Boolean = false) {
        val acc = _accounts.value.getOrNull(slot) ?: return
        refreshAccount(acc.id, isMockGlobal)
    }

    suspend fun refreshAll(isMockGlobal: Boolean = false) {
        if (_isRefreshingAll.value) return
        _isRefreshingAll.value = true
        try {
            coroutineScope {
                _accounts.value.forEach { account ->
                    launch {
                        refreshAccount(account.id, isMockGlobal)
                    }
                }
            }
        } finally {
            _isRefreshingAll.value = false
        }
    }

    private fun putSnapshot(accountId: String, snapshot: CodexUsageSnapshot) {
        val map = _snapshots.value.toMutableMap()
        map[accountId] = snapshot
        _snapshots.value = map
    }

    private fun setLoading(accountId: String, loading: Boolean) {
        val map = _loadingMap.value.toMutableMap()
        map[accountId] = loading
        _loadingMap.value = map
    }

    companion object {
        private const val TAG = "CodexAccountRepo"
    }
}
