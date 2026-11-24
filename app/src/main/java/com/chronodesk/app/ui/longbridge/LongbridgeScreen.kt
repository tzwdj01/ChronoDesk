package com.chronodesk.app.ui.longbridge

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.LongbridgeFullSnapshot
import com.chronodesk.app.data.model.LongbridgeIndexQuote
import com.chronodesk.app.data.model.LongbridgeMarketTemp
import com.chronodesk.app.data.model.LongbridgePositionItem
import com.chronodesk.app.data.model.LongbridgeSettings
import com.chronodesk.app.ui.clock.MockBadge
import com.chronodesk.app.ui.theme.DarkBackground
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkProgressTrack
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBackground
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkProgressTrack
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
import com.chronodesk.app.ui.theme.LocalPureInkMode
import com.chronodesk.app.ui.theme.PureInkBorder
import com.chronodesk.app.ui.theme.PureInkTextPrimary
import com.chronodesk.app.ui.theme.PureInkTextSecondary
import com.chronodesk.app.ui.theme.resolveChronoPalette
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

private data class LongbridgeCockpitPalette(
    val isNight: Boolean,
    val isPureInk: Boolean,
    val bg: Color,
    val cardBg: Color,
    val innerBg: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val positive: Color,
    val warning: Color,
    val danger: Color,
    val trackBg: Color
) {
    companion object {
        fun from(isNightMode: Boolean, isPureInk: Boolean = false): LongbridgeCockpitPalette {
            val t = resolveChronoPalette(isNightMode, !isNightMode && isPureInk)
            return LongbridgeCockpitPalette(
                isNight = t.isNight,
                isPureInk = t.isPureInk,
                bg = t.bg,
                cardBg = t.surface,
                innerBg = t.surfaceElevated,
                border = t.border,
                primaryText = t.primaryText,
                secondaryText = t.secondaryText,
                mutedText = t.mutedText,
                accent = t.accent,
                positive = t.positive,
                warning = t.warning,
                danger = t.danger,
                trackBg = t.progressTrack
            )
        }
    }
}

/**
 * 统一涨跌配色解析（支持红涨绿跌、绿涨红跌及纯水墨纯黑白灰模式）
 */
fun resolveLongbridgePnlColor(
    value: Double?,
    isRedUpGreenDown: Boolean,
    isNightMode: Boolean,
    isPureInk: Boolean = false
): Color {
    if (!isNightMode && isPureInk) {
        return if (value == null || !value.isFinite() || abs(value) <= 0.004) PureInkTextSecondary else PureInkTextPrimary
    }
    val neutral = if (isNightMode) TextSecondary else InkTextSecondary
    if (value == null || !value.isFinite() || abs(value) <= 0.004) return neutral
    val red = if (isNightMode) TechRose else InkWarningRed
    val green = if (isNightMode) TechEmerald else InkStatusGreen
    return if (value > 0.0) {
        if (isRedUpGreenDown) red else green
    } else {
        if (isRedUpGreenDown) green else red
    }
}

/**
 * 长桥证券官方品牌徽标（1:1 还原 open.longbridge.com app-icon.svg 多色立柱；全水墨模式收敛为纯灰阶立柱）
 */
@Composable
fun LongbridgeProviderLogo(
    size: Dp = 22.dp,
    isNightMode: Boolean = false,
    isPureInk: Boolean = LocalPureInkMode.current,
    modifier: Modifier = Modifier
) {
    val usePureInk = !isNightMode && isPureInk
    val bgColor = if (usePureInk) Color(0xFF1A1A1A) else Color(0xFF0A0E17)
    val borderColor = if (usePureInk) PureInkBorder else if (isNightMode) DarkBorder else InkBorder.copy(alpha = 0.65f)
    val shape = RoundedCornerShape(if (size <= 18.dp) 5.dp else 7.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgColor)
            .border(0.6.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.56f)) {
            val sx = this.size.width / 69f
            val sy = this.size.height / 69f

            fun bar(x: Float, y: Float, w: Float, h: Float, hexColor: Long) {
                drawRect(
                    color = Color(hexColor),
                    topLeft = Offset(x * sx, y * sy),
                    size = Size(w * sx, h * sy)
                )
            }

            if (usePureInk) {
                bar(0f, 0f, 3f, 69f, 0xFFFFFFFF)
                bar(7f, 0f, 10f, 69f, 0xFFD4D4D4)
                bar(21f, 60f, 9f, 9f, 0xFFE5E5E5)
                bar(33f, 60f, 3f, 9f, 0xFFFFFFFF)
                bar(40f, 52f, 10f, 17f, 0xFFA3A3A3)
                bar(53f, 43f, 9f, 26f, 0xFFFFFFFF)
                bar(66f, 26f, 3f, 43f, 0xFFA3A3A3)
            } else {
                bar(0f, 0f, 3f, 69f, 0xFFFFFFFF)
                bar(7f, 0f, 10f, 69f, 0xFF00DBB6)
                bar(21f, 60f, 9f, 9f, 0xFFFFE000)
                bar(33f, 60f, 3f, 9f, 0xFFFFFFFF)
                bar(40f, 52f, 10f, 17f, 0xFFFC5200)
                bar(53f, 43f, 9f, 26f, 0xFFFFFFFF)
                bar(66f, 26f, 3f, 43f, 0xFFFC5200)
            }
        }
    }
}

@Composable
fun LongbridgeScreen(
    snapshot: LongbridgeFullSnapshot,
    settings: LongbridgeSettings,
    isNightMode: Boolean,
    currentTimeString: String,
    isCompact: Boolean = false,
    onRefresh: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onTogglePrivacy: () -> Unit = {},
    onCycleCurrency: () -> Unit = {},
    onToggleColorScheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPureInk = !isNightMode && LocalPureInkMode.current
    val palette = remember(isNightMode, isPureInk) { LongbridgeCockpitPalette.from(isNightMode, isPureInk) }
    val masked = settings.privacyMaskAmounts
    val showLiveData = snapshot.hasValidAssetData || snapshot.isMock

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
            .padding(
                start = if (isCompact) 12.dp else 18.dp,
                end = if (isCompact) 12.dp else 18.dp,
                top = if (isCompact) 5.dp else 10.dp,
                bottom = if (isCompact) 12.dp else 18.dp
            )
    ) {
        LongbridgeCockpitTopBar(
            settings = settings,
            snapshot = snapshot,
            currentTimeString = currentTimeString,
            palette = palette,
            isCompact = isCompact,
            onTogglePrivacy = onTogglePrivacy,
            onCycleCurrency = onCycleCurrency,
            onToggleColorScheme = onToggleColorScheme,
            onRefresh = onRefresh,
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 10.dp))

        if (snapshot.connectionState.isAuthExpired) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isCompact) 6.dp else 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.danger.copy(alpha = 0.12f))
                    .border(1.dp, palette.danger.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 12.dp, vertical = if (isCompact) 6.dp else 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = palette.danger,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = snapshot.connectionState.errorMessage ?: "长桥授权凭据已失效，请重新授权",
                            color = palette.danger,
                            fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "重新授权 ›",
                        color = palette.accent,
                        fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (!snapshot.connectionState.errorMessage.isNullOrBlank() && showLiveData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isCompact) 5.dp else 7.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.warning.copy(alpha = 0.12f))
                    .border(0.8.dp, palette.warning.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "同步提示: ${snapshot.connectionState.errorMessage}（当前展示最近缓存快照）",
                    color = palette.warning,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (snapshot.safeMarketIndices.isNotEmpty() || snapshot.safeMarketTemperatures.isNotEmpty()) {
            LongbridgeMarketOverviewStrip(
                snapshot = snapshot,
                settings = settings,
                palette = palette,
                isNightMode = isNightMode,
                isCompact = isCompact
            )
            Spacer(modifier = Modifier.height(if (isCompact) 5.dp else 8.dp))
        }

        if (!showLiveData) {
            LongbridgeEmptyStateCard(
                snapshot = snapshot,
                palette = palette,
                isNightMode = isNightMode,
                isCompact = isCompact,
                onRefresh = onRefresh,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            return
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 14.dp)
        ) {
            // 左栏：核心账户资产（当日盈亏 + 净资产/现金余额同卡并列 + 次级指标 + 分币种现金）
            LongbridgeAccountSummaryCard(
                snapshot = snapshot,
                settings = settings,
                palette = palette,
                isNightMode = isNightMode,
                isCompact = isCompact,
                masked = masked,
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight()
            )

            // 右栏：持仓标的列表（按当日盈亏绝对波动降序）
            LongbridgeHoldingsCard(
                snapshot = snapshot,
                settings = settings,
                palette = palette,
                isNightMode = isNightMode,
                isCompact = isCompact,
                masked = masked,
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun LongbridgeMarketOverviewStrip(
    snapshot: LongbridgeFullSnapshot,
    settings: LongbridgeSettings,
    palette: LongbridgeCockpitPalette,
    isNightMode: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val indicesByMarket = snapshot.safeMarketIndices.groupBy { it.market.uppercase(Locale.US) }
    val tempsByMarket = snapshot.safeMarketTemperatures.associateBy { it.market.uppercase(Locale.US) }
    val markets = listOf("US" to "美股", "HK" to "港股", "CN" to "A股")
    val radius = if (isCompact) 9.dp else 12.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
    ) {
        markets.forEach { (marketKey, marketLabel) ->
            val temp: LongbridgeMarketTemp? = tempsByMarket[marketKey]
            val marketIndices: List<LongbridgeIndexQuote> = indicesByMarket[marketKey].orEmpty()
                .take(if (isCompact) 2 else 3)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(radius))
                    .background(palette.cardBg)
                    .border(0.9.dp, palette.border, RoundedCornerShape(radius))
                    .padding(horizontal = if (isCompact) 7.dp else 10.dp, vertical = if (isCompact) 4.5.dp else 7.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.5.dp else 4.dp)
            ) {
                // 市场标题 + 估值/情绪 + 温度徽标
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = marketLabel,
                            color = palette.primaryText,
                            fontSize = if (isCompact) 10.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (temp != null && (temp.valuation != null || temp.sentiment != null)) {
                            Text(
                                text = "估${temp.valuation ?: "—"}/情${temp.sentiment ?: "—"}",
                                color = palette.mutedText,
                                fontSize = if (isCompact) 7.8.sp else 9.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (temp != null) {
                        val tempColor = when {
                            palette.isPureInk -> palette.primaryText
                            temp.temperature >= 70 -> palette.danger
                            temp.temperature >= 55 -> palette.warning
                            temp.temperature >= 40 -> palette.accent
                            else -> palette.positive
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(tempColor.copy(alpha = 0.14f))
                                .padding(horizontal = if (isCompact) 4.5.dp else 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${temp.temperature}° ${temp.statusLabel}",
                                color = tempColor,
                                fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Text(
                            text = "温度 —",
                            color = palette.mutedText,
                            fontSize = if (isCompact) 8.sp else 9.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 指数列表
                if (marketIndices.isEmpty()) {
                    Text(
                        text = "指数待同步",
                        color = palette.mutedText,
                        fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                        maxLines = 1
                    )
                } else {
                    marketIndices.forEach { quote ->
                        val changeColor = resolveLongbridgePnlColor(
                            value = quote.changeRatePercent,
                            isRedUpGreenDown = settings.isRedUpGreenDown,
                            isNightMode = isNightMode,
                            isPureInk = palette.isPureInk
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = quote.shortName,
                                color = palette.secondaryText,
                                fontSize = if (isCompact) 8.8.sp else 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
                            ) {
                                Text(
                                    text = quote.formattedLevel(),
                                    color = palette.primaryText,
                                    fontSize = if (isCompact) 8.8.sp else 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = quote.formattedChangeRate(),
                                    color = changeColor,
                                    fontSize = if (isCompact) 8.8.sp else 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LongbridgeCockpitTopBar(
    settings: LongbridgeSettings,
    snapshot: LongbridgeFullSnapshot,
    currentTimeString: String,
    palette: LongbridgeCockpitPalette,
    isCompact: Boolean,
    onTogglePrivacy: () -> Unit,
    onCycleCurrency: () -> Unit,
    onToggleColorScheme: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isLoading = snapshot.connectionState.isLoading
    val infiniteTransition = rememberInfiniteTransition(label = "lbRefreshSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lbSpinAngle"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧身份与状态徽标
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 9.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            LongbridgeProviderLogo(
                size = if (isCompact) 22.dp else 26.dp,
                isNightMode = palette.isNight
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = settings.accountAlias.ifBlank { "长桥证券" },
                        color = palette.primaryText,
                        fontSize = if (isCompact) 13.5.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (snapshot.isMock) {
                        MockBadge(isNightMode = palette.isNight)
                    }
                    val statusColor = when {
                        snapshot.connectionState.isAuthExpired -> palette.danger
                        snapshot.connectionState.errorMessage != null -> palette.warning
                        snapshot.hasValidAssetData -> palette.positive
                        else -> palette.mutedText
                    }
                    val statusLabel = when {
                        isLoading -> "同步中"
                        snapshot.connectionState.isAuthExpired -> "需重新授权"
                        snapshot.isMock -> "只读演示"
                        snapshot.connectionState.lastSuccessTime != null -> "只读 · ${snapshot.formattedSyncTime}"
                        else -> "只读 MCP"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.14f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                if (!isCompact) {
                    val accountLabel = buildString {
                        append(if (settings.normalizedRegion == "GLOBAL") "国际站 .com" else "大陆站 .cn")
                        if (!snapshot.accountName.isNullOrBlank()) {
                            append(" · ${snapshot.accountName}")
                        }
                        append(" · ${snapshot.positions.size} 持仓")
                    }
                    Text(
                        text = accountLabel,
                        color = palette.mutedText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 右侧快捷切换与时钟控件
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 5.dp else 8.dp)
        ) {
            // 计价币种一键切换
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.innerBg)
                    .border(0.8.dp, palette.border, RoundedCornerShape(6.dp))
                    .clickable { onCycleCurrency() }
                    .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = if (isCompact) 3.dp else 4.dp)
            ) {
                Text(
                    text = "币种 ${snapshot.displayCurrency}",
                    color = palette.primaryText,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 红涨绿跌 / 绿涨红跌 一键切换
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.innerBg)
                    .border(0.8.dp, palette.border, RoundedCornerShape(6.dp))
                    .clickable { onToggleColorScheme() }
                    .padding(
                        horizontal = if (isCompact) 6.dp else 8.dp,
                        vertical = if (isCompact) 3.dp else 4.dp
                    )
            ) {
                Text(
                    text = if (isCompact) {
                        if (settings.isRedUpGreenDown) "红涨" else "绿涨"
                    } else {
                        if (settings.isRedUpGreenDown) "红涨绿跌" else "绿涨红跌"
                    },
                    color = palette.secondaryText,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 金额脱敏眼睛按钮
            Box(
                modifier = Modifier
                    .size(if (isCompact) 26.dp else 30.dp)
                    .clip(CircleShape)
                    .background(palette.innerBg)
                    .border(0.8.dp, palette.border, CircleShape)
                    .clickable { onTogglePrivacy() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (settings.privacyMaskAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (settings.privacyMaskAmounts) "显示金额" else "隐藏金额",
                    tint = palette.secondaryText,
                    modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                )
            }

            Text(
                text = currentTimeString,
                color = palette.primaryText,
                fontSize = if (isCompact) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Box(
                modifier = Modifier
                    .size(if (isCompact) 26.dp else 30.dp)
                    .clip(CircleShape)
                    .background(palette.innerBg)
                    .border(0.8.dp, palette.border, CircleShape)
                    .clickable(enabled = !isLoading) { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新长桥数据",
                    tint = if (isLoading) palette.accent else palette.primaryText,
                    modifier = Modifier
                        .size(if (isCompact) 14.dp else 16.dp)
                        .rotate(if (isLoading) spinAngle else 0f)
                )
            }

            Box(
                modifier = Modifier
                    .size(if (isCompact) 26.dp else 30.dp)
                    .clip(CircleShape)
                    .background(palette.innerBg)
                    .border(0.8.dp, palette.border, CircleShape)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "长桥设置",
                    tint = palette.secondaryText,
                    modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                )
            }
        }
    }
}

@Composable
private fun LongbridgeAccountSummaryCard(
    snapshot: LongbridgeFullSnapshot,
    settings: LongbridgeSettings,
    palette: LongbridgeCockpitPalette,
    isNightMode: Boolean,
    isCompact: Boolean,
    masked: Boolean,
    modifier: Modifier = Modifier
) {
    val radius = if (isCompact) 12.dp else 16.dp
    val pnlColor = resolveLongbridgePnlColor(snapshot.todayPl, settings.isRedUpGreenDown, isNightMode, palette.isPureInk)
    val cumColor = resolveLongbridgePnlColor(snapshot.cumulativePl, settings.isRedUpGreenDown, isNightMode, palette.isPureInk)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(palette.cardBg)
            .border(1.dp, palette.border, RoundedCornerShape(radius))
            .verticalScroll(rememberScrollState())
            .padding(if (isCompact) 8.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 5.dp else 10.dp)
    ) {
        // 1. 当日盈亏主视图
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
                .background(palette.innerBg)
                .border(0.8.dp, palette.border, RoundedCornerShape(if (isCompact) 9.dp else 11.dp))
                .padding(horizontal = if (isCompact) 9.dp else 12.dp, vertical = if (isCompact) 5.5.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "当日盈亏 (${snapshot.displayCurrency})",
                        color = palette.secondaryText,
                        fontSize = if (isCompact) 9.5.sp else 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = snapshot.formattedTodayPl(masked),
                        color = pnlColor,
                        fontSize = if (isCompact) 16.5.sp else 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                val rateStr = snapshot.formattedTodayPlRate()
                val arrow = when {
                    (snapshot.todayPl ?: 0.0) > 0.004 -> "▲ "
                    (snapshot.todayPl ?: 0.0) < -0.004 -> "▼ "
                    else -> ""
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(pnlColor.copy(alpha = 0.14f))
                        .padding(horizontal = if (isCompact) 7.dp else 9.dp, vertical = if (isCompact) 3.5.dp else 5.dp)
                ) {
                    Text(
                        text = "$arrow$rateStr",
                        color = pnlColor,
                        fontSize = if (isCompact) 11.sp else 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 2. 同卡并列：净资产 vs 现金余额
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                    .background(palette.innerBg)
                    .border(0.8.dp, palette.border, RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                    .padding(horizontal = if (isCompact) 7.dp else 11.dp, vertical = if (isCompact) 5.5.dp else 9.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "净资产 (${snapshot.displayCurrency})",
                        color = palette.secondaryText,
                        fontSize = if (isCompact) 9.sp else 10.sp
                    )
                    val netAssetsText = if (isCompact && abs(snapshot.netAssets ?: 0.0) >= 1_000_000.0) {
                        LongbridgeFullSnapshot.formatUnsignedCompactMetric(snapshot.netAssets, snapshot.displayCurrency, masked)
                    } else {
                        snapshot.formattedNetAssets(masked)
                    }
                    Text(
                        text = netAssetsText,
                        color = palette.primaryText,
                        fontSize = if (isCompact) 12.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                    .background(palette.innerBg)
                    .border(0.8.dp, palette.border, RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                    .padding(horizontal = if (isCompact) 7.dp else 11.dp, vertical = if (isCompact) 5.5.dp else 9.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "现金余额",
                        color = palette.secondaryText,
                        fontSize = if (isCompact) 9.sp else 10.sp
                    )
                    val cashText = if (isCompact && abs(snapshot.totalCash ?: 0.0) >= 1_000_000.0) {
                        LongbridgeFullSnapshot.formatUnsignedCompactMetric(snapshot.totalCash, snapshot.displayCurrency, masked)
                    } else {
                        snapshot.formattedTotalCash(masked)
                    }
                    Text(
                        text = cashText,
                        color = palette.primaryText,
                        fontSize = if (isCompact) 12.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. 次级资产三联栏：股票市值 | 购买力 | 累计盈亏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                .background(palette.innerBg.copy(alpha = 0.65f))
                .border(0.8.dp, palette.border, RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                .padding(horizontal = if (isCompact) 7.dp else 10.dp, vertical = if (isCompact) 5.dp else 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricMiniColumn(
                label = "股票市值",
                value = if (isCompact) {
                    LongbridgeFullSnapshot.formatUnsignedCompactMetric(snapshot.marketValueTotal, snapshot.displayCurrency, masked)
                } else {
                    snapshot.formattedMarketValue(masked)
                },
                valueColor = palette.primaryText,
                secondaryColor = palette.mutedText,
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
            MetricMiniColumn(
                label = "购买力",
                value = if (isCompact) {
                    LongbridgeFullSnapshot.formatUnsignedCompactMetric(snapshot.buyPower, snapshot.displayCurrency, masked)
                } else {
                    snapshot.formattedBuyPower(masked)
                },
                valueColor = palette.primaryText,
                secondaryColor = palette.mutedText,
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
            MetricMiniColumn(
                label = "累计盈亏",
                value = if (isCompact) {
                    LongbridgeFullSnapshot.formatSignedCompactMoney(snapshot.cumulativePl, snapshot.displayCurrency, masked)
                } else {
                    snapshot.formattedCumulativePl(masked)
                },
                valueColor = cumColor,
                secondaryColor = palette.mutedText,
                isCompact = isCompact,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. 分币种现金明细 & 风控等级
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "可用现金明细",
                color = palette.secondaryText,
                fontSize = if (isCompact) 9.sp else 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            val riskColor = when (snapshot.riskLevel) {
                2 -> palette.warning
                3 -> palette.danger
                else -> palette.positive
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(riskColor.copy(alpha = 0.14f))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = "风控: ${snapshot.riskLevelLabel}",
                    color = riskColor,
                    fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (snapshot.cashDetails.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 5.dp else 7.dp)
            ) {
                snapshot.cashDetails.take(3).forEach { cash ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.innerBg)
                            .border(0.6.dp, palette.border, RoundedCornerShape(6.dp))
                            .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = if (isCompact) 4.dp else 5.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = cash.currency,
                                color = palette.mutedText,
                                fontSize = if (isCompact) 8.sp else 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = LongbridgeFullSnapshot.formatCompactMoney(
                                    cash.availableCash ?: cash.withdrawCash,
                                    cash.currency,
                                    masked
                                ),
                                color = palette.primaryText,
                                fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniColumn(
    label: String,
    value: String,
    valueColor: Color,
    secondaryColor: Color,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = secondaryColor,
            fontSize = if (isCompact) 8.5.sp else 9.5.sp,
            maxLines = 1
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = if (isCompact) 9.5.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LongbridgeHoldingsCard(
    snapshot: LongbridgeFullSnapshot,
    settings: LongbridgeSettings,
    palette: LongbridgeCockpitPalette,
    isNightMode: Boolean,
    isCompact: Boolean,
    masked: Boolean,
    modifier: Modifier = Modifier
) {
    val radius = if (isCompact) 12.dp else 16.dp
    val sortedPositions = remember(snapshot.positions, snapshot.exchangeRates, snapshot.displayCurrency) {
        snapshot.positions.sortedByDescending { pos ->
            abs(snapshot.positionTodayPlInDisplay(pos) ?: pos.todayPl ?: 0.0)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(palette.cardBg)
            .border(1.dp, palette.border, RoundedCornerShape(radius))
            .padding(if (isCompact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 5.dp else 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "持仓明细 (${sortedPositions.size})",
                color = palette.primaryText,
                fontSize = if (isCompact) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (settings.includeExtendedHours) "含美股盘前盘后 · 按当日盈亏排序" else "仅常规盘中 · 按当日盈亏排序",
                color = palette.mutedText,
                fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (sortedPositions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当前暂无股票/基金持仓 · 全仓现金",
                    color = palette.secondaryText,
                    fontSize = if (isCompact) 11.sp else 12.5.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
            ) {
                itemsIndexed(
                    items = sortedPositions,
                    key = { _, item -> item.symbol }
                ) { index, pos ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = palette.border.copy(alpha = 0.55f),
                            thickness = 0.6.dp,
                            modifier = Modifier.padding(bottom = if (isCompact) 4.dp else 6.dp)
                        )
                    }
                    LongbridgePositionRow(
                        pos = pos,
                        isRedUpGreenDown = settings.isRedUpGreenDown,
                        isNightMode = isNightMode,
                        palette = palette,
                        isCompact = isCompact,
                        masked = masked
                    )
                }
            }
        }
    }
}

@Composable
private fun LongbridgePositionRow(
    pos: LongbridgePositionItem,
    isRedUpGreenDown: Boolean,
    isNightMode: Boolean,
    palette: LongbridgeCockpitPalette,
    isCompact: Boolean,
    masked: Boolean
) {
    val plColor = resolveLongbridgePnlColor(pos.todayPl, isRedUpGreenDown, isNightMode, palette.isPureInk)
    val marketBadgeColor = when {
        palette.isPureInk -> palette.secondaryText
        pos.market.uppercase(Locale.US) == "US" -> if (isNightMode) Color(0xFF38BDF8) else Color(0xFF0369A1)
        pos.market.uppercase(Locale.US) == "HK" -> if (isNightMode) TechAmber else Color(0xFFB45309)
        pos.market.uppercase(Locale.US) in listOf("SH", "SZ") -> if (isNightMode) TechRose else Color(0xFFB91C1C)
        else -> palette.secondaryText
    }
    val qtyText = remember(pos.quantity) {
        if (abs(pos.quantity - pos.quantity.toLong()) < 0.0001) {
            "${pos.quantity.toLong()}股"
        } else {
            "${DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US)).format(pos.quantity)}股"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧标的识别与价格
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(marketBadgeColor.copy(alpha = 0.14f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = pos.market.uppercase(Locale.US),
                        color = marketBadgeColor,
                        fontSize = if (isCompact) 7.5.sp else 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = pos.symbolName.ifBlank { pos.symbol },
                    color = palette.primaryText,
                    fontSize = if (isCompact) 11.sp else 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!pos.sessionTag.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(palette.accent.copy(alpha = 0.14f))
                            .padding(horizontal = 4.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = pos.sessionTag,
                            color = palette.accent,
                            fontSize = if (isCompact) 7.5.sp else 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Text(
                text = "${pos.symbol} · $qtyText · 现价 ${LongbridgeFullSnapshot.formatPrice(pos.lastDone, pos.currency)}",
                color = palette.mutedText,
                fontSize = if (isCompact) 8.5.sp else 9.5.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 右侧市值与当日盈亏
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = LongbridgeFullSnapshot.formatMoney(pos.marketValue, pos.currency, masked),
                color = palette.primaryText,
                fontSize = if (isCompact) 10.5.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = LongbridgeFullSnapshot.formatSignedMoney(pos.todayPl, pos.currency, masked),
                    color = plColor,
                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = LongbridgeFullSnapshot.formatSignedPercent(pos.todayPlRatePercent),
                    color = plColor,
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun LongbridgeEmptyStateCard(
    snapshot: LongbridgeFullSnapshot,
    palette: LongbridgeCockpitPalette,
    isNightMode: Boolean,
    isCompact: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(if (isCompact) 28.dp else 36.dp)
            )
            val isConfigured = snapshot.connectionState.isConfigured
            Text(
                text = when {
                    snapshot.connectionState.isAuthExpired -> "长桥证券登录态已过期"
                    isConfigured -> "长桥账户数据等待同步"
                    else -> "尚未授权长桥证券账户"
                },
                color = palette.primaryText,
                fontSize = if (isCompact) 14.sp else 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = snapshot.connectionState.errorMessage
                    ?: "通过长桥官方托管 MCP 只读获取账户净资产、现金与当日盈亏，不申请交易下单权限",
                color = palette.secondaryText,
                fontSize = if (isCompact) 11.sp else 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isConfigured && !snapshot.connectionState.isAuthExpired) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.innerBg)
                            .border(0.8.dp, palette.border, RoundedCornerShape(8.dp))
                            .clickable { onRefresh() }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "立即刷新同步",
                            color = palette.primaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.accent)
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (isConfigured) "管理长桥授权与设置" else "前往授权连接长桥",
                        color = if (isNightMode) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
