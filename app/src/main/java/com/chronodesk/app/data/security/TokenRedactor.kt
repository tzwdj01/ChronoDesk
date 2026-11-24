package com.chronodesk.app.data.security

object TokenRedactor {
    /**
     * 脱敏单个 Token 字符串，保留前 4 位，其余替换为 ***
     */
    fun redactToken(token: String?): String {
        if (token.isNullOrBlank()) return "null"
        return if (token.length <= 8) {
            "***"
        } else {
            "${token.take(4)}***${token.takeLast(4)}"
        }
    }

    /**
     * 对请求头 Authorization 内容进行脱敏
     */
    fun redactAuthHeader(header: String?): String {
        if (header.isNullOrBlank()) return "null"
        return if (header.startsWith("Bearer ", ignoreCase = true)) {
            val token = header.substring(7)
            "Bearer ${redactToken(token)}"
        } else {
            redactToken(header)
        }
    }

    /**
     * 对包含敏感凭据的 JSON 文本做正则表达式模糊化，防止输出到日志
     */
    fun redactJsonString(json: String): String {
        return json
            .replace(Regex("\"(access_token|refresh_token|id_token|grafana_token|token|api_key|client_secret|registration_access_token)\"\\s*:\\s*\"[^\"]+\"")) { match ->
                val key = match.groupValues[1]
                "\"$key\":\"***REDACTED***\""
            }
            .replace(Regex("\"(code|code_verifier|auth_code)\"\\s*:\\s*\"[^\"]+\"")) { match ->
                val key = match.groupValues[1]
                "\"$key\":\"***REDACTED***\""
            }
    }

    /**
     * 对请求 URL 中的敏感查询参数（如 key, token, apikey, secret）进行脱敏，避免泄露在日志中
     */
    fun redactUrl(url: String): String {
        return url.replace(Regex("(?i)([?&](?:key|token|apikey|secret)=)[^&#]+")) { match ->
            "${match.groupValues[1]}***REDACTED***"
        }
    }
}
