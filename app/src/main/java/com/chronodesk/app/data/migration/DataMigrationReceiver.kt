package com.chronodesk.app.data.migration

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chronodesk.app.data.security.CredentialStorage
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

data class MigrationPayload(
    val exportTimestamp: Long = System.currentTimeMillis(),
    val accountsMeta: Map<String, String> = emptyMap(),
    val credentials: Map<String, Any> = emptyMap(),
    val settings: Map<String, Any> = emptyMap()
)

class DataMigrationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "DataMigrationReceiver received action: $action")
        val gson = GsonBuilder().setPrettyPrinting().create()
        val payloadFile = File(context.filesDir, PAYLOAD_FILENAME)
        val statusFile = File(context.filesDir, STATUS_FILENAME)

        when (action) {
            ACTION_EXPORT_DATA -> {
                try {
                    // 1. Export accounts meta
                    val metaPrefs = context.getSharedPreferences("chronodesk_accounts_meta", Context.MODE_PRIVATE)
                    val accountsMeta = mutableMapOf<String, String>()
                    for ((k, v) in metaPrefs.all) {
                        if (v is String) {
                            accountsMeta[k] = v
                        }
                    }

                    // 2. Export decrypted credentials
                    val credStorage = CredentialStorage(context)
                    val credentials = credStorage.getAllDecryptedCredentials()

                    // 3. Export settings
                    val settingsPrefs = context.getSharedPreferences("chronodesk_settings", Context.MODE_PRIVATE)
                    val settings = mutableMapOf<String, Any>()
                    for ((k, v) in settingsPrefs.all) {
                        if (v != null) {
                            settings[k] = v
                        }
                    }

                    val payload = MigrationPayload(
                        exportTimestamp = System.currentTimeMillis(),
                        accountsMeta = accountsMeta,
                        credentials = credentials,
                        settings = settings
                    )

                    val json = gson.toJson(payload)
                    payloadFile.writeText(json)
                    statusFile.writeText("EXPORT_SUCCESS: accounts=${accountsMeta.size}, credentials=${credentials.size}, settings=${settings.size}")
                    Log.i(TAG, "Exported successfully: accounts=${accountsMeta.size}, credentials=${credentials.size}")
                } catch (e: Exception) {
                    Log.e(TAG, "Export failed", e)
                    statusFile.writeText("EXPORT_FAILED: ${e.message}")
                }
            }

            ACTION_IMPORT_DATA -> {
                try {
                    if (!payloadFile.exists()) {
                        statusFile.writeText("IMPORT_FAILED: Payload file not found")
                        return
                    }

                    val json = payloadFile.readText()
                    val type = object : TypeToken<MigrationPayload>() {}.type
                    val payload: MigrationPayload = gson.fromJson(json, type)

                    // 1. Import accounts meta
                    val metaPrefs = context.getSharedPreferences("chronodesk_accounts_meta", Context.MODE_PRIVATE)
                    val metaEditor = metaPrefs.edit().clear()
                    for ((k, v) in payload.accountsMeta) {
                        metaEditor.putString(k, v)
                    }
                    metaEditor.commit()

                    // 2. Import credentials
                    val credStorage = CredentialStorage(context)
                    credStorage.importDecryptedCredentials(payload.credentials)

                    // 3. Import common settings (cities, intervals, etc.) if provided
                    if (payload.settings.isNotEmpty()) {
                        val settingsPrefs = context.getSharedPreferences("chronodesk_settings", Context.MODE_PRIVATE)
                        val sEditor = settingsPrefs.edit()
                        for ((k, v) in payload.settings) {
                            // 保留当前设备的屏幕布局特定设置（如果已有），其它通用设置进行同步
                            if (k in listOf("quota_card_density", "clock_scale", "clock_offset_x", "clock_offset_y")) {
                                continue
                            }
                            when (k) {
                                "city_lat", "city_lng", "night_brightness" -> {
                                    val num = (v as? Number)?.toFloat() ?: v.toString().toFloatOrNull() ?: 0f
                                    sEditor.putFloat(k, num)
                                }
                                "refresh_interval_min", "night_start_hour", "night_end_hour", "quota_width_percent" -> {
                                    val num = (v as? Number)?.toInt() ?: v.toString().toIntOrNull() ?: 0
                                    sEditor.putInt(k, num)
                                }
                                "is_24_hour", "show_seconds", "keep_screen_on", "auto_night_dim", "auto_location", "use_mock_data" -> {
                                    val b = (v as? Boolean) ?: v.toString().toBoolean()
                                    sEditor.putBoolean(k, b)
                                }
                                else -> {
                                    sEditor.putString(k, v.toString())
                                }
                            }
                        }
                        sEditor.commit()
                    }

                    statusFile.writeText("IMPORT_SUCCESS: accounts=${payload.accountsMeta.size}, credentials=${payload.credentials.size}")
                    Log.i(TAG, "Imported successfully: accounts=${payload.accountsMeta.size}, credentials=${payload.credentials.size}")
                } catch (e: Exception) {
                    Log.e(TAG, "Import failed", e)
                    statusFile.writeText("IMPORT_FAILED: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "DataMigration"
        const val ACTION_EXPORT_DATA = "com.chronodesk.app.ACTION_EXPORT_DATA"
        const val ACTION_IMPORT_DATA = "com.chronodesk.app.ACTION_IMPORT_DATA"
        const val PAYLOAD_FILENAME = "migration_payload.json"
        const val STATUS_FILENAME = "migration_status.txt"
    }
}
