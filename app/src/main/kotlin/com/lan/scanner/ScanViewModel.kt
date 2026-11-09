package com.lan.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lan.scanner.data.DeviceInfo
import com.lan.scanner.data.PortCatalog
import com.lan.scanner.data.ScanUiState
import com.lan.scanner.scan.NetworkUtils
import com.lan.scanner.scan.PortParseResult
import com.lan.scanner.scan.PortParser
import com.lan.scanner.scan.ScannerService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiEvent {
    data class Toast(val message: String) : UiEvent
    data class Share(val text: String) : UiEvent
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val scanner = ScannerService()

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var scanJob: Job? = null

    init {
        autoFillCidr(notify = false)
    }

    fun autoFillCidr(notify: Boolean = true) {
        val cidr = NetworkUtils.autoDetectCidr(app)
        if (cidr != null) {
            _uiState.update { it.copy(cidr = cidr, autoDetected = true) }
            if (notify) sendEvent(UiEvent.Toast("已自动获取当前网段"))
        } else if (notify) {
            sendEvent(UiEvent.Toast("无法获取 WiFi 网段，请手动输入"))
        }
    }

    fun onCidrChange(value: String) {
        if (!_uiState.value.scanning) _uiState.update { it.copy(cidr = value) }
    }

    fun onPortsChange(value: String) {
        if (!_uiState.value.scanning) _uiState.update { it.copy(portsText = value) }
    }

    fun toggleDefaults() {
        _uiState.update { it.copy(showDefaults = !it.showDefaults) }
    }

    fun togglePort(port: Int) {
        _uiState.update { state ->
            val current = state.portsText
                .split(Regex("[,\\s，、]+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableList()
            val token = port.toString()
            if (current.contains(token)) current.remove(token) else current.add(token)
            state.copy(portsText = current.joinToString(","))
        }
    }

    fun clearResults() {
        if (_uiState.value.scanning) return
        _uiState.update {
            it.copy(
                devices = emptyList(),
                scanned = 0,
                total = 0,
                statusText = "",
            )
        }
    }

    fun toggleScan() {
        if (_uiState.value.scanning) stopScan() else startScan()
    }

    private fun startScan() {
        val state = _uiState.value
        val cidr = state.cidr.trim()
        if (cidr.isEmpty()) {
            sendEvent(UiEvent.Toast("请输入网段，例如 192.168.1.0/24"))
            return
        }

        val ports = when (val result = PortParser.parse(state.portsText)) {
            PortParseResult.UseDefault -> PortCatalog.defaults
            is PortParseResult.Ok -> result.ports
            PortParseResult.Invalid -> {
                sendEvent(UiEvent.Toast("端口格式错误，请用逗号分隔，例如：80,443,22"))
                return
            }
        }

        _uiState.update {
            it.copy(
                devices = emptyList(),
                scanning = true,
                scanned = 0,
                total = 0,
                statusText = "正在扫描…",
            )
        }

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                scanner.scan(cidr, ports).collect { progress ->
                    _uiState.update { current ->
                        val devices = if (progress.newDevice != null) {
                            listOf(progress.newDevice) + current.devices.filterNot { d ->
                                d.ip == progress.newDevice.ip
                            }
                        } else {
                            current.devices
                        }
                        current.copy(
                            scanned = maxOf(current.scanned, progress.scanned),
                            total = progress.total,
                            devices = devices,
                            statusText = "扫描中 ${progress.scanned} / ${progress.total}",
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        scanning = false,
                        statusText = "扫描完成，发现 ${it.devices.size} 台设备",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(scanning = false, statusText = "错误：${e.message}")
                }
                sendEvent(UiEvent.Toast("扫描失败：${e.message}"))
            }
        }
    }

    private fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update {
            it.copy(
                scanning = false,
                statusText = "已停止，发现 ${it.devices.size} 台设备",
            )
        }
    }

    fun exportResults() {
        val state = _uiState.value
        if (state.devices.isEmpty()) return
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val text = buildString {
            appendLine("LAN 扫描结果 - $time")
            appendLine("网段: ${state.cidr}")
            appendLine("─".repeat(40))
            state.devices.forEach { d ->
                appendLine("IP: ${d.ip}")
                if (d.hostname.isNotBlank() && d.hostname != d.ip) {
                    appendLine("  主机名: ${d.hostname}")
                }
                if (d.openPorts.isNotEmpty()) {
                    appendLine("  开放端口: ${d.portSummary}")
                }
                appendLine()
            }
        }
        sendEvent(UiEvent.Share(text))
    }

    fun onCopied(ip: String) = sendEvent(UiEvent.Toast("已复制 $ip"))

    private fun sendEvent(event: UiEvent) {
        _events.trySend(event)
    }

    override fun onCleared() {
        scanJob?.cancel()
        super.onCleared()
    }
}

/** 供 UI 预览/工具使用 */
internal fun previewDevices(): List<DeviceInfo> = listOf(
    DeviceInfo("192.168.1.1", "router.local", listOf(53, 80, 443)),
    DeviceInfo("192.168.1.23", "Office-PC", listOf(22, 3389, 445)),
    DeviceInfo("192.168.1.42", "", listOf(80, 8080, 9100)),
)
