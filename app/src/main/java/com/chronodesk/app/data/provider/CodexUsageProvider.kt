package com.chronodesk.app.data.provider

import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CredentialStatus

sealed class DeviceCodeResult {
    data class Success(
        val deviceAuthId: String,
        val userCode: String,
        val verificationUrl: String,
        val intervalSeconds: Long,
        val expiresInSeconds: Long
    ) : DeviceCodeResult()

    data class Error(val message: String) : DeviceCodeResult()
}

sealed class PollResult {
    data class Success(val authorizationCode: String, val codeVerifier: String) : PollResult()
    object Pending : PollResult()
    data class Expired(val message: String) : PollResult()
    data class Error(val message: String) : PollResult()
}

sealed class TokenResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String?,
        val idToken: String?,
        val expiresInSeconds: Long
    ) : TokenResult()

    data class ExpiredOrInvalid(val message: String) : TokenResult()
    data class TransientError(val message: String) : TokenResult()
}

sealed class UsageFetchResult {
    data class Success(val snapshot: CodexUsageSnapshot) : UsageFetchResult()
    data class AuthExpired(val message: String, val statusCode: Int = 401) : UsageFetchResult()
    data class AccessDenied(val message: String, val statusCode: Int = 403) : UsageFetchResult()
    data class RateLimited(val message: String, val retryAfterSeconds: Long? = null) : UsageFetchResult()
    data class ServerError(val message: String, val statusCode: Int = 500) : UsageFetchResult()
    data class DataFormatError(val message: String) : UsageFetchResult()
    data class TransientError(val message: String) : UsageFetchResult()
}

interface CodexUsageProvider {
    suspend fun startDeviceAuth(): DeviceCodeResult
    suspend fun pollDeviceAuth(deviceAuthId: String, userCode: String): PollResult
    suspend fun exchangeToken(code: String, codeVerifier: String): TokenResult
    suspend fun refreshToken(refreshToken: String): TokenResult
    suspend fun fetchUsage(
        accountId: String,
        accountAlias: String,
        accessToken: String,
        chatgptAccountId: String?
    ): UsageFetchResult
}
