package com.chronodesk.app.ui.tesla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.data.model.VehicleSnapshot
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.InkBorder
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

/**
 * 车辆俯视矢量图与四轮胎压监控组件
 * 纯 Compose Canvas 自绘俯视轮廓，严禁任何第三方商业车标
 */
@Composable
fun TirePressureDiagram(
    snapshot: VehicleSnapshot,
    isNightMode: Boolean,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val carStrokeColor = if (isNightMode) TechCyan.copy(alpha = 0.55f) else Color(0xFF475569)
    val carFillColor = if (isNightMode) Color(0xFF0F172A).copy(alpha = 0.7f) else Color(0xFFE2E8F0).copy(alpha = 0.8f)
    val glassStrokeColor = if (isNightMode) TechCyan.copy(alpha = 0.35f) else Color(0xFF94A3B8)
    val tireColor = if (isNightMode) Color(0xFF1E293B) else Color(0xFF334155)

    val boxWidth = if (isCompact) 210.dp else 250.dp
    val boxHeight = if (isCompact) 155.dp else 185.dp

    Box(
        modifier = modifier
            .width(boxWidth)
            .height(boxHeight),
        contentAlignment = Alignment.Center
    ) {
        // 中央自绘车辆俯视轮廓 Canvas
        Canvas(modifier = Modifier.size(if (isCompact) 85.dp else 105.dp, if (isCompact) 145.dp else 175.dp)) {
            val w = size.width
            val h = size.height

            // 1. 四只车轮绘制 (位于车身两侧)
            val tireW = w * 0.16f
            val tireH = h * 0.22f
            val tireRadius = CornerRadius(tireW * 0.35f, tireW * 0.35f)

            // 左前轮
            drawRoundRect(
                color = tireColor,
                topLeft = Offset(w * 0.04f, h * 0.16f),
                size = Size(tireW, tireH),
                cornerRadius = tireRadius
            )
            // 右前轮
            drawRoundRect(
                color = tireColor,
                topLeft = Offset(w * 0.80f, h * 0.16f),
                size = Size(tireW, tireH),
                cornerRadius = tireRadius
            )
            // 左后轮
            drawRoundRect(
                color = tireColor,
                topLeft = Offset(w * 0.04f, h * 0.62f),
                size = Size(tireW, tireH),
                cornerRadius = tireRadius
            )
            // 右后轮
            drawRoundRect(
                color = tireColor,
                topLeft = Offset(w * 0.80f, h * 0.62f),
                size = Size(tireW, tireH),
                cornerRadius = tireRadius
            )

            // 2. 车身主体轮廓路径
            val bodyPath = Path().apply {
                // 车头中心
                moveTo(w * 0.5f, h * 0.04f)
                // 车头右侧圆弧过渡
                cubicTo(w * 0.72f, h * 0.04f, w * 0.86f, h * 0.12f, w * 0.86f, h * 0.25f)
                // 车身右侧腰线
                cubicTo(w * 0.88f, h * 0.40f, w * 0.88f, h * 0.60f, w * 0.86f, h * 0.75f)
                // 车尾右侧过渡
                cubicTo(w * 0.86f, h * 0.88f, w * 0.72f, h * 0.96f, w * 0.5f, h * 0.96f)
                // 车尾左侧过渡
                cubicTo(w * 0.28f, h * 0.96f, w * 0.14f, h * 0.88f, w * 0.14f, h * 0.75f)
                // 车身左侧腰线
                cubicTo(w * 0.12f, h * 0.60f, w * 0.12f, h * 0.40f, w * 0.14f, h * 0.25f)
                // 车头左侧过渡
                cubicTo(w * 0.14f, h * 0.12f, w * 0.28f, h * 0.04f, w * 0.5f, h * 0.04f)
                close()
            }

            drawPath(bodyPath, color = carFillColor)
            drawPath(bodyPath, color = carStrokeColor, style = Stroke(width = 1.6f))

            // 3. 前后挡风玻璃与全景天幕线条
            // 前挡风玻璃弧线
            val frontWindshield = Path().apply {
                moveTo(w * 0.25f, h * 0.32f)
                cubicTo(w * 0.36f, h * 0.26f, w * 0.64f, h * 0.26f, w * 0.75f, h * 0.32f)
            }
            drawPath(frontWindshield, color = glassStrokeColor, style = Stroke(width = 1.2f))

            // 全景玻璃天幕车顶矩形
            drawRoundRect(
                color = glassStrokeColor.copy(alpha = 0.2f),
                topLeft = Offset(w * 0.28f, h * 0.34f),
                size = Size(w * 0.44f, h * 0.40f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // 后挡风玻璃弧线
            val rearWindshield = Path().apply {
                moveTo(w * 0.25f, h * 0.74f)
                cubicTo(w * 0.36f, h * 0.78f, w * 0.64f, h * 0.78f, w * 0.75f, h * 0.74f)
            }
            drawPath(rearWindshield, color = glassStrokeColor, style = Stroke(width = 1.2f))
        }

        // 4. 四个角落的胎压指标胶囊卡片
        // 左前 FL
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            TireBadge(
                label = "左前",
                pressure = snapshot.tpmsPressureFl,
                isNightMode = isNightMode,
                isCompact = isCompact
            )
        }

        // 右前 FR
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            TireBadge(
                label = "右前",
                pressure = snapshot.tpmsPressureFr,
                isNightMode = isNightMode,
                isCompact = isCompact
            )
        }

        // 左后 RL
        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            TireBadge(
                label = "左后",
                pressure = snapshot.tpmsPressureRl,
                isNightMode = isNightMode,
                isCompact = isCompact
            )
        }

        // 右后 RR
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            TireBadge(
                label = "右后",
                pressure = snapshot.tpmsPressureRr,
                isNightMode = isNightMode,
                isCompact = isCompact
            )
        }
    }
}

@Composable
private fun TireBadge(
    label: String,
    pressure: Double?,
    isNightMode: Boolean,
    isCompact: Boolean
) {
    val bg = if (isNightMode) DarkSurfaceElevated else InkSurfaceElevated
    val border = if (isNightMode) DarkBorder else InkBorder

    // 胎压状态评级 (Tesla 标准冷胎压一般为 2.8 ~ 2.9 bar)
    val (statusColor, _) = when {
        pressure == null -> (if (isNightMode) TextTertiary else InkTextTertiary) to "未采样"
        pressure < 2.4 -> (if (isNightMode) TechRose else InkWarningRed) to "偏低"
        pressure > 3.2 -> (if (isNightMode) TechAmber else InkWarningAmber) to "偏高"
        else -> (if (isNightMode) TechEmerald else Color(0xFF16A34A)) to "正常"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(
                horizontal = if (isCompact) 6.dp else 8.dp,
                vertical = if (isCompact) 3.5.dp else 5.dp
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(statusColor)
                )
                Text(
                    text = label,
                    color = if (isNightMode) TextSecondary else InkTextSecondary,
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = if (pressure != null) String.format("%.2f bar", pressure) else "— bar",
                color = if (isNightMode) TextPrimary else InkTextPrimary,
                fontSize = if (isCompact) 11.sp else 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
