package com.chronodesk.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.model.CredentialStatus
import com.chronodesk.app.data.provider.DeviceCodeResult
import com.chronodesk.app.data.provider.PollResult
import com.chronodesk.app.data.provider.RealCodexUsageProvider
import com.chronodesk.app.data.provider.TokenResult
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun DeviceAuthDialog(
    slot: Int,
    onDismiss: () -> Unit,
    onSuccess: (account: CodexAccount, refreshToken: String, accessToken: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val provider = remember { RealCodexUsageProvider() }

    var userCode by remember { mutableStateOf<String?>(null) }
    var verifyUrl by remember { mutableStateOf("https://auth.openai.com/codex/device") }
    var deviceAuthId by remember { mutableStateOf<String?>(null) }
    var pollIntervalSec by remember { mutableStateOf(5L) }
    var statusText by remember { mutableStateOf("正在获取设备授权码…") }
    var isPolling by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 启动 Device Code 流程
    LaunchedEffect(Unit) {
        when (val result = provider.startDeviceAuth()) {
            is DeviceCodeResult.Success -> {
                userCode = result.userCode
                verifyUrl = result.verificationUrl
                deviceAuthId = result.deviceAuthId
                pollIntervalSec = result.intervalSeconds
                statusText = "请在浏览器打开验证页面并确认"
                isPolling = true
            }
            is DeviceCodeResult.Error -> {
                errorMessage = result.message
                statusText = "获取授权码失败"
            }
        }
    }

    // 后台轮询
    LaunchedEffect(isPolling, deviceAuthId, userCode) {
        if (!isPolling || deviceAuthId == null || userCode == null) return@LaunchedEffect

        val dId = deviceAuthId!!
        val uCode = userCode!!

        while (isActive && isPolling) {
            delay(pollIntervalSec * 1000L)
            when (val poll = provider.pollDeviceAuth(dId, uCode)) {
                is PollResult.Success -> {
                    statusText = "授权成功！正在换取令牌…"
                    isPolling = false

                    when (val tokenRes = provider.exchangeToken(poll.authorizationCode, poll.codeVerifier)) {
                        is TokenResult.Success -> {
                            val acc = CodexAccount(
                                alias = if (slot == 0) "账号 A" else "账号 B",
                                credentialStatus = CredentialStatus.VALID,
                                authenticatedAt = System.currentTimeMillis(),
                                providerType = "OAUTH_DEVICE"
                            )
                            onSuccess(acc, tokenRes.refreshToken ?: "", tokenRes.accessToken)
                            onDismiss()
                        }
                        is TokenResult.ExpiredOrInvalid -> {
                            errorMessage = tokenRes.message
                        }
                        is TokenResult.TransientError -> {
                            errorMessage = tokenRes.message
                        }
                    }
                }
                is PollResult.Pending -> {
                    // 继续轮询
                }
                is PollResult.Expired -> {
                    errorMessage = poll.message
                    isPolling = false
                }
                is PollResult.Error -> {
                    errorMessage = poll.message
                    isPolling = false
                }
            }
        }
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
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "绑定 ChatGPT / Codex 账号",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "使用 OpenAI 官方设备码授权流程。请点击下方按钮在浏览器中完成登录并确认授权。",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                if (userCode != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "用户设备码",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userCode ?: "",
                            color = TechCyan,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 复制授权码按钮
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Codex Device Code", userCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "已复制授权码", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "复制", fontSize = 12.sp)
                        }

                        // 打开浏览器
                        Button(
                            onClick = {
                                val urlWithCode = "$verifyUrl?user_code=$userCode"
                                val customTabsIntent = CustomTabsIntent.Builder().build()
                                customTabsIntent.launchUrl(context, Uri.parse(urlWithCode))
                            },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "打开浏览器授权", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 轮询状态指示器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isPolling) {
                        CircularProgressIndicator(
                            color = TechCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = statusText,
                        color = if (errorMessage != null) TechRose else TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = TechRose,
                        fontSize = 11.sp
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "取消", color = TextSecondary)
                }
            }
        }
    }
}
