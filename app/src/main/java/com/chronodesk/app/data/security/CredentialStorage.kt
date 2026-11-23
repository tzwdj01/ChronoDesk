package com.chronodesk.app.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

class CredentialStorage @JvmOverloads constructor(
    private val context: Context,
    testPrefs: SharedPreferences? = null,
    forceStorageUnavailable: Boolean = false
) {

    val isStorageAvailable: Boolean
        get() = prefs != null

    private var prefs: SharedPreferences? = null

    init {
        cleanupLegacyPlaintextFiles(context)
        if (forceStorageUnavailable) {
            prefs = null
        } else if (testPrefs != null) {
            prefs = testPrefs
        } else {
            prefs = try {
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
                // 不再创建或回退到明文 SharedPreferences
                Log.e(TAG, "EncryptedSharedPreferences unavailable, secure storage disabled", e)
                null
            }
        }
    }

    fun saveCodexCredentials(
        accountId: String,
        refreshToken: String,
        accessToken: String? = null,
        expiresAtMs: Long = 0L,
        idToken: String? = null
    ): Boolean {
        val p = prefs ?: return false
        val editor = p.edit()
            .putString(keyRefresh(accountId), refreshToken)
        if (!accessToken.isNullOrBlank()) {
            editor.putString(keyAccess(accountId), accessToken)
            editor.putLong(keyAccessExpires(accountId), expiresAtMs)
        }
        if (!idToken.isNullOrBlank()) {
            editor.putString(keyIdToken(accountId), idToken)
        }
        return editor.commit()
    }

    fun saveRefreshToken(accountId: String, token: String): Boolean {
        val p = prefs ?: return false
        return p.edit().putString(keyRefresh(accountId), token).commit()
    }

    fun getRefreshToken(accountId: String): String? {
        return prefs?.getString(keyRefresh(accountId), null)
    }

    fun saveAccessToken(accountId: String, token: String, expiresAtMs: Long): Boolean {
        val p = prefs ?: return false
        return p.edit()
            .putString(keyAccess(accountId), token)
            .putLong(keyAccessExpires(accountId), expiresAtMs)
            .commit()
    }

    fun getAccessToken(accountId: String): String? {
        val p = prefs ?: return null
        val expiresAt = p.getLong(keyAccessExpires(accountId), 0L)
        val now = System.currentTimeMillis()
        // 提前 60 秒判定过期
        if (expiresAt - now < BUFFER_EXPIRY_MS) {
            return null
        }
        return p.getString(keyAccess(accountId), null)
    }

    fun saveIdToken(accountId: String, idToken: String): Boolean {
        val p = prefs ?: return false
        return p.edit().putString(keyIdToken(accountId), idToken).commit()
    }

    fun getIdToken(accountId: String): String? {
        return prefs?.getString(keyIdToken(accountId), null)
    }

    fun removeAccountCredentials(accountId: String): Boolean {
        val p = prefs ?: return false
        return p.edit()
            .remove(keyRefresh(accountId))
            .remove(keyAccess(accountId))
            .remove(keyAccessExpires(accountId))
            .remove(keyIdToken(accountId))
            .commit()
    }

    fun saveGrafanaToken(token: String): Boolean {
        val p = prefs ?: return false
        return p.edit().putString(KEY_GRAFANA_TOKEN, token.trim()).commit()
    }

    fun getGrafanaToken(): String? {
        return prefs?.getString(KEY_GRAFANA_TOKEN, null)
    }

    fun clearGrafanaToken(): Boolean {
        val p = prefs ?: return false
        return p.edit().remove(KEY_GRAFANA_TOKEN).commit()
    }

    fun clearAll(): Boolean {
        val p = prefs ?: return false
        return p.edit().clear().commit()
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

        /**
         * 启动或更新时幂等清理历史遗留的明文迁移文件及降级明文文件
         */
        fun cleanupLegacyPlaintextFiles(context: Context) {
            try {
                val payload = File(context.filesDir, "migration_payload.json")
                if (payload.exists()) {
                    payload.delete()
                }
                val status = File(context.filesDir, "migration_status.txt")
                if (status.exists()) {
                    status.delete()
                }
                val fallbackPrefs = context.getSharedPreferences(
                    PREFS_FILE_NAME + "_fallback",
                    Context.MODE_PRIVATE
                )
                fallbackPrefs.edit().clear().commit()
                val parentDir = context.filesDir.parentFile
                if (parentDir != null) {
                    val fallbackFile = File(parentDir, "shared_prefs/${PREFS_FILE_NAME}_fallback.xml")
                    if (fallbackFile.exists()) {
                        fallbackFile.delete()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cleanup legacy plaintext files, will retry next launch", e)
            }
        }
    }
}
