package com.chronodesk.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chronodesk.app.data.provider.LongbridgeAuthResult
import com.chronodesk.app.data.repository.LongbridgeOAuthLaunch
import com.chronodesk.app.ui.longbridge.LongbridgeProviderLogo
import com.chronodesk.app.ui.longbridge.LongbridgeViewModel
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkStatusGreen
import com.chronodesk.app.ui.theme.InkSurface
import com.chronodesk.app.ui.theme.InkSurfaceElevated
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.InkTextTertiary
import com.chronodesk.app.ui.theme.InkWarningAmber
import com.chronodesk.app.ui.theme.InkWarningRed
import com.chronodesk.app.ui.theme.TechAmber
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TechRose
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary
import com.chronodesk.app.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LongbridgeSettingsDialog(
    viewModel: LongbridgeViewModel,
    isNightMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isCompact = config.screenHeightDp < 420
    val maxDialogHeight = (config.screenHeightDp * 0.90f).dp.coerceAtMost(580.dp)

    val currentSettings by viewModel.settings.collectAsState()
    val snapshot by viewModel.snapshot.collectAsState()

    var enabled by remember { mutableStateOf(currentSettings.enabled) }
    var showOnHome by remember { mutableStateOf(currentSettings.showOnHome) }
    var displayCurrency by remember { mutableStateOf(currentSettings.normalizedDisplayCurrency) }
    var colorScheme by remember {
        mutableStateOf(if (currentSettings.isRedUpGreenDown) "RED_UP_GREEN_DOWN" else "GREEN_UP_RED_DOWN")
    }
    var includeExtendedHours by remember { mutableStateOf(currentSettings.includeExtendedHours) }
    var privacyMaskAmounts by remember { mutableStateOf(currentSettings.privacyMaskAmounts) }
    var region by remember { mutableStateOf(currentSettings.normalizedRegion) }
    var refreshIntervalSec by remember { mutableIntStateOf(currentSettings.refreshIntervalSec) }
    var accountAlias by remember { mutableStateOf(currentSettings.accountAlias) }

    var showAuthModal by remember { mutableStateOf(false) }

    val bg = if (isNightMode) DarkSurface else InkSurface
    val innerBg = if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
    val border = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val accentColor = if (isNightMode) TechCyan else Color(0xFF0284C7)
    val onAccentText = if (isNightMode) Color.Black else Color.White
    val dangerColor = if (isNightMode) TechRose else InkWarningRed
    val positiveColor = if (isNightMode) TechEmerald else InkStatusGreen
    val warningColor = if (isNightMode) TechAmber else InkWarningAmber

    if (showAuthModal) {
        LongbridgeAuthModalDialog(
            viewModel = viewModel,
            selectedRegion = region,
            isNightMode = isNightMode,
            onRegionSyncBeforeAuth = { chosenRegion ->
                viewModel.updateSettings { it.copy(region = chosenRegion, enabled = true) }
            },
            onAuthSuccess = {
                enabled = true
                showAuthModal = false
                Toast.makeText(context, "长桥只读授权成功，已自动同步资产", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAuthModal = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 620.dp)
                .heightIn(max = maxDialogHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(16.dp))
                .padding(if (isCompact) 14.dp else 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 顶栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LongbridgeProviderLogo(size = 22.dp, isNightMode = isNightMode)
                        Text(
                            text = "长桥证券与资产看板设置 (只读 MCP)",
                            color = primaryText,
                            fontSize = if (isCompact) 14.sp else 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

                // 可滚动配置区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
                ) {
                    // 1. 账号授权状态卡片
                    val hasCreds = viewModel.hasCredentials()
                    val conn = snapshot.connectionState
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(innerBg)
                            .border(0.8.dp, border, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = when {
                                            conn.isAuthExpired -> dangerColor
                                            hasCreds -> positiveColor
                                            else -> warningColor
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = when {
                                            conn.isAuthExpired -> "授权状态：令牌已过期，请重新授权"
                                            hasCreds -> "授权状态：已连接长桥只读通道"
                                            else -> "授权状态：未授权长桥账号"
                                        },
                                        color = primaryText,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (conn.lastSuccessTime != null) {
                                    Text(
                                        text = "上次同步 ${snapshot.formattedSyncTime}",
                                        color = secondaryText,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Text(
                                text = "采用官方无状态 Streamable HTTP MCP 协议获取资产、盈亏与持仓，不申请交易下单权限，不占用唯一行情长连接。",
                                color = secondaryText,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(accentColor)
                                        .clickable { showAuthModal = true }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = onAccentText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (hasCreds) "更换或重新授权" else "立即只读授权连接",
                                            color = onAccentText,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (hasCreds) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(dangerColor.copy(alpha = 0.14f))
                                            .clickable {
                                                viewModel.disconnect()
                                                Toast.makeText(context, "已清除长桥凭据与缓存", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = dangerColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "断开连接",
                                                color = dangerColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. 开关区：启用全屏看板 + 首页并列卡 + 盘前盘后 + 金额脱敏
                    LongbridgeToggleRow(
                        title = "启用长桥资产看板",
                        subtitle = "开启后主界面将增加独立全屏「长桥资产」滑屏页面",
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        isNightMode = isNightMode,
                        innerBg = innerBg,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentColor = accentColor
                    )

                    LongbridgeToggleRow(
                        title = "首页并列显示「长桥证券」卡片",
                        subtitle = "与 ChatGPT / DeepSeek / Antigravity 并列展示净资产、现金余额与当日盈亏",
                        checked = showOnHome,
                        onCheckedChange = { showOnHome = it },
                        isNightMode = isNightMode,
                        innerBg = innerBg,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentColor = accentColor
                    )

                    LongbridgeToggleRow(
                        title = "含美股盘前 / 盘后 / 夜盘报价",
                        subtitle = "美股非常规盘中时段优先取盘前、盘后或夜盘实时价计算持仓当日盈亏",
                        checked = includeExtendedHours,
                        onCheckedChange = { includeExtendedHours = it },
                        isNightMode = isNightMode,
                        innerBg = innerBg,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentColor = accentColor
                    )

                    LongbridgeToggleRow(
                        title = "金额隐私脱敏 (****)",
                        subtitle = "隐藏净资产、现金与盈亏具体金额，仅保留百分比涨跌幅变化",
                        checked = privacyMaskAmounts,
                        onCheckedChange = { privacyMaskAmounts = it },
                        isNightMode = isNightMode,
                        innerBg = innerBg,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentColor = accentColor
                    )

                    // 3. 折算币种 (默认 HKD)
                    LongbridgeChipSelector(
                        label = "主展示计价币种",
                        options = listOf(
                            "HKD" to "港币 HKD",
                            "USD" to "美元 USD",
                            "CNY" to "人民币 CNY",
                            "ACCOUNT" to "跟随账户本位"
                        ),
                        selected = displayCurrency,
                        onSelect = { displayCurrency = it },
                        isNightMode = isNightMode,
                        innerBg = innerBg,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentColor = accentColor,
                        onAccentText = onAccentText
                    )

                    // 4. 涨跌配色方案 (默认 红涨绿跌)
                    LongbridgeChipSelector(
                        label = "当日盈亏与涨跌配色",
                        options = listOf(
                            "RED_UP_GREEN_DOWN" to "红涨绿跌 (港股/A股习惯)",
                            "GREEN_UP_RED_DOWN" to "绿涨红跌 (美股国际习惯)"
                        ),
                        selected = colorScheme,
                        onSelect = { colorScheme = it },
                        isNightMode = isNightMode,
                        innerBg = innerBg,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentColor = accentColor,
                        onAccentText = onAccentText
                    )

                    // 5. 接入区域与刷新频率
                    LongbridgeChipSelector(
                        label = "长桥账户接入区域",
                        options = listOf(
                            "CN" to "中国大陆站 (.cn)",
                            "GLOBAL" to "国际站 (.com)"
                        ),
                        selected = region,
                        onSelect = { region = it },
                        isNightMode = isNightMode,
                        innerBg = innerBg,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentColor = accentColor,
                        onAccentText = onAccentText
                    )

                    LongbridgeChipSelector(
                        label = "前台自动轮询间隔",
                        options = listOf(
                            "30" to "30 秒",
                            "60" to "60 秒 (默认)",
                            "120" to "2 分钟",
                            "300" to "5 分钟"
                        ),
                        selected = refreshIntervalSec.toString(),
                        onSelect = { refreshIntervalSec = it.toIntOrNull() ?: 60 },
                        isNightMode = isNightMode,
                        innerBg = innerBg,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentColor = accentColor,
                        onAccentText = onAccentText
                    )

                    // 6. 显示名称自定义
                    OutlinedTextField(
                        value = accountAlias,
                        onValueChange = { accountAlias = it },
                        label = { Text("看板卡片名称", fontSize = 11.sp) },
                        placeholder = { Text("长桥证券", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = border,
                            focusedTextColor = primaryText,
                            unfocusedTextColor = primaryText,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = secondaryText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))

                // 底部取消 & 保存按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, border, RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(text = "取消", color = secondaryText, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor)
                            .clickable {
                                viewModel.updateSettings { old ->
                                    old.copy(
                                        enabled = enabled,
                                        showOnHome = showOnHome,
                                        displayCurrency = displayCurrency,
                                        colorScheme = colorScheme,
                                        includeExtendedHours = includeExtendedHours,
                                        privacyMaskAmounts = privacyMaskAmounts,
                                        region = region,
                                        refreshIntervalSec = refreshIntervalSec,
                                        accountAlias = accountAlias.trim().ifBlank { "长桥证券" }
                                    )
                                }
                                Toast.makeText(context, "长桥设置已保存", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = onAccentText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "保存设置",
                                color = onAccentText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LongbridgeToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isNightMode: Boolean,
    innerBg: Color,
    primaryText: Color,
    secondaryText: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(innerBg)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Text(
                text = title,
                color = primaryText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = secondaryText,
                fontSize = 10.sp,
                lineHeight = 13.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isNightMode) Color.Black else Color.White,
                checkedTrackColor = accentColor
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LongbridgeChipSelector(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    isNightMode: Boolean,
    innerBg: Color,
    primaryText: Color,
    secondaryText: Color,
    accentColor: Color,
    onAccentText: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            color = primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (key, title) ->
                val isSel = selected == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) accentColor else innerBg)
                        .clickable { onSelect(key) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = title,
                        color = if (isSel) onAccentText else secondaryText,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 长桥只读授权子弹窗（3 种方式）：
 * 0. 10 分钟只读授权码兑换 (/connect -> mcp.longbridge.cn/agent authenticate)
 * 1. OAuth 2.1 PKCE 浏览器回环授权
 * 2. 本地 Token / ~/.longbridge/credentials.json 导入
 */
@Composable
fun LongbridgeAuthModalDialog(
    viewModel: LongbridgeViewModel,
    selectedRegion: String,
    isNightMode: Boolean,
    onRegionSyncBeforeAuth: (String) -> Unit,
    onAuthSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val maxDialogHeight = (config.screenHeightDp * 0.90f).dp.coerceAtMost(560.dp)
    val scope = rememberCoroutineScope()

    var authMode by remember { mutableIntStateOf(0) } // 0: auth_code, 1: oauth_pkce, 2: token_import
    var region by remember { mutableStateOf(selectedRegion) }
    var oneTimeCode by remember { mutableStateOf("") }
    var tokenImportText by remember { mutableStateOf("") }
    var manualOAuthInput by remember { mutableStateOf("") }

    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // OAuth loopback state
    var activeSocket by remember { mutableStateOf<ServerSocket?>(null) }
    var oauthLaunch by remember { mutableStateOf<LongbridgeOAuthLaunch?>(null) }
    var oauthInitAttempt by remember { mutableIntStateOf(0) }

    val cardBg = if (isNightMode) DarkSurface else InkSurface
    val innerBg = if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
    val border = if (isNightMode) DarkBorder else InkBorder
    val primaryText = if (isNightMode) TextPrimary else InkTextPrimary
    val secondaryText = if (isNightMode) TextSecondary else InkTextSecondary
    val mutedText = if (isNightMode) TextTertiary else InkTextTertiary
    val accentColor = if (isNightMode) TechCyan else Color(0xFF0284C7)
    val onAccentText = if (isNightMode) Color.Black else Color.White
    val dangerColor = if (isNightMode) TechRose else InkWarningRed
    val warningColor = if (isNightMode) TechAmber else InkWarningAmber

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = accentColor,
        contentColor = onAccentText,
        disabledContainerColor = border.copy(alpha = 0.6f),
        disabledContentColor = mutedText
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                activeSocket?.close()
            } catch (_: Exception) {
            }
        }
    }

    // 当处于 OAuth 模式时启动本机回环监听并注册公开 PKCE 客户端
    LaunchedEffect(authMode, region, oauthInitAttempt) {
        if (authMode != 1) {
            try {
                activeSocket?.close()
            } catch (_: Exception) {
            }
            activeSocket = null
            oauthLaunch = null
            return@LaunchedEffect
        }

        errorMessage = null
        statusMessage = "正在初始化本机 OAuth 2.1 PKCE 回调服务..."
        onRegionSyncBeforeAuth(region)
        var server: ServerSocket? = null
        try {
            server = withContext(Dispatchers.IO) { ServerSocket(0) }
            activeSocket = server
            val redirectUri = "http://127.0.0.1:${server.localPort}/callback"
            val reg = viewModel.registerAndBuildOAuthRequest(redirectUri)
            val launchObj = reg.getOrElse { ex ->
                errorMessage = ex.message ?: "OAuth 客户端注册失败"
                statusMessage = "初始化失败，可点击重试或改用「10分钟授权码」"
                return@LaunchedEffect
            }
            oauthLaunch = launchObj
            statusMessage = "回调监听就绪，点击下方按钮前往长桥浏览器只读授权"

            val capturedRawCallback = withContext(Dispatchers.IO) {
                var callbackTarget: String? = null
                while (isActive && !server.isClosed && callbackTarget == null) {
                    val sock: Socket = try {
                        server.accept()
                    } catch (_: Exception) {
                        break
                    }
                    sock.use { client ->
                        client.soTimeout = 5000
                        val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                        val requestLine = reader.readLine() ?: return@use
                        var line = reader.readLine()
                        while (!line.isNullOrEmpty()) {
                            line = reader.readLine()
                        }
                        val parts = requestLine.split(" ")
                        if (parts.size < 2 || parts[0] != "GET") {
                            writeLoopbackHtml(client, "404 Not Found", "")
                            return@use
                        }
                        val target = parts[1]
                        if (!target.startsWith("/callback")) {
                            writeLoopbackHtml(client, "404 Not Found", "")
                            return@use
                        }
                        writeLoopbackHtml(
                            client,
                            "200 OK",
                            "<html><meta charset='utf-8'><body style='font-family:sans-serif;text-align:center;padding:40px'><h2>长桥授权回调已接收</h2><p>请返回 ChronoDesk 桌面时钟等待完成资产同步。</p></body></html>"
                        )
                        callbackTarget = target
                    }
                }
                callbackTarget
            }

            if (capturedRawCallback != null && isActive) {
                isBusy = true
                statusMessage = "已捕获授权回调，正在换取只读访问令牌..."
                val res = viewModel.completeOAuthAuthorization(launchObj, capturedRawCallback)
                isBusy = false
                when (res) {
                    is LongbridgeAuthResult.Success -> onAuthSuccess()
                    is LongbridgeAuthResult.AuthError -> errorMessage = res.message
                    is LongbridgeAuthResult.NetworkError -> errorMessage = res.message
                }
            }
        } catch (e: Exception) {
            if (isActive && authMode == 1) {
                errorMessage = "OAuth 监听异常：${e.message}"
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
                .fillMaxWidth(0.84f)
                .widthIn(max = 560.dp)
                .heightIn(max = maxDialogHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, border, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "长桥只读授权连接",
                        color = primaryText,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭", tint = secondaryText)
                    }
                }

                // 区域快速切换 (.cn vs .com)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("接入节点:", color = secondaryText, fontSize = 11.5.sp)
                    listOf("CN" to "大陆站 .cn", "GLOBAL" to "国际站 .com").forEach { (rKey, rLabel) ->
                        val isSel = region == rKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) accentColor else innerBg)
                                .clickable {
                                    region = rKey
                                    onRegionSyncBeforeAuth(rKey)
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = rLabel,
                                color = if (isSel) onAccentText else secondaryText,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // 授权方式切换栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "10分钟授权码",
                        1 to "OAuth 浏览器",
                        2 to "Token/JSON导入"
                    ).forEach { (idx, label) ->
                        val isSel = authMode == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isSel) accentColor else innerBg)
                                .clickable {
                                    authMode = idx
                                    errorMessage = null
                                    statusMessage = null
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) onAccentText else secondaryText,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 安全警告提示条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(warningColor.copy(alpha = 0.12f))
                        .border(0.8.dp, warningColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "安全提示：请务必仅授予「读取账户/资产/持仓」只读权限，切勿勾选交易下单 (`trade.write`) 权限。",
                        color = if (isNightMode) TechAmber else Color(0xFFB45309),
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp
                    )
                }

                when (authMode) {
                    0 -> {
                        val connectUrl = if (region == "GLOBAL") {
                            "https://open.longbridge.com/connect"
                        } else {
                            "https://open.longbridge.cn/connect"
                        }
                        Text(
                            text = "1. 打开长桥授权页面，登录并确认只读权限，获取 10 分钟一次性授权码：\n$connectUrl",
                            color = secondaryText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    openBrowserUrl(context, connectUrl)
                                },
                                modifier = Modifier.weight(1f),
                                colors = buttonColors
                            ) {
                                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("打开长桥授权页", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Longbridge Connect URL", connectUrl))
                                    Toast.makeText(context, "授权网址已复制", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(15.dp), tint = secondaryText)
                                Spacer(Modifier.width(4.dp))
                                Text("复制网址", fontSize = 11.5.sp, color = primaryText)
                            }
                        }

                        OutlinedTextField(
                            value = oneTimeCode,
                            onValueChange = {
                                oneTimeCode = it
                                errorMessage = null
                            },
                            label = { Text("2. 粘贴一次性授权码 (10分钟内有效)", fontSize = 11.sp) },
                            placeholder = { Text("例如 6位~32位授权码或验证口令", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = border,
                                focusedTextColor = primaryText,
                                unfocusedTextColor = primaryText
                            )
                        )

                        Button(
                            onClick = {
                                isBusy = true
                                errorMessage = null
                                statusMessage = "正在通过长桥 MCP 兑换只读访问令牌..."
                                onRegionSyncBeforeAuth(region)
                                scope.launch {
                                    val res = viewModel.authenticateWithOneTimeCode(oneTimeCode)
                                    isBusy = false
                                    when (res) {
                                        is LongbridgeAuthResult.Success -> onAuthSuccess()
                                        is LongbridgeAuthResult.AuthError -> errorMessage = res.message
                                        is LongbridgeAuthResult.NetworkError -> errorMessage = res.message
                                    }
                                }
                            },
                            enabled = !isBusy && oneTimeCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColors
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    strokeWidth = 2.dp,
                                    color = onAccentText
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("正在兑换令牌...", fontSize = 12.sp)
                            } else {
                                Text("立即兑换令牌并同步资产", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    1 -> {
                        val launch = oauthLaunch
                        Button(
                            onClick = {
                                launch?.let { openBrowserUrl(context, it.authorizeUrl) }
                            },
                            enabled = !isBusy && launch != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColors
                        ) {
                            Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (launch == null) "正在注册 PKCE 客户端..." else "在浏览器中完成 OAuth 只读授权",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (launch == null && errorMessage != null) {
                            OutlinedButton(
                                onClick = { oauthInitAttempt++ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("重试 OAuth 初始化", fontSize = 11.5.sp, color = primaryText)
                            }
                        }

                        OutlinedTextField(
                            value = manualOAuthInput,
                            onValueChange = {
                                manualOAuthInput = it
                                errorMessage = null
                            },
                            label = { Text("回调 URL 或授权 Code（若未自动跳转则手动粘贴）", fontSize = 10.5.sp) },
                            placeholder = { Text("http://127.0.0.1:.../callback?code=...&state=...", fontSize = 10.5.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = border,
                                focusedTextColor = primaryText,
                                unfocusedTextColor = primaryText
                            )
                        )

                        Button(
                            onClick = {
                                val targetLaunch = oauthLaunch ?: return@Button
                                isBusy = true
                                errorMessage = null
                                statusMessage = "正在交换 OAuth 令牌..."
                                scope.launch {
                                    val res = viewModel.completeOAuthAuthorization(targetLaunch, manualOAuthInput)
                                    isBusy = false
                                    when (res) {
                                        is LongbridgeAuthResult.Success -> onAuthSuccess()
                                        is LongbridgeAuthResult.AuthError -> errorMessage = res.message
                                        is LongbridgeAuthResult.NetworkError -> errorMessage = res.message
                                    }
                                }
                            },
                            enabled = !isBusy && oauthLaunch != null && manualOAuthInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColors
                        ) {
                            Text("手动提交回调完成授权", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        Text(
                            text = "支持粘贴长桥 CLI 凭据文件内容（~/.longbridge/credentials.json，包含 access_token / refresh_token），或直接粘贴 Bearer Token。",
                            color = secondaryText,
                            fontSize = 10.5.sp,
                            lineHeight = 14.5.sp
                        )
                        OutlinedTextField(
                            value = tokenImportText,
                            onValueChange = {
                                tokenImportText = it
                                errorMessage = null
                            },
                            label = { Text("凭据 JSON 或 Bearer Token", fontSize = 11.sp) },
                            placeholder = { Text("{\"access_token\":\"...\",\"refresh_token\":\"...\"} 或直接粘贴 Token", fontSize = 10.5.sp) },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = border,
                                focusedTextColor = primaryText,
                                unfocusedTextColor = primaryText
                            )
                        )
                        Button(
                            onClick = {
                                isBusy = true
                                errorMessage = null
                                statusMessage = "正在校验并导入长桥凭据..."
                                onRegionSyncBeforeAuth(region)
                                scope.launch {
                                    val res = viewModel.importCredentialText(tokenImportText)
                                    isBusy = false
                                    when (res) {
                                        is LongbridgeAuthResult.Success -> onAuthSuccess()
                                        is LongbridgeAuthResult.AuthError -> errorMessage = res.message
                                        is LongbridgeAuthResult.NetworkError -> errorMessage = res.message
                                    }
                                }
                            },
                            enabled = !isBusy && tokenImportText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = buttonColors
                        ) {
                            Text(
                                text = if (isBusy) "校验中..." else "导入凭据并立即测试连接",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage!!,
                        color = accentColor,
                        fontSize = 11.sp
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        color = dangerColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun openBrowserUrl(context: Context, url: String) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun writeLoopbackHtml(socket: Socket, statusLine: String, htmlBody: String) {
    val bodyBytes = htmlBody.toByteArray(Charsets.UTF_8)
    val resp = buildString {
        append("HTTP/1.1 $statusLine\r\n")
        append("Content-Type: text/html; charset=utf-8\r\n")
        append("Content-Length: ${bodyBytes.size}\r\n")
        append("Connection: close\r\n")
        append("\r\n")
    }
    val out = socket.getOutputStream()
    out.write(resp.toByteArray(Charsets.US_ASCII))
    if (bodyBytes.isNotEmpty()) {
        out.write(bodyBytes)
    }
    out.flush()
}
