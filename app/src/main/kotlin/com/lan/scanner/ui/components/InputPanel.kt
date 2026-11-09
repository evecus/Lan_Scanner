package com.lan.scanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lan.scanner.data.PortCatalog
import com.lan.scanner.data.ScanUiState
import com.lan.scanner.ui.theme.BrandBlue
import com.lan.scanner.ui.theme.BrandBluePale
import com.lan.scanner.ui.theme.OutlineSoft
import com.lan.scanner.ui.theme.SurfaceSoft
import com.lan.scanner.ui.theme.SurfaceWhite
import com.lan.scanner.ui.theme.TextPrimary
import com.lan.scanner.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputPanel(
    state: ScanUiState,
    onCidrChange: (String) -> Unit,
    onPortsChange: (String) -> Unit,
    onAutoDetect: () -> Unit,
    onToggleDefaults: () -> Unit,
    onTogglePort: (Int) -> Unit,
    onToggleScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            // 面板标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(BrandBluePale),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "扫描设置",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                if (state.autoDetected) {
                    Text(text = "已识别当前网络", fontSize = 11.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(18.dp))

            FieldLabel("扫描网段", "CIDR")
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = state.cidr,
                onValueChange = { input ->
                    onCidrChange(input.filter { it.isDigit() || it == '.' || it == '/' })
                },
                enabled = !state.scanning,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Icon(Icons.Rounded.Lan, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    IconButton(onClick = onAutoDetect, enabled = !state.scanning) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "自动获取网段", tint = BrandBlue)
                    }
                },
                placeholder = { Text("例如 192.168.1.0/24", fontSize = 13.sp, color = TextSecondary) },
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = fieldColors(),
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("自定义端口", "留空使用默认 13 个端口")
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = state.portsText,
                onValueChange = { input ->
                    onPortsChange(input.filter { it.isDigit() || it == ',' || it == ' ' || it == '，' || it == '、' })
                },
                enabled = !state.scanning,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Icon(Icons.Rounded.Lan, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    androidx.compose.material3.TextButton(
                        onClick = onToggleDefaults,
                        enabled = !state.scanning,
                    ) {
                        Text(
                            text = if (state.showDefaults) "收起" else "默认",
                            fontSize = 12.sp,
                            color = BrandBlue,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                placeholder = { Text("留空或输入: 80,443,8080", fontSize = 13.sp, color = TextSecondary) },
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors(),
            )

            AnimatedVisibility(
                visible = state.showDefaults,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "点击添加 / 移除常用端口",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        PortCatalog.defaults.forEach { port ->
                            DefaultPortChip(
                                port = port,
                                selected = isPortSelected(state.portsText, port),
                                enabled = !state.scanning,
                                onClick = { onTogglePort(port) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            ScanButton(scanning = state.scanning, onClick = onToggleScan)
        }
    }
}

@Composable
private fun FieldLabel(title: String, hint: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(Modifier.width(8.dp))
        Text(text = hint, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandBlue,
    unfocusedBorderColor = OutlineSoft,
    disabledBorderColor = OutlineSoft,
    focusedContainerColor = SurfaceSoft,
    unfocusedContainerColor = SurfaceSoft,
    disabledContainerColor = SurfaceSoft,
    cursorColor = BrandBlue,
    focusedLeadingIconColor = BrandBlue,
    unfocusedLeadingIconColor = TextSecondary,
)

@Composable
private fun DefaultPortChip(
    port: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) BrandBlue else SurfaceSoft
    val fg = if (selected) Color.White else TextSecondary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            text = "$port",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

@Composable
private fun ScanButton(scanning: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "buttonScale",
    )

    val brush = if (scanning) {
        Brush.horizontalGradient(com.lan.scanner.ui.theme.StopGradient)
    } else {
        Brush.horizontalGradient(com.lan.scanner.ui.theme.BrandGradient)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale)
            .clip(RoundedCornerShape(17.dp))
            .background(brush)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (scanning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (scanning) "停止扫描" else "开始扫描",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

private fun isPortSelected(text: String, port: Int): Boolean =
    text.split(Regex("[,\\s，、]+")).any { it.trim() == port.toString() }
