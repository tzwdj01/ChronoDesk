package com.chronodesk.app.data.security

import android.util.Base64
import android.util.Log
import org.json.JSONObject

object JwtUtils {
    private const val TAG = "JwtUtils"

    /**
     * 从 JWT Token (id_token 或 access_token) 的 Payload 中提取用户 Email
     */
    fun extractEmail(jwt: String?): String? {
        if (jwt.isNullOrBlank()) return null
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val jsonString = String(payloadBytes, Charsets.UTF_8)
            val json = JSONObject(jsonString)

            // 1. 直接 email 字段
            var email = json.optString("email", "").takeIf { it.isNotBlank() && it.contains("@") }

            // 2. OpenAI 专属 profile 结构: https://api.openai.com/profile
            if (email == null) {
                val profile = json.optJSONObject("https://api.openai.com/profile")
                email = profile?.optString("email", "")?.takeIf { it.isNotBlank() && it.contains("@") }
            }

            // 3. OpenAI auth 结构: https://api.openai.com/auth
            if (email == null) {
                val auth = json.optJSONObject("https://api.openai.com/auth")
                email = auth?.optString("email", "")?.takeIf { it.isNotBlank() && it.contains("@") }
            }

            // 4. preferred_username 字段
            if (email == null) {
                email = json.optString("preferred_username", "").takeIf { it.isNotBlank() && it.contains("@") }
            }

            // 5. sub 字段若包含邮箱
            if (email == null) {
                val sub = json.optString("sub", "")
                if (sub.contains("@")) {
                    email = if (sub.contains("|")) sub.substringAfter("|") else sub
                }
            }

            email
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode JWT payload: ${e.message}")
            null
        }
    }

    /**
     * 从 JWT Token 中提取 chatgpt_account_id
     */
    fun extractChatGptAccountId(jwt: String?): String? {
        if (jwt.isNullOrBlank()) return null
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val jsonString = String(payloadBytes, Charsets.UTF_8)
            val json = JSONObject(jsonString)
            json.optString("chatgpt_account_id", "").takeIf { it.isNotBlank() }
                ?: json.optJSONObject("https://api.openai.com/auth")?.optString("chatgpt_account_id", "")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
