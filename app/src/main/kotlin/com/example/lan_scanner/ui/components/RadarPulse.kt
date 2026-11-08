package com.example.lan_scanner.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lan_scanner.ui.theme.BrandBlue
import com.example.lan_scanner.ui.theme.BrandCyan

/** 呼吸式雷达波纹，扫描时持续扩散，空闲时静止显示中心图标。 */
@Composable
fun RadarPulse(
    scanning: Boolean,
    modifier: Modifier = Modifier,
    diameter: Dp = 92.dp,
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarPhase",
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            // 3 圈错开的扩散波纹
            repeat(3) { index ->
                val p = if (scanning) (phase + index / 3f) % 1f else 0.55f
                val alpha = if (scanning) (1f - p) * 0.38f else 0.08f
                drawCircle(
                    color = BrandBlue.copy(alpha = alpha),
                    radius = maxRadius * (0.32f + p * 0.66f),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            // 中心渐变实心圆
            drawCircle(
                brush = Brush.linearGradient(listOf(BrandBlue, BrandCyan)),
                radius = maxRadius * 0.34f,
                center = center,
            )
        }
        Icon(
            imageVector = Icons.Rounded.Wifi,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(diameter * 0.34f),
        )
    }
}
