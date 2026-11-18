package com.chronodesk.app.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialStorage(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            // 在单元测试或低端模拟器不支持硬件 Keystore 时降级处理
            Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back to private SharedPreferences", e)
            context.getSharedPreferences(PREFS_FILE_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveRefreshToken(accountId: String, token: String) {
        prefs.edit().putString(keyRefresh(accountId), token).apply()
    }

    fun getRefreshToken(accountId: String): String? {
        return prefs.getString(keyRefresh(accountId), null)
    }

    fun saveAccessToken(accountId: String, token: String, expiresAtMs: Long) {
        prefs.edit()
            .putString(keyAccess(accountId), token)
            .putLong(keyAccessExpires(accountId), expiresAtMs)
            .apply()
    }

    fun getAccessToken(accountId: String): String? {
        val expiresAt = prefs.getLong(keyAccessExpires(accountId), 0L)
        val now = System.currentTimeMillis()
        // 提前 60 秒判定过期
        if (expiresAt - now < BUFFER_EXPIRY_MS) {
            return null
        }
        return prefs.getString(keyAccess(accountId), null)
    }

    fun saveIdToken(accountId: String, idToken: String) {
        prefs.edit().putString(keyIdToken(accountId), idToken).apply()
    }

    fun getIdToken(accountId: String): String? {
        return prefs.getString(keyIdToken(accountId), null)
    }

    fun removeAccountCredentials(accountId: String) {
        prefs.edit()
            .remove(keyRefresh(accountId))
            .remove(keyAccess(accountId))
            .remove(keyAccessExpires(accountId))
            .remove(keyIdToken(accountId))
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun keyRefresh(accountId: String) = "acc_${accountId}_refresh_token"
    private fun keyAccess(accountId: String) = "acc_${accountId}_access_token"
    private fun keyAccessExpires(accountId: String) = "acc_${accountId}_access_expires"
    private fun keyIdToken(accountId: String) = "acc_${accountId}_id_token"

    companion object {
        private const val TAG = "CredentialStorage"
        private const val PREFS_FILE_NAME = "chronodesk_secure_credentials"
        private const val BUFFER_EXPIRY_MS = 60_000L // 60s
    }
}
