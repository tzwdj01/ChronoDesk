package com.chronodesk.app.data

import com.chronodesk.app.data.network.GrafanaApiClient
import com.chronodesk.app.data.network.IntranetDns
import com.chronodesk.app.data.network.isPrivateAddress
import com.chronodesk.app.data.security.TokenRedactor
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress

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

    @Test
    fun testAllowVariousPrivateIps() {
        listOf(
            "10.0.0.1", "172.16.0.1", "172.31.255.255", "192.168.1.1", "127.0.0.1", "169.254.1.1",
            "100.64.0.1", "100.127.255.254", "198.18.1.44", "198.19.255.254"
        ).forEach { ip ->
            val sanitized = client.sanitizeAndValidateUrl("http://$ip:3000", allowHttp = true)
            assertEquals("http://$ip:3000/", sanitized)
        }
    }

    @Test
    fun testRejectPublicIpHttpEvenWhenEnabled() {
        listOf("8.8.8.8", "1.1.1.1", "114.114.114.114", "100.128.0.1", "198.20.0.1").forEach { publicIp ->
            val ex = assertThrows(IllegalArgumentException::class.java) {
                client.sanitizeAndValidateUrl("http://$publicIp:3000", allowHttp = true)
            }
            assertTrue("应拦截公网 IP 访问", ex.message?.contains("禁止连接公网地址") == true)
        }
    }

    @Test
    fun testIntranetDnsPinning() {
        val customHost = "my-teslamate.internal"
        val privateAddr = InetAddress.getByName("192.168.50.10")
        IntranetDns.pin(customHost, listOf(privateAddr))

        val resolved = IntranetDns.lookup(customHost)
        assertEquals(1, resolved.size)
        assertEquals("192.168.50.10", resolved[0].hostAddress)

        val sanitized = client.sanitizeAndValidateUrl("http://$customHost:3000", allowHttp = true)
        assertEquals("http://$customHost:3000/", sanitized)

        IntranetDns.clearPin(customHost)
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
    fun testIntranetHttpFlowWithMockWebServer() = kotlinx.coroutines.runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val serverUrl = server.url("/").toString()
            val provider = com.chronodesk.app.data.provider.TeslaDataProvider(client)

            // 1. Mock datasources
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""[{"id":1,"uid":"pg1","name":"TeslaMate PG","type":"postgres","isDefault":true}]""")
            )
            val dsResult = provider.discoverDatasources(serverUrl, "token123", allowHttp = true)
            assertTrue("发现数据源应成功", dsResult.isSuccess)
            assertEquals("pg1", dsResult.getOrNull()?.first()?.uid)

            // 2. Mock test connection & discover cars
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"results":{"A":{"frames":[{"schema":{"fields":[{"name":"ok","type":"number"}]},"data":{"values":[[1]]}}]}}}""")
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"results":{"A":{"frames":[{"schema":{"fields":[{"name":"id","type":"number"},{"name":"name","type":"string"}]},"data":{"values":[[1],["Model Y"]]}}]}}}""")
            )
            val settings = com.chronodesk.app.data.model.TeslaSettings(
                enabled = true,
                baseUrl = serverUrl,
                dataSourceUid = "pg1",
                allowHttp = true
            )
            val carResult = provider.testConnectionAndDiscoverCars(settings, "token123")
            assertTrue("连接与发现车辆应成功", carResult.isSuccess)
            assertEquals(1, carResult.getOrNull()?.size)
            assertEquals("Model Y", carResult.getOrNull()?.first()?.name)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun testRedirectThrowsIOException() = kotlinx.coroutines.runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val serverUrl = server.url("/").toString()
            val provider = com.chronodesk.app.data.provider.TeslaDataProvider(client)

            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .setHeader("Location", "http://127.0.0.1:9999/other")
            )
            val dsResult = provider.discoverDatasources(serverUrl, "token123", allowHttp = true)
            assertTrue("重定向必须失败", dsResult.isFailure)
            val msg = dsResult.exceptionOrNull()?.message ?: ""
            assertTrue("应拦截自动重定向以保护凭据: $msg", msg.contains("重定向") || msg.contains("302"))
        } finally {
            server.shutdown()
        }
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
