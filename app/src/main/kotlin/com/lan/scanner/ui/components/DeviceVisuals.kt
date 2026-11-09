package com.lan.scanner.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lan.scanner.data.DeviceKind
import com.lan.scanner.ui.theme.BrandBlue
import com.lan.scanner.ui.theme.BrandCyan
import com.lan.scanner.ui.theme.BrandViolet
import com.lan.scanner.ui.theme.DangerRed
import com.lan.scanner.ui.theme.OnlineGreen
import com.lan.scanner.ui.theme.TextSecondary

fun iconFor(kind: DeviceKind): ImageVector = when (kind) {
    DeviceKind.Desktop -> Icons.Rounded.Computer
    DeviceKind.Linux -> Icons.Rounded.Tv
    DeviceKind.Storage -> Icons.Rounded.Storage
    DeviceKind.Printer -> Icons.Rounded.Print
    DeviceKind.Phone -> Icons.Rounded.Smartphone
    DeviceKind.Iot -> Icons.Rounded.Memory
    DeviceKind.Router -> Icons.Rounded.Router
    DeviceKind.Web -> Icons.Rounded.Language
    DeviceKind.Generic -> Icons.Rounded.DevicesOther
}

data class PortColors(val background: Color, val foreground: Color)

private val neutral = PortColors(Color(0xFFF0F3FA), TextSecondary)

private val presets: Map<Int, PortColors> = mapOf(
    80 to PortColors(Color(0xFFE8F0FF), Color(0xFF2563EB)),
    443 to PortColors(Color(0xFFE6F8F1), Color(0xFF0E9F6E)),
    22 to PortColors(Color(0xFFFFF1E7), Color(0xFFD97706)),
    3389 to PortColors(Color(0xFFF1EBFF), BrandViolet),
    445 to PortColors(Color(0xFFE8F0FF), BrandBlue),
    5555 to PortColors(Color(0xFFFFE9EC), DangerRed),
    9100 to PortColors(Color(0xFFE5F7F6), BrandCyan),
    8080 to PortColors(Color(0xFFE8F0FF), Color(0xFF2563EB)),
    8443 to PortColors(Color(0xFFE6F8F1), Color(0xFF0E9F6E)),
    21 to PortColors(Color(0xFFFFF1E7), Color(0xFFD97706)),
    23 to PortColors(Color(0xFFFFF1E7), Color(0xFFD97706)),
    1883 to PortColors(Color(0xFFF1EBFF), BrandViolet),
    5000 to PortColors(Color(0xFFE5F7F6), BrandCyan),
    53 to PortColors(Color(0xFFE6F8F1), OnlineGreen),
)

fun colorsForPort(port: Int): PortColors = presets[port] ?: neutral
