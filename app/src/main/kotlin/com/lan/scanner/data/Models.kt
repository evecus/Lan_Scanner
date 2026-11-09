package com.lan.scanner.data

/** 设备类型，用于 UI 层选择图标（data 层不依赖 Compose）。 */
enum class DeviceKind { Desktop, Linux, Storage, Printer, Phone, Iot, Router, Web, Generic }

data class DeviceInfo(
    val ip: String,
    val hostname: String = "",
    val openPorts: List<Int> = emptyList(),
    val discoveredAt: Long = System.currentTimeMillis(),
) {
    val displayName: String
        get() = if (hostname.isNotBlank() && hostname != ip) hostname else ip

    val portSummary: String
        get() = openPorts.joinToString(", ") { PortCatalog.label(it) }

    val kind: DeviceKind
        get() {
            for (p in openPorts) {
                when (p) {
                    3389 -> return DeviceKind.Desktop
                    22 -> return DeviceKind.Linux
                    445 -> return DeviceKind.Storage
                    9100 -> return DeviceKind.Printer
                    5555 -> return DeviceKind.Phone
                    1883 -> return DeviceKind.Iot
                }
            }
            val h = hostname.lowercase()
            if (h.contains("router") || h.contains("gateway")) return DeviceKind.Router
            if (openPorts.contains(80) || openPorts.contains(443)) return DeviceKind.Web
            return DeviceKind.Generic
        }
}

object PortCatalog {
    val defaults: List<Int> = listOf(
        21, 22, 23, 80, 443, 445, 1883, 3389, 5000, 5555, 8080, 8443, 9100
    )

    private val labels: Map<Int, String> = mapOf(
        21 to "FTP",
        22 to "SSH",
        23 to "Telnet",
        80 to "HTTP",
        443 to "HTTPS",
        445 to "SMB",
        1883 to "MQTT",
        3389 to "RDP",
        5000 to "UPnP",
        5555 to "ADB",
        8080 to "HTTP-Alt",
        8443 to "HTTPS-Alt",
        9100 to "Print",
    )

    fun label(port: Int): String = labels[port] ?: port.toString()
}

data class ScanProgress(
    val total: Int,
    val scanned: Int,
    val newDevice: DeviceInfo?,
) {
    val percent: Float get() = if (total == 0) 0f else scanned.toFloat() / total
}

data class ScanUiState(
    val cidr: String = "",
    val portsText: String = "",
    val showDefaults: Boolean = false,
    val devices: List<DeviceInfo> = emptyList(),
    val scanning: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 0,
    val statusText: String = "",
    val autoDetected: Boolean = false,
) {
    val progress: Float get() = if (total == 0) 0f else scanned.toFloat() / total
    val finished: Boolean get() = !scanning && total > 0
}
