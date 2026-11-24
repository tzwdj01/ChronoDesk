package com.chronodesk.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chronodesk.app.data.model.CodexAccount
import com.chronodesk.app.data.provider.GoogleAntigravityUsageProvider
import com.chronodesk.app.data.provider.TokenResult
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBackground
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkWarningRed
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Antigravity (agy) 官方 Google OAuth 2.0 PKCE 登录授权对话框：
 * - 在 127.0.0.1 随机端口起本地 HTTP 回调监听，浏览器完成 Google 授权后自动换取 Token 并落库；
 * - 同时支持复制链接到电脑/主力机浏览器授权后，把跳转地址 `http://127.0.0.1:...` 粘贴回来完成换取。
 */
@Composable
fun AntigravityOAuthDialog(
    currentAccount: CodexAccount?,
    isNightMode: Boolean = false,
    onDismiss: () -> Unit,
    onSuccess: (
        email: String?,
        refreshToken: String,
        accessToken: String?,
        expiresInSeconds: Long,
        idToken: String?
    ) -> Boolean
) {
    val dialogBg = if (isNightMode) DarkSurface else InkBackground
    val cardBg = if (isNightMode) DarkSurfaceElevated else InkSurface
    val borderColor = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val accentColor = if (isNightMode) TechCyan else Color(0xFF0284C7)
    val onAccentColor = if (isNightMode) Color.Black else Color.White
    val errorColor = if (isNightMode) TechRose else InkWarningRed

    val context = LocalContext.current
    val provider = remember { GoogleAntigravityUsageProvider() }
    val pkce = remember { provider.generatePkcePair() }
    val scope = rememberCoroutineScope()

    var authUrl by remember { mutableStateOf<String?>(null) }
    var redirectUri by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("正在启动本机回调服务…") }
    var isWaiting by remember { mutableStateOf(true) }
    var isExchanging by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var manualCallbackInput by remember { mutableStateOf("") }
    var activeSocket by remember { mutableStateOf<ServerSocket?>(null) }

    suspend fun exchangeAndComplete(code: String) {
        val redir = redirectUri ?: return
        isExchanging = true
        isWaiting = false
        errorMessage = null
        statusText = "正在向 Google 换取访问令牌…"
        when (val tokenRes = provider.exchangeCode(code, redir, pkce.verifier)) {
            is TokenResult.Success -> {
                val refresh = tokenRes.refreshToken
                if (refresh.isNullOrBlank()) {
                    isExchanging = false
                    errorMessage = "Google 未返回 refresh_token，请重试并在授权页勾选全部权限"
                    statusText = "授权不完整"
                    return
                }
                statusText = "正在识别 Google 账号邮箱…"
                val email = provider.fetchIdentityEmail(tokenRes.accessToken, tokenRes.idToken)
                val saved = onSuccess(
                    email,
                    refresh,
                    tokenRes.accessToken,
                    tokenRes.expiresInSeconds,
                    tokenRes.idToken
                )
                if (saved) {
                    onDismiss()
                } else {
                    isExchanging = false
                    errorMessage = "设备安全存储不可用，凭据未保存"
                    statusText = "保存失败"
                }
            }
            is TokenResult.ExpiredOrInvalid -> {
                isExchanging = false
                errorMessage = tokenRes.message
                statusText = "授权无效"
            }
            is TokenResult.TransientError -> {
                isExchanging = false
                errorMessage = tokenRes.message
                statusText = "换取令牌失败"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                activeSocket?.close()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        var server: ServerSocket? = null
        try {
            server = withContext(Dispatchers.IO) {
                ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"))
            }
            activeSocket = server
            val port = server.localPort
            val redir = provider.redirectUri(port)
            redirectUri = redir
            authUrl = provider.authorizationUrl(redir, pkce)
            statusText = "请点击上方按钮打开浏览器完成 Google 授权"

            val capturedCode = withContext(Dispatchers.IO) {
                var resultCode: String? = null
                while (isActive && !server.isClosed && resultCode == null) {
                    val socket: Socket = try {
                        server.accept()
                    } catch (_: Exception) {
                        break
                    }
                    socket.use { clientSock ->
                        clientSock.soTimeout = 5000
                        val reader = BufferedReader(InputStreamReader(clientSock.getInputStream()))
                        val requestLine = reader.readLine() ?: return@use
                        // 读取其余请求头直至空行
                        var line = reader.readLine()
                        while (!line.isNullOrEmpty()) {
                            line = reader.readLine()
                        }
                        val parts = requestLine.split(" ")
                        if (parts.size < 2 || parts[0] != "GET") {
                            writeHttpResponse(clientSock, "404 Not Found", "")
                            return@use
                        }
                        val target = parts[1]
                        val path = target.substringBefore("?")
                        if (path != GoogleAntigravityUsageProvider.CALLBACK_PATH) {
                            writeHttpResponse(clientSock, "404 Not Found", "")
                            return@use
                        }
                        val parsed = provider.extractCodeFromCallbackInput(target, expectedState = pkce.state)
                        if (parsed.isSuccess) {
                            writeHttpResponse(clientSock, "200 OK", HTML_CALLBACK_SUCCESS)
                            resultCode = parsed.getOrNull()
                        } else {
                            writeHttpResponse(clientSock, "400 Bad Request", HTML_CALLBACK_FAILED)
                            errorMessage = parsed.exceptionOrNull()?.message
                        }
                    }
                }
                resultCode
            }

            if (capturedCode != null && !isExchanging) {
                exchangeAndComplete(capturedCode)
            }
        } catch (e: Exception) {
            if (isActive && !isExchanging) {
                isWaiting = false
                errorMessage = "本机回调监听启动失败: ${e.message}"
                statusText = "回调服务异常"
            }
        } finally {
            try {
                server?.close()
            } catch (_: Exception) {
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 560.dp)
                .heightIn(max = 460.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(dialogBg)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "授权 Google Antigravity 账号",
                    color = primaryText,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "复用 agy 官方 OAuth 2.0 PKCE 流程登录 Google 账号，直接查询 Antigravity 各模型组（Gemini / Claude / GPT）真实额度与套餐。",
                    color = secondaryText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                if (currentAccount?.maskedEmail != null) {
                    Text(
                        text = "当前绑定：${currentAccount.maskedEmail}",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val url = authUrl ?: return@OutlinedButton
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Antigravity OAuth URL", url))
                            Toast.makeText(context, "已复制登录链接", Toast.LENGTH_SHORT).show()
                        },
                        enabled = authUrl != null && !isExchanging,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "复制链接", fontSize = 11.5.sp, color = secondaryText)
                    }

                    Button(
                        onClick = {
                            val url = authUrl ?: return@Button
                            try {
                                val customTabsIntent = CustomTabsIntent.Builder().build()
                                customTabsIntent.launchUrl(context, Uri.parse(url))
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "无法打开浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = authUrl != null && !isExchanging,
                        modifier = Modifier.weight(1.8f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = onAccentColor,
                            disabledContainerColor = borderColor.copy(alpha = 0.55f),
                            disabledContentColor = secondaryText
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "打开浏览器登录 Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 跨设备登录回调粘贴兜底区
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "若在另一台设备打开链接登录，请复制浏览器跳转后的 http://127.0.0.1:... 完整地址粘贴到下方：",
                        color = secondaryText,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualCallbackInput,
                            onValueChange = { manualCallbackInput = it },
                            placeholder = { Text("http://127.0.0.1:.../oauth-callback?code=...", fontSize = 10.5.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = dialogBg,
                                unfocusedContainerColor = dialogBg,
                                focusedTextColor = primaryText,
                                unfocusedTextColor = primaryText,
                                focusedIndicatorColor = accentColor,
                                unfocusedIndicatorColor = borderColor
                            )
                        )
                        Button(
                            onClick = {
                                val parsed = provider.extractCodeFromCallbackInput(
                                    manualCallbackInput,
                                    expectedState = pkce.state
                                )
                                val code = parsed.getOrNull()
                                if (code != null) {
                                    scope.launch {
                                        exchangeAndComplete(code)
                                    }
                                } else {
                                    errorMessage = parsed.exceptionOrNull()?.message ?: "回调地址无效"
                                }
                            },
                            enabled = manualCallbackInput.isNotBlank() && redirectUri != null && !isExchanging,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = onAccentColor,
                            disabledContainerColor = borderColor.copy(alpha = 0.55f),
                            disabledContentColor = secondaryText
                        )
                        ) {
                            Text("完成授权", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isWaiting || isExchanging) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = statusText,
                        color = if (errorMessage != null) errorColor else secondaryText,
                        fontSize = 11.5.sp
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = errorColor,
                        fontSize = 11.sp
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text(text = "取消", color = secondaryText)
                }
            }
        }
    }
}

/**
 * Antigravity 凭据 / Token 导入对话框：
 * 支持导入 cc-switch (`agy_accounts.json`) / agy 钥匙串 JSON、Refresh Token (`1//...`) 或 Access Token (`ya29...`)。
 */
@Composable
fun AntigravityTokenDialog(
    currentAccount: CodexAccount?,
    isNightMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (
        alias: String,
        email: String?,
        refreshToken: String?,
        accessToken: String?,
        expiresInSeconds: Long,
        idToken: String?
    ) -> Boolean
) {
    val dialogBg = if (isNightMode) DarkSurface else InkBackground
    val fieldBg = if (isNightMode) DarkSurfaceElevated else InkSurface
    val borderColor = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val accentColor = if (isNightMode) TechCyan else Color(0xFF0284C7)
    val onAccentColor = if (isNightMode) Color.Black else Color.White
    val errorColor = if (isNightMode) TechRose else InkWarningRed

    val provider = remember { GoogleAntigravityUsageProvider() }
    val scope = rememberCoroutineScope()

    var alias by remember {
        val raw = currentAccount?.alias?.trim().orEmpty()
        val initial = if (raw == "Antigravity" || raw == "Gemini API" || raw == "Google Antigravity") "" else raw
        mutableStateOf(initial)
    }
    var rawCredential by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .widthIn(max = 540.dp)
                .heightIn(max = 460.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(dialogBg)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "导入 Antigravity 凭据 / Token",
                    color = primaryText,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "支持粘贴 CC Switch 账号 JSON（agy_accounts.json 单条）、Refresh Token (1//...) 或 Access Token (ya29...)。凭据仅保存在 Android Keystore 加密存储中。",
                    color = secondaryText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("自定义显示名称（留空展示脱敏邮箱）", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = borderColor
                    )
                )

                OutlinedTextField(
                    value = rawCredential,
                    onValueChange = { rawCredential = it },
                    label = { Text("OAuth 凭据 JSON / Refresh Token / Access Token", fontSize = 11.5.sp) },
                    placeholder = { Text("1//... 或 ya29... 或 {\"token\":{...}}", fontSize = 11.sp) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = borderColor
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = errorColor,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isVerifying,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Text(text = "取消", color = secondaryText)
                    }

                    Button(
                        onClick = {
                            val parsed = GoogleAntigravityUsageProvider.parseCredentialInput(rawCredential)
                            if (parsed == null) {
                                errorMessage = "请输入有效的 Antigravity OAuth JSON、Refresh Token (1//...) 或 Access Token (ya29...)"
                                return@Button
                            }
                            scope.launch {
                                isVerifying = true
                                errorMessage = null
                                var finalAccess = parsed.accessToken
                                var finalRefresh = parsed.refreshToken
                                var finalId = parsed.idToken
                                var finalExpires = parsed.expiresInSeconds
                                var finalEmail = parsed.email

                                if (finalAccess.isNullOrBlank() && !finalRefresh.isNullOrBlank()) {
                                    when (val refreshRes = provider.refreshToken(finalRefresh)) {
                                        is TokenResult.Success -> {
                                            finalAccess = refreshRes.accessToken
                                            finalRefresh = refreshRes.refreshToken ?: finalRefresh
                                            finalId = refreshRes.idToken ?: finalId
                                            finalExpires = refreshRes.expiresInSeconds
                                        }
                                        is TokenResult.ExpiredOrInvalid -> {
                                            isVerifying = false
                                            errorMessage = refreshRes.message
                                            return@launch
                                        }
                                        is TokenResult.TransientError -> {
                                            isVerifying = false
                                            errorMessage = refreshRes.message
                                            return@launch
                                        }
                                    }
                                }

                                if (finalEmail.isNullOrBlank() && !finalAccess.isNullOrBlank()) {
                                    finalEmail = provider.fetchIdentityEmail(finalAccess, finalId)
                                }

                                val saved = onSave(
                                    alias.trim().ifEmpty { "Antigravity" },
                                    finalEmail,
                                    finalRefresh,
                                    finalAccess,
                                    finalExpires,
                                    finalId
                                )
                                isVerifying = false
                                if (saved) {
                                    onDismiss()
                                } else {
                                    errorMessage = "设备安全存储不可用，凭据未保存"
                                }
                            }
                        },
                        enabled = !isVerifying,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = onAccentColor,
                            disabledContainerColor = borderColor.copy(alpha = 0.55f),
                            disabledContentColor = secondaryText
                        )
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = onAccentColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(text = if (isVerifying) "验证中…" else "保存并验证", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun writeHttpResponse(socket: Socket, status: String, body: String) {
    val bodyBytes = body.toByteArray(Charsets.UTF_8)
    val header = "HTTP/1.1 $status\r\n" +
        "Content-Type: text/html; charset=utf-8\r\n" +
        "Content-Length: ${bodyBytes.size}\r\n" +
        "Connection: close\r\n" +
        "Cache-Control: no-store\r\n\r\n"
    val out = socket.getOutputStream()
    out.write(header.toByteArray(Charsets.UTF_8))
    out.write(bodyBytes)
    out.flush()
}

private const val HTML_CALLBACK_SUCCESS =
    "<!doctype html><meta charset=utf-8><title>ChronoDesk</title><body style=\"font-family:system-ui;padding:42px\"><h2>Antigravity 授权成功</h2><p>可以关闭此浏览器页面，返回 ChronoDesk 额度时钟查看实时额度。</p></body>"

private const val HTML_CALLBACK_FAILED =
    "<!doctype html><meta charset=utf-8><title>ChronoDesk</title><body style=\"font-family:system-ui;padding:42px\"><h2>登录没有完成</h2><p>请返回 ChronoDesk 额度时钟重新发起授权。</p></body>"
