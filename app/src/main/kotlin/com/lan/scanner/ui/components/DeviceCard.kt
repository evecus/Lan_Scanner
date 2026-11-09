package com.lan.scanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lan.scanner.data.DeviceInfo
import com.lan.scanner.data.PortCatalog
import com.lan.scanner.ui.theme.BrandBlue
import com.lan.scanner.ui.theme.BrandViolet
import com.lan.scanner.ui.theme.SurfaceSoft
import com.lan.scanner.ui.theme.TextPrimary
import com.lan.scanner.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceCard(
    device: DeviceInfo,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember(device.ip) { mutableStateOf(false) }
    LaunchedEffect(device.ip) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 3 },
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = androidx.compose.ui.graphics.Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 设备图标：柔和渐变圆角方块
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(BrandBlue.copy(alpha = 0.12f), BrandViolet.copy(alpha = 0.12f))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconFor(device.kind),
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.ip,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        if (device.hostname.isNotBlank() && device.hostname != device.ip) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = device.hostname,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }

                    // 对齐原版：无开放端口时 subtitle 为空，不显示任何说明文字
                    if (device.openPorts.isNotEmpty()) {
                        Spacer(Modifier.height(7.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            device.openPorts.forEach { port ->
                                PortChip(port = port)
                            }
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceSoft),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "复制 IP",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PortChip(port: Int) {
    val colors = colorsForPort(port)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(colors.background)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = "$port · ${PortCatalog.label(port)}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.foreground,
        )
    }
}
