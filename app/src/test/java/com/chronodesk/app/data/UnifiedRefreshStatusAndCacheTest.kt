package com.chronodesk.app.data

import android.content.Context
import android.content.SharedPreferences
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.model.QuotaSyncStatus
import com.chronodesk.app.data.model.SyncErrorType
import com.chronodesk.app.data.provider.CodexUsageProvider
import com.chronodesk.app.data.provider.DeviceCodeResult
import com.chronodesk.app.data.provider.PollResult
import com.chronodesk.app.data.provider.TokenResult
import com.chronodesk.app.data.provider.UsageFetchResult
import com.chronodesk.app.data.repository.CodexAccountRepository
import com.chronodesk.app.data.security.CredentialStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import java.util.concurrent.atomic.AtomicInteger

class UnifiedRefreshStatusAndCacheTest {

    @Test
    fun testNotConfiguredStatus() {
        val snapshot = CodexUsageSnapshot(
            accountId = "acc_1",
            accountAlias = "ChatGPT 1",
            credentialStatus = CredentialStatus.NOT_LOGGED_IN
        )
        val status = snapshot.computeStatus(
            isConfigured = false,
            isRefreshing = false
        )
        assertEquals(QuotaSyncStatus.NOT_CONFIGURED, status)
    }

    @Test
    fun testSyncingStatus() {
        val snapshot = CodexUsageSnapshot(
            accountId = "acc_1",
            accountAlias = "ChatGPT 1",
            credentialStatus = CredentialStatus.VALID,
            lastSuccessTime = System.currentTimeMillis() - 1000
        )
        val status = snapshot.computeStatus(
            isConfigured = true,
            isRefreshing = true
        )
        assertEquals(QuotaSyncStatus.SYNCING, status)
    }

    @Test
    fun testUpdatedStatusWithin2xInterval() {
        val now = System.currentTimeMillis()
        val snapshot = CodexUsageSnapshot(
            accountId = "acc_1",
            accountAlias = "ChatGPT 1",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(
                usedPercent = 20.0,
                limitWindowSeconds = 18000L,
                resetAtUnix = now / 1000 + 3600
            ),
            lastSuccessTime = now - (10 * 60 * 1000L), // 10 mins ago (< 2 * 15m)
            queriedAt = now - (10 * 60 * 1000L)
        )
        val status = snapshot.computeStatus(
            isConfigured = true,
            isRefreshing = false,
            refreshIntervalMinutes = 15,
            nowMs = now
        )
        assertEquals(QuotaSyncStatus.UPDATED, status)
    }

    @Test
    fun testCacheExpiredBeyond2xInterval() {
        val now = System.currentTimeMillis()
        val snapshot = CodexUsageSnapshot(
            accountId = "acc_1",
            accountAlias = "ChatGPT 1",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(
                usedPercent = 20.0,
                limitWindowSeconds = 18000L,
                resetAtUnix = now / 1000 + 3600
            ),
            lastSuccessTime = now - (35 * 60 * 1000L), // 35 mins ago (> 2 * 15m = 30m)
            queriedAt = now - (35 * 60 * 1000L)
        )
        val status = snapshot.computeStatus(
            isConfigured = true,
            isRefreshing = false,
            refreshIntervalMinutes = 15,
            nowMs = now
        )
        assertEquals(QuotaSyncStatus.CACHE_EXPIRED, status)
        // Numerical values must be preserved
        assertEquals(80.0, snapshot.primaryWindow?.remainingPercent!!, 0.001)
    }

    @Test
    fun testCachedStaleWhenLastErrorOccurredWithRecentGoodCache() {
        val now = System.currentTimeMillis()
        val lastGood = CodexUsageSnapshot(
            accountId = "acc_1",
            accountAlias = "ChatGPT 1",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(
                usedPercent = 10.0,
                limitWindowSeconds = 18000L,
                resetAtUnix = now / 1000 + 3600
            ),
            lastSuccessTime = now - (5 * 60 * 1000L), // 5 mins ago (< 30m)
            queriedAt = now - (5 * 60 * 1000L)
        )
        val errorSnapshot = CodexUsageSnapshot.error(
            accountId = "acc_1",
            alias = "ChatGPT 1",
            status = CredentialStatus.VALID,
            message = "网络连接超时",
            lastGood = lastGood,
            errorType = SyncErrorType.NETWORK_TIMEOUT
        )
        val status = errorSnapshot.computeStatus(
            isConfigured = true,
            isRefreshing = false,
            refreshIntervalMinutes = 15,
            nowMs = now
        )
        assertEquals(QuotaSyncStatus.CACHED_STALE, status)
        // Preserves numerical values and old success time
        assertEquals(90.0, errorSnapshot.primaryWindow?.remainingPercent!!, 0.001)
        assertEquals(lastGood.lastSuccessTime, errorSnapshot.lastSuccessTime)
    }

    @Test
    fun testAuthExpiredClearsOldQuotaNumbers() {
        val now = System.currentTimeMillis()
        val lastGood = CodexUsageSnapshot(
            accountId = "acc_1",
            accountAlias = "ChatGPT 1",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(
                usedPercent = 10.0,
                limitWindowSeconds = 18000L,
                resetAtUnix = now / 1000 + 3600
            ),
            lastSuccessTime = now - (5 * 60 * 1000L)
        )
        val errorSnapshot = CodexUsageSnapshot.error(
            accountId = "acc_1",
            alias = "ChatGPT 1",
            status = CredentialStatus.EXPIRED,
            message = "登录已过期，请重新登录",
            lastGood = lastGood,
            errorType = SyncErrorType.AUTH_FAILED
        )
        val status = errorSnapshot.computeStatus(
            isConfigured = true,
            isRefreshing = false
        )
        assertEquals(QuotaSyncStatus.AUTH_EXPIRED, status)
        // Quota numbers MUST BE cleared for untrusted expired credentials
        assertNull(errorSnapshot.primaryWindow)
        assertNull(errorSnapshot.secondaryWindow)
    }

    @Test
    fun testRateLimitedStatusAndCooldownCountdown() {
        val now = System.currentTimeMillis()
        val targetCooldown = now + 45 * 1000L // 45 seconds remaining
        val lastGood = CodexUsageSnapshot(
            accountId = "acc_1",
            accountAlias = "ChatGPT 1",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(
                usedPercent = 50.0,
                limitWindowSeconds = 18000L,
                resetAtUnix = now / 1000 + 3600
            ),
            lastSuccessTime = now - 60000L
        )
        val snapshot = CodexUsageSnapshot.error(
            accountId = "acc_1",
            alias = "ChatGPT 1",
            status = CredentialStatus.VALID,
            message = "请求过于频繁，请稍后重试",
            lastGood = lastGood,
            errorType = SyncErrorType.RATE_LIMITED,
            retryAfterUnixMs = targetCooldown
        )
        val status = snapshot.computeStatus(
            isConfigured = true,
            isRefreshing = false
        )
        assertEquals(QuotaSyncStatus.SERVICE_UNAVAILABLE, status)
        // Old numbers preserved
        assertNotNull(snapshot.primaryWindow)
        // Remaining cooldown
        val remainingSec = snapshot.getRemainingRetrySeconds(now)
        assertTrue(remainingSec in 44..45)
    }

    @Test
    fun testDataFormatErrorStatus() {
        val now = System.currentTimeMillis()
        val lastGood = CodexUsageSnapshot(
            accountId = "acc_1",
            accountAlias = "ChatGPT 1",
            credentialStatus = CredentialStatus.VALID,
            creditsBalance = 66.8,
            lastSuccessTime = now - 60000L
        )
        val snapshot = CodexUsageSnapshot.error(
            accountId = "acc_1",
            alias = "ChatGPT 1",
            status = CredentialStatus.VALID,
            message = "返回空响应",
            lastGood = lastGood,
            errorType = SyncErrorType.DATA_FORMAT
        )
        val status = snapshot.computeStatus(
            isConfigured = true,
            isRefreshing = false
        )
        assertEquals(QuotaSyncStatus.DATA_FORMAT_ERROR, status)
        assertEquals(66.8, snapshot.creditsBalance!!, 0.001)
    }

    @Test
    fun testAccountIsolation() {
        val now = System.currentTimeMillis()
        // Account A is successful
        val accA = CodexUsageSnapshot(
            accountId = "acc_A",
            accountAlias = "账号 A",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(usedPercent = 10.0, limitWindowSeconds = 18000L, resetAtUnix = now / 1000 + 3600),
            lastSuccessTime = now - 60000L,
            queriedAt = now - 60000L
        )
        // Account B failed with rate limit
        val accB = CodexUsageSnapshot.error(
            accountId = "acc_B",
            alias = "账号 B",
            status = CredentialStatus.VALID,
            message = "请求频率受限",
            errorType = SyncErrorType.RATE_LIMITED,
            retryAfterUnixMs = now + 30000L
        )

        val statusA = accA.computeStatus(isConfigured = true, isRefreshing = false)
        val statusB = accB.computeStatus(isConfigured = true, isRefreshing = false)

        assertEquals(QuotaSyncStatus.UPDATED, statusA)
        assertEquals(QuotaSyncStatus.SERVICE_UNAVAILABLE, statusB)
    }

    @Test
    fun testRepositoryRequestMerging() = runBlocking {
        val mockContext = Mockito.mock(Context::class.java)
        val mockPrefs = Mockito.mock(SharedPreferences::class.java)
        val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
        Mockito.`when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        Mockito.`when`(mockPrefs.getString(anyString(), anyString())).thenReturn(null)
        Mockito.`when`(mockPrefs.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.remove(anyString())).thenReturn(mockEditor)

        val mockStorage = Mockito.mock(CredentialStorage::class.java)
        Mockito.`when`(mockStorage.getAccessToken("acc_merge")).thenReturn("test_token")
        Mockito.`when`(mockStorage.isStorageAvailable).thenReturn(true)

        val callCount = AtomicInteger(0)
        val fakeProvider = object : CodexUsageProvider {
            override suspend fun startDeviceAuth(): DeviceCodeResult = DeviceCodeResult.Error("stub")
            override suspend fun pollDeviceAuth(deviceAuthId: String, userCode: String): PollResult = PollResult.Error("stub")
            override suspend fun exchangeToken(code: String, codeVerifier: String): TokenResult = TokenResult.ExpiredOrInvalid("stub")
            override suspend fun refreshToken(refreshToken: String): TokenResult = TokenResult.ExpiredOrInvalid("stub")
            override suspend fun fetchUsage(
                accountId: String,
                accountAlias: String,
                accessToken: String,
                chatgptAccountId: String?
            ): UsageFetchResult {
                callCount.incrementAndGet()
                delay(200) // simulate network delay
                val successSnap = CodexUsageSnapshot(
                    accountId = accountId,
                    accountAlias = accountAlias,
                    credentialStatus = CredentialStatus.VALID,
                    primaryWindow = CodexUsageWindow.fromRaw(usedPercent = 20.0, limitWindowSeconds = 18000L, resetAtUnix = System.currentTimeMillis() / 1000 + 3600),
                    lastSuccessTime = System.currentTimeMillis()
                )
                return UsageFetchResult.Success(successSnap)
            }
        }

        val repo = CodexAccountRepository(mockContext, mockStorage, fakeProvider)
        repo.addAccount(
            CodexAccount(
                id = "acc_merge",
                alias = "测试账号",
                email = "test@test.com",
                credentialStatus = CredentialStatus.VALID
            )
        )

        // Concurrently launch 5 refresh requests for the same account
        val jobs = (1..5).map {
            async {
                repo.refreshAccount("acc_merge")
            }
        }
        jobs.awaitAll()

        // Merging ensures only 1 upstream network request is triggered
        assertEquals(1, callCount.get())

        val snapshot = repo.snapshots.first()["acc_merge"]
        assertNotNull(snapshot)
        assertEquals(QuotaSyncStatus.UPDATED, snapshot?.computeStatus(isConfigured = true, isRefreshing = false))
    }

    @Test
    fun test401ControlledTokenRefreshNoLoop() = runBlocking {
        val mockContext = Mockito.mock(Context::class.java)
        val mockPrefs = Mockito.mock(SharedPreferences::class.java)
        val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
        Mockito.`when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        Mockito.`when`(mockPrefs.getString(anyString(), anyString())).thenReturn(null)
        Mockito.`when`(mockPrefs.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.remove(anyString())).thenReturn(mockEditor)

        val mockStorage = Mockito.mock(CredentialStorage::class.java)
        Mockito.`when`(mockStorage.getAccessToken("acc_auth")).thenReturn("expired_token")
        Mockito.`when`(mockStorage.getRefreshToken("acc_auth")).thenReturn("refresh_token")
        Mockito.`when`(mockStorage.isStorageAvailable).thenReturn(true)

        val refreshCallCount = AtomicInteger(0)
        val fetchCallCount = AtomicInteger(0)

        val fakeProvider = object : CodexUsageProvider {
            override suspend fun startDeviceAuth(): DeviceCodeResult = DeviceCodeResult.Error("stub")
            override suspend fun pollDeviceAuth(deviceAuthId: String, userCode: String): PollResult = PollResult.Error("stub")
            override suspend fun exchangeToken(code: String, codeVerifier: String): TokenResult = TokenResult.ExpiredOrInvalid("stub")
            override suspend fun refreshToken(refreshToken: String): TokenResult {
                refreshCallCount.incrementAndGet()
                // Refresh token exchange fails
                return TokenResult.ExpiredOrInvalid("refresh failed")
            }
            override suspend fun fetchUsage(
                accountId: String,
                accountAlias: String,
                accessToken: String,
                chatgptAccountId: String?
            ): UsageFetchResult {
                fetchCallCount.incrementAndGet()
                // Usage endpoint always returns 401
                return UsageFetchResult.AuthExpired("401 Unauthorized")
            }
        }

        val repo = CodexAccountRepository(mockContext, mockStorage, fakeProvider)
        repo.addAccount(
            CodexAccount(
                id = "acc_auth",
                alias = "测试账号",
                email = "test@test.com",
                credentialStatus = CredentialStatus.VALID
            )
        )

        repo.refreshAccount("acc_auth")

        // Verify only 1 refreshToken attempt occurs, without infinite loop
        assertEquals(1, refreshCallCount.get())
        assertEquals(1, fetchCallCount.get())

        val snapshot = repo.snapshots.first()["acc_auth"]
        assertNotNull(snapshot)
        assertEquals(QuotaSyncStatus.AUTH_EXPIRED, snapshot?.computeStatus(isConfigured = true, isRefreshing = false))
        assertEquals(CredentialStatus.EXPIRED, snapshot?.credentialStatus)
    }
}
