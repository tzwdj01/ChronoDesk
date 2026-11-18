package com.chronodesk.app.data

import com.chronodesk.app.data.security.TokenRedactor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenRedactorTest {

    @Test
    fun testTokenRedaction() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val redacted = TokenRedactor.redactToken(token)

        assertFalse("脱敏后不应包含完整Token", redacted.contains("I1NiIsInR5"))
        assertTrue("脱敏后应包含前缀", redacted.startsWith("eyJh"))
        assertTrue("脱敏后应包含后缀", redacted.endsWith("VCJ9"))
        assertTrue("脱敏后应包含***", redacted.contains("***"))
    }

    @Test
    fun testAuthHeaderRedaction() {
        val header = "Bearer secret_access_token_123456789"
        val redacted = TokenRedactor.redactAuthHeader(header)

        assertTrue("应以 Bearer 开头", redacted.startsWith("Bearer "))
        assertFalse("不应泄露完整 secret", redacted.contains("secret_access_token"))
        assertTrue("应包含掩码", redacted.contains("***"))
    }

    @Test
    fun testJsonRedaction() {
        val json = """{"access_token":"sk-1234567890abcdef","refresh_token":"rt-987654321","plan":"plus"}"""
        val redacted = TokenRedactor.redactJsonString(json)

        assertFalse("JSON 脱敏后不应包含 access_token 明文", redacted.contains("sk-1234567890abcdef"))
        assertFalse("JSON 脱敏后不应包含 refresh_token 明文", redacted.contains("rt-987654321"))
        assertTrue("应被替换为 REDACTED", redacted.contains("***REDACTED***"))
        assertTrue("普通字段应保留", redacted.contains("\"plan\":\"plus\""))
    }
}
