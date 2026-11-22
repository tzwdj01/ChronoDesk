package com.chronodesk.app.data

import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.provider.DeepSeekUsageProvider
import com.chronodesk.app.data.provider.GoogleAntigravityUsageProvider
import com.chronodesk.app.data.provider.UsageFetchResult
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
    fun testGeminiApiConnectionSuccess() = runBlocking {
        val json = """
            {
                "models": [
                    { "name": "models/gemini-1.5-pro" },
                    { "name": "models/gemini-1.5-flash" }
                ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val provider = GoogleAntigravityUsageProvider(baseUrl = server.url("/v1beta/models").toString())
        val result = provider.fetchUsage("ag_1", "Gemini API", "AIzaSyValidKey")

        assertTrue(result is UsageFetchResult.Success)
        val snapshot = (result as UsageFetchResult.Success).snapshot
        assertTrue(snapshot.isAvailable == true)
        assertEquals(CredentialStatus.VALID, snapshot.credentialStatus)
        // 确保完全删除了伪造的 5小时窗口、日窗口与 Pro 套餐
        assertNull(snapshot.primaryWindow)
        assertNull(snapshot.secondaryWindow)
        assertNull(snapshot.planName)
        assertNotNull(snapshot.formattedVerifiedTime)
    }

    @Test
    fun testGeminiApiInvalidKey400() = runBlocking {
        val json = """
            {
                "error": {
                    "code": 400,
                    "message": "API key not valid. Please pass a valid API key.",
                    "status": "INVALID_ARGUMENT"
                }
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(400).setBody(json))

        val provider = GoogleAntigravityUsageProvider(baseUrl = server.url("/v1beta/models").toString())
        val result = provider.fetchUsage("ag_1", "Gemini API", "AIzaSyBadKey")

        assertTrue(result is UsageFetchResult.AuthExpired)
        val message = (result as UsageFetchResult.AuthExpired).message
        assertTrue(message.contains("400"))
    }
}
