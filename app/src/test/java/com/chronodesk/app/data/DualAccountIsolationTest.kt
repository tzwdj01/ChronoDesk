package com.chronodesk.app.data

import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CodexUsageSnapshot
import com.chronodesk.app.data.model.CodexUsageWindow
import com.chronodesk.app.data.model.CredentialStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DualAccountIsolationTest {

    @Test
    fun testTwoAccountsHaveIndependentIdsAndAliases() {
        val accountA = CodexAccount(
            alias = "工作账号",
            email = "user.a@example.com",
            credentialStatus = CredentialStatus.VALID
        )
        val accountB = CodexAccount(
            alias = "个人主账号",
            email = "user.b@work.com",
            credentialStatus = CredentialStatus.VALID
        )

        assertNotEquals("两个账号ID必须完全独立", accountA.id, accountB.id)
        assertEquals("工作账号", accountA.displayTitle)
        assertEquals("个人主账号", accountB.displayTitle)

        // 当别名为空时回退至脱敏邮箱
        val accountWithoutAlias = CodexAccount(
            alias = "",
            email = "user.a@example.com",
            credentialStatus = CredentialStatus.VALID
        )
        assertEquals("us***a@example.com", accountWithoutAlias.displayTitle)

        // 当无邮箱时回退至别名
        val accountWithoutEmail = CodexAccount(
            alias = "独立备用账号",
            email = null,
            credentialStatus = CredentialStatus.VALID
        )
        assertEquals("独立备用账号", accountWithoutEmail.displayTitle)
    }

    @Test
    fun testKeepLastGoodOnTransientFailure() {
        val originalSnapshot = CodexUsageSnapshot(
            accountId = "acc-001",
            accountAlias = "主账号",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(20.0, 18000L, 1700010000L),
            secondaryWindow = CodexUsageWindow.fromRaw(50.0, 604800L, 1700500000L),
            queriedAt = 1700000000000L,
            isLastGood = false
        )

        // 模拟瞬时网络失败触发 Keep-Last-Good
        val degradedSnapshot = CodexUsageSnapshot.error(
            accountId = "acc-001",
            alias = "主账号",
            status = CredentialStatus.VALID,
            message = "网络超时，保留上次成功数据",
            lastGood = originalSnapshot
        )

        assertTrue("应被标记为 LastGood 缓存数据", degradedSnapshot.isLastGood)
        assertEquals("网络超时，保留上次成功数据", degradedSnapshot.errorMessage)
        assertNotNull("5小时限额数据应完整保留", degradedSnapshot.primaryWindow)
        assertEquals(80.0, degradedSnapshot.primaryWindow!!.remainingPercent!!, 0.01)
        assertNotNull("周限额数据应完整保留", degradedSnapshot.secondaryWindow)
        assertEquals(50.0, degradedSnapshot.secondaryWindow!!.remainingPercent!!, 0.01)
    }

    @Test
    fun testAccountAFailureDoesNotAffectAccountB() {
        val snapshotA = CodexUsageSnapshot.error(
            accountId = "acc-A",
            alias = "账号 A",
            status = CredentialStatus.EXPIRED,
            message = "Refresh token expired",
            lastGood = null
        )

        val snapshotB = CodexUsageSnapshot(
            accountId = "acc-B",
            accountAlias = "账号 B",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(15.0, 18000L, 1700020000L),
            queriedAt = 1700005000000L,
            isLastGood = false
        )

        assertEquals(CredentialStatus.EXPIRED, snapshotA.credentialStatus)
        assertNull("账号 A 额度应为空", snapshotA.primaryWindow)

        assertEquals(CredentialStatus.VALID, snapshotB.credentialStatus)
        assertNotNull("账号 B 额度应正常保持", snapshotB.primaryWindow)
        assertEquals(85.0, snapshotB.primaryWindow!!.remainingPercent!!, 0.01)
    }

    @Test
    fun testPrimaryExhaustedWithLimitReachedDoesNotAffectSecondaryQuota() {
        // 模拟 OpenAI 5小时额度用尽 (used=100%, allowed=false, limit_reached=true)，但周额度剩余 79% (used=21%)
        val snapshot = CodexUsageSnapshot(
            accountId = "acc-wang",
            accountAlias = "wang",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(100.0, 18000L, 1700010000L),
            secondaryWindow = CodexUsageWindow.fromRaw(21.0, 604800L, 1700500000L),
            limitReached = true,
            queriedAt = 1700000000000L,
            lastSuccessTime = 1700000000000L
        )

        val pw = snapshot.primaryWindow
        val sw = snapshot.secondaryWindow
        assertNotNull("主窗口数据不为空", pw)
        assertNotNull("副窗口数据不为空", sw)

        assertEquals("5小时主窗口剩余应为 0%", 0.0, pw!!.remainingPercent!!, 0.01)
        assertEquals("主窗口格式化应为耗尽", "耗尽", pw.formattedRemaining())

        assertEquals("周额度副窗口剩余应为 79%", 79.0, sw!!.remainingPercent!!, 0.01)
        assertEquals("副窗口格式化应为 79%", "79%", sw.formattedRemaining())

        // 验证主副窗口状态解耦：5小时限额绝不连带影响周额度显示
        val isPrimaryExhausted = (pw.remainingPercent != null && pw.remainingPercent!! <= 0.0) || snapshot.limitReached
        val isSecondaryExhausted = sw.remainingPercent != null && sw.remainingPercent!! <= 0.0

        assertTrue("5小时主窗口应被判定为耗尽/限额", isPrimaryExhausted)
        assertFalse("周额度副窗口绝不能被 limitReached 连带判定为耗尽", isSecondaryExhausted)
    }

    @Test
    fun testAccountA403KeepsLastGoodWhileAccountBUpdates() {
        val initialA = CodexUsageSnapshot(
            accountId = "acc-A",
            accountAlias = "账号 A",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(30.0, 18000L, 1700010000L),
            secondaryWindow = CodexUsageWindow.fromRaw(20.0, 604800L, 1700500000L),
            queriedAt = 1700000000000L,
            lastSuccessTime = 1700000000000L,
            isLastGood = false
        )
        // 账号 A 遇到 403 访问受限
        val snapA403 = CodexUsageSnapshot.error(
            accountId = "acc-A",
            alias = "账号 A",
            status = CredentialStatus.VALID,
            message = "访问受限",
            lastGood = initialA,
            errorType = com.chronodesk.app.data.model.SyncErrorType.ACCESS_DENIED
        )
        // 账号 B 正常刷新
        val snapB = CodexUsageSnapshot(
            accountId = "acc-B",
            accountAlias = "账号 B",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(10.0, 18000L, 1700020000L),
            secondaryWindow = CodexUsageWindow.fromRaw(15.0, 604800L, 1700520000L),
            queriedAt = 1700010000000L,
            lastSuccessTime = 1700010000000L,
            isLastGood = false
        )

        assertEquals("账号 A 应保持 VALID 状态而非失效", CredentialStatus.VALID, snapA403.credentialStatus)
        assertTrue("账号 A 应标记为 LastGood", snapA403.isLastGood)
        assertEquals("访问受限", snapA403.errorMessage)
        assertEquals(com.chronodesk.app.data.model.SyncErrorType.ACCESS_DENIED, snapA403.lastErrorType)
        assertEquals(70.0, snapA403.primaryWindow?.remainingPercent!!, 0.01)
        assertEquals(80.0, snapA403.secondaryWindow?.remainingPercent!!, 0.01)
        assertEquals(1700000000000L, snapA403.lastSuccessTime)

        assertEquals("账号 B 正常更新", 90.0, snapB.primaryWindow?.remainingPercent!!, 0.01)
        assertEquals(85.0, snapB.secondaryWindow?.remainingPercent!!, 0.01)
    }

    @Test
    fun testAuthExpiredVsTransientRefreshTimeout() {
        val originalSnapshot = CodexUsageSnapshot(
            accountId = "acc-test",
            accountAlias = "测试账号",
            credentialStatus = CredentialStatus.VALID,
            primaryWindow = CodexUsageWindow.fromRaw(40.0, 18000L, 1700010000L),
            secondaryWindow = CodexUsageWindow.fromRaw(30.0, 604800L, 1700500000L),
            queriedAt = 1700000000000L,
            lastSuccessTime = 1700000000000L
        )

        // 1. 刷新超时 (TransientError)：保留缓存
        val transientSnap = CodexUsageSnapshot.error(
            accountId = "acc-test",
            alias = "测试账号",
            status = CredentialStatus.VALID,
            message = "刷新超时，保留当前数据",
            lastGood = originalSnapshot,
            errorType = com.chronodesk.app.data.model.SyncErrorType.NETWORK_TIMEOUT
        )
        assertTrue("超时应保留缓存数据", transientSnap.isLastGood)
        assertNotNull("5小时额度不为空", transientSnap.primaryWindow)
        assertEquals(CredentialStatus.VALID, transientSnap.credentialStatus)

        // 2. 明确失效 (ExpiredOrInvalid)：清除缓存
        val expiredSnap = CodexUsageSnapshot.error(
            accountId = "acc-test",
            alias = "测试账号",
            status = CredentialStatus.EXPIRED,
            message = "Refresh token expired",
            lastGood = originalSnapshot,
            errorType = com.chronodesk.app.data.model.SyncErrorType.AUTH_FAILED
        )
        assertFalse("明确失效不应标记为 LastGood", expiredSnap.isLastGood)
        assertNull("明确失效必须清空额度", expiredSnap.primaryWindow)
        assertNull("明确失效必须清空周额度", expiredSnap.secondaryWindow)
        assertEquals(CredentialStatus.EXPIRED, expiredSnap.credentialStatus)
    }
}
