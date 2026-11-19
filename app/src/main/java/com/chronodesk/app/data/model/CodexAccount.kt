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
     * 账号显示名称：优先显示真实登录邮箱（如 user@gmail.com），其次为用户自定义别名
     */
    val displayTitle: String
        get() = when {
            !email.isNullOrBlank() -> email
            alias.isNotBlank() && !alias.matches(Regex("账号\\s*[A-Za-z0-9]+.*")) -> alias
            alias.isNotBlank() -> alias.replace(Regex("账号\\s*[A-Za-z0-9]+\\s*"), "").ifBlank { "Codex 账号" }
            !chatgptAccountId.isNullOrBlank() -> "Account (${chatgptAccountId.take(8)})"
            else -> "未绑定账号"
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
