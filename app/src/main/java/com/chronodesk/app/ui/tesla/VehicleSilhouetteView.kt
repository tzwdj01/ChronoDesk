package com.chronodesk.app.ui.tesla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBorder
import com.chronodesk.app.ui.theme.InkSurfaceElevated
import com.chronodesk.app.ui.theme.InkTextPrimary
import com.chronodesk.app.ui.theme.InkTextSecondary
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TechEmerald
import com.chronodesk.app.ui.theme.TextPrimary
import com.chronodesk.app.ui.theme.TextSecondary

/**
 * 侧面车型自绘矢量剪影组件 (支持 Model 3 / Y / S / X)
 * 纯 Compose Canvas 代码几何绘制，严禁使用任何未授权商标或商业位图
 */
@Composable
fun VehicleSilhouetteCanvas(
    model: String?,
    isNightMode: Boolean,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val normalized = when (model?.uppercase()?.trim()) {
        "Y", "MODEL Y", "MODELY" -> "Model Y"
        "S", "MODEL S", "MODELS" -> "Model S"
        "X", "MODEL X", "MODELX" -> "Model X"
        else -> "Model 3"
    }

    val carStroke = if (isNightMode) TechCyan.copy(alpha = 0.85f) else Color(0xFF334155)
    val carFill = if (isNightMode) Color(0xFF0F172A).copy(alpha = 0.85f) else Color(0xFFE2E8F0)
    val glassColor = if (isNightMode) TechCyan.copy(alpha = 0.25f) else Color(0xFF94A3B8).copy(alpha = 0.45f)
    val wheelTire = if (isNightMode) Color(0xFF1E293B) else Color(0xFF1E293B)
    val wheelRim = if (isNightMode) TechCyan.copy(alpha = 0.7f) else Color(0xFF64748B)
    val lightAccent = if (isNightMode) TechCyan else Color(0xFF0284C7)

    val canvasW = if (isCompact) 210.dp else 250.dp
    val canvasH = if (isCompact) 110.dp else 135.dp

    Box(
        modifier = modifier
            .width(canvasW)
            .height(canvasH),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 轮胎参数
            val wheelRadius = w * 0.082f
            val frontWheelCenter = Offset(w * 0.23f, h * 0.74f)
            val rearWheelCenter = Offset(w * 0.77f, h * 0.74f)

            // 1. 车身轮廓路径 (根据车型微调车顶拱度与乘员舱高度)
            val bodyPath = Path().apply {
                when (normalized) {
                    "Model Y" -> {
                        // Model Y: 跨界 SUV 溜背微高拱
                        moveTo(w * 0.05f, h * 0.65f) // 前唇
                        lineTo(w * 0.08f, h * 0.52f) // 前机盖倾角
                        cubicTo(w * 0.15f, h * 0.46f, w * 0.24f, h * 0.44f, w * 0.32f, h * 0.42f) // 机盖到风挡底
                        cubicTo(w * 0.40f, h * 0.25f, w * 0.48f, h * 0.20f, w * 0.56f, h * 0.21f) // 前风挡与高拱车顶
                        cubicTo(w * 0.68f, h * 0.24f, w * 0.80f, h * 0.34f, w * 0.88f, h * 0.50f) // 溜背尾门
                        lineTo(w * 0.93f, h * 0.54f) // 鸭尾后檐
                        lineTo(w * 0.91f, h * 0.68f) // 后包围
                        lineTo(w * 0.85f, h * 0.72f) // 后轮眉后部
                    }
                    "Model S" -> {
                        // Model S: 修长旗舰轿跑，低趴长车头
                        moveTo(w * 0.04f, h * 0.66f)
                        lineTo(w * 0.07f, h * 0.52f)
                        cubicTo(w * 0.18f, h * 0.44f, w * 0.28f, h * 0.41f, w * 0.36f, h * 0.38f) // 极长且低矮机盖
                        cubicTo(w * 0.44f, h * 0.24f, w * 0.54f, h * 0.21f, w * 0.64f, h * 0.22f) // 扁平车顶
                        cubicTo(w * 0.75f, h * 0.25f, w * 0.84f, h * 0.36f, w * 0.92f, h * 0.48f) // 优雅掀背
                        lineTo(w * 0.96f, h * 0.50f)
                        lineTo(w * 0.92f, h * 0.68f)
                        lineTo(w * 0.85f, h * 0.72f)
                    }
                    "Model X" -> {
                        // Model X: 大尺寸 SUV，挺拔前脸与高穹顶
                        moveTo(w * 0.05f, h * 0.66f)
                        lineTo(w * 0.08f, h * 0.51f)
                        cubicTo(w * 0.16f, h * 0.44f, w * 0.25f, h * 0.41f, w * 0.33f, h * 0.39f)
                        cubicTo(w * 0.42f, h * 0.21f, w * 0.52f, h * 0.18f, w * 0.62f, h * 0.19f) // 挺拔高穹顶
                        cubicTo(w * 0.74f, h * 0.22f, w * 0.84f, h * 0.34f, w * 0.91f, h * 0.48f)
                        lineTo(w * 0.94f, h * 0.52f)
                        lineTo(w * 0.92f, h * 0.68f)
                        lineTo(w * 0.85f, h * 0.72f)
                    }
                    else -> {
                        // Model 3: 灵动紧凑型运动轿车溜背
                        moveTo(w * 0.05f, h * 0.66f)
                        lineTo(w * 0.08f, h * 0.53f)
                        cubicTo(w * 0.16f, h * 0.46f, w * 0.26f, h * 0.43f, w * 0.33f, h * 0.40f) // 机盖
                        cubicTo(w * 0.42f, h * 0.26f, w * 0.50f, h * 0.22f, w * 0.60f, h * 0.23f) // 车顶
                        cubicTo(w * 0.72f, h * 0.26f, w * 0.82f, h * 0.38f, w * 0.89f, h * 0.48f) // 溜背
                        lineTo(w * 0.94f, h * 0.50f) // 小鸭尾
                        lineTo(w * 0.91f, h * 0.68f) // 后包围
                        lineTo(w * 0.85f, h * 0.72f)
                    }
                }

                // 后轮眉绕开
                cubicTo(w * 0.83f, h * 0.64f, w * 0.71f, h * 0.64f, w * 0.69f, h * 0.72f)
                // 底盘平整底护板
                lineTo(w * 0.31f, h * 0.72f)
                // 前轮眉绕开
                cubicTo(w * 0.29f, h * 0.64f, w * 0.17f, h * 0.64f, w * 0.15f, h * 0.72f)
                // 前下唇衔接
                lineTo(w * 0.05f, h * 0.68f)
                close()
            }

            // 填充并描边车身
            drawPath(path = bodyPath, color = carFill, style = Fill)
            drawPath(path = bodyPath, color = carStroke, style = Stroke(width = 2.2f))

            // 2. 侧窗区域 (DLO 玻璃)
            val windowPath = Path().apply {
                when (normalized) {
                    "Model Y" -> {
                        moveTo(w * 0.35f, h * 0.42f)
                        cubicTo(w * 0.42f, h * 0.28f, w * 0.49f, h * 0.25f, w * 0.56f, h * 0.25f)
                        cubicTo(w * 0.66f, h * 0.27f, w * 0.74f, h * 0.34f, w * 0.80f, h * 0.44f)
                        lineTo(w * 0.35f, h * 0.44f)
                        close()
                    }
                    "Model S" -> {
                        moveTo(w * 0.38f, h * 0.39f)
                        cubicTo(w * 0.46f, h * 0.27f, w * 0.55f, h * 0.25f, w * 0.64f, h * 0.26f)
                        cubicTo(w * 0.73f, h * 0.28f, w * 0.82f, h * 0.36f, w * 0.86f, h * 0.42f)
                        lineTo(w * 0.38f, h * 0.42f)
                        close()
                    }
                    "Model X" -> {
                        moveTo(w * 0.36f, h * 0.39f)
                        cubicTo(w * 0.44f, h * 0.25f, w * 0.53f, h * 0.22f, w * 0.62f, h * 0.23f)
                        cubicTo(w * 0.71f, h * 0.26f, w * 0.79f, h * 0.33f, w * 0.84f, h * 0.43f)
                        lineTo(w * 0.36f, h * 0.43f)
                        close()
                    }
                    else -> {
                        // Model 3
                        moveTo(w * 0.36f, h * 0.41f)
                        cubicTo(w * 0.43f, h * 0.28f, w * 0.51f, h * 0.26f, w * 0.59f, h * 0.27f)
                        cubicTo(w * 0.68f, h * 0.29f, w * 0.76f, h * 0.36f, w * 0.82f, h * 0.43f)
                        lineTo(w * 0.36f, h * 0.43f)
                        close()
                    }
                }
            }
            drawPath(path = windowPath, color = glassColor, style = Fill)
            drawPath(path = windowPath, color = carStroke.copy(alpha = 0.5f), style = Stroke(width = 1.3f))

            // B 柱与 C 柱分割线
            drawLine(
                color = carStroke.copy(alpha = 0.7f),
                start = Offset(w * 0.56f, h * 0.26f),
                end = Offset(w * 0.56f, h * 0.43f),
                strokeWidth = 2.5f
            )

            // 3. 隐藏式车门把手 (两处微小横线)
            drawLine(
                color = carStroke.copy(alpha = 0.75f),
                start = Offset(w * 0.44f, h * 0.49f),
                end = Offset(w * 0.48f, h * 0.49f),
                strokeWidth = 2f
            )
            drawLine(
                color = carStroke.copy(alpha = 0.75f),
                start = Offset(w * 0.63f, h * 0.49f),
                end = Offset(w * 0.67f, h * 0.49f),
                strokeWidth = 2f
            )

            // 4. 前后车灯光带细节点缀
            drawLine(
                color = lightAccent,
                start = Offset(w * 0.07f, h * 0.54f),
                end = Offset(w * 0.11f, h * 0.52f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = if (isNightMode) Color(0xFFFF3D71) else Color(0xFFDC2626),
                start = Offset(w * 0.91f, h * 0.51f),
                end = Offset(w * 0.93f, h * 0.52f),
                strokeWidth = 2.5f
            )

            // 5. 绘制前后轮毂与轮胎
            listOf(frontWheelCenter, rearWheelCenter).forEach { center ->
                // 外侧橡胶轮胎
                drawCircle(color = wheelTire, radius = wheelRadius, center = center)
                drawCircle(color = carStroke, radius = wheelRadius, center = center, style = Stroke(width = 1.8f))

                // 金属轮圈
                drawCircle(color = wheelRim.copy(alpha = 0.35f), radius = wheelRadius * 0.7f, center = center, style = Fill)
                drawCircle(color = wheelRim, radius = wheelRadius * 0.7f, center = center, style = Stroke(width = 1.6f))
                drawCircle(color = wheelRim, radius = wheelRadius * 0.25f, center = center, style = Fill)

                // 运动轮辐线条 (5 辐)
                for (i in 0 until 5) {
                    val angle = Math.toRadians((i * 72.0))
                    val start = Offset(
                        (center.x + Math.cos(angle) * (wheelRadius * 0.25f)).toFloat(),
                        (center.y + Math.sin(angle) * (wheelRadius * 0.25f)).toFloat()
                    )
                    val end = Offset(
                        (center.x + Math.cos(angle) * (wheelRadius * 0.68f)).toFloat(),
                        (center.y + Math.sin(angle) * (wheelRadius * 0.68f)).toFloat()
                    )
                    drawLine(color = wheelRim, start = start, end = end, strokeWidth = 1.6f)
                }
            }

            // 6. 地面微光阴影线
            drawLine(
                color = if (isNightMode) TechCyan.copy(alpha = 0.35f) else Color(0xFFCBD5E1),
                start = Offset(w * 0.08f, h * 0.83f),
                end = Offset(w * 0.92f, h * 0.83f),
                strokeWidth = 1.8f
            )
        }

        // 车型标识徽章
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isNightMode) TechCyan.copy(alpha = 0.16f) else Color(0xFFF1F5F9))
                .border(1.dp, if (isNightMode) TechCyan.copy(alpha = 0.35f) else InkBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = normalized,
                color = if (isNightMode) TechCyan else InkTextPrimary,
                fontSize = if (isCompact) 9.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
