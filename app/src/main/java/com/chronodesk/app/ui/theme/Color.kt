package com.chronodesk.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalPureInkMode = compositionLocalOf { false }

// ==================== 纯水墨模式色彩系统（严格纯黑白灰 R==G==B） ====================
val PureInkBackground = Color(0xFFF4F4F4)
val PureInkSurface = Color(0xFFFFFFFF)
val PureInkSurfaceElevated = Color(0xFFEBEBEB)
val PureInkBorder = Color(0xFFD4D4D4)
val PureInkTextPrimary = Color(0xFF141414)
val PureInkTextSecondary = Color(0xFF525252)
val PureInkTextTertiary = Color(0xFF858585)
val PureInkProgress = Color(0xFF1F1F1F)
val PureInkTrack = Color(0xFFE0E0E0)
val PureInkClockBorder = Color(0xFFD0D0D0)
val PureInkSeam = Color(0xFFD4D4D4)

data class ChronoDayNightPalette(
    val isNight: Boolean,
    val isPureInk: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val dividerSubtle: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val progressActive: Color,
    val progressTrack: Color,
    val accent: Color,
    val positive: Color,
    val warning: Color,
    val danger: Color
)

fun resolveChronoPalette(isNightMode: Boolean, isPureInk: Boolean): ChronoDayNightPalette = when {
    isNightMode -> ChronoDayNightPalette(
        isNight = true,
        isPureInk = false,
        bg = DarkBackground,
        surface = DarkSurface,
        surfaceElevated = DarkSurfaceElevated,
        border = DarkBorder,
        dividerSubtle = DarkBorderSubtle,
        primaryText = TextPrimary,
        secondaryText = TextSecondary,
        mutedText = TextTertiary,
        progressActive = TechEmerald,
        progressTrack = DarkProgressTrack,
        accent = TechCyan,
        positive = TechEmerald,
        warning = TechAmber,
        danger = TechRose
    )
    isPureInk -> ChronoDayNightPalette(
        isNight = false,
        isPureInk = true,
        bg = PureInkBackground,
        surface = PureInkSurface,
        surfaceElevated = PureInkSurfaceElevated,
        border = PureInkBorder,
        dividerSubtle = PureInkBorder,
        primaryText = PureInkTextPrimary,
        secondaryText = PureInkTextSecondary,
        mutedText = PureInkTextTertiary,
        progressActive = PureInkProgress,
        progressTrack = PureInkTrack,
        accent = PureInkTextPrimary,
        positive = Color(0xFF2B2B2B),
        warning = Color(0xFF525252),
        danger = PureInkTextPrimary
    )
    else -> ChronoDayNightPalette(
        isNight = false,
        isPureInk = false,
        bg = InkBackground,
        surface = InkSurface,
        surfaceElevated = InkSurfaceElevated,
        border = InkBorder,
        dividerSubtle = InkBorderSubtle,
        primaryText = InkTextPrimary,
        secondaryText = InkTextSecondary,
        mutedText = InkTextTertiary,
        progressActive = InkStatusGreen,
        progressTrack = InkProgressTrack,
        accent = Color(0xFF0284C7),
        positive = InkStatusGreen,
        warning = InkWarningAmber,
        danger = InkWarningRed
    )
}

// ==================== 日间水墨 / 电子墨水屏主题色彩系统 ====================
// 米白、宣纸浅灰与浓淡墨色，柔和、护眼、清晰呈现电子墨水屏质感
val InkBackground = Color(0xFFF5F3EC)          // 温暖宣纸底色
val InkBackgroundSecondary = Color(0xFFEBE8DF) // 水墨渐变过渡
val InkSurface = Color(0xFFFFFFFF)             // 卡片纯净底色
val InkSurfaceElevated = Color(0xFFF1EEE6)     // 悬浮/胶囊底色
val InkBorder = Color(0xFFDDD7CC)              // 柔和水墨轮廓线
val InkBorderSubtle = Color(0xFFE7E3D8)        // 细微分割线
val InkTextPrimary = Color(0xFF1D1B1A)         // 焦墨浓黑 (90%对比)
val InkTextSecondary = Color(0xFF5A5852)       // 浓墨深灰
val InkTextTertiary = Color(0xFF8E8B83)        // 淡墨灰
val InkProgress = Color(0xFF2C2A27)            // 水墨炭黑进度条
val InkProgressTrack = Color(0xFFE5E0D5)       // 进度条浅灰底槽
val InkStatusGreen = Color(0xFF2D6A4F)         // 竹青墨绿 (正常状态)
val InkStatusTagBg = Color(0xFFE8F1EC)         // 竹青浅底
val InkWarningRed = Color(0xFFB91C1C)          // 水墨朱砂红 (额度耗尽)
val InkWarningAmber = Color(0xFFD97706)        // 水墨藤黄 (警示额度)
val InkClockCardBg = Color(0xFFFFFFFF)         // 翻牌卡片米白
val InkClockCardBorder = Color(0xFFDBD5C8)     // 翻牌卡片浅灰边框
val InkClockText = Color(0xFF141312)           // 翻牌时钟纯墨黑
val InkColon = Color(0xFF4A4844)               // 水墨冒号

// ==================== 夜间深色科技风格色彩系统 ====================
// 纯黑至午夜深蓝背景、深色微光卡片、青蓝色点缀与翡翠绿额度条，精致克制
val PureBlack = Color(0xFF000000)
val DarkBackground = Color(0xFF070C15)         // 午夜深蓝近黑
val DarkSurface = Color(0xFF0D1627)            // 科技暗蓝卡片底色
val DarkSurfaceElevated = Color(0xFF142137)     // 悬浮/按钮底色
val DarkBorder = Color(0xFF1B2C46)             // 科技轮廓微光线
val DarkBorderSubtle = Color(0xFF152236)
val DarkClockCardBg = Color(0xFF10192B)        // 翻牌夜间暗底
val DarkClockCardBorder = Color(0xFF1E314F)    // 翻牌夜间边框
val DarkClockText = Color(0xFFFFFFFF)          // 翻牌荧光冷白
val DarkColon = Color(0xFF00E5FF)              // 赛博青闪烁冒号
val DarkProgressTrack = Color(0xFF18263B)      // 夜间进度条底槽

val TechCyan = Color(0xFF00E5FF)               // 赛博青高光
val TechCyanMuted = Color(0xFF00B4D8)
val TechEmerald = Color(0xFF10B981)            // 翡翠绿 (正常额度)
val TechAmber = Color(0xFFF59E0B)              // 琥珀金 (警示额度)
val TechRose = Color(0xFFFF3D71)               // 玫瑰红 (耗尽)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

// ==================== 苹果 StandBy 极夜红色彩系统 (兼容保留) ====================
val AppleNightRedPrimary = Color(0xFFFF3B30)
val AppleNightRedSecondary = Color(0xFFD32F2F)
val AppleNightRedMuted = Color(0xFF881B1B)
val AppleNightSurface = Color(0xFF0C0303)
val AppleNightSurfaceElevated = Color(0xFF170606)
val AppleNightBorder = Color(0xFF2C0A0A)
val AppleNightTrack = Color(0xFF1A0505)

val NightBackground = Color(0xFF000000)
val NightTextPrimary = AppleNightRedPrimary
val NightTextSecondary = AppleNightRedSecondary
val NightAccent = AppleNightRedPrimary
