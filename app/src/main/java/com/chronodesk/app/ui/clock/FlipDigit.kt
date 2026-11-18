package com.chronodesk.app.ui.clock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.geometry.Rect
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
import com.chronodesk.app.ui.theme.TextPrimary

private val TopHalfShape = GenericShape { size, _ ->
    addRect(Rect(0f, 0f, size.width, size.height / 2f))
}

private val BottomHalfShape = GenericShape { size, _ ->
    addRect(Rect(0f, size.height / 2f, size.width, size.height))
}

@Composable
fun FlipDigit(
    digit: Int,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 96.dp,
    fontSize: TextUnit = 64.sp,
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
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            previousDigit = currentDigit
            flipProgress.snapTo(0f)
        }
    }

    val progress = flipProgress.value
    val isFlipping = flipProgress.isRunning || progress > 0f
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
        // 底层静态背景上半部（新数字）
        FullDigitCard(
            digit = currentDigit,
            fontSize = fontSize,
            textColor = textColor,
            cardBg = cardBgElevated,
            modifier = Modifier
                .fillMaxSize()
                .clip(TopHalfShape)
        )

        // 底层静态背景下半部（未翻转完时为旧数字，翻转完成后为新数字）
        FullDigitCard(
            digit = if (isFlipping) previousDigit else currentDigit,
            fontSize = fontSize,
            textColor = textColor,
            cardBg = cardBg,
            modifier = Modifier
                .fillMaxSize()
                .clip(BottomHalfShape)
        )

        // 动态翻转叶片
        if (isFlipping) {
            if (progress < 0.5f) {
                // 前半段：旧数字上半部向下折叠 (0° -> 90°)
                val rotationAngle = -progress * 180f
                FullDigitCard(
                    digit = previousDigit,
                    fontSize = fontSize,
                    textColor = textColor,
                    cardBg = cardBgElevated,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationX = rotationAngle
                            cameraDistance = 16f * density
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .clip(TopHalfShape)
                )
            } else {
                // 后半段：新数字下半部展开落下 (90° -> 0°)
                val rotationAngle = 180f - (progress * 180f)
                FullDigitCard(
                    digit = currentDigit,
                    fontSize = fontSize,
                    textColor = textColor,
                    cardBg = cardBg,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationX = rotationAngle
                            cameraDistance = 16f * density
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .clip(BottomHalfShape)
                )
            }
        }

        // 翻页时钟物理中央中缝线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF000000))
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun FullDigitCard(
    digit: Int,
    fontSize: TextUnit,
    textColor: Color,
    cardBg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(cardBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            color = textColor,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FlipDigitPair(
    value: Int,
    modifier: Modifier = Modifier,
    digitWidth: Dp = 64.dp,
    digitHeight: Dp = 96.dp,
    fontSize: TextUnit = 64.sp,
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
