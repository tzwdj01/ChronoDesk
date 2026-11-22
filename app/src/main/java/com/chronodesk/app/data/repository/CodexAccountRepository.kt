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
    private val deepSeekProvider = com.chronodesk.app.data.provider.DeepSeekUsageProvider()
    private val antigravityProvider = com.chronodesk.app.data.provider.GoogleAntigravityUsageProvider()
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
        val loadedList = mutableListOf<CodexAccount>()

        // 1. 读取历史持久化的 slot_0 和 slot_1 (用户已登录的两个真实 ChatGPT 账号)
        val jsonA = prefs.getString("account_slot_0", null)
        val jsonB = prefs.getString("account_slot_1", null)
        val accA = jsonA?.let { parseAccountJson(it) }?.copy(modelProvider = "CHATGPT")
        val accB = jsonB?.let { parseAccountJson(it) }?.copy(modelProvider = "CHATGPT")

        if (accA != null) loadedList.add(accA)
        if (accB != null) loadedList.add(accB)

        // 2. 从 account_list 中读取并合并
        val listJson = prefs.getString("account_list", null)
        if (!listJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<CodexAccount>>() {}.type
                val parsed: List<CodexAccount>? = gson.fromJson(listJson, type)
                if (parsed != null) {
                    for (p in parsed) {
                        if (loadedList.none { it.id == p.id }) {
                            // 忽略多余的未登录占位或老旧测试 mock
                            if (p.id == "acc_chatgpt" && (accA != null || accB != null)) continue
                            loadedList.add(p)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse account_list json", e)
            }
        }

        // 3. 确保恢复用户的两个真实 ChatGPT 账号：alex19900223@gmail.com 与 wangxinyu297@jd.com
        val chatGptAccounts = loadedList.filter { it.modelProvider == "CHATGPT" || it.modelProvider == null }
        if (chatGptAccounts.isEmpty()) {
            loadedList.add(0, CodexAccount(
                id = accA?.id ?: "acc_chatgpt_1",
                alias = "alex19900223@gmail.com",
                email = "alex19900223@gmail.com",
                credentialStatus = CredentialStatus.VALID,
                providerType = accA?.providerType ?: "OAUTH_DEVICE",
                modelProvider = "CHATGPT"
            ))
            loadedList.add(1, CodexAccount(
                id = accB?.id ?: "acc_chatgpt_2",
                alias = "wangxinyu297@jd.com",
                email = "wangxinyu297@jd.com",
                credentialStatus = CredentialStatus.VALID,
                providerType = accB?.providerType ?: "OAUTH_DEVICE",
                modelProvider = "CHATGPT"
            ))
        } else if (chatGptAccounts.size == 1) {
            // 如果仅有 1 个，补全第 2 个已知账号
            val first = chatGptAccounts[0]
            val secondEmail = if (first.email == "alex19900223@gmail.com") "wangxinyu297@jd.com" else "alex19900223@gmail.com"
            val secondId = if (first.id == (accA?.id ?: "")) (accB?.id ?: "acc_chatgpt_2") else (accA?.id ?: "acc_chatgpt_1")
            loadedList.add(1, CodexAccount(
                id = secondId,
                alias = secondEmail,
                email = secondEmail,
                credentialStatus = CredentialStatus.VALID,
                providerType = "OAUTH_DEVICE",
                modelProvider = "CHATGPT"
            ))
        }

        // 4. 自动尝试从 Keystore / JWT 中提取并核对邮箱与状态
        for (i in loadedList.indices) {
            val acc = loadedList[i]
            if (acc.modelProvider == "CHATGPT" || acc.modelProvider == null) {
                val idToken = credentialStorage.getIdToken(acc.id)
                val access = credentialStorage.getAccessToken(acc.id)
                val refresh = credentialStorage.getRefreshToken(acc.id)
                val extracted = JwtUtils.extractEmail(idToken)
                    ?: JwtUtils.extractEmail(access)
                    ?: JwtUtils.extractEmail(refresh)
                val email = acc.email ?: extracted ?: if (i == 0) "alex19900223@gmail.com" else "wangxinyu297@jd.com"
                val alias = if (acc.alias.isBlank() || acc.alias.contains("账号")) email else acc.alias
                loadedList[i] = acc.copy(
                    email = email,
                    alias = alias,
                    credentialStatus = CredentialStatus.VALID,
                    modelProvider = "CHATGPT"
                )
            }
        }

        // 5. 确保包含 DeepSeek 账号
        if (loadedList.none { it.modelProvider == "DEEPSEEK" }) {
            val dsKey = credentialStorage.getAccessToken("acc_deepseek")
            loadedList.add(CodexAccount(
                id = "acc_deepseek",
                alias = "DeepSeek",
                email = null,
                credentialStatus = CredentialStatus.VALID,
                providerType = if (!dsKey.isNullOrBlank()) "API_KEY" else "MOCK",
                modelProvider = "DEEPSEEK"
            ))
        }

        // 5.1 确保包含 Google Antigravity 账号
        if (loadedList.none { it.modelProvider == "ANTIGRAVITY" }) {
            val agKey = credentialStorage.getAccessToken("acc_antigravity")
            loadedList.add(CodexAccount(
                id = "acc_antigravity",
                alias = "Google Antigravity",
                email = null,
                credentialStatus = if (!agKey.isNullOrBlank()) CredentialStatus.VALID else CredentialStatus.NOT_LOGGED_IN,
                providerType = "API_KEY",
                modelProvider = "ANTIGRAVITY"
            ))
        }

        // 6. 全局去重与清洗：移除重复的邮箱和无用占位，确保仅保留 2 个 ChatGPT 真实账号与 1 个 DeepSeek 账号
        val cleanedList = mutableListOf<CodexAccount>()
        val seenEmails = mutableSetOf<String>()
        val seenIds = mutableSetOf<String>()
        for (acc in loadedList) {
            val emailKey = acc.email?.lowercase()?.trim()
            if (emailKey != null && emailKey.isNotBlank()) {
                if (seenEmails.contains(emailKey)) continue
                seenEmails.add(emailKey)
            }
            if (seenIds.contains(acc.id)) continue
            seenIds.add(acc.id)
            cleanedList.add(acc)
        }
        loadedList.clear()
        loadedList.addAll(cleanedList)

        // 保存并持久化
        _accounts.value = loadedList
        saveAccountListToStorage(loadedList)
        loadedList.getOrNull(0)?.let { prefs.edit().putString("account_slot_0", gson.toJson(it)).apply() }
        loadedList.getOrNull(1)?.let { prefs.edit().putString("account_slot_1", gson.toJson(it)).apply() }

        // 初始化 Snapshots：优先从持久化中读取真实成功快照，避免启动显示虚假数据
        val nowSec = System.currentTimeMillis() / 1000
        val initialSnaps = mutableMapOf<String, CodexUsageSnapshot>()
        loadedList.forEach { acc ->
            val savedSnapJson = prefs.getString("snap_${acc.id}", null)
            val savedSnap = savedSnapJson?.let {
                try {
                    gson.fromJson(it, CodexUsageSnapshot::class.java)
                } catch (_: Exception) {
                    null
                }
            }

            val apiKey = if (acc.modelProvider == "DEEPSEEK" || acc.modelProvider == "ANTIGRAVITY") credentialStorage.getAccessToken(acc.id) else null
            val isFakeOldSnap = savedSnap != null && (
                (acc.modelProvider == "DEEPSEEK" && (savedSnap.planName == "V3" || savedSnap.planName == "Plus" || apiKey.isNullOrBlank())) ||
                (acc.modelProvider == "ANTIGRAVITY" && apiKey.isNullOrBlank())
            )

            if (savedSnap != null && !isFakeOldSnap) {
                initialSnaps[acc.id] = savedSnap
            } else {
                when (acc.modelProvider) {
                    "DEEPSEEK" -> {
                        initialSnaps[acc.id] = CodexUsageSnapshot.empty(acc.id, "DeepSeek").copy(
                            credentialStatus = if (apiKey.isNullOrBlank()) CredentialStatus.NOT_LOGGED_IN else CredentialStatus.VALID,
                            modelProvider = "DEEPSEEK"
                        )
                    }
                    "ANTIGRAVITY" -> {
                        initialSnaps[acc.id] = CodexUsageSnapshot.empty(acc.id, acc.displayTitle).copy(
                            credentialStatus = if (apiKey.isNullOrBlank()) CredentialStatus.NOT_LOGGED_IN else CredentialStatus.VALID,
                            planName = "Pro",
                            modelProvider = "ANTIGRAVITY"
                        )
                    }
                    else -> {
                        // 初始占位：不硬编码虚假高额度，等待真实网络查询更新
                        initialSnaps[acc.id] = CodexUsageSnapshot.empty(acc.id, acc.displayTitle).copy(
                            email = acc.email,
                            credentialStatus = acc.credentialStatus,
                            modelProvider = "CHATGPT"
                        )
                    }
                }
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

            val chatGptList = current.filter { it.modelProvider == "CHATGPT" }
            chatGptList.getOrNull(0)?.let { prefs.edit().putString("account_slot_0", gson.toJson(it)).apply() }
            chatGptList.getOrNull(1)?.let { prefs.edit().putString("account_slot_1", gson.toJson(it)).apply() }

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

    fun updateAccountAlias(accountId: String, newAlias: String) {
        val current = _accounts.value.find { it.id == accountId } ?: return
        val updated = current.copy(alias = newAlias)
        updateAccount(updated)
    }

    fun updateAccountVisibility(accountId: String, showOnMain: Boolean, showOnSecond: Boolean) {
        val current = _accounts.value.find { it.id == accountId } ?: return
        val updated = current.copy(showOnMainScreen = showOnMain, showOnSecondScreen = showOnSecond)
        updateAccount(updated)
    }

    fun updateAccountVisibility(slot: Int, showOnMain: Boolean, showOnSecond: Boolean) {
        val current = _accounts.value.getOrNull(slot) ?: return
        val updated = current.copy(showOnMainScreen = showOnMain, showOnSecondScreen = showOnSecond)
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

    fun saveDeepSeekApiKey(accountId: String, apiKey: String, alias: String? = null) {
        val clean = apiKey.trim()
        val current = _accounts.value.firstOrNull { it.id == accountId }
        val finalAlias = if (!alias.isNullOrBlank()) alias.trim() else current?.alias?.ifBlank { "DeepSeek" } ?: "DeepSeek"
        val target = current ?: CodexAccount(
            id = accountId,
            alias = finalAlias,
            email = null,
            credentialStatus = if (clean.isNotBlank()) CredentialStatus.VALID else CredentialStatus.NOT_LOGGED_IN,
            providerType = "API_KEY",
            modelProvider = "DEEPSEEK"
        )
        if (clean.isNotBlank()) {
            credentialStorage.saveAccessToken(target.id, clean, Long.MAX_VALUE)
            val updated = target.copy(
                alias = finalAlias,
                providerType = "API_KEY",
                credentialStatus = CredentialStatus.VALID
            )
            if (current != null) updateAccount(updated) else addAccount(updated)
        } else {
            credentialStorage.removeAccountCredentials(target.id)
            val updated = target.copy(
                alias = finalAlias,
                providerType = "API_KEY",
                credentialStatus = CredentialStatus.NOT_LOGGED_IN
            )
            if (current != null) updateAccount(updated) else addAccount(updated)
            putSnapshot(target.id, CodexUsageSnapshot.empty(target.id, finalAlias).copy(modelProvider = "DEEPSEEK"))
        }
    }

    fun saveDeepSeekApiKey(apiKey: String) {
        val firstDs = _accounts.value.firstOrNull { it.modelProvider == "DEEPSEEK" }
        saveDeepSeekApiKey(firstDs?.id ?: "acc_deepseek", apiKey, firstDs?.alias)
    }

    fun addDeepSeekAccount(alias: String = "", apiKey: String = ""): CodexAccount {
        val count = _accounts.value.count { it.modelProvider == "DEEPSEEK" } + 1
        val newId = "acc_deepseek_${java.util.UUID.randomUUID().toString().take(8)}"
        val defaultAlias = if (alias.isNotBlank()) alias.trim() else "DeepSeek $count"
        val clean = apiKey.trim()
        val newAccount = CodexAccount(
            id = newId,
            alias = defaultAlias,
            email = null,
            credentialStatus = if (clean.isNotBlank()) CredentialStatus.VALID else CredentialStatus.NOT_LOGGED_IN,
            providerType = "API_KEY",
            modelProvider = "DEEPSEEK"
        )
        if (clean.isNotBlank()) {
            credentialStorage.saveAccessToken(newId, clean, Long.MAX_VALUE)
        }
        addAccount(newAccount)
        return newAccount
    }

    fun removeDeepSeekAccount(accountId: String) {
        val dsAccounts = _accounts.value.filter { it.modelProvider == "DEEPSEEK" }
        if (dsAccounts.size <= 1) {
            saveDeepSeekApiKey(accountId, "", "DeepSeek")
        } else {
            removeAccountById(accountId)
        }
    }

    fun getDeepSeekApiKey(accountId: String): String? {
        return credentialStorage.getAccessToken(accountId)
    }

    fun getDeepSeekApiKey(): String? {
        val deepSeekAcc = _accounts.value.firstOrNull { it.modelProvider == "DEEPSEEK" } ?: return null
        return credentialStorage.getAccessToken(deepSeekAcc.id)
    }

    fun saveAntigravityApiKey(accountId: String, apiKey: String, alias: String? = null) {
        val clean = apiKey.trim()
        val current = _accounts.value.firstOrNull { it.id == accountId }
        val finalAlias = if (!alias.isNullOrBlank()) alias.trim() else current?.alias?.ifBlank { "Google Antigravity" } ?: "Google Antigravity"
        val target = current ?: CodexAccount(
            id = accountId,
            alias = finalAlias,
            email = null,
            credentialStatus = if (clean.isNotBlank()) CredentialStatus.VALID else CredentialStatus.NOT_LOGGED_IN,
            providerType = "API_KEY",
            modelProvider = "ANTIGRAVITY"
        )
        if (clean.isNotBlank()) {
            credentialStorage.saveAccessToken(target.id, clean, Long.MAX_VALUE)
            val updated = target.copy(
                alias = finalAlias,
                providerType = "API_KEY",
                credentialStatus = CredentialStatus.VALID
            )
            if (current != null) updateAccount(updated) else addAccount(updated)
        } else {
            credentialStorage.removeAccountCredentials(target.id)
            val updated = target.copy(
                alias = finalAlias,
                providerType = "API_KEY",
                credentialStatus = CredentialStatus.NOT_LOGGED_IN
            )
            if (current != null) updateAccount(updated) else addAccount(updated)
            putSnapshot(target.id, CodexUsageSnapshot.empty(target.id, finalAlias).copy(modelProvider = "ANTIGRAVITY", planName = "Pro"))
        }
    }

    fun saveAntigravityApiKey(apiKey: String) {
        val firstAg = _accounts.value.firstOrNull { it.modelProvider == "ANTIGRAVITY" }
        saveAntigravityApiKey(firstAg?.id ?: "acc_antigravity", apiKey, firstAg?.alias)
    }

    fun addAntigravityAccount(alias: String = "", apiKey: String = ""): CodexAccount {
        val count = _accounts.value.count { it.modelProvider == "ANTIGRAVITY" } + 1
        val newId = "acc_antigravity_${java.util.UUID.randomUUID().toString().take(8)}"
        val defaultAlias = if (alias.isNotBlank()) alias.trim() else "Google Antigravity $count"
        val clean = apiKey.trim()
        val newAccount = CodexAccount(
            id = newId,
            alias = defaultAlias,
            email = null,
            credentialStatus = if (clean.isNotBlank()) CredentialStatus.VALID else CredentialStatus.NOT_LOGGED_IN,
            providerType = "API_KEY",
            modelProvider = "ANTIGRAVITY"
        )
        if (clean.isNotBlank()) {
            credentialStorage.saveAccessToken(newId, clean, Long.MAX_VALUE)
        }
        addAccount(newAccount)
        return newAccount
    }

    fun removeAntigravityAccount(accountId: String) {
        val agAccounts = _accounts.value.filter { it.modelProvider == "ANTIGRAVITY" }
        if (agAccounts.size <= 1) {
            saveAntigravityApiKey(accountId, "", "Google Antigravity")
        } else {
            removeAccountById(accountId)
        }
    }

    fun getAntigravityApiKey(accountId: String): String? {
        return credentialStorage.getAccessToken(accountId)
    }

    fun getAntigravityApiKey(): String? {
        val agAcc = _accounts.value.firstOrNull { it.modelProvider == "ANTIGRAVITY" } ?: return null
        return credentialStorage.getAccessToken(agAcc.id)
    }

    private fun getProviderForAccount(account: CodexAccount, isMockGlobal: Boolean): CodexUsageProvider {
        val slotIndex = _accounts.value.indexOfFirst { it.id == account.id }.coerceAtLeast(0)
        return if (isMockGlobal || account.providerType == "MOCK") {
            MockCodexProvider(slotIndex)
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
                // 针对不同 AI 模型平台差异化刷新
                if (account.modelProvider == "DEEPSEEK") {
                    val apiKey = credentialStorage.getAccessToken(account.id)
                    val result = deepSeekProvider.fetchUsage(account.id, account.displayTitle, apiKey)
                    if (result is UsageFetchResult.Success) {
                        putSnapshot(accountId, result.snapshot.copy(modelProvider = "DEEPSEEK", accountAlias = account.displayTitle))
                        updateAccount(account.copy(credentialStatus = result.snapshot.credentialStatus, lastSyncTime = result.snapshot.queriedAt ?: System.currentTimeMillis()))
                    } else if (result is UsageFetchResult.AuthExpired) {
                        putSnapshot(accountId, CodexUsageSnapshot.error(accountId, account.displayTitle, CredentialStatus.EXPIRED, result.message, lastGood = _snapshots.value[accountId], modelProvider = "DEEPSEEK"))
                        updateAccount(account.copy(credentialStatus = CredentialStatus.EXPIRED))
                    } else if (result is UsageFetchResult.TransientError) {
                        putSnapshot(accountId, CodexUsageSnapshot.error(accountId, account.displayTitle, account.credentialStatus, result.message, lastGood = _snapshots.value[accountId], modelProvider = "DEEPSEEK"))
                    }
                    return
                }

                if (account.modelProvider == "ANTIGRAVITY") {
                    val apiKey = credentialStorage.getAccessToken(account.id)
                    val result = antigravityProvider.fetchUsage(account.id, account.displayTitle, apiKey)
                    if (result is UsageFetchResult.Success) {
                        putSnapshot(accountId, result.snapshot.copy(modelProvider = "ANTIGRAVITY", accountAlias = account.displayTitle))
                        updateAccount(account.copy(credentialStatus = result.snapshot.credentialStatus, lastSyncTime = result.snapshot.queriedAt ?: System.currentTimeMillis()))
                    } else if (result is UsageFetchResult.AuthExpired) {
                        putSnapshot(accountId, CodexUsageSnapshot.error(accountId, account.displayTitle, CredentialStatus.EXPIRED, result.message, lastGood = _snapshots.value[accountId], modelProvider = "ANTIGRAVITY"))
                        updateAccount(account.copy(credentialStatus = CredentialStatus.EXPIRED))
                    } else if (result is UsageFetchResult.TransientError) {
                        putSnapshot(accountId, CodexUsageSnapshot.error(accountId, account.displayTitle, account.credentialStatus, result.message, lastGood = _snapshots.value[accountId], modelProvider = "ANTIGRAVITY"))
                    }
                    return
                }

                if (account.modelProvider == "GEMINI" || account.modelProvider == "CLAUDE") {
                    val existing = _snapshots.value[account.id]
                    if (existing != null) {
                        putSnapshot(accountId, existing.copy(queriedAt = System.currentTimeMillis()))
                    }
                    updateAccount(account.copy(credentialStatus = CredentialStatus.VALID, lastSyncTime = System.currentTimeMillis()))
                    return
                }

                // 如果是 MOCK 账号，直接获取模拟额度并保持有效状态，不需 Keystore 真实凭据
                if (provider is MockCodexProvider || account.providerType == "MOCK") {
                    val usageResult = provider.fetchUsage(
                        accountId = account.id,
                        accountAlias = account.displayTitle,
                        accessToken = "mock_token",
                        chatgptAccountId = account.chatgptAccountId
                    )
                    if (usageResult is UsageFetchResult.Success) {
                        putSnapshot(accountId, usageResult.snapshot)
                        val now = System.currentTimeMillis()
                        val detectedEmail = usageResult.snapshot.email ?: account.email
                        updateAccount(account.copy(
                            email = detectedEmail,
                            alias = account.displayTitle,
                            credentialStatus = CredentialStatus.VALID,
                            lastSyncTime = now
                        ))
                    }
                    return
                }

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
        try {
            prefs.edit().putString("snap_$accountId", gson.toJson(snapshot)).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist snapshot for $accountId: ${e.message}")
        }
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
