package com.chronodesk.app.ui.settings

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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    accountA: CodexAccount?,
    accountB: CodexAccount?,
    currentDeepSeekApiKey: String? = null,
    onDismiss: () -> Unit,
    onSaveAccount: (slot: Int, account: CodexAccount, refreshToken: String, accessToken: String?) -> Unit,
    onRemoveAccount: (slot: Int) -> Unit,
    onUpdateAlias: (slot: Int, newAlias: String) -> Unit,
    onSaveDeepSeekApiKey: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(initialSlot.coerceIn(0, 2)) }
    val currentAccount = when (selectedTab) {
        0 -> accountA
        1 -> accountB
        else -> null
    }

    var showDeviceAuthDialog by remember { mutableStateOf(false) }
    var showDirectTokenDialog by remember { mutableStateOf(false) }
    var aliasEdit by remember {
        mutableStateOf(currentAccount?.email ?: currentAccount?.displayTitle ?: if (selectedTab == 0) "alex19900223@gmail.com" else "wangxinyu297@jd.com")
    }
    var deepSeekKeyInput by remember { mutableStateOf(currentDeepSeekApiKey ?: "") }

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

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AI 平台账号与接口管理",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // 平台与槽位切换 Tab (ChatGPT 1, ChatGPT 2, DeepSeek)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurfaceElevated,
                    contentColor = TechCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = TechCyan
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            aliasEdit = accountA?.email ?: accountA?.displayTitle ?: "alex19900223@gmail.com"
                        },
                        text = {
                            Text(
                                text = "ChatGPT ①",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            aliasEdit = accountB?.email ?: accountB?.displayTitle ?: "wangxinyu297@jd.com"
                        },
                        text = {
                            Text(
                                text = "ChatGPT ②",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "DeepSeek",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
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
                                    text = currentAccount.email ?: currentAccount.alias,
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
                    // ================= DeepSeek 官方接口配置面板 =================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "DeepSeek 官方 API 配置",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "直接填入 DeepSeek API Key (sk-...)，时钟将直连官方 /user/balance 接口查询账户真实余额与用量明细。",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = deepSeekKeyInput,
                            onValueChange = { deepSeekKeyInput = it },
                            label = { Text("DeepSeek API Key", fontSize = 11.sp) },
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

                        Button(
                            onClick = {
                                onSaveDeepSeekApiKey(deepSeekKeyInput.trim())
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TechEmerald,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "保存并立即验证 DeepSeek 余额", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
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
