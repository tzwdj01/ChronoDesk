package com.chronodesk.app.data

import com.chronodesk.app.data.network.GrafanaApiClient
import com.chronodesk.app.data.security.TokenRedactor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaSettingsSecurityTest {

    private val client = GrafanaApiClient.instance

    @Test
    fun testValidHttpsUrlSanitization() {
        val input = "https://grafana.myhome.org:3000///"
        val sanitized = client.sanitizeAndValidateUrl(input, allowHttp = false)
        assertEquals("https://grafana.myhome.org:3000/", sanitized)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRejectHttpWhenNotAllowed() {
        client.sanitizeAndValidateUrl("http://192.168.1.100:3000", allowHttp = false)
    }

    @Test
    fun testAllowHttpWhenExplicitlyEnabled() {
        val sanitized = client.sanitizeAndValidateUrl("http://192.168.1.100:3000", allowHttp = true)
        assertEquals("http://192.168.1.100:3000/", sanitized)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRejectUserCredentialsInUrl() {
        client.sanitizeAndValidateUrl("https://admin:password@grafana.example.com", allowHttp = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRejectQueryParametersInUrl() {
        client.sanitizeAndValidateUrl("https://grafana.example.com?token=12345", allowHttp = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRejectFragmentInUrl() {
        client.sanitizeAndValidateUrl("https://grafana.example.com/#/dashboard", allowHttp = false)
    }

    @Test
    fun testTokenRedactionInLogs() {
        val secretToken = "glsa_1234567890abcdefghijklmnopqrstuvwxyz"
        val authHeader = "Bearer $secretToken"
        val redactedHeader = TokenRedactor.redactAuthHeader(authHeader)

        assertFalse("请求头中不可包含完整Token", redactedHeader.contains(secretToken))
        assertTrue("应包含Bearer前缀", redactedHeader.startsWith("Bearer "))
        assertTrue("应脱敏中段字符", redactedHeader.contains("***"))

        val jsonWithToken = """{"grafana_token":"$secretToken","user":"admin"}"""
        val redactedJson = TokenRedactor.redactJsonString(jsonWithToken)
        assertFalse("脱敏JSON不可泄漏Token", redactedJson.contains(secretToken))
        assertTrue("应被标记为REDACTED", redactedJson.contains("***REDACTED***"))
    }
}
