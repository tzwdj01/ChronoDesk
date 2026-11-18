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
import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CodexAccountRepository(
    private val context: Context,
    private val credentialStorage: CredentialStorage = CredentialStorage(context),
    private val realProvider: CodexUsageProvider = RealCodexUsageProvider()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chronodesk_accounts_meta", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val mutexA = Mutex()
    private val mutexB = Mutex()

    private val _accountA = MutableStateFlow<CodexAccount?>(null)
    val accountA: StateFlow<CodexAccount?> = _accountA.asStateFlow()

    private val _accountB = MutableStateFlow<CodexAccount?>(null)
    val accountB: StateFlow<CodexAccount?> = _accountB.asStateFlow()

    private val _snapshotA = MutableStateFlow<CodexUsageSnapshot?>(null)
    val snapshotA: StateFlow<CodexUsageSnapshot?> = _snapshotA.asStateFlow()

    private val _snapshotB = MutableStateFlow<CodexUsageSnapshot?>(null)
    val snapshotB: StateFlow<CodexUsageSnapshot?> = _snapshotB.asStateFlow()

    private val _isLoadingA = MutableStateFlow(false)
    val isLoadingA: StateFlow<Boolean> = _isLoadingA.asStateFlow()

    private val _isLoadingB = MutableStateFlow(false)
    val isLoadingB: StateFlow<Boolean> = _isLoadingB.asStateFlow()

    private val _isRefreshingAll = MutableStateFlow(false)
    val isRefreshingAll: StateFlow<Boolean> = _isRefreshingAll.asStateFlow()

    init {
        loadAccountsFromStorage()
    }

    private fun loadAccountsFromStorage() {
        val jsonA = prefs.getString("account_slot_0", null)
        val jsonB = prefs.getString("account_slot_1", null)

        val accA = jsonA?.let { parseAccountJson(it) }
        val accB = jsonB?.let { parseAccountJson(it) }

        _accountA.value = accA
        _accountB.value = accB

        if (accA != null) {
            _snapshotA.value = CodexUsageSnapshot.empty(accA.id, accA.displayTitle)
        }
        if (accB != null) {
            _snapshotB.value = CodexUsageSnapshot.empty(accB.id, accB.displayTitle)
        }
    }

    private fun parseAccountJson(json: String): CodexAccount? {
        return try {
            gson.fromJson(json, CodexAccount::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse account json", e)
            null
        }
    }

    private fun saveAccountToStorage(slot: Int, account: CodexAccount?) {
        val key = if (slot == 0) "account_slot_0" else "account_slot_1"
        if (account == null) {
            prefs.edit().remove(key).apply()
        } else {
            val json = gson.toJson(account)
            prefs.edit().putString(key, json).apply()
        }
    }

    fun saveAccount(
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
        saveAccountToStorage(slot, account)

        if (slot == 0) {
            _accountA.value = account
            _snapshotA.value = CodexUsageSnapshot.empty(account.id, account.displayTitle)
        } else {
            _accountB.value = account
            _snapshotB.value = CodexUsageSnapshot.empty(account.id, account.displayTitle)
        }
    }

    fun removeAccount(slot: Int) {
        val account = if (slot == 0) _accountA.value else _accountB.value
        if (account != null) {
            credentialStorage.removeAccountCredentials(account.id)
        }
        saveAccountToStorage(slot, null)
        if (slot == 0) {
            _accountA.value = null
            _snapshotA.value = null
        } else {
            _accountB.value = null
            _snapshotB.value = null
        }
    }

    fun updateAccountAlias(slot: Int, newAlias: String) {
        val current = (if (slot == 0) _accountA.value else _accountB.value) ?: return
        val updated = current.copy(alias = newAlias)
        saveAccountToStorage(slot, updated)
        if (slot == 0) {
            _accountA.value = updated
            _snapshotA.value = _snapshotA.value?.copy(accountAlias = updated.displayTitle)
        } else {
            _accountB.value = updated
            _snapshotB.value = _snapshotB.value?.copy(accountAlias = updated.displayTitle)
        }
    }

    fun getProviderForSlot(slot: Int, isMockGlobal: Boolean): CodexUsageProvider {
        val account = if (slot == 0) _accountA.value else _accountB.value
        return if (isMockGlobal || account?.providerType == "MOCK") {
            MockCodexProvider(slot)
        } else {
            realProvider
        }
    }

    suspend fun refreshSlot(slot: Int, isMockGlobal: Boolean = false) {
        val mutex = if (slot == 0) mutexA else mutexB
        mutex.withLock {
            val accountFlow = if (slot == 0) _accountA else _accountB
            val snapshotFlow = if (slot == 0) _snapshotA else _snapshotB
            val loadingFlow = if (slot == 0) _isLoadingA else _isLoadingB

            val account = accountFlow.value
            if (account == null) {
                loadingFlow.value = false
                return
            }

            loadingFlow.value = true
            val provider = getProviderForSlot(slot, isMockGlobal)

            try {
                // 1. 获取有效 Access Token（优先从缓存取，若过期则使用 Refresh Token 刷新）
                var accessToken = credentialStorage.getAccessToken(account.id)
                if (accessToken.isNullOrBlank()) {
                    val refreshToken = credentialStorage.getRefreshToken(account.id)
                    if (refreshToken.isNullOrBlank()) {
                        val failedSnap = CodexUsageSnapshot.error(
                            accountId = account.id,
                            alias = account.displayTitle,
                            status = CredentialStatus.NOT_LOGGED_IN,
                            message = "未找到登录凭据，请重新登录",
                            lastGood = snapshotFlow.value
                        )
                        snapshotFlow.value = failedSnap
                        accountFlow.value = account.copy(credentialStatus = CredentialStatus.NOT_LOGGED_IN)
                        return
                    }

                    // 刷新 Token
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
                            accountFlow.value = account.copy(credentialStatus = CredentialStatus.VALID)
                        }
                        is TokenResult.ExpiredOrInvalid -> {
                            val failedSnap = CodexUsageSnapshot.error(
                                accountId = account.id,
                                alias = account.displayTitle,
                                status = CredentialStatus.EXPIRED,
                                message = refreshResult.message,
                                lastGood = null // 凭据永久失效，丢弃旧快照
                            )
                            snapshotFlow.value = failedSnap
                            accountFlow.value = account.copy(credentialStatus = CredentialStatus.EXPIRED)
                            return
                        }
                        is TokenResult.TransientError -> {
                            // 瞬时失败，保留上一次成功数据（Keep-Last-Good）
                            val failedSnap = CodexUsageSnapshot.error(
                                accountId = account.id,
                                alias = account.displayTitle,
                                status = account.credentialStatus,
                                message = refreshResult.message,
                                lastGood = snapshotFlow.value
                            )
                            snapshotFlow.value = failedSnap
                            return
                        }
                    }
                }

                // 2. 使用有效 Access Token 请求额度
                val usageResult = provider.fetchUsage(
                    accountId = account.id,
                    accountAlias = account.displayTitle,
                    accessToken = accessToken,
                    chatgptAccountId = account.chatgptAccountId
                )

                when (usageResult) {
                    is UsageFetchResult.Success -> {
                        snapshotFlow.value = usageResult.snapshot
                        val now = System.currentTimeMillis()
                        val updated = account.copy(
                            credentialStatus = CredentialStatus.VALID,
                            lastSyncTime = now
                        )
                        accountFlow.value = updated
                        saveAccountToStorage(slot, updated)
                    }
                    is UsageFetchResult.AuthExpired -> {
                        // Token 在使用端点被拒，清除 Access Token
                        credentialStorage.saveAccessToken(account.id, "", 0L)
                        val failedSnap = CodexUsageSnapshot.error(
                            accountId = account.id,
                            alias = account.displayTitle,
                            status = CredentialStatus.EXPIRED,
                            message = usageResult.message,
                            lastGood = null
                        )
                        snapshotFlow.value = failedSnap
                        val updated = account.copy(credentialStatus = CredentialStatus.EXPIRED)
                        accountFlow.value = updated
                        saveAccountToStorage(slot, updated)
                    }
                    is UsageFetchResult.TransientError -> {
                        // 保留上次成功数据
                        val failedSnap = CodexUsageSnapshot.error(
                            accountId = account.id,
                            alias = account.displayTitle,
                            status = account.credentialStatus,
                            message = usageResult.message,
                            lastGood = snapshotFlow.value
                        )
                        snapshotFlow.value = failedSnap
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Slot $slot refresh error", e)
                val failedSnap = CodexUsageSnapshot.error(
                    accountId = account.id,
                    alias = account.displayTitle,
                    status = account.credentialStatus,
                    message = "未知刷新异常: ${e.message}",
                    lastGood = snapshotFlow.value
                )
                snapshotFlow.value = failedSnap
            } finally {
                loadingFlow.value = false
            }
        }
    }

    suspend fun refreshAll(isMockGlobal: Boolean = false) {
        if (_isRefreshingAll.value) return
        _isRefreshingAll.value = true
        try {
            coroutineScope {
                launch { refreshSlot(0, isMockGlobal) }
                launch { refreshSlot(1, isMockGlobal) }
            }
        } finally {
            _isRefreshingAll.value = false
        }
    }

    companion object {
        private const val TAG = "CodexAccountRepo"
    }
}
