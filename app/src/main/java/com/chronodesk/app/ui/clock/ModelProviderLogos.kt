package com.chronodesk.app.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class AiModelProvider(val code: String, val displayName: String, val defaultPlan: String) {
    CHATGPT("CHATGPT", "ChatGPT Plus", "Plus"),
    DEEPSEEK("DEEPSEEK", "DeepSeek", "Plus"),
    GEMINI("GEMINI", "Gemini", "Plus"),
    CLAUDE("CLAUDE", "Claude", "Plus");

    companion object {
        fun fromCode(code: String?): AiModelProvider {
            val upper = code?.uppercase() ?: ""
            return when {
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
 * 根据日间（水墨风单色）与夜间（深色科技品牌彩色）自动适配高精度矢量图标
 */
@Composable
fun ModelProviderLogo(
    provider: AiModelProvider,
    size: Dp = 32.dp,
    isNightMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 背景与边框适配
    val containerBg = if (isNightMode) {
        when (provider) {
            AiModelProvider.CHATGPT -> Color(0xFF0F261E)
            AiModelProvider.DEEPSEEK -> Color(0xFF0E1F38)
            AiModelProvider.GEMINI -> Color(0xFF0E2338)
            AiModelProvider.CLAUDE -> Color(0xFF26190F)
        }
    } else {
        Color(0xFFF0EDE5)
    }

    val containerBorder = if (isNightMode) {
        when (provider) {
            AiModelProvider.CHATGPT -> Color(0xFF1B4D3E)
            AiModelProvider.DEEPSEEK -> Color(0xFF1E3A66)
            AiModelProvider.GEMINI -> Color(0xFF1E4366)
            AiModelProvider.CLAUDE -> Color(0xFF4D2E1B)
        }
    } else {
        Color(0xFFDDD7CC)
    }

    val brandColor = if (isNightMode) {
        when (provider) {
            AiModelProvider.CHATGPT -> Color(0xFF10A37F)
            AiModelProvider.DEEPSEEK -> Color(0xFF3B82F6)
            AiModelProvider.GEMINI -> Color(0xFF38BDF8)
            AiModelProvider.CLAUDE -> Color(0xFFEA580C)
        }
    } else {
        Color(0xFF242220) // 墨黑色
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(containerBg)
            .border(0.8.dp, containerBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val iconPadding = size * 0.22f
        val canvasSize = size - (iconPadding * 2)

        Canvas(modifier = Modifier.size(canvasSize)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            when (provider) {
                AiModelProvider.CHATGPT -> {
                    // OpenAI 螺旋六瓣花环矢量绘制
                    val strokeW = w * 0.11f
                    val r = w * 0.28f
                    val petalLen = w * 0.22f
                    for (i in 0 until 6) {
                        val angleDeg = i * 60f
                        rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
                            drawRoundRect(
                                color = brandColor,
                                topLeft = Offset(cx - r * 0.5f, cy - r - petalLen * 0.3f),
                                size = Size(r, petalLen * 1.2f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r / 2f, r / 2f),
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                AiModelProvider.DEEPSEEK -> {
                    // DeepSeek 跃水鲸鱼矢量 Path
                    val path = Path().apply {
                        // 鲸鱼流线型脊背与头部
                        moveTo(w * 0.15f, h * 0.65f)
                        cubicTo(
                            w * 0.1f, h * 0.35f,
                            w * 0.45f, h * 0.15f,
                            w * 0.85f, h * 0.35f
                        )
                        // 尾柄收紧与分叉尾鳍
                        cubicTo(
                            w * 0.7f, h * 0.5f,
                            w * 0.88f, h * 0.75f,
                            w * 0.95f, h * 0.85f
                        )
                        lineTo(w * 0.72f, h * 0.8f)
                        cubicTo(
                            w * 0.55f, h * 0.7f,
                            w * 0.3f, h * 0.78f,
                            w * 0.15f, h * 0.65f
                        )
                        close()
                    }
                    drawPath(path = path, color = brandColor)

                    // 背鳍与胸鳍点缀
                    val fin = Path().apply {
                        moveTo(w * 0.42f, h * 0.32f)
                        lineTo(w * 0.52f, h * 0.18f)
                        lineTo(w * 0.56f, h * 0.35f)
                        close()
                    }
                    drawPath(path = fin, color = brandColor)
                }

                AiModelProvider.GEMINI -> {
                    // Google Gemini 四角耀星矢量绘制 (内凹四芒星)
                    val path = Path().apply {
                        moveTo(cx, h * 0.05f)
                        // 到右顶点
                        quadraticBezierTo(cx + w * 0.1f, cy - h * 0.1f, w * 0.95f, cy)
                        // 到下顶点
                        quadraticBezierTo(cx + w * 0.1f, cy + h * 0.1f, cx, h * 0.95f)
                        // 到左顶点
                        quadraticBezierTo(cx - w * 0.1f, cy + h * 0.1f, w * 0.05f, cy)
                        // 回到上顶点
                        quadraticBezierTo(cx - w * 0.1f, cy - h * 0.1f, cx, h * 0.05f)
                        close()
                    }
                    drawPath(path = path, color = brandColor)
                }

                AiModelProvider.CLAUDE -> {
                    // Anthropic Claude 经典八向放射光芒矢量绘制
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
