package com.chronodesk.app.data

import android.content.Context
import android.content.SharedPreferences
import com.chronodesk.app.data.repository.ClockSettingsRepository
import com.chronodesk.app.data.repository.SettingsValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ClockSettingsJsonTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repo: ClockSettingsRepository

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockPrefs.getBoolean(anyString(), anyInt() == 1)).thenReturn(true)
        `when`(mockPrefs.getInt(anyString(), anyInt())).thenAnswer { it.arguments[1] as Int }
        `when`(mockPrefs.getFloat(anyString(), anyFloat())).thenAnswer { it.arguments[1] as Float }
        `when`(mockPrefs.getString(anyString(), anyString())).thenAnswer { it.arguments[1] as String }

        repo = ClockSettingsRepository(mockContext)
    }

    @Test
    fun testExportSettingsContainsSchemaVersionOneAndNoSensitiveData() {
        val json = repo.exportSettingsJson()

        assertTrue("导出的配置必须包含 schemaVersion", json.contains("\"schemaVersion\": 1"))
        assertTrue("必须包含时间制式配置", json.contains("\"is24Hour\":"))
        assertTrue("必须包含夜间模式配置", json.contains("\"nightModeOption\":"))
        assertTrue("必须包含时钟缩放配置", json.contains("\"clockScale\":"))

        // 验证绝对不包含敏感数据
        assertFalse("绝对不可包含 credentials", json.contains("credentials"))
        assertFalse("绝对不可包含 accountsMeta", json.contains("accountsMeta"))
        assertFalse("绝对不可包含 token", json.contains("token"))
        assertFalse("绝对不可包含 base_url", json.contains("base_url"))
    }

    @Test
    fun testValidSettingsJsonRoundTrip() {
        val exported = repo.exportSettingsJson()
        val result = repo.validateAndParseSettingsJson(exported)

        assertTrue("导出的配置必须可通过校验", result is SettingsValidationResult.Valid)
        val valid = result as SettingsValidationResult.Valid
        assertEquals(1, valid.backup.schemaVersion)
        assertTrue(valid.previewSummary.contains("配置版本: v1"))
    }

    @Test
    fun testRejectLegacyMigrationPayload() {
        val legacyPayload = """
            {
                "exportTimestamp": 1700000000000,
                "credentials": { "acc_chatgpt_token": "secret" },
                "accountsMeta": { "user": "test" },
                "schemaVersion": 1
            }
        """.trimIndent()

        val result = repo.validateAndParseSettingsJson(legacyPayload)
        assertTrue("必须拒绝旧凭据迁移文件", result is SettingsValidationResult.Invalid)
        val invalid = result as SettingsValidationResult.Invalid
        assertTrue(invalid.errorMessage.contains("已拒绝"))
    }

    @Test
    fun testRejectMissingOrUnknownSchemaVersion() {
        val missingVersion = """{"is24Hour": true}"""
        val resMissing = repo.validateAndParseSettingsJson(missingVersion)
        assertTrue("缺少 schemaVersion 必须被拒绝", resMissing is SettingsValidationResult.Invalid)

        val unknownVersion = """{"schemaVersion": 99, "is24Hour": true}"""
        val resUnknown = repo.validateAndParseSettingsJson(unknownVersion)
        assertTrue("未知版本必须被拒绝", resUnknown is SettingsValidationResult.Invalid)
    }

    @Test
    fun testRejectInvalidFieldRanges() {
        val invalidHour = """{"schemaVersion": 1, "nightDimStartHour": 26}"""
        val resHour = repo.validateAndParseSettingsJson(invalidHour)
        assertTrue("小时越界必须被拒绝", resHour is SettingsValidationResult.Invalid)

        val invalidBrightness = """{"schemaVersion": 1, "nightBrightness": 2.5}"""
        val resBright = repo.validateAndParseSettingsJson(invalidBrightness)
        assertTrue("亮度越界必须被拒绝", resBright is SettingsValidationResult.Invalid)

        val invalidScale = """{"schemaVersion": 1, "clockScale": 10.0}"""
        val resScale = repo.validateAndParseSettingsJson(invalidScale)
        assertTrue("缩放比例越界必须被拒绝", resScale is SettingsValidationResult.Invalid)

        val invalidTheme = """{"schemaVersion": 1, "nightModeOption": "DARK_MATRIX"}"""
        val resTheme = repo.validateAndParseSettingsJson(invalidTheme)
        assertTrue("未知主题模式必须被拒绝", resTheme is SettingsValidationResult.Invalid)
    }

    @Test
    fun testRejectCorruptJson() {
        val corrupt = "{ not a valid json"
        val result = repo.validateAndParseSettingsJson(corrupt)
        assertTrue("损坏的 JSON 必须被拒绝", result is SettingsValidationResult.Invalid)
    }
}
