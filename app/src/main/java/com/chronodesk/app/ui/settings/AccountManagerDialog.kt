package com.chronodesk.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.ui.clock.AiModelProvider
import com.chronodesk.app.ui.clock.ModelProviderLogo
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary

@Composable
fun AccountManagerDialog(
    initialSlot: Int = 0,
    initialAccountId: String? = null,
    accountA: CodexAccount?,
    accountB: CodexAccount?,
    deepSeekAccounts: List<CodexAccount> = emptyList(),
    getDeepSeekApiKey: (accountId: String) -> String? = { null },
    antigravityAccounts: List<CodexAccount> = emptyList(),
    getAntigravityApiKey: (accountId: String) -> String? = { null },
    onDismiss: () -> Unit,
    onSaveAccount: (slot: Int, account: CodexAccount, refreshToken: String, accessToken: String?) -> Boolean,
    onRemoveAccount: (slot: Int) -> Unit,
    onUpdateAlias: (slot: Int, newAlias: String) -> Unit,
    onSaveDeepSeekAccount: (accountId: String, apiKey: String, alias: String) -> Boolean = { _, _, _ -> true },
    onAddDeepSeekAccount: () -> CodexAccount? = { null },
    onRemoveDeepSeekAccount: (accountId: String) -> Unit = {},
    currentDeepSeekApiKey: String? = null,
    onSaveDeepSeekApiKey: (String) -> Boolean = { true },
    onSaveAntigravityAccount: (accountId: String, apiKey: String, alias: String) -> Boolean = { _, _, _ -> true },
    onAddAntigravityAccount: () -> CodexAccount? = { null },
    onRemoveAntigravityAccount: (accountId: String) -> Unit = {},
    currentAntigravityApiKey: String? = null,
    onSaveAntigravityApiKey: (String) -> Boolean = { true },
    onUpdateAccountVisibility: (accountId: String, showOnMain: Boolean, showOnSecond: Boolean, isPinnedMain: Boolean) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var localAccountA by remember(accountA) { mutableStateOf(accountA) }
    var localAccountB by remember(accountB) { mutableStateOf(accountB) }
    var localDeepSeekAccounts by remember(deepSeekAccounts) {
        mutableStateOf(
            if (deepSeekAccounts.isNotEmpty()) deepSeekAccounts
            else listOf(CodexAccount(id = "acc_deepseek", alias = "DeepSeek", modelProvider = "DEEPSEEK"))
        )
    }

    var localAntigravityAccounts by remember(antigravityAccounts) {
        mutableStateOf(
            if (antigravityAccounts.isNotEmpty()) antigravityAccounts
            else listOf(CodexAccount(id = "acc_antigravity", alias = "Google Antigravity", modelProvider = "ANTIGRAVITY"))
        )
    }

    val dsStartIndex = 2
    val dsCount = localDeepSeekAccounts.size
    val agStartIndex = dsStartIndex + dsCount
    val agCount = localAntigravityAccounts.size

    val defaultTab = remember {
        if (initialAccountId != null) {
            val agIdx = localAntigravityAccounts.indexOfFirst { it.id == initialAccountId }
            if (agIdx != -1) {
                agStartIndex + agIdx
            } else {
                val dsIdx = localDeepSeekAccounts.indexOfFirst { it.id == initialAccountId }
                if (dsIdx != -1) {
                    dsStartIndex + dsIdx
                } else if (accountB?.id == initialAccountId) {
                    1
                } else {
                    0
                }
            }
        } else {
            initialSlot.coerceAtLeast(0)
        }
    }
    var selectedTab by remember { mutableIntStateOf(defaultTab) }

    val currentAccount = when (selectedTab) {
        0 -> localAccountA
        1 -> localAccountB
        else -> null
    }

    var showDeviceAuthDialog by remember { mutableStateOf(false) }
    var showDirectTokenDialog by remember { mutableStateOf(false) }
    var aliasEdit by remember(selectedTab, currentAccount?.id) {
        mutableStateOf(
            if (currentAccount?.alias != currentAccount?.email && !currentAccount?.alias.isNullOrBlank() && !currentAccount?.alias!!.contains("账号")) {
                currentAccount.alias
            } else ""
        )
    }

    if (showDeviceAuthDialog) {
        DeviceAuthDialog(
            slot = selectedTab,
            onDismiss = { showDeviceAuthDialog = false },
            onSuccess = { account, refresh, access ->
                val saved = onSaveAccount(selectedTab, account, refresh, access)
                if (saved) {
                    showDeviceAuthDialog = false
                }
                saved
            }
        )
        return
    }

    if (showDirectTokenDialog) {
        DirectTokenDialog(
            slot = selectedTab,
            currentAccount = currentAccount,
            onDismiss = { showDirectTokenDialog = false },
            onSave = { account, refresh, access ->
                val saved = onSaveAccount(selectedTab, account, refresh, access)
                if (saved) {
                    showDirectTokenDialog = false
                }
                saved
            }
        )
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 540.dp, max = 660.dp)
                .fillMaxWidth(0.58f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AI 平台账号与接口管理",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // 平台与槽位切换 Tab (ChatGPT 1, ChatGPT 2, DeepSeek 1..., Google Antigravity 1...)
                val totalTabs = 2 + dsCount + agCount + (if (dsCount < 4) 1 else 0) + (if (agCount < 4) 1 else 0)
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.coerceIn(0, totalTabs - 1),
                    containerColor = DarkSurfaceElevated,
                    contentColor = TechCyan,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        val safeIdx = selectedTab.coerceIn(0, tabPositions.lastIndex)
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[safeIdx]),
                            color = TechCyan
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            aliasEdit = accountA?.alias?.takeIf { it != accountA.email } ?: ""
                        },
                        text = {
                            Text(
                                text = "ChatGPT ①",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            aliasEdit = accountB?.alias?.takeIf { it != accountB.email } ?: ""
                        },
                        text = {
                            Text(
                                text = "ChatGPT ②",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                    val circleSymbols = listOf("①", "②", "③", "④", "⑤")
                    localDeepSeekAccounts.forEachIndexed { idx, dsAcc ->
                        val tabIdx = dsStartIndex + idx
                        val defaultLabel = "DeepSeek ${circleSymbols.getOrElse(idx) { "${idx + 1}" }}"
                        val tabLabel = if (localDeepSeekAccounts.size == 1) {
                            if (dsAcc.alias.isNotBlank() && dsAcc.alias != "DeepSeek") dsAcc.alias.take(8) else "DeepSeek"
                        } else if (dsAcc.alias.isNotBlank() && !dsAcc.alias.startsWith("DeepSeek")) {
                            dsAcc.alias.take(8)
                        } else {
                            defaultLabel
                        }
                        Tab(
                            selected = selectedTab == tabIdx,
                            onClick = { selectedTab = tabIdx },
                            text = {
                                Text(
                                    text = tabLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }

                    localAntigravityAccounts.forEachIndexed { idx, agAcc ->
                        val tabIdx = agStartIndex + idx
                        val defaultLabel = "Gemini ${circleSymbols.getOrElse(idx) { "${idx + 1}" }}"
                        val tabLabel = if (localAntigravityAccounts.size == 1) {
                            if (agAcc.alias.isNotBlank() && agAcc.alias != "Google Antigravity" && agAcc.alias != "Gemini API") agAcc.alias.take(8) else "Gemini"
                        } else if (agAcc.alias.isNotBlank() && !agAcc.alias.startsWith("Gemini") && !agAcc.alias.startsWith("Google")) {
                            agAcc.alias.take(8)
                        } else {
                            defaultLabel
                        }
                        Tab(
                            selected = selectedTab == tabIdx,
                            onClick = { selectedTab = tabIdx },
                            text = {
                                Text(
                                    text = tabLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }

                    if (localDeepSeekAccounts.size < 4) {
                        Tab(
                            selected = false,
                            onClick = {
                                val added = onAddDeepSeekAccount()
                                if (added != null) {
                                    localDeepSeekAccounts = localDeepSeekAccounts + added
                                    selectedTab = dsStartIndex + localDeepSeekAccounts.lastIndex
                                    Toast.makeText(context, "已新增 DeepSeek 账号卡槽", Toast.LENGTH_SHORT).show()
                                }
                            },
                            text = {
                                Text(
                                    text = "+ DS账号",
                                    fontSize = 11.5.sp,
                                    color = TechCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }

                    if (localAntigravityAccounts.size < 4) {
                        Tab(
                            selected = false,
                            onClick = {
                                val added = onAddAntigravityAccount()
                                if (added != null) {
                                    localAntigravityAccounts = localAntigravityAccounts + added
                                    selectedTab = agStartIndex + localAntigravityAccounts.lastIndex
                                    Toast.makeText(context, "已新增 Gemini 账号卡槽", Toast.LENGTH_SHORT).show()
                                }
                            },
                            text = {
                                Text(
                                    text = "+ Gemini",
                                    fontSize = 11.5.sp,
                                    color = TechCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }

                if (selectedTab < 2) {
                    // ================= ChatGPT 账号面板 =================
                    if (currentAccount != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceElevated)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "账号绑定邮箱", color = TextTertiary, fontSize = 11.sp)
                                Text(
                                    text = currentAccount.maskedEmail ?: currentAccount.displayTitle,
                                    color = TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "账号状态", color = TextTertiary, fontSize = 11.sp)
                                val (stColor, stText) = when (currentAccount.credentialStatus) {
                                    CredentialStatus.VALID -> TechEmerald to "已授权 / 正常在线"
                                    CredentialStatus.EXPIRED -> TechRose to "凭据已失效 (需重新登录)"
                                    else -> TechEmerald to "已授权 / 正常在线"
                                }
                                Text(text = stText, color = stColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "接入类型", color = TextTertiary, fontSize = 11.sp)
                                Text(text = "Codex OAuth 2.0 PKCE", color = TextSecondary, fontSize = 11.sp)
                            }

                            HorizontalDivider(
                                color = DarkBorder.copy(alpha = 0.6f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            Text(
                                text = "自定义显示名称 (优先展示，避免邮箱暴露)：",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = aliasEdit,
                                    onValueChange = { aliasEdit = it },
                                    placeholder = { Text("例如：主力账号、工作号", fontSize = 11.5.sp, color = TextTertiary) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedIndicatorColor = TechCyan,
                                        unfocusedIndicatorColor = DarkBorder
                                    )
                                )
                                Button(
                                    onClick = {
                                        onUpdateAlias(selectedTab, aliasEdit.trim())
                                        Toast.makeText(context, "已更新账号显示名称", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TechCyan.copy(alpha = 0.22f),
                                        contentColor = TechCyan
                                    )
                                ) {
                                    Text("保存名称", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(
                                color = DarkBorder.copy(alpha = 0.6f),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            // 账号在主屏/第二屏的展示位置选择与置顶
                            AccountVisibilityToggleRow(
                                showOnMain = currentAccount.isVisibleOnMain,
                                showOnSecond = currentAccount.isVisibleOnSecond,
                                isPinnedMain = currentAccount.isPinnedMain,
                                onVisibilityChanged = { m, s, p ->
                                    onUpdateAccountVisibility(currentAccount.id, m, s, p)
                                    if (selectedTab == 0) {
                                        localAccountA = localAccountA?.copy(showOnMainScreen = m, showOnSecondScreen = s, isPinnedMain = p)
                                    } else {
                                        localAccountB = localAccountB?.copy(showOnMainScreen = m, showOnSecondScreen = s, isPinnedMain = p)
                                    }
                                }
                            )
                        }
                    }

                    // 接入方式操作按钮列表
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showDeviceAuthDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TechCyan.copy(alpha = 0.15f),
                                contentColor = TechCyan
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "重新进行官方设备码授权 (Device Code)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showDirectTokenDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceElevated,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "直接导入 / 更新 Access Token 凭据", fontSize = 13.sp)
                        }
                    }
                } else if (selectedTab in dsStartIndex until agStartIndex) {
                    // ================= DeepSeek 官方接口配置面板 (支持多账号独立配置与自定义别名) =================
                    val curDsIndex = (selectedTab - dsStartIndex).coerceIn(0, localDeepSeekAccounts.lastIndex)
                    val curDsAcc = localDeepSeekAccounts[curDsIndex]

                    var curDsKeyInput by remember(curDsAcc.id) {
                        mutableStateOf(getDeepSeekApiKey(curDsAcc.id) ?: (if (curDsIndex == 0) currentDeepSeekApiKey ?: "" else ""))
                    }
                    var curDsAliasInput by remember(curDsAcc.id, curDsAcc.alias) {
                        mutableStateOf(if (curDsAcc.alias != "DeepSeek" && !curDsAcc.alias.startsWith("DeepSeek ")) curDsAcc.alias else "")
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ModelProviderLogo(provider = AiModelProvider.DEEPSEEK, size = 18.dp, isNightMode = true)
                                Text(
                                    text = "DeepSeek 官方 API 配置",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val isDsConfigured = curDsAcc.credentialStatus == CredentialStatus.VALID && curDsKeyInput.isNotBlank()
                            val (stColor, stText) = when {
                                isDsConfigured -> TechEmerald to "已配置 / 正常在线"
                                curDsAcc.credentialStatus == CredentialStatus.EXPIRED -> TechRose to "Key失效"
                                else -> TextTertiary to "未配置"
                            }
                            Text(text = stText, color = stColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "直连官方 /user/balance 接口实时查询真实余额与明细。支持多个不同 API Key 独立展示与命名。",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        // 1. 自定义账号名称 (参考 ChatGPT 方式)
                        OutlinedTextField(
                            value = curDsAliasInput,
                            onValueChange = { curDsAliasInput = it },
                            label = { Text("自定义账号名称 / 备注 (留空显示 DeepSeek)", fontSize = 11.sp) },
                            placeholder = { Text("例如：主力开发、测试账号、个人号", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedIndicatorColor = TechCyan,
                                unfocusedIndicatorColor = DarkBorder
                            )
                        )

                        // 2. API Key 输入框
                        OutlinedTextField(
                            value = curDsKeyInput,
                            onValueChange = { curDsKeyInput = it },
                            label = { Text("DeepSeek API Key (sk-...)", fontSize = 11.sp) },
                            placeholder = { Text("sk-xxxxxxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedIndicatorColor = TechCyan,
                                unfocusedIndicatorColor = DarkBorder
                            )
                        )

                        // 账号在主屏/第二屏的展示位置选择与置顶
                        AccountVisibilityToggleRow(
                            showOnMain = curDsAcc.isVisibleOnMain,
                            showOnSecond = curDsAcc.isVisibleOnSecond,
                            isPinnedMain = curDsAcc.isPinnedMain,
                            onVisibilityChanged = { m, s, p ->
                                onUpdateAccountVisibility(curDsAcc.id, m, s, p)
                                localDeepSeekAccounts = localDeepSeekAccounts.mapIndexed { idx, acc ->
                                    if (idx == curDsIndex) acc.copy(showOnMainScreen = m, showOnSecondScreen = s, isPinnedMain = p) else acc
                                }
                            }
                        )

                        // 3. 操作按钮组
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val aliasToSave = curDsAliasInput.trim()
                                    val saved = onSaveDeepSeekAccount(curDsAcc.id, curDsKeyInput.trim(), aliasToSave)
                                    if (saved) {
                                        val label = aliasToSave.ifBlank { "DeepSeek" }
                                        Toast.makeText(context, "已保存「$label」API Key 并触发验证", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "设备安全存储不可用，凭据未保存", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TechEmerald,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "保存并立即验证余额", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }

                            if (localDeepSeekAccounts.size > 1) {
                                OutlinedButton(
                                    onClick = {
                                        onRemoveDeepSeekAccount(curDsAcc.id)
                                        Toast.makeText(context, "已删除该 DeepSeek 账号", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TechRose)
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "删除", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    // ================= Google Antigravity 官方接口配置面板 (支持多账号独立配置与自定义别名) =================
                    val curAgIndex = (selectedTab - agStartIndex).coerceIn(0, localAntigravityAccounts.lastIndex)
                    val curAgAcc = localAntigravityAccounts[curAgIndex]

                    var curAgKeyInput by remember(curAgAcc.id) {
                        mutableStateOf(getAntigravityApiKey(curAgAcc.id) ?: (if (curAgIndex == 0) currentAntigravityApiKey ?: "" else ""))
                    }
                    var curAgAliasInput by remember(curAgAcc.id, curAgAcc.alias) {
                        mutableStateOf(if (curAgAcc.alias != "Google Antigravity" && curAgAcc.alias != "Gemini API" && !curAgAcc.alias.startsWith("Google Antigravity ") && !curAgAcc.alias.startsWith("Gemini API ")) curAgAcc.alias else "")
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ModelProviderLogo(provider = AiModelProvider.ANTIGRAVITY, size = 18.dp, isNightMode = true)
                                Text(
                                    text = "Gemini API 连接配置",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val isAgConfigured = curAgAcc.credentialStatus == CredentialStatus.VALID && curAgKeyInput.isNotBlank()
                            val (stColor, stText) = when {
                                isAgConfigured -> TechEmerald to "已配置 / 正常在线"
                                curAgAcc.credentialStatus == CredentialStatus.EXPIRED -> TechRose to "Key失效"
                                else -> TextTertiary to "未配置"
                            }
                            Text(text = stText, color = stColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "直连 Google Generative Language 官方接口验证模型列表与连接状态。支持自定义别名与多 API Key 接入。",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        // 1. 自定义账号名称
                        OutlinedTextField(
                            value = curAgAliasInput,
                            onValueChange = { curAgAliasInput = it },
                            label = { Text("自定义账号名称 / 备注 (留空显示 Gemini API)", fontSize = 11.sp) },
                            placeholder = { Text("例如：工作主号、Gemini Flash、实验账号", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedIndicatorColor = TechCyan,
                                unfocusedIndicatorColor = DarkBorder
                            )
                        )

                        // 2. Google API Key 输入框
                        OutlinedTextField(
                            value = curAgKeyInput,
                            onValueChange = { curAgKeyInput = it },
                            label = { Text("Gemini API Key (AIzaSy...)", fontSize = 11.sp) },
                            placeholder = { Text("AIzaSyxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedIndicatorColor = TechCyan,
                                unfocusedIndicatorColor = DarkBorder
                            )
                        )

                        // 账号在主屏/第二屏的展示位置选择与置顶
                        AccountVisibilityToggleRow(
                            showOnMain = curAgAcc.isVisibleOnMain,
                            showOnSecond = curAgAcc.isVisibleOnSecond,
                            isPinnedMain = curAgAcc.isPinnedMain,
                            onVisibilityChanged = { m, s, p ->
                                onUpdateAccountVisibility(curAgAcc.id, m, s, p)
                                localAntigravityAccounts = localAntigravityAccounts.mapIndexed { idx, acc ->
                                    if (idx == curAgIndex) acc.copy(showOnMainScreen = m, showOnSecondScreen = s, isPinnedMain = p) else acc
                                }
                            }
                        )

                        // 3. 操作按钮组
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val aliasToSave = curAgAliasInput.trim()
                                    val saved = onSaveAntigravityAccount(curAgAcc.id, curAgKeyInput.trim(), aliasToSave)
                                    if (saved) {
                                        val label = aliasToSave.ifBlank { "Gemini API" }
                                        Toast.makeText(context, "已保存「$label」API Key 并触发验证", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "设备安全存储不可用，凭据未保存", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TechEmerald,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "保存并立即验证连接", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }

                            if (localAntigravityAccounts.size > 1) {
                                OutlinedButton(
                                    onClick = {
                                        onRemoveAntigravityAccount(curAgAcc.id)
                                        Toast.makeText(context, "已删除该 Gemini 账号", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TechRose)
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "删除", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "关闭", color = TextSecondary)
                }
            }
        }
    }
}

/**
 * 账号在屏幕上的显示位置选择组件（主屏幕显示 / 第二屏显示 / 主屏常用置顶）
 */
@Composable
private fun AccountVisibilityToggleRow(
    showOnMain: Boolean,
    showOnSecond: Boolean,
    isPinnedMain: Boolean,
    onVisibilityChanged: (showOnMain: Boolean, showOnSecond: Boolean, isPinnedMain: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "看板显示与排序：",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            val statusHint = when {
                isPinnedMain && showOnMain && showOnSecond -> "两屏展示 · 主屏置顶"
                isPinnedMain && showOnMain -> "仅主屏展示 · 主屏置顶"
                showOnMain && showOnSecond -> "两屏均展示"
                showOnMain -> "仅主屏幕展示"
                showOnSecond -> "仅第二屏展示"
                else -> "已完全隐藏"
            }
            Text(
                text = statusHint,
                color = if (showOnMain || showOnSecond) TechCyan else TechRose,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 主屏幕显示 Toggle Chip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showOnMain) TechCyan.copy(alpha = 0.18f) else DarkSurfaceElevated)
                    .border(1.dp, if (showOnMain) TechCyan else DarkBorder, RoundedCornerShape(6.dp))
                    .clickable {
                        val nextMain = !showOnMain
                        onVisibilityChanged(nextMain, showOnSecond, if (nextMain) isPinnedMain else false)
                    }
                    .padding(vertical = 6.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showOnMain) "✓ 主屏幕" else "+ 主屏幕",
                    color = if (showOnMain) TechCyan else TextTertiary,
                    fontSize = 10.5.sp,
                    fontWeight = if (showOnMain) FontWeight.Bold else FontWeight.Normal
                )
            }

            // 第二屏显示 Toggle Chip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showOnSecond) TechEmerald.copy(alpha = 0.18f) else DarkSurfaceElevated)
                    .border(1.dp, if (showOnSecond) TechEmerald else DarkBorder, RoundedCornerShape(6.dp))
                    .clickable { onVisibilityChanged(showOnMain, !showOnSecond, isPinnedMain) }
                    .padding(vertical = 6.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showOnSecond) "✓ 扩展屏" else "+ 扩展屏",
                    color = if (showOnSecond) TechEmerald else TextTertiary,
                    fontSize = 10.5.sp,
                    fontWeight = if (showOnSecond) FontWeight.Bold else FontWeight.Normal
                )
            }

            // 主屏常用置顶 Toggle Chip
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isPinnedMain) TechAmber.copy(alpha = 0.18f) else DarkSurfaceElevated)
                    .border(1.dp, if (isPinnedMain) TechAmber else DarkBorder, RoundedCornerShape(6.dp))
                    .clickable {
                        val nextPinned = !isPinnedMain
                        onVisibilityChanged(if (nextPinned) true else showOnMain, showOnSecond, nextPinned)
                    }
                    .padding(vertical = 6.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPinnedMain) "★ 主屏常用" else "☆ 主屏常用",
                    color = if (isPinnedMain) TechAmber else TextTertiary,
                    fontSize = 10.5.sp,
                    fontWeight = if (isPinnedMain) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
