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
        assertEquals("user.a@example.com", accountA.displayTitle)
        assertEquals("user.b@work.com", accountB.displayTitle)

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
        assertEquals(80.0, degradedSnapshot.primaryWindow!!.remainingPercent, 0.01)
        assertNotNull("周限额数据应完整保留", degradedSnapshot.secondaryWindow)
        assertEquals(50.0, degradedSnapshot.secondaryWindow!!.remainingPercent, 0.01)
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
        assertEquals(85.0, snapshotB.primaryWindow!!.remainingPercent, 0.01)
    }
}
