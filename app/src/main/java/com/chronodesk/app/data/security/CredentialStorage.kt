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

    fun getAllDecryptedCredentials(): Map<String, Any> {
        return try {
            val allMap = prefs.all
            val result = mutableMapOf<String, Any>()
            for ((k, v) in allMap) {
                if (!k.startsWith("__androidx_") && v != null) {
                    result[k] = v
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all credentials", e)
            emptyMap()
        }
    }

    fun importDecryptedCredentials(entries: Map<String, Any>) {
        val editor = prefs.edit()
        for ((k, v) in entries) {
            if (k.startsWith("__androidx_")) continue
            when (v) {
                is String -> editor.putString(k, v)
                is Long -> editor.putLong(k, v)
                is Int -> editor.putInt(k, v)
                is Boolean -> editor.putBoolean(k, v)
                is Float -> editor.putFloat(k, v)
                is Number -> editor.putLong(k, v.toLong())
                else -> editor.putString(k, v.toString())
            }
        }
        editor.commit()
    }

    fun saveGrafanaToken(token: String) {
        prefs.edit().putString(KEY_GRAFANA_TOKEN, token.trim()).commit()
    }

    fun getGrafanaToken(): String? {
        return prefs.getString(KEY_GRAFANA_TOKEN, null)
    }

    fun clearGrafanaToken() {
        prefs.edit().remove(KEY_GRAFANA_TOKEN).commit()
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
        private const val KEY_GRAFANA_TOKEN = "tesla_grafana_token"
        private const val BUFFER_EXPIRY_MS = 60_000L // 60s
    }
}
