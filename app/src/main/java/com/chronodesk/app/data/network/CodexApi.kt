package com.chronodesk.app.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface CodexAuthApi {
    @POST("api/accounts/deviceauth/usercode")
    suspend fun requestDeviceCode(
        @Body body: Map<String, String>
    ): Response<DeviceCodeResponse>

    @POST("api/accounts/deviceauth/token")
    suspend fun pollDeviceToken(
        @Body body: Map<String, String>
    ): Response<DeviceTokenResponse>

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun exchangeToken(
        @FieldMap fields: Map<String, String>
    ): Response<OAuthTokenResponse>
}

interface CodexUsageApi {
    @GET("backend-api/wham/usage")
    suspend fun getUsage(
        @Header("Authorization") authHeader: String,
        @Header("ChatGPT-Account-Id") accountId: String?,
        @Header("User-Agent") userAgent: String = "codex-cli",
        @Header("Accept") accept: String = "application/json"
    ): Response<WhamUsageResponse>

    @GET("backend-api/wham/rate-limit-reset-credits")
    suspend fun getResetCredits(
        @Header("Authorization") authHeader: String,
        @Header("ChatGPT-Account-Id") accountId: String?,
        @Header("User-Agent") userAgent: String = "codex-cli",
        @Header("Accept") accept: String = "application/json",
        @Header("OpenAI-Beta") beta: String = "codex-1"
    ): Response<WhamResetCreditsResponse>

    @GET("backend-api/me")
    suspend fun getMe(
        @Header("Authorization") authHeader: String,
        @Header("User-Agent") userAgent: String = "codex-cli",
        @Header("Accept") accept: String = "application/json"
    ): Response<ChatGPTMeResponse>
}

data class ChatGPTMeResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("picture") val picture: String? = null
)

data class DeviceCodeResponse(
    @SerializedName("device_auth_id") val deviceAuthId: String,
    @SerializedName("user_code") val userCode: String,
    @SerializedName("interval") val interval: Long? = 5L,
    @SerializedName("expires_in") val expiresIn: Long? = 900L
)

data class DeviceTokenResponse(
    @SerializedName("authorization_code") val authorizationCode: String? = null,
    @SerializedName("code_verifier") val codeVerifier: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("error_description") val errorDescription: String? = null
)

data class OAuthTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("id_token") val idToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = 3600L
)

data class WhamUsageResponse(
    @SerializedName("rate_limit") val rateLimit: WhamRateLimit? = null,
    @SerializedName("credits") val credits: WhamCredits? = null,
    @SerializedName("plan_type") val planType: String? = null
)

data class WhamRateLimit(
    @SerializedName("allowed") val allowed: Boolean? = true,
    @SerializedName("limit_reached") val limitReached: Boolean? = false,
    @SerializedName("primary_window") val primaryWindow: WhamLimitWindow? = null,
    @SerializedName("secondary_window") val secondaryWindow: WhamLimitWindow? = null
)

data class WhamLimitWindow(
    @SerializedName("used_percent") val usedPercent: Double? = null,
    @SerializedName("limit_window_seconds") val limitWindowSeconds: Long? = null,
    @SerializedName("reset_at") val resetAt: Long? = null
)

data class WhamCredits(
    @SerializedName("has_credits") val hasCredits: Boolean? = false,
    @SerializedName("unlimited") val unlimited: Boolean? = false,
    @SerializedName("balance") val balance: Any? = null // 可以是数字或字符串
) {
    fun parseBalance(): Double? {
        if (hasCredits != true || unlimited == true) return null
        return when (balance) {
            is Number -> balance.toDouble()
            is String -> balance.toDoubleOrNull()
            else -> null
        }?.takeIf { it > 0.0 }
    }
}

data class WhamResetCreditsResponse(
    @SerializedName("credits") val credits: List<WhamResetCreditItem>? = null
)

data class WhamResetCreditItem(
    @SerializedName("status") val status: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null
)
