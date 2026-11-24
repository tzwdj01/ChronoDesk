package com.chronodesk.app.data.model

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class CodexUsageWindow(
    val name: String,
    val label: String,
    val limitWindowSeconds: Long?,
    val usedPercent: Double?,
    val remainingPercent: Double?,
    val resetAtUnix: Long?,
    val resetAtIso: String? = null
) {
    val windowLabel: String get() = label
    /**
     * 格式化剩余百分比展示：
     * 1. 缺失、非有限或越界值显示 "—"
     * 2. 真实剩余 <= 0% 显示 "耗尽"
     * 3. 真实剩余在 0% 到 1% 之间显示 "<1%"
     * 4. 其余正常四舍五入为 "XX%"
     */
    fun formattedRemaining(): String {
        val rem = remainingPercent ?: return "—"
        if (!rem.isFinite() || rem < 0.0 || rem > 100.0) return "—"
        return when {
            rem <= 0.0 -> "耗尽"
            rem < 1.0 -> "<1%"
            else -> "${rem.roundToInt()}%"
        }
    }

    /**
     * 格式化重置倒计时 / 状态：
     * 1. 没有重置时间时显示「重置时间未知」
     * 2. 设备时间已超过上游提供的重置时间且尚未同步刷新时显示「等待更新」
     * 3. 仍在有效期内时计算倒计时
     */
    fun formattedCountdown(
        nowUnix: Long = System.currentTimeMillis() / 1000,
        hasSyncedSinceReset: Boolean = false
    ): String {
        if (resetAtUnix == null) {
            return "重置时间未知"
        }
        if (nowUnix >= resetAtUnix) {
            return if (!hasSyncedSinceReset) "等待更新" else "已就绪"
        }
        val diffSec = resetAtUnix - nowUnix
        val hours = diffSec / 3600
        val minutes = (diffSec % 3600) / 60
        return when {
            hours > 24 -> "${hours / 24}天${hours % 24}小时"
            hours > 0 -> "${hours}小时${minutes}分"
            minutes > 0 -> "${minutes}分钟"
            else -> "<1分钟"
        }
    }

    /**
     * 紧凑倒计时格式（用于纯数字看板或窄屏卡片，如 "3天14h" / "2h30m" / "45m"）
     */
    fun formattedCompactCountdown(
        nowUnix: Long = System.currentTimeMillis() / 1000
    ): String {
        if (resetAtUnix == null) return "未知"
        if (nowUnix >= resetAtUnix) return "待更新"
        val diffSec = resetAtUnix - nowUnix
        val hours = diffSec / 3600
        val minutes = (diffSec % 3600) / 60
        return when {
            hours > 24 -> "${hours / 24}天${hours % 24}h"
            hours > 0 -> "${hours}h${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    /**
     * 格式化具体重置时间点，如 "18:45"
     */
    fun formattedResetTime(nowUnix: Long = System.currentTimeMillis() / 1000): String {
        if (resetAtUnix == null) return "重置时间未知"
        if (nowUnix >= resetAtUnix) return "等待更新"
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(resetAtUnix * 1000))
    }

    /**
     * 格式化完整重置日期与时间，如 "11-21 19:42"
     */
    fun formattedFullDateTime(nowUnix: Long = System.currentTimeMillis() / 1000): String {
        if (resetAtUnix == null) return "重置时间未知"
        if (nowUnix >= resetAtUnix) return "等待更新"
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(resetAtUnix * 1000))
    }

    companion object {
        const val TIER_AGY_GEMINI_FIVE_HOUR = "agy_gemini_five_hour"
        const val TIER_AGY_GEMINI_WEEKLY = "agy_gemini_weekly"
        const val TIER_AGY_GEMINI_ARGON_WEEKLY = "agy_gemini_argon_weekly"
        const val TIER_AGY_THIRD_PARTY_FIVE_HOUR = "agy_third_party_five_hour"
        const val TIER_AGY_THIRD_PARTY_WEEKLY = "agy_third_party_weekly"

        fun antigravitySortOrder(name: String): Int = when (name) {
            TIER_AGY_GEMINI_FIVE_HOUR -> 0
            TIER_AGY_GEMINI_WEEKLY -> 1
            TIER_AGY_GEMINI_ARGON_WEEKLY -> 2
            TIER_AGY_THIRD_PARTY_FIVE_HOUR -> 3
            else -> 4
        }

        fun fromAntigravityBucket(
            bucketId: String?,
            remainingFraction: Double?,
            resetTimeIso: String?
        ): CodexUsageWindow? {
            if (remainingFraction == null || !remainingFraction.isFinite()) return null
            val (name, label, windowSeconds) = when (bucketId?.trim()) {
                "gemini-5h" -> Triple(TIER_AGY_GEMINI_FIVE_HOUR, "Gemini 5h", 18000L)
                "gemini-weekly" -> Triple(TIER_AGY_GEMINI_WEEKLY, "Gemini 周", 604800L)
                "gemini-argon-wtu-weekly" -> Triple(TIER_AGY_GEMINI_ARGON_WEEKLY, "Argon 周", 604800L)
                "3p-5h" -> Triple(TIER_AGY_THIRD_PARTY_FIVE_HOUR, "Claude/GPT 5h", 18000L)
                "3p-weekly" -> Triple(TIER_AGY_THIRD_PARTY_WEEKLY, "Claude/GPT 周", 604800L)
                else -> return null
            }
            val clampedRemaining = remainingFraction.coerceIn(0.0, 1.0)
            val remainingPercent = clampedRemaining * 100.0
            val usedPercent = (1.0 - clampedRemaining) * 100.0
            val cleanIso = resetTimeIso?.trim()?.takeIf { it.isNotEmpty() }
            val resetUnix = parseIsoToUnixSeconds(cleanIso)
            return CodexUsageWindow(
                name = name,
                label = label,
                limitWindowSeconds = windowSeconds,
                usedPercent = usedPercent,
                remainingPercent = remainingPercent,
                resetAtUnix = resetUnix,
                resetAtIso = cleanIso
            )
        }

        fun parseIsoToUnixSeconds(iso: String?): Long? {
            val clean = iso?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return try {
                Instant.parse(clean).epochSecond
            } catch (_: Exception) {
                try {
                    OffsetDateTime.parse(clean).toEpochSecond()
                } catch (_: Exception) {
                    null
                }
            }
        }

        fun fromRaw(
            usedPercent: Double?,
            limitWindowSeconds: Long?,
            resetAtUnix: Long?
        ): CodexUsageWindow? {
            // limit_window_seconds 缺失时显示「额度窗口」，只有上游确实返回 18000 或 604800 才标记「5小时额度」或「周额度」
            val (name, label) = when (limitWindowSeconds) {
                18000L -> "five_hour" to "5小时额度"
                604800L -> "seven_day" to "周额度"
                else -> "quota_window" to "额度窗口"
            }

            // used_percent 在 0–100 内才计算 100 - used_percent。缺失、非有限或越界值显示 —
            val isValidUsed = usedPercent != null && usedPercent.isFinite() && usedPercent in 0.0..100.0
            val validUsed = if (isValidUsed) usedPercent else null
            val remaining = if (isValidUsed) (100.0 - validUsed!!) else null

            return CodexUsageWindow(
                name = name,
                label = label,
                limitWindowSeconds = limitWindowSeconds,
                usedPercent = validUsed,
                remainingPercent = remaining,
                resetAtUnix = resetAtUnix,
                resetAtIso = resetAtUnix?.let { formatUnixToIso(it) }
            )
        }

        private fun formatUnixToIso(unixSeconds: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date(unixSeconds * 1000))
        }
    }
}
