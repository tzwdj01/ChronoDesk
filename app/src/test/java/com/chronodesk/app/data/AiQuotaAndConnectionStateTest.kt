package com.chronodesk.app.data

import android.content.Context
import android.content.SharedPreferences
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.model.QuotaSyncStatus
import com.chronodesk.app.data.provider.CodexUsageProvider
import com.chronodesk.app.data.provider.DeepSeekUsageProvider
import com.chronodesk.app.data.provider.GoogleAntigravityUsageProvider
import com.chronodesk.app.data.provider.TokenResult
import com.chronodesk.app.data.provider.UsageFetchResult
import com.chronodesk.app.data.repository.CodexAccountRepository
import com.chronodesk.app.data.security.CredentialStorage
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import java.util.concurrent.atomic.AtomicInteger

class AiQuotaAndConnectionStateTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testDeepSeekZeroBalanceWithValidKey() = runBlocking {
        val json = """
            {
                "is_available": true,
                "balance_infos": [
                    {
                        "currency": "CNY",
                        "total_balance": "0.00",
                        "granted_balance": "0.00",
                        "topped_up_balance": "0.00"
                    }
                ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val provider = DeepSeekUsageProvider(baseUrl = server.url("/user/balance").toString())
        val result = provider.fetchUsage("ds_1", "DeepSeek", "sk-valid-key")

        assertTrue(result is UsageFetchResult.Success)
        val snapshot = (result as UsageFetchResult.Success).snapshot
        assertEquals(CredentialStatus.VALID, snapshot.credentialStatus)
        assertEquals(0.0, snapshot.creditsBalance!!, 0.001)
        assertTrue(snapshot.isAvailable == true)
        assertEquals("CNY 余额为零", snapshot.formattedBalanceText)
    }

    @Test
    fun testDeepSeekServiceUnavailable() = runBlocking {
        val json = """
            {
                "is_available": false,
                "balance_infos": [
                    {
                        "currency": "CNY",
                        "total_balance": "50.00",
                        "granted_balance": "10.00",
                        "topped_up_balance": "40.00"
                    }
                ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val provider = DeepSeekUsageProvider(baseUrl = server.url("/user/balance").toString())
        val result = provider.fetchUsage("ds_1", "DeepSeek", "sk-valid-key")

        assertTrue(result is UsageFetchResult.Success)
        val snapshot = (result as UsageFetchResult.Success).snapshot
        assertFalse(snapshot.isAvailable == true)
        assertEquals("服务不可用", snapshot.formattedBalanceText)
    }

    @Test
    fun testDeepSeekMultiCurrency() = runBlocking {
        val json = """
            {
                "is_available": true,
                "balance_infos": [
                    {
                        "currency": "CNY",
                        "total_balance": "88.50",
                        "granted_balance": "0.00",
                        "topped_up_balance": "88.50"
                    },
                    {
                        "currency": "USD",
                        "total_balance": "12.00",
                        "granted_balance": "0.00",
                        "topped_up_balance": "12.00"
                    }
                ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val provider = DeepSeekUsageProvider(baseUrl = server.url("/user/balance").toString())
        val result = provider.fetchUsage("ds_1", "DeepSeek", "sk-valid-key")

        assertTrue(result is UsageFetchResult.Success)
        val snapshot = (result as UsageFetchResult.Success).snapshot
        assertEquals(2, snapshot.balanceList?.size)
        assertTrue(snapshot.formattedBalanceText.contains("¥88.50 CNY"))
        assertTrue(snapshot.formattedBalanceText.contains("$12.00 USD"))
    }

    @Test
    fun testDeepSeekMissingBalance() = runBlocking {
        val json = """
            {
                "is_available": true
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val provider = DeepSeekUsageProvider(baseUrl = server.url("/user/balance").toString())
        val result = provider.fetchUsage("ds_1", "DeepSeek", "sk-valid-key")

        assertTrue(result is UsageFetchResult.Success)
        val snapshot = (result as UsageFetchResult.Success).snapshot
        assertNull(snapshot.creditsBalance)
        assertEquals("暂无余额数据", snapshot.formattedBalanceText)
    }

    @Test
    fun testDeepSeekInvalidKey401() = runBlocking {
        val json = """{"error": {"message": "Authentication Fails"}}"""
        server.enqueue(MockResponse().setResponseCode(401).setBody(json))

        val provider = DeepSeekUsageProvider(baseUrl = server.url("/user/balance").toString())
        val result = provider.fetchUsage("ds_1", "DeepSeek", "sk-invalid-key")

        assertTrue(result is UsageFetchResult.AuthExpired)
        val message = (result as UsageFetchResult.AuthExpired).message
        assertTrue(message.contains("401"))
    }

    @Test
    fun testAntigravityQuotaTwoStepFlow_parsesBucketsPlanAndUserAgent() = runBlocking {
        val sampleSummary = """
            {"groups": [
              {"displayName": "Gemini Models", "buckets": [
                {"bucketId": "gemini-weekly", "window": "weekly", "resetTime": "2026-10-28T07:41:54Z", "remainingFraction": 1},
                {"bucketId": "gemini-5h", "window": "5h", "resetTime": "2026-10-21T12:41:54Z", "remainingFraction": 0.5},
                {"bucketId": "gemini-argon-wtu-weekly", "window": "weekly", "resetTime": "2026-10-28T09:00:00Z", "remainingFraction": 0.6}
              ]},
              {"displayName": "Claude and GPT models", "buckets": [
                {"bucketId": "3p-weekly", "window": "weekly", "resetTime": "2026-10-28T04:42:02Z", "remainingFraction": 0.988746},
                {"bucketId": "3p-5h", "window": "5h", "resetTime": "2026-10-21T09:42:02Z", "remainingFraction": 0},
                {"bucketId": "future-bucket", "remainingFraction": 0.3}
              ]}
            ]}
        """.trimIndent()

        val recordedPaths = mutableListOf<String>()
        val recordedBodies = mutableListOf<String>()
        val recordedUAs = mutableListOf<String?>()

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                recordedPaths.add(request.path ?: "")
                recordedBodies.add(request.body.readUtf8())
                recordedUAs.add(request.getHeader("User-Agent"))
                return when {
                    request.path?.endsWith(":loadCodeAssist") == true -> MockResponse().setResponseCode(200).setBody(
                        """{"cloudaicompanionProject":"aicode-consumers","currentTier":{"id":"free-tier","name":"Antigravity"},"paidTier":{"id":"g1-ultra-tier","name":"Google AI Ultra"},"allowedTiers":[{"id":"free-tier"}]}"""
                    )
                    request.path?.endsWith(":retrieveUserQuotaSummary") == true -> MockResponse().setResponseCode(200).setBody(sampleSummary)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val provider = GoogleAntigravityUsageProvider(
            quotaApiBase = server.url("/v1internal").toString(),
            agyVersion = "1.5.14"
        )
        val result = provider.fetchUsage("acc_antigravity", "Antigravity", "ya29.valid_oauth_token", "user@example.com")

        assertTrue(result is UsageFetchResult.Success)
        val snapshot = (result as UsageFetchResult.Success).snapshot
        assertEquals(CredentialStatus.VALID, snapshot.credentialStatus)
        assertEquals("AI Ultra 20x", snapshot.planName)
        assertTrue(snapshot.isAvailable == true)
        assertTrue(snapshot.limitReached)

        // 验证请求顺序、project 传递与 User-Agent
        assertEquals(listOf("/v1internal:loadCodeAssist", "/v1internal:retrieveUserQuotaSummary"), recordedPaths)
        assertTrue(recordedBodies[0].contains("ANTIGRAVITY"))
        assertTrue(recordedBodies[1].contains("aicode-consumers"))
        assertTrue(recordedUAs.all { it?.contains("antigravity/cli/1.5.14") == true })

        // 验证 5 个有效桶与排序（跳过 unknown future-bucket）
        val tiers = snapshot.quotaWindows
        assertNotNull(tiers)
        assertEquals(
            listOf(
                CodexUsageWindow.TIER_AGY_GEMINI_FIVE_HOUR,
                CodexUsageWindow.TIER_AGY_GEMINI_WEEKLY,
                CodexUsageWindow.TIER_AGY_GEMINI_ARGON_WEEKLY,
                CodexUsageWindow.TIER_AGY_THIRD_PARTY_FIVE_HOUR,
                CodexUsageWindow.TIER_AGY_THIRD_PARTY_WEEKLY
            ),
            tiers!!.map { it.name }
        )
        assertEquals(50.0, tiers[0].remainingPercent!!, 0.01)
        assertEquals(100.0, tiers[1].remainingPercent!!, 0.01)
        assertEquals(60.0, tiers[2].remainingPercent!!, 0.01)
        assertEquals(0.0, tiers[3].remainingPercent!!, 0.01)
        assertEquals("耗尽", tiers[3].formattedRemaining())
        assertEquals(98.8746, tiers[4].remainingPercent!!, 0.01)

        // 验证组瓶颈投影：Gemini 组最紧为 5h (50%)，Claude/GPT 组最紧为 5h (0%)
        assertEquals("Gemini", snapshot.primaryWindow?.label)
        assertEquals(50.0, snapshot.primaryWindow?.remainingPercent!!, 0.01)
        assertEquals("Claude/GPT", snapshot.secondaryWindow?.label)
        assertEquals(0.0, snapshot.secondaryWindow?.remainingPercent!!, 0.01)
    }

    @Test
    fun testAntigravityIneligibleAndDegradationAndLegacyKeyRejection() = runBlocking {
        // 1. 锁区在 loadCodeAssist 一步直接返回 AccessDenied 并保留 Google 原因，不发第 2 次请求
        val callCount = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                callCount.incrementAndGet()
                return MockResponse().setResponseCode(200).setBody(
                    """{"currentTier":{"id":"standard-tier"},"paidTier":{"id":"g1-ultra-tier","name":"Google AI Ultra"},"allowedTiers":[{"id":"standard-tier"}],"ineligibleTiers":[{"tierId":"free-tier","reasonCode":"UNSUPPORTED_LOCATION","reasonMessage":"Not available in your location."}]}"""
                )
            }
        }
        val provider = GoogleAntigravityUsageProvider(quotaApiBase = server.url("/v1internal").toString())
        val ineligibleRes = provider.fetchUsage("ag_1", "Antigravity", "ya29.valid")
        assertTrue(ineligibleRes is UsageFetchResult.AccessDenied)
        assertEquals("Not available in your location.", (ineligibleRes as UsageFetchResult.AccessDenied).message)
        assertEquals(1, callCount.get())

        // 2. loadCodeAssist 500 降级为不带项目直查 retrieveUserQuotaSummary
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.endsWith(":loadCodeAssist") == true -> MockResponse().setResponseCode(500).setBody("oops")
                    request.path?.endsWith(":retrieveUserQuotaSummary") == true -> MockResponse().setResponseCode(200).setBody(
                        """{"groups":[{"buckets":[{"bucketId":"3p-5h","remainingFraction":0.75,"resetTime":"2026-10-21T09:42:02Z"}]}]}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        val degradedRes = provider.fetchUsage("ag_1", "Antigravity", "ya29.valid")
        assertTrue(degradedRes is UsageFetchResult.Success)
        val degSnap = (degradedRes as UsageFetchResult.Success).snapshot
        assertNull(degSnap.planName)
        assertEquals(75.0, degSnap.quotaWindows?.single()?.remainingPercent!!, 0.01)

        // 3. 缺失 remainingFraction 的桶不得当成满额
        assertNull(
            GoogleAntigravityUsageProvider.parseSummaryTiers(
                """{"groups":[{"buckets":[{"bucketId":"gemini-5h"}]}]}"""
            )
        )

        // 4. 旧版 Gemini API Key (AIza...) 直接拒绝
        val legacyKeyRes = provider.fetchUsage("ag_1", "Antigravity", "AIzaSyOldGeminiKey")
        assertTrue(legacyKeyRes is UsageFetchResult.AuthExpired)
    }

    @Test
    fun testAntigravityPlanFormattingPkceAndCredentialParsing() {
        assertEquals(
            "AI Pro",
            GoogleAntigravityUsageProvider.codeAssistPlan("""{"currentTier":{"id":"standard-tier"},"paidTier":{"id":"g1-pro-tier","name":"Google AI Pro"}}""")
        )
        assertEquals(
            "AI Ultra 5x",
            GoogleAntigravityUsageProvider.codeAssistPlan("""{"paidTier":{"id":"g1-ultra-lite-tier","name":"Google AI Ultra"}}""")
        )
        assertEquals(
            "AI Ultra 20x",
            GoogleAntigravityUsageProvider.codeAssistPlan("""{"paidTier":{"id":"g1-ultra-tier","name":"Google AI Ultra"}}""")
        )
        assertEquals(
            "Google AI",
            GoogleAntigravityUsageProvider.codeAssistPlan("""{"paidTier":{"id":"other","name":"Google AI"}}""")
        )
        assertEquals(
            "Free",
            GoogleAntigravityUsageProvider.codeAssistPlan("""{"currentTier":{"id":"free-tier"}}""")
        )

        // PKCE 与回调解析
        val provider = GoogleAntigravityUsageProvider()
        val pkce = provider.generatePkcePair()
        assertEquals(43, pkce.verifier.length)
        val url = provider.authorizationUrl(provider.redirectUri(51234), pkce)
        assertTrue(url.contains("client_id=${GoogleAntigravityUsageProvider.CLIENT_ID}"))
        assertTrue(url.contains("code_challenge=${pkce.challenge}"))

        val okCode = provider.extractCodeFromCallbackInput(
            "http://127.0.0.1:51234/oauth-callback?code=auth_123&state=${pkce.state}",
            expectedState = pkce.state
        )
        assertEquals("auth_123", okCode.getOrNull())
        assertTrue(
            provider.extractCodeFromCallbackInput(
                "http://127.0.0.1:51234/oauth-callback?code=x&state=wrong",
                expectedState = pkce.state
            ).isFailure
        )

        // 凭据 JSON 解析（agy 钥匙串结构 + 裸 Refresh Token）
        val agyJson = """{"token":{"access_token":"ya29.abc","refresh_token":"1//ref","expires_in":3599},"email":"dev@example.com"}"""
        val parsed = GoogleAntigravityUsageProvider.parseCredentialInput(agyJson)
        assertNotNull(parsed)
        assertEquals("ya29.abc", parsed?.accessToken)
        assertEquals("1//ref", parsed?.refreshToken)
        assertEquals("dev@example.com", parsed?.email)
        assertNull(GoogleAntigravityUsageProvider.parseCredentialInput("AIzaSyBadLegacyKey"))
    }

    @Test
    fun testAntigravity401ControlledRefreshAndSameEmailAcrossProviders() = runBlocking {
        // 1. 同一个邮箱在 CHATGPT 和 ANTIGRAVITY 之间共存不被误删
        val mockContext = Mockito.mock(Context::class.java)
        val mockPrefs = Mockito.mock(SharedPreferences::class.java)
        val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
        Mockito.`when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        Mockito.`when`(mockPrefs.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.remove(anyString())).thenReturn(mockEditor)

        val sharedEmail = "same.user@gmail.com"
        val gptAcc = CodexAccount(
            id = "acc_chatgpt_1",
            alias = "主 ChatGPT",
            email = sharedEmail,
            credentialStatus = CredentialStatus.VALID,
            providerType = "OAUTH_DEVICE",
            modelProvider = "CHATGPT"
        )
        val agyAcc = CodexAccount(
            id = "acc_antigravity",
            alias = "Antigravity",
            email = sharedEmail,
            credentialStatus = CredentialStatus.VALID,
            providerType = "OAUTH_PKCE",
            modelProvider = "ANTIGRAVITY"
        )
        Mockito.`when`(mockPrefs.getString("account_list", null)).thenReturn(Gson().toJson(listOf(gptAcc, agyAcc)))
        Mockito.`when`(mockPrefs.getString("account_slot_0", null)).thenReturn(Gson().toJson(gptAcc))

        val mockStorage = Mockito.mock(CredentialStorage::class.java)
        Mockito.`when`(mockStorage.isStorageAvailable).thenReturn(true)
        Mockito.`when`(mockStorage.getAccessToken("acc_chatgpt_1")).thenReturn("gpt_at")
        Mockito.`when`(mockStorage.getRefreshToken("acc_chatgpt_1")).thenReturn("gpt_rt")
        Mockito.`when`(mockStorage.getAccessToken("acc_antigravity")).thenReturn("expired_agy_at")
        Mockito.`when`(mockStorage.getRefreshToken("acc_antigravity")).thenReturn("agy_rt")

        val refreshCount = AtomicInteger(0)
        val fetchCount = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.contains("/token") == true -> {
                        refreshCount.incrementAndGet()
                        MockResponse().setResponseCode(200).setBody(
                            """{"access_token":"ya29.refreshed_token","expires_in":3599,"token_type":"Bearer"}"""
                        )
                    }
                    request.path?.endsWith(":loadCodeAssist") == true -> {
                        val auth = request.getHeader("Authorization")
                        if (auth == "Bearer expired_agy_at") {
                            MockResponse().setResponseCode(401)
                        } else {
                            MockResponse().setResponseCode(200).setBody(
                                """{"currentTier":{"id":"free-tier"},"paidTier":{"id":"g1-pro-tier","name":"Google AI Pro"}}"""
                            )
                        }
                    }
                    request.path?.endsWith(":retrieveUserQuotaSummary") == true -> {
                        fetchCount.incrementAndGet()
                        MockResponse().setResponseCode(200).setBody(
                            """{"groups":[{"buckets":[{"bucketId":"gemini-5h","remainingFraction":0.9,"resetTime":"2026-10-21T12:41:54Z"}]}]}"""
                        )
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val realAgyProvider = GoogleAntigravityUsageProvider(
            quotaApiBase = server.url("/v1internal").toString(),
            tokenEndpoint = server.url("/token").toString(),
            userInfoEndpoint = server.url("/userinfo").toString()
        )
        val repo = CodexAccountRepository(
            context = mockContext,
            credentialStorage = mockStorage,
            realProvider = Mockito.mock(CodexUsageProvider::class.java),
            deepSeekProvider = Mockito.mock(DeepSeekUsageProvider::class.java),
            antigravityProvider = realAgyProvider
        )

        // 验证同邮箱跨平台账号均保留
        assertTrue(repo.accounts.value.any { it.modelProvider == "CHATGPT" && it.email == sharedEmail })
        assertTrue(repo.accounts.value.any { it.modelProvider == "ANTIGRAVITY" && it.email == sharedEmail })

        // 触发 Antigravity 401 -> 受控刷新一次 -> 重试拿到额度
        repo.refreshAccount("acc_antigravity")
        assertEquals(1, refreshCount.get())
        assertEquals(1, fetchCount.get())
        val snap = repo.snapshots.value["acc_antigravity"]
        assertNotNull(snap)
        assertEquals(QuotaSyncStatus.UPDATED, snap?.computeStatus(isConfigured = true, isRefreshing = false))
        assertEquals("AI Pro", snap?.planName)
        assertEquals(90.0, snap?.quotaWindows?.firstOrNull()?.remainingPercent!!, 0.01)
    }

    @Test
    fun testSaveAntigravityOAuthAutoShowsUnconfiguredHiddenAccountAndPreservesManualHideAfterwards() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockPrefs = Mockito.mock(SharedPreferences::class.java)
        val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
        Mockito.`when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        Mockito.`when`(mockPrefs.edit()).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        Mockito.`when`(mockEditor.remove(anyString())).thenReturn(mockEditor)

        val hiddenLegacyAcc = CodexAccount(
            id = "acc_antigravity",
            alias = "Antigravity",
            email = null,
            credentialStatus = CredentialStatus.NOT_LOGGED_IN,
            providerType = "OAUTH_PKCE",
            modelProvider = "ANTIGRAVITY",
            showOnMainScreen = false,
            showOnSecondScreen = false
        )
        Mockito.`when`(mockPrefs.getString("account_list", null)).thenReturn(Gson().toJson(listOf(hiddenLegacyAcc)))

        val mockStorage = Mockito.mock(CredentialStorage::class.java)
        Mockito.`when`(mockStorage.isStorageAvailable).thenReturn(true)
        Mockito.`when`(
            mockStorage.saveCodexCredentials(
                anyString(),
                anyString(),
                Mockito.nullable(String::class.java),
                Mockito.anyLong(),
                Mockito.nullable(String::class.java)
            )
        ).thenReturn(true)

        val repo = CodexAccountRepository(
            context = mockContext,
            credentialStorage = mockStorage,
            realProvider = Mockito.mock(CodexUsageProvider::class.java),
            deepSeekProvider = Mockito.mock(DeepSeekUsageProvider::class.java),
            antigravityProvider = Mockito.mock(GoogleAntigravityUsageProvider::class.java)
        )

        val before = repo.accounts.value.first { it.id == "acc_antigravity" }
        assertFalse(before.isVisibleOnMain)
        assertFalse(before.isVisibleOnSecond)

        // 首次授权从 NOT_LOGGED_IN -> VALID 时，自动打开首页与详情页展示
        assertTrue(
            repo.saveAntigravityOAuthAccount(
                accountId = "acc_antigravity",
                alias = "Antigravity",
                email = "user@gmail.com",
                refreshToken = "1//new_rt",
                accessToken = "ya29.new_at"
            )
        )
        val afterFirstAuth = repo.accounts.value.first { it.id == "acc_antigravity" }
        assertTrue(afterFirstAuth.isVisibleOnMain)
        assertTrue(afterFirstAuth.isVisibleOnSecond)

        // 若用户后续主动隐藏首页展示，再次更新 Token 时应尊重用户选择
        repo.updateAccountVisibility(
            accountId = "acc_antigravity",
            showOnMain = false,
            showOnSecond = true
        )
        assertFalse(repo.accounts.value.first { it.id == "acc_antigravity" }.isVisibleOnMain)

        assertTrue(
            repo.saveAntigravityOAuthAccount(
                accountId = "acc_antigravity",
                alias = "Antigravity",
                email = "user@gmail.com",
                refreshToken = "1//rotated_rt",
                accessToken = "ya29.rotated_at"
            )
        )
        val afterUpdate = repo.accounts.value.first { it.id == "acc_antigravity" }
        assertFalse(afterUpdate.isVisibleOnMain)
        assertTrue(afterUpdate.isVisibleOnSecond)
    }
}
