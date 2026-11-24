package com.chronodesk.app.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronodesk.app.R
import com.chronodesk.app.ui.theme.LocalPureInkMode
import com.chronodesk.app.ui.theme.PureInkBorder
import com.chronodesk.app.ui.theme.PureInkSurfaceElevated
import com.chronodesk.app.ui.theme.PureInkTextPrimary

enum class AiModelProvider(val code: String, val displayName: String, val defaultPlan: String) {
    CHATGPT("CHATGPT", "ChatGPT Plus", "Plus"),
    DEEPSEEK("DEEPSEEK", "DeepSeek", ""),
    ANTIGRAVITY("ANTIGRAVITY", "Google Antigravity", "Pro"),
    GEMINI("GEMINI", "Gemini", "Plus"),
    CLAUDE("CLAUDE", "Claude", "Plus");

    companion object {
        fun fromCode(code: String?): AiModelProvider {
            val upper = code?.uppercase() ?: ""
            return when {
                upper.contains("ANTIGRAVITY") || upper.contains("GOOGLE") -> ANTIGRAVITY
                upper.contains("DEEPSEEK") -> DEEPSEEK
                upper.contains("GEMINI") -> GEMINI
                upper.contains("CLAUDE") || upper.contains("ANTHROPIC") -> CLAUDE
                else -> CHATGPT
            }
        }
    }
}

/**
 * 极简 AI 模型品牌 Logo 渲染组件
 * 夜间：深色科技品牌彩色；日间彩色：浅底+官方原彩；日间全水墨：纯黑白灰墨水屏质感
 */
@Composable
fun ModelProviderLogo(
    provider: AiModelProvider,
    size: Dp = 32.dp,
    isNightMode: Boolean = false,
    isPureInk: Boolean = LocalPureInkMode.current,
    modifier: Modifier = Modifier
) {
    val usePureInk = !isNightMode && isPureInk
    val containerBg = when {
        isNightMode -> when (provider) {
            AiModelProvider.CHATGPT -> Color(0xFF0F261E)
            AiModelProvider.DEEPSEEK -> Color(0xFF0E1F38)
            AiModelProvider.ANTIGRAVITY -> Color(0xFF101B30)
            AiModelProvider.GEMINI -> Color(0xFF0E2338)
            AiModelProvider.CLAUDE -> Color(0xFF26190F)
        }
        usePureInk -> PureInkSurfaceElevated
        else -> when (provider) {
            AiModelProvider.CHATGPT -> Color(0xFFEAF6F2)
            AiModelProvider.DEEPSEEK -> Color(0xFFECF1FE)
            AiModelProvider.ANTIGRAVITY -> Color(0xFFF1F5FD)
            AiModelProvider.GEMINI -> Color(0xFFEBF6FE)
            AiModelProvider.CLAUDE -> Color(0xFFFEF1E8)
        }
    }

    val containerBorder = when {
        isNightMode -> when (provider) {
            AiModelProvider.CHATGPT -> Color(0xFF1B4D3E)
            AiModelProvider.DEEPSEEK -> Color(0xFF1E3A66)
            AiModelProvider.ANTIGRAVITY -> Color(0xFF1E3860)
            AiModelProvider.GEMINI -> Color(0xFF1E4366)
            AiModelProvider.CLAUDE -> Color(0xFF4D2E1B)
        }
        usePureInk -> PureInkBorder
        else -> when (provider) {
            AiModelProvider.CHATGPT -> Color(0xFFC6E7DC)
            AiModelProvider.DEEPSEEK -> Color(0xFFCBD9FC)
            AiModelProvider.ANTIGRAVITY -> Color(0xFFD6E2FA)
            AiModelProvider.GEMINI -> Color(0xFFC7E4FC)
            AiModelProvider.CLAUDE -> Color(0xFFF9D5BD)
        }
    }

    val brandColor = when {
        isNightMode -> when (provider) {
            AiModelProvider.CHATGPT -> Color(0xFF10A37F)
            AiModelProvider.DEEPSEEK -> Color(0xFF4D6BFE)
            AiModelProvider.ANTIGRAVITY -> Color(0xFF4285F4)
            AiModelProvider.GEMINI -> Color(0xFF38BDF8)
            AiModelProvider.CLAUDE -> Color(0xFFEA580C)
        }
        usePureInk -> PureInkTextPrimary
        else -> when (provider) {
            AiModelProvider.CHATGPT -> Color(0xFF10A37F)
            AiModelProvider.DEEPSEEK -> Color(0xFF4D6BFE)
            AiModelProvider.ANTIGRAVITY -> Color(0xFF4285F4)
            AiModelProvider.GEMINI -> Color(0xFF0284C7)
            AiModelProvider.CLAUDE -> Color(0xFFEA580C)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(containerBg)
            .border(0.8.dp, containerBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (provider) {
            AiModelProvider.CHATGPT -> {
                // OpenAI / ChatGPT 官方六瓣几何花环高清图标 (支持暗色与水墨双模式精准着色)
                Icon(
                    painter = painterResource(id = R.drawable.ic_chatgpt_clean),
                    contentDescription = "ChatGPT",
                    tint = brandColor,
                    modifier = Modifier.size(size * 0.65f)
                )
            }

            AiModelProvider.DEEPSEEK -> {
                // DeepSeek 官方跃水灵鲸矢量图标
                Icon(
                    painter = painterResource(id = R.drawable.ic_deepseek_logo),
                    contentDescription = "DeepSeek",
                    tint = brandColor,
                    modifier = Modifier.size(size * 0.62f)
                )
            }

            AiModelProvider.ANTIGRAVITY -> {
                // Google Antigravity 官方色彩标识（日间彩色/夜间均保持四色，全水墨收敛为纯黑灰）
                Icon(
                    painter = painterResource(id = R.drawable.ic_antigravity_logo),
                    contentDescription = "Google Antigravity",
                    tint = if (usePureInk) brandColor else Color.Unspecified,
                    modifier = Modifier.size(size * 0.62f)
                )
            }

            AiModelProvider.GEMINI -> {
                // Google Gemini 四角耀星
                val iconPadding = size * 0.22f
                val canvasSize = size - (iconPadding * 2)
                Canvas(modifier = Modifier.size(canvasSize)) {
                    val w = this.size.width
                    val h = this.size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val path = Path().apply {
                        moveTo(cx, h * 0.05f)
                        quadraticBezierTo(cx + w * 0.1f, cy - h * 0.1f, w * 0.95f, cy)
                        quadraticBezierTo(cx + w * 0.1f, cy + h * 0.1f, cx, h * 0.95f)
                        quadraticBezierTo(cx - w * 0.1f, cy + h * 0.1f, w * 0.05f, cy)
                        quadraticBezierTo(cx - w * 0.1f, cy - h * 0.1f, cx, h * 0.05f)
                        close()
                    }
                    drawPath(path = path, color = brandColor)
                }
            }

            AiModelProvider.CLAUDE -> {
                // Anthropic Claude 经典八向放射光芒
                val iconPadding = size * 0.22f
                val canvasSize = size - (iconPadding * 2)
                Canvas(modifier = Modifier.size(canvasSize)) {
                    val w = this.size.width
                    val h = this.size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val spokeWidth = w * 0.12f
                    val spokeLength = w * 0.42f
                    for (i in 0 until 8) {
                        val angleDeg = i * 45f
                        rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
                            drawLine(
                                color = brandColor,
                                start = Offset(cx, cy - spokeLength * 0.25f),
                                end = Offset(cx, cy - spokeLength),
                                strokeWidth = spokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    drawCircle(color = brandColor, radius = spokeWidth * 0.9f, center = Offset(cx, cy))
                }
            }
        }
    }
}
