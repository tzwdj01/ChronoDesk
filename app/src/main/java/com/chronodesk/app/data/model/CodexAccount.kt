package com.chronodesk.app.data.model

import java.util.UUID

data class CodexAccount(
    val id: String = UUID.randomUUID().toString(),
    val alias: String = "",
    val email: String? = null,
    val chatgptAccountId: String? = null,
    val credentialStatus: CredentialStatus = CredentialStatus.NOT_LOGGED_IN,
    val authenticatedAt: Long = System.currentTimeMillis(),
    val lastSyncTime: Long? = null,
    val providerType: String = "OAUTH_DEVICE", // OAUTH_DEVICE, DIRECT_TOKEN, MOCK
    val modelProvider: String = "CHATGPT"      // CHATGPT, DEEPSEEK, GEMINI, CLAUDE
) {
    /**
     * 脱敏邮箱：例如 wa***7@jd.com, al***3@gmail.com，避免个人真实信息泄露
     */
    val maskedEmail: String?
        get() {
            val em = email?.trim() ?: return null
            val atIdx = em.indexOf('@')
            if (atIdx <= 0) return em.take(2) + "***"
            val username = em.substring(0, atIdx)
            val domain = em.substring(atIdx) // 包含 @
            val maskedUser = when {
                username.length <= 2 -> "${username.first()}***"
                username.length <= 4 -> "${username.first()}***${username.last()}"
                else -> "${username.take(2)}***${username.takeLast(1)}"
            }
            return "$maskedUser$domain"
        }

    /**
     * 账号显示名称：
     * 1. 优先展示用户自定义别名（不等于原始邮箱且非空）
     * 2. 其次展示脱敏邮箱（如 wa***7@jd.com），坚决不暴露完整邮箱
     */
    val displayTitle: String
        get() = when {
            modelProvider == "DEEPSEEK" -> {
                if (alias.isNotBlank()) alias else "DeepSeek"
            }
            modelProvider == "ANTIGRAVITY" -> {
                if (alias.isNotBlank() && alias != "Google Antigravity") alias else "Antigravity"
            }
            alias.isNotBlank() && alias != email && !alias.matches(Regex("账号\\s*[A-Za-z0-9]+.*")) -> alias
            !maskedEmail.isNullOrBlank() -> maskedEmail!!
            !chatgptAccountId.isNullOrBlank() -> "Account (${chatgptAccountId.take(8)})"
            else -> "ChatGPT 账号"
        }

    val isConfigured: Boolean
        get() = credentialStatus == CredentialStatus.VALID || credentialStatus == CredentialStatus.EXPIRED

    companion object {
        fun createPlaceholder(defaultAlias: String = ""): CodexAccount {
            return CodexAccount(
                id = UUID.randomUUID().toString(),
                alias = defaultAlias,
                credentialStatus = CredentialStatus.NOT_LOGGED_IN
            )
        }
    }
}
