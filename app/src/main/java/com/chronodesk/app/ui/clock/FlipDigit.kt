package com.chronodesk.app.ui.clock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronodesk.app.ui.theme.DarkBorder
import com.chronodesk.app.ui.theme.DarkSurface
import com.chronodesk.app.ui.theme.DarkSurfaceElevated
import com.chronodesk.app.ui.theme.TechCyan
import com.chronodesk.app.ui.theme.TextPrimary

@Composable
fun FlipDigit(
    digit: Int,
    modifier: Modifier = Modifier,
    width: Dp = 58.dp,
    height: Dp = 86.dp,
    fontSize: TextUnit = 60.sp,
    textColor: Color = TextPrimary,
    isNightMode: Boolean = false
) {
    var previousDigit by remember { mutableIntStateOf(digit) }
    var currentDigit by remember { mutableIntStateOf(digit) }
    val flipProgress = remember { Animatable(0f) }

    LaunchedEffect(digit) {
        if (digit != currentDigit) {
            previousDigit = currentDigit
            currentDigit = digit
            flipProgress.snapTo(0f)
            flipProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            )
        }
    }

    val progress = flipProgress.value
    val cardBg = if (isNightMode) Color(0xFF0F0F0F) else DarkSurface
    val cardBgElevated = if (isNightMode) Color(0xFF141414) else DarkSurfaceElevated
    val cardBorder = if (isNightMode) Color(0xFF1A1A1A) else DarkBorder

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(cardBgElevated, cardBg)
                )
            )
            .border(1.dp, cardBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        // 底层静态背景
        Column(modifier = Modifier.fillMaxSize()) {
            // 上半部分：准备显示新数字
            HalfDigitBox(
                digit = currentDigit,
                isTopHalf = true,
                width = width,
                height = height / 2,
                fontSize = fontSize,
                textColor = textColor
            )
            // 下半部分：显示旧数字，等待翻牌盖下
            HalfDigitBox(
                digit = previousDigit,
                isTopHalf = false,
                width = width,
                height = height / 2,
                fontSize = fontSize,
                textColor = textColor
            )
        }

        // 动态翻转叶片
        if (progress < 0.5f) {
            // 前半段：旧数字上半部向下折叠 (0° -> 90°)
            val rotationAngle = -progress * 180f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        rotationX = rotationAngle
                        cameraDistance = 16f * density
                        transformOrigin = TransformOrigin(0.5f, 1.0f)
                    }
                    .background(cardBgElevated)
            ) {
                HalfDigitBox(
                    digit = previousDigit,
                    isTopHalf = true,
                    width = width,
                    height = height / 2,
                    fontSize = fontSize,
                    textColor = textColor
                )
            }
        } else {
            // 后半段：新数字下半部展开落下 (90° -> 0°)
            val rotationAngle = 180f - (progress * 180f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height / 2)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        rotationX = rotationAngle
                        cameraDistance = 16f * density
                        transformOrigin = TransformOrigin(0.5f, 0.0f)
                    }
                    .background(cardBg)
            ) {
                HalfDigitBox(
                    digit = currentDigit,
                    isTopHalf = false,
                    width = width,
                    height = height / 2,
                    fontSize = fontSize,
                    textColor = textColor
                )
            }
        }

        // 物理翻页时钟中央缝隙阴影分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(Color(0xFF040608))
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun HalfDigitBox(
    digit: Int,
    isTopHalf: Boolean,
    width: Dp,
    height: Dp,
    fontSize: TextUnit,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clipToBounds(),
        contentAlignment = if (isTopHalf) Alignment.BottomCenter else Alignment.TopCenter
    ) {
        Text(
            text = digit.toString(),
            color = textColor,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                // 将文字垂直位移使半个字符居中
                translationY = if (isTopHalf) (height.toPx() * 0.5f) else (-height.toPx() * 0.5f)
            }
        )
    }
}

@Composable
fun FlipDigitPair(
    value: Int,
    modifier: Modifier = Modifier,
    digitWidth: Dp = 58.dp,
    digitHeight: Dp = 86.dp,
    fontSize: TextUnit = 60.sp,
    textColor: Color = TextPrimary,
    isNightMode: Boolean = false
) {
    val tens = (value.coerceIn(0, 99)) / 10
    val units = (value.coerceIn(0, 99)) % 10

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        FlipDigit(
            digit = tens,
            width = digitWidth,
            height = digitHeight,
            fontSize = fontSize,
            textColor = textColor,
            isNightMode = isNightMode
        )
        Spacer(modifier = Modifier.width(5.dp))
        FlipDigit(
            digit = units,
            width = digitWidth,
            height = digitHeight,
            fontSize = fontSize,
            textColor = textColor,
            isNightMode = isNightMode
        )
    }
}
