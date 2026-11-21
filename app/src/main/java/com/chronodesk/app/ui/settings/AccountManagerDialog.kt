package com.chronodesk.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    onDismiss: () -> Unit,
    onSaveAccount: (slot: Int, account: CodexAccount, refreshToken: String, accessToken: String?) -> Unit,
    onRemoveAccount: (slot: Int) -> Unit,
    onUpdateAlias: (slot: Int, newAlias: String) -> Unit,
    onSaveDeepSeekAccount: (accountId: String, apiKey: String, alias: String) -> Unit = { _, _, _ -> },
    onAddDeepSeekAccount: () -> CodexAccount? = { null },
    onRemoveDeepSeekAccount: (accountId: String) -> Unit = {},
    currentDeepSeekApiKey: String? = null,
    onSaveDeepSeekApiKey: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var localDeepSeekAccounts by remember(deepSeekAccounts) {
        mutableStateOf(
            if (deepSeekAccounts.isNotEmpty()) deepSeekAccounts
            else listOf(CodexAccount(id = "acc_deepseek", alias = "DeepSeek", modelProvider = "DEEPSEEK"))
        )
    }

    val defaultTab = remember {
        if (initialAccountId != null) {
            val dsIdx = localDeepSeekAccounts.indexOfFirst { it.id == initialAccountId }
            if (dsIdx != -1) {
                2 + dsIdx
            } else if (accountB?.id == initialAccountId) {
                1
            } else {
                0
            }
        } else {
            initialSlot.coerceAtLeast(0)
        }
    }
    var selectedTab by remember { mutableIntStateOf(defaultTab) }

    val currentAccount = when (selectedTab) {
        0 -> accountA
        1 -> accountB
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
                onSaveAccount(selectedTab, account, refresh, access)
                showDeviceAuthDialog = false
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
                onSaveAccount(selectedTab, account, refresh, access)
                showDirectTokenDialog = false
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

                // 平台与槽位切换 Tab (ChatGPT 1, ChatGPT 2, DeepSeek 1, DeepSeek 2...)
                val totalTabs = 2 + localDeepSeekAccounts.size + (if (localDeepSeekAccounts.size < 4) 1 else 0)
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
                        val tabIdx = 2 + idx
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
                    if (localDeepSeekAccounts.size < 4) {
                        Tab(
                            selected = false,
                            onClick = {
                                val added = onAddDeepSeekAccount()
                                if (added != null) {
                                    localDeepSeekAccounts = localDeepSeekAccounts + added
                                    selectedTab = 2 + localDeepSeekAccounts.lastIndex
                                    Toast.makeText(context, "已新增 DeepSeek 账号卡槽", Toast.LENGTH_SHORT).show()
                                }
                            },
                            text = {
                                Text(
                                    text = "+ 新增账号",
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
                } else {
                    // ================= DeepSeek 官方接口配置面板 (支持多账号独立配置与自定义别名) =================
                    val curDsIndex = (selectedTab - 2).coerceIn(0, localDeepSeekAccounts.lastIndex)
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
                            Text(
                                text = "DeepSeek 官方 API 配置",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
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

                        // 3. 操作按钮组
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val aliasToSave = curDsAliasInput.trim()
                                    onSaveDeepSeekAccount(curDsAcc.id, curDsKeyInput.trim(), aliasToSave)
                                    val label = aliasToSave.ifBlank { "DeepSeek" }
                                    Toast.makeText(context, "已保存「$label」API Key 并触发验证", Toast.LENGTH_SHORT).show()
                                    onDismiss()
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
