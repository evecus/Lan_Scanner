package com.lan.scanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lan.scanner.data.ScanUiState
import com.lan.scanner.ui.theme.BrandBlue
import com.lan.scanner.ui.theme.BrandCyan
import com.lan.scanner.ui.theme.OnlineGreen
import com.lan.scanner.ui.theme.OnlineGreenPale
import com.lan.scanner.ui.theme.SurfaceSoft
import com.lan.scanner.ui.theme.SurfaceWhite
import com.lan.scanner.ui.theme.TextPrimary
import com.lan.scanner.ui.theme.TextSecondary

@Composable
fun StatusArea(state: ScanUiState, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = state.statusText.isNotEmpty() || state.scanning,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceWhite,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = BrandBlue,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = state.statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.scanning) BrandBlue else OnlineGreen,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.devices.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(OnlineGreenPale)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "${state.devices.size} 台在线",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnlineGreen,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(13.dp))

                val target = when {
                    state.scanning -> state.progress
                    state.total > 0 -> 1f
                    else -> 0f
                }
                val progress by animateFloatAsState(
                    targetValue = target.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 260),
                    label = "progress",
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceSoft),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.horizontalGradient(listOf(BrandBlue, BrandCyan))),
                    )
                }

                if (state.total > 0) {
                    Spacer(Modifier.height(9.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                        )
                        Text(
                            text = "${state.scanned} / ${state.total}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
