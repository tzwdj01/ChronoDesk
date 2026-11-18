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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Science
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
    onDismiss: () -> Unit,
    onSaveAccount: (slot: Int, account: CodexAccount, refreshToken: String, accessToken: String?) -> Unit,
    onRemoveAccount: (slot: Int) -> Unit,
    onUpdateAlias: (slot: Int, newAlias: String) -> Unit
) {
    var selectedSlot by remember { mutableIntStateOf(initialSlot) }
    val currentAccount = if (selectedSlot == 0) accountA else accountB

    var showDeviceAuthDialog by remember { mutableStateOf(false) }
    var showDirectTokenDialog by remember { mutableStateOf(false) }
    var aliasEdit by remember { mutableStateOf(currentAccount?.alias ?: if (selectedSlot == 0) "账号 A" else "账号 B") }

    if (showDeviceAuthDialog) {
        DeviceAuthDialog(
            slot = selectedSlot,
            onDismiss = { showDeviceAuthDialog = false },
            onSuccess = { account, refresh, access ->
                onSaveAccount(selectedSlot, account, refresh, access)
                showDeviceAuthDialog = false
            }
        )
        return
    }

    if (showDirectTokenDialog) {
        DirectTokenDialog(
            slot = selectedSlot,
            currentAccount = currentAccount,
            onDismiss = { showDirectTokenDialog = false },
            onSave = { account, refresh, access ->
                onSaveAccount(selectedSlot, account, refresh, access)
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
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "账号与凭据管理",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // 槽位切换 Tab
                TabRow(
                    selectedTabIndex = selectedSlot,
                    containerColor = DarkSurfaceElevated,
                    contentColor = TechCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSlot]),
                            color = TechCyan
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedSlot == 0,
                        onClick = {
                            selectedSlot = 0
                            aliasEdit = accountA?.alias ?: "账号 A"
                        },
                        text = {
                            Text(
                                text = "账号 A ${if (accountA != null) "✓" else ""}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                    Tab(
                        selected = selectedSlot == 1,
                        onClick = {
                            selectedSlot = 1
                            aliasEdit = accountB?.alias ?: "账号 B"
                        },
                        text = {
                            Text(
                                text = "账号 B ${if (accountB != null) "✓" else ""}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }

                // 当前账号状态面板
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
                            Text(text = "账号状态", color = TextTertiary, fontSize = 11.sp)
                            val (stColor, stText) = when (currentAccount.credentialStatus) {
                                CredentialStatus.VALID -> TechEmerald to "已授权 / 正常"
                                CredentialStatus.EXPIRED -> TechRose to "凭据已失效"
                                else -> TechAmber to "待同步"
                            }
                            Text(text = stText, color = stColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "接入类型", color = TextTertiary, fontSize = 11.sp)
                            Text(text = currentAccount.providerType, color = TextSecondary, fontSize = 11.sp)
                        }

                        // 修改别名
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = aliasEdit,
                                onValueChange = { aliasEdit = it },
                                label = { Text("账号别名", fontSize = 10.sp) },
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onUpdateAlias(selectedSlot, aliasEdit) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TechCyan.copy(alpha = 0.2f), contentColor = TechCyan)
                            ) {
                                Text("更新", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "当前槽位未配置账号，请选择下方接入方式",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // 接入方式操作按钮列表
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Device Code
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
                        Text(text = "官方设备码授权 (Device Code 推荐)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // 2. Direct Token
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
                        Text(text = "直接导入 Token / 凭证 (高级)", fontSize = 13.sp)
                    }

                    // 3. Mock Provider
                    OutlinedButton(
                        onClick = {
                            val mockAccount = CodexAccount(
                                alias = if (selectedSlot == 0) "高保真测试账号 A" else "高保真测试账号 B",
                                email = if (selectedSlot == 0) "dev-a@example.com" else "dev-b@example.com",
                                credentialStatus = CredentialStatus.VALID,
                                authenticatedAt = System.currentTimeMillis(),
                                providerType = "MOCK"
                            )
                            onSaveAccount(selectedSlot, mockAccount, "mock_refresh_token", "mock_access_token")
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp), tint = TechEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "启用模拟数据账号 (离线测试与展示)", fontSize = 13.sp, color = TechEmerald)
                    }

                    // 4. 删除账号
                    if (currentAccount != null) {
                        OutlinedButton(
                            onClick = {
                                onRemoveAccount(selectedSlot)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = TechRose)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "删除此账号并清除凭据", fontSize = 13.sp, color = TechRose)
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
