package com.chronodesk.app.data

import android.content.Context
import android.content.SharedPreferences
import com.chronodesk.app.data.security.CredentialStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class CredentialStorageSecurityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        filesDir = tempFolder.newFolder("files")
        `when`(mockContext.filesDir).thenReturn(filesDir)

        val mockPrefs = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.clear()).thenReturn(mockEditor)
        `when`(mockEditor.commit()).thenReturn(true)
        `when`(mockContext.getSharedPreferences("chronodesk_secure_credentials_fallback", Context.MODE_PRIVATE))
            .thenReturn(mockPrefs)
    }

    @Test
    fun testStorageUnavailableBehavior() {
        val storage = CredentialStorage(mockContext, forceStorageUnavailable = true)

        assertFalse("存储不可用时 isStorageAvailable 必须为 false", storage.isStorageAvailable)
        assertFalse("存储不可用时保存 RefreshToken 必须失败", storage.saveRefreshToken("acc_1", "rt_123"))
        assertFalse("存储不可用时保存 AccessToken 必须失败", storage.saveAccessToken("acc_1", "at_123", 1000L))
        assertFalse("存储不可用时保存 GrafanaToken 必须失败", storage.saveGrafanaToken("glsa_123"))
        assertFalse("存储不可用时保存 IdToken 必须失败", storage.saveIdToken("acc_1", "id_123"))
        assertFalse("存储不可用时删除凭据必须返回 false", storage.removeAccountCredentials("acc_1"))

        assertNull("存储不可用时读取 RefreshToken 必须返回 null", storage.getRefreshToken("acc_1"))
        assertNull("存储不可用时读取 AccessToken 必须返回 null", storage.getAccessToken("acc_1"))
        assertNull("存储不可用时读取 GrafanaToken 必须返回 null", storage.getGrafanaToken())
        assertNull("存储不可用时读取 IdToken 必须返回 null", storage.getIdToken("acc_1"))
    }

    @Test
    fun testCleanupLegacyPlaintextFiles() {
        // 创建遗留明文文件
        val legacyPayload = File(filesDir, "migration_payload.json")
        legacyPayload.writeText("""{"credentials":{"secret":"123"}}""")
        val legacyStatus = File(filesDir, "migration_status.txt")
        legacyStatus.writeText("EXPORT_SUCCESS")

        assertTrue("遗留 payload 文件已存在", legacyPayload.exists())
        assertTrue("遗留 status 文件已存在", legacyStatus.exists())

        // 执行清理
        CredentialStorage.cleanupLegacyPlaintextFiles(mockContext)

        assertFalse("清理后 migration_payload.json 必须已被删除", legacyPayload.exists())
        assertFalse("清理后 migration_status.txt 必须已被删除", legacyStatus.exists())

        // 验证幂等性：重复清理无副作用且不抛异常
        CredentialStorage.cleanupLegacyPlaintextFiles(mockContext)
        assertFalse(legacyPayload.exists())
        assertFalse(legacyStatus.exists())
    }
}
