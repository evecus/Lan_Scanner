package com.example.lan_scanner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lan_scanner.ui.theme.TextPrimary
import com.example.lan_scanner.ui.theme.TextSecondary

@Composable
fun EmptyState(scanning: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RadarPulse(scanning = scanning, diameter = 104.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (scanning) "正在扫描局域网…" else "开始发现身边的设备",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = if (scanning) {
                "正在逐一探测主机，结果会实时出现在这里"
            } else {
                "输入网段后点击「开始扫描」\n默认扫描 13 个常用端口"
            },
            fontSize = 12.5.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
