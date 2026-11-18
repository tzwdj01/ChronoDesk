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
            .replace(Regex("\"(access_token|refresh_token|id_token)\"\\s*:\\s*\"[^\"]+\"")) { match ->
                val key = match.groupValues[1]
                "\"$key\":\"***REDACTED***\""
            }
            .replace(Regex("\"(code|code_verifier)\"\\s*:\\s*\"[^\"]+\"")) { match ->
                val key = match.groupValues[1]
                "\"$key\":\"***REDACTED***\""
            }
    }
}
