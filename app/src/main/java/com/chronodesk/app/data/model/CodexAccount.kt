package com.chronodesk.app.data.model

import java.util.UUID

data class CodexAccount(
    val id: String = UUID.randomUUID().toString(),
    val alias: String,
    val email: String? = null,
    val chatgptAccountId: String? = null,
    val credentialStatus: CredentialStatus = CredentialStatus.NOT_LOGGED_IN,
    val authenticatedAt: Long = System.currentTimeMillis(),
    val lastSyncTime: Long? = null,
    val providerType: String = "OAUTH_DEVICE" // OAUTH_DEVICE, DIRECT_TOKEN, MOCK
) {
    val displayTitle: String
        get() = when {
            alias.isNotBlank() -> alias
            !email.isNullOrBlank() -> email
            !chatgptAccountId.isNullOrBlank() -> "Account (${chatgptAccountId.take(8)})"
            else -> "Codex 账号"
        }
}
