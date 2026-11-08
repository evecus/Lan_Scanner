package com.example.lan_scanner.scan

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

/** 自动推断当前 WiFi 的 /24 网段，功能对齐 network_info_plus.getWifiIP()。 */
object NetworkUtils {

    fun autoDetectCidr(context: Context): String? {
        fromWifiManager(context)?.let { return it }
        return fromInterfaces()
    }

    @Suppress("DEPRECATION")
    private fun fromWifiManager(context: Context): String? = try {
        val wifi = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val raw = wifi?.connectionInfo?.ipAddress ?: 0
        if (raw == 0) {
            null
        } else {
            // WifiManager 返回的是小端序整数
            val a = raw and 0xFF
            val b = (raw shr 8) and 0xFF
            val c = (raw shr 16) and 0xFF
            "$a.$b.$c.0/24"
        }
    } catch (e: Exception) {
        null
    }

    private fun fromInterfaces(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            var result: String? = null
            loop@ while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                val addresses = nif.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val parts = addr.hostAddress?.split(".") ?: continue
                        if (parts.size == 4) {
                            result = "${parts[0]}.${parts[1]}.${parts[2]}.0/24"
                            break@loop
                        }
                    }
                }
            }
            result
        } catch (e: Exception) {
            null
        }
    }
}
