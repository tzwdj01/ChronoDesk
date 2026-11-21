package com.chronodesk.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CodexUsageWindow(
    val name: String,
    val label: String,
    val limitWindowSeconds: Long,
    val usedPercent: Double,
    val remainingPercent: Double,
    val resetAtUnix: Long?,
    val resetAtIso: String? = null
) {
    /**
     * 计算倒计时文本，如 "2小时14分" 或 "45分"
     */
    fun formattedCountdown(nowUnix: Long = System.currentTimeMillis() / 1000): String {
        if (resetAtUnix == null || resetAtUnix <= nowUnix || usedPercent <= 0.0) {
            return "已就绪"
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
     * 格式化具体重置时间，如 "18:45"
     */
    fun formattedResetTime(): String {
        if (resetAtUnix == null) return "--:--"
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(resetAtUnix * 1000))
    }

    /**
     * 格式化完整到期月日与时间，如 "11-21 19:42"
     */
    fun formattedFullDateTime(): String {
        if (resetAtUnix == null) return "--"
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(resetAtUnix * 1000))
    }

    companion object {
        fun fromRaw(
            usedPercent: Double?,
            limitWindowSeconds: Long?,
            resetAtUnix: Long?
        ): CodexUsageWindow? {
            if (usedPercent == null) return null
            val secs = limitWindowSeconds ?: 18000
            val used = usedPercent.coerceIn(0.0, 100.0)
            val remaining = (100.0 - used).coerceIn(0.0, 100.0)

            val effectiveResetUnix = resetAtUnix

            val (name, label) = when (secs) {
                18000L -> "five_hour" to "5小时额度"
                604800L -> "seven_day" to "周额度"
                2592000L -> "thirty_day" to "月度额度"
                else -> {
                    val hours = secs / 3600
                    if (hours >= 24) "${hours / 24}_day" to "${hours / 24}天额度"
                    else "${hours}_hour" to "${hours}小时额度"
                }
            }

            return CodexUsageWindow(
                name = name,
                label = label,
                limitWindowSeconds = secs,
                usedPercent = used,
                remainingPercent = remaining,
                resetAtUnix = effectiveResetUnix,
                resetAtIso = effectiveResetUnix?.let { formatUnixToIso(it) }
            )
        }

        private fun formatUnixToIso(unixSeconds: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date(unixSeconds * 1000))
        }
    }
}
