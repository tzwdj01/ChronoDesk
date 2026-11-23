package com.chronodesk.app.data

import android.content.Context
import android.content.SharedPreferences
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.provider.CodexUsageProvider
import com.chronodesk.app.data.provider.DeepSeekUsageProvider
import com.chronodesk.app.data.provider.GoogleAntigravityUsageProvider
import com.chronodesk.app.data.provider.UsageFetchResult
import com.chronodesk.app.data.repository.CodexAccountRepository
import com.chronodesk.app.data.security.CredentialStorage
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class MockIsolationAndPinningTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockCredentialStorage: CredentialStorage
    private lateinit var mockRealCodexProvider: CodexUsageProvider
    private lateinit var mockDeepSeekProvider: DeepSeekUsageProvider
    private lateinit var mockAntigravityProvider: GoogleAntigravityUsageProvider
    private val gson = Gson()

    private val accountChatGpt = CodexAccount(
        id = "acc_chatgpt_1",
        alias = "我的 ChatGPT",
        modelProvider = "CHATGPT",
        credentialStatus = CredentialStatus.VALID
    )
    private val accountDeepSeek = CodexAccount(
        id = "acc_deepseek_1",
        alias = "我的 DeepSeek",
        modelProvider = "DEEPSEEK",
        credentialStatus = CredentialStatus.VALID
    )
    private val accountAntigravity = CodexAccount(
        id = "acc_gemini_1",
        alias = "我的 Gemini",
        modelProvider = "ANTIGRAVITY",
        credentialStatus = CredentialStatus.VALID
    )

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)
        mockCredentialStorage = mock(CredentialStorage::class.java)
        mockRealCodexProvider = mock(CodexUsageProvider::class.java)
        mockDeepSeekProvider = mock(DeepSeekUsageProvider::class.java)
        mockAntigravityProvider = mock(GoogleAntigravityUsageProvider::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)

        val accountsJson = gson.toJson(listOf(accountChatGpt, accountDeepSeek, accountAntigravity))
        `when`(mockPrefs.getString("account_list", null)).thenReturn(accountsJson)
        `when`(mockPrefs.getString("account_slot_0", null)).thenReturn(gson.toJson(accountChatGpt))
        `when`(mockPrefs.getString("account_slot_1", null)).thenReturn(null)

        `when`(mockCredentialStorage.getAccessToken("acc_chatgpt_1")).thenReturn("valid_chatgpt_token")
        `when`(mockCredentialStorage.getRefreshToken("acc_chatgpt_1")).thenReturn("valid_refresh_token")
        `when`(mockCredentialStorage.getAccessToken("acc_deepseek_1")).thenReturn("sk-valid-deepseek-key")
        `when`(mockCredentialStorage.getAccessToken("acc_gemini_1")).thenReturn("AIzaSy-valid-gemini-key")
    }

    @Test
    fun testMockModeIsolation_threeRealProvidersZeroRequests() = runBlocking {
        val repo = CodexAccountRepository(
            context = mockContext,
            credentialStorage = mockCredentialStorage,
            realProvider = mockRealCodexProvider,
            deepSeekProvider = mockDeepSeekProvider,
            antigravityProvider = mockAntigravityProvider
        )

        // 开启演示模式全量刷新
        repo.refreshAll(isMockGlobal = true)

        // 验证：3 个真实 Provider 的网络交互/请求必须严格为零！
        verifyNoInteractions(mockRealCodexProvider)
        verifyNoInteractions(mockDeepSeekProvider)
        verifyNoInteractions(mockAntigravityProvider)

        val snapshots = repo.snapshots.value

        // 验证 ChatGPT：使用本地示例，标记 isMock == true
        val chatGptSnap = snapshots[accountChatGpt.id]
        requireNotNull(chatGptSnap)
        assertTrue("ChatGPT 演示快照必须带有 isMock 标记", chatGptSnap.isMock)
        assertEquals("Plus", chatGptSnap.planName)

        // 验证 DeepSeek：无示例提供商明确标为「暂无演示数据」
        val deepSeekSnap = snapshots[accountDeepSeek.id]
        requireNotNull(deepSeekSnap)
        assertTrue("DeepSeek 演示快照必须带有 isMock 标记", deepSeekSnap.isMock)
        assertEquals("暂无演示数据", deepSeekSnap.errorMessage)
        assertEquals("暂无演示数据", deepSeekSnap.formattedBalanceText)

        // 验证 Gemini API：无示例提供商明确标为「暂无演示数据」
        val antigravitySnap = snapshots[accountAntigravity.id]
        requireNotNull(antigravitySnap)
        assertTrue("Gemini 演示快照必须带有 isMock 标记", antigravitySnap.isMock)
        assertEquals("暂无演示数据", antigravitySnap.errorMessage)
    }

    @Test
    fun testExitMockMode_clearsMockSnapshotsAndRestoresRealRefresh() = runBlocking {
        val repo = CodexAccountRepository(
            context = mockContext,
            credentialStorage = mockCredentialStorage,
            realProvider = mockRealCodexProvider,
            deepSeekProvider = mockDeepSeekProvider,
            antigravityProvider = mockAntigravityProvider
        )

        repo.refreshAll(isMockGlobal = true)
        assertTrue(repo.snapshots.value[accountChatGpt.id]?.isMock == true)

        // 退出演示模式：清除本地演示快照
        repo.clearMockSnapshots()
        assertFalse(
            "退出演示模式后，快照不应再为演示状态",
            repo.snapshots.value[accountChatGpt.id]?.isMock == true
        )
    }

    @Test
    fun testAccountPinning_pinnedTopWithinProviderGroupOnly() {
        val gpt1 = CodexAccount(id = "gpt_1", alias = "普通账号1", modelProvider = "CHATGPT", isPinnedMain = false)
        val gpt2 = CodexAccount(id = "gpt_2", alias = "常用账号2", modelProvider = "CHATGPT", isPinnedMain = true)
        val gpt3 = CodexAccount(id = "gpt_3", alias = "普通账号3", modelProvider = "CHATGPT", isPinnedMain = false)

        val ds1 = CodexAccount(id = "ds_1", alias = "DeepSeek普通", modelProvider = "DEEPSEEK", isPinnedMain = false)
        val ds2 = CodexAccount(id = "ds_2", alias = "DeepSeek常用", modelProvider = "DEEPSEEK", isPinnedMain = true)

        val ag1 = CodexAccount(id = "ag_1", alias = "Gemini账号", modelProvider = "ANTIGRAVITY", isPinnedMain = false)

        val allAccounts = listOf(gpt1, gpt2, gpt3, ds1, ds2, ag1)

        // 按分组稳定排序
        val sortedGpt = allAccounts
            .filter { (it.modelProvider == "CHATGPT" || it.modelProvider == null) && it.isVisibleOnMain }
            .sortedByDescending { it.isPinnedMain }

        val sortedDs = allAccounts
            .filter { it.modelProvider == "DEEPSEEK" && it.isVisibleOnMain }
            .sortedByDescending { it.isPinnedMain }

        val sortedAg = allAccounts
            .filter { it.modelProvider == "ANTIGRAVITY" && it.isVisibleOnMain }
            .sortedByDescending { it.isPinnedMain }

        // 验证 ChatGPT 组内置顶
        assertEquals("gpt_2", sortedGpt[0].id)
        assertEquals("gpt_1", sortedGpt[1].id)
        assertEquals("gpt_3", sortedGpt[2].id)

        // 验证 DeepSeek 组内置顶
        assertEquals("ds_2", sortedDs[0].id)
        assertEquals("ds_1", sortedDs[1].id)

        // 验证 Gemini 组未受干扰
        assertEquals("ag_1", sortedAg[0].id)

        // 验证隐藏置顶账号后恢复默认顺序
        val gpt2Hidden = gpt2.copy(showOnMainScreen = false)
        val afterHidden = listOf(gpt1, gpt2Hidden, gpt3)
            .filter { it.isVisibleOnMain }
            .sortedByDescending { it.isPinnedMain }

        assertEquals(2, afterHidden.size)
        assertEquals("gpt_1", afterHidden[0].id)
        assertEquals("gpt_3", afterHidden[1].id)

        // 验证删除置顶账号后恢复默认顺序
        val afterDeleted = listOf(gpt1, gpt3)
            .filter { it.isVisibleOnMain }
            .sortedByDescending { it.isPinnedMain }

        assertEquals(2, afterDeleted.size)
        assertEquals("gpt_1", afterDeleted[0].id)
        assertEquals("gpt_3", afterDeleted[1].id)
    }
}
