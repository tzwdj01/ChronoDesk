package com.chronodesk.app.data.provider

import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import kotlinx.coroutines.delay

class MockCodexProvider(
    private val slotIndex: Int = 0 // 0 for Account A, 1 for Account B
) : CodexUsageProvider {

    override suspend fun startDeviceAuth(): DeviceCodeResult {
        delay(300)
        return DeviceCodeResult.Success(
            deviceAuthId = "mock-device-auth-id-$slotIndex",
            userCode = if (slotIndex == 0) "MOCK-1024" else "CHRN-9988",
            verificationUrl = "https://auth.openai.com/codex/device",
            intervalSeconds = 2L,
            expiresInSeconds = 900L
        )
    }

    private var pollCount = 0

    override suspend fun pollDeviceAuth(deviceAuthId: String, userCode: String): PollResult {
        delay(200)
        pollCount++
        return if (pollCount >= 2) {
            pollCount = 0
            PollResult.Success(
                authorizationCode = "mock_auth_code_${System.currentTimeMillis()}",
                codeVerifier = "mock_verifier_xyz"
            )
        } else {
            PollResult.Pending
        }
    }

    override suspend fun exchangeToken(code: String, codeVerifier: String): TokenResult {
        delay(300)
        return TokenResult.Success(
            accessToken = "mock_access_token_slot_$slotIndex",
            refreshToken = "mock_refresh_token_slot_$slotIndex",
            idToken = "mock_id_token_slot_$slotIndex",
            expiresInSeconds = 3600L
        )
    }

    override suspend fun refreshToken(refreshToken: String): TokenResult {
        delay(300)
        return TokenResult.Success(
            accessToken = "mock_refreshed_access_token_$slotIndex",
            refreshToken = refreshToken,
            idToken = "mock_id_token_$slotIndex",
            expiresInSeconds = 3600L
        )
    }

    override suspend fun fetchUsage(
        accountId: String,
        accountAlias: String,
        accessToken: String,
        chatgptAccountId: String?
    ): UsageFetchResult {
        delay(400)
        val nowSec = System.currentTimeMillis() / 1000

        // Slot 0 (Account A): 18.5% used (81.5% remaining), reset in 3h 15m
        // Slot 1 (Account B): 48.0% used (52.0% remaining), reset in 1h 45m
        val (used5h, resetIn5h, usedWeekly, resetInWeekly, plan) = if (slotIndex == 0) {
            Tuple5(18.5, 3 * 3600L + 15 * 60L, 35.0, 5 * 86400L, "Plus")
        } else {
            Tuple5(48.0, 1 * 3600L + 45 * 60L, 62.0, 2 * 86400L, "Pro 200")
        }

        val primary = CodexUsageWindow.fromRaw(
            usedPercent = used5h,
            limitWindowSeconds = 18000L,
            resetAtUnix = nowSec + resetIn5h
        )

        val secondary = CodexUsageWindow.fromRaw(
            usedPercent = usedWeekly,
            limitWindowSeconds = 604800L,
            resetAtUnix = nowSec + resetInWeekly
        )

        val snapshot = CodexUsageSnapshot(
            accountId = accountId,
            accountAlias = accountAlias,
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = primary,
            secondaryWindow = secondary,
            creditsBalance = null,
            planName = plan,
            queriedAt = System.currentTimeMillis(),
            isLastGood = false,
            errorMessage = null
        )

        return UsageFetchResult.Success(snapshot)
    }

    private data class Tuple5(
        val a: Double,
        val b: Long,
        val c: Double,
        val d: Long,
        val e: String
    )
}
