package com.lan.scanner.ui

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lan.scanner.ScanViewModel
import com.lan.scanner.UiEvent
import com.lan.scanner.ui.components.DeviceCard
import com.lan.scanner.ui.components.EmptyState
import com.lan.scanner.ui.components.InputPanel
import com.lan.scanner.ui.components.RadarPulse
import com.lan.scanner.ui.components.StatusArea
import com.lan.scanner.ui.theme.Background
import com.lan.scanner.ui.theme.BrandBlue
import com.lan.scanner.ui.theme.BrandViolet
import com.lan.scanner.ui.theme.SurfaceWhite
import com.lan.scanner.ui.theme.TextPrimary
import com.lan.scanner.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: ScanViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Toast -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Share -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        putExtra(Intent.EXTRA_SUBJECT, "LAN 扫描结果")
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(send, "导出扫描结果"))
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = TextPrimary,
                    contentColor = Color.White,
                    dismissActionContentColor = BrandViolet,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Header(
                    scanning = state.scanning,
                    canShare = state.devices.isNotEmpty() && !state.scanning,
                    onShare = viewModel::exportResults,
                )
            }

            item(key = "input") {
                InputPanel(
                    state = state,
                    onCidrChange = viewModel::onCidrChange,
                    onPortsChange = viewModel::onPortsChange,
                    onAutoDetect = { viewModel.autoFillCidr() },
                    onToggleDefaults = viewModel::toggleDefaults,
                    onTogglePort = viewModel::togglePort,
                    onToggleScan = viewModel::toggleScan,
                )
            }

            item(key = "status") {
                StatusArea(state = state)
            }

            if (state.devices.isEmpty()) {
                item(key = "empty") { EmptyState(scanning = state.scanning) }
            } else {
                item(key = "listHeader") {
                    ListHeader(
                        count = state.devices.size,
                        showClear = !state.scanning,
                        onClear = viewModel::clearResults,
                    )
                }
                items(state.devices, key = { it.ip }) { device ->
                    DeviceCard(
                        device = device,
                        onCopy = {
                            clipboard.setText(AnnotatedString(device.ip))
                            viewModel.onCopied(device.ip)
                        },
                    )
                }
            }

            item(key = "footer") { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun Header(
    scanning: Boolean,
    canShare: Boolean,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadarPulse(scanning = scanning, diameter = 58.dp)

        Spacer(Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "LAN Scanner",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = (-0.6).sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "局域网设备扫描 · 白色轻快版",
                fontSize = 11.5.sp,
                color = TextSecondary,
            )
        }

        if (canShare) {
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(SurfaceWhite),
            ) {
                Icon(
                    Icons.Rounded.IosShare,
                    contentDescription = "导出结果",
                    tint = BrandBlue,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ListHeader(count: Int, showClear: Boolean, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(BrandBlue, BrandViolet))),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = "发现的设备",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(BrandBlue.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlue,
            )
        }
        Spacer(Modifier.weight(1f))
        if (showClear) {
            IconButton(onClick = onClear, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Rounded.DeleteSweep,
                    contentDescription = "清空结果",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}
