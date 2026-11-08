package com.example.lan_scanner.scan

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import kotlin.coroutines.coroutineContext

/**
 * 轻量级 mDNS（Bonjour）发现。
 *
 * 向 224.0.0.251:5353 组播 PTR 查询，解析应答包中的 A / PTR / SRV 记录，
 * 建立「IP -> 设备名」映射写入 [sink]。全程尽力而为：任何异常都被吞掉，
 * 因为 mDNS 只是 TCP 扫描的补充，不能影响主流程。
 *
 * 采用纯 JDK UDP 实现，不依赖 Android NsdManager（后者只能按服务类型逐个发现）。
 */
object MdnsDiscovery {

    private const val GROUP = "224.0.0.251"
    private const val PORT = 5353
    private const val RECEIVE_TIMEOUT_MS = 350

    private val QUERY_NAMES = listOf(
        "_services._dns-sd._udp.local",
        "_workstation._tcp.local",
        "_http._tcp.local",
        "_smb._tcp.local",
        "_ssh._tcp.local",
        "_printer._tcp.local",
        "_ipp._tcp.local",
        "_device-info._tcp.local",
        "_googlecast._tcp.local",
        "_airplay._tcp.local",
        "_raop._tcp.local",
    )

    suspend fun discoverInto(
        context: Context,
        sink: MutableMap<String, String>,
        durationMs: Long,
    ) = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = try {
            wifi?.createMulticastLock("LanScanner.mDNS")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            null
        }

        var socket: MulticastSocket? = null
        try {
            socket = MulticastSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(PORT))
                soTimeout = RECEIVE_TIMEOUT_MS
                timeToLive = 255
            }
            val group = InetAddress.getByName(GROUP)
            try {
                socket.joinGroup(group)
            } catch (e: Exception) {
                // 部分设备需要显式网络接口，失败则仍尝试收包
            }

            for (name in QUERY_NAMES) {
                val query = buildPtrQuery(name)
                try {
                    socket.send(DatagramPacket(query, query.size, group, PORT))
                } catch (e: Exception) {
                    // 忽略单个查询失败
                }
            }

            val deadline = System.currentTimeMillis() + durationMs
            val buffer = ByteArray(4096)
            while (coroutineContext.isActive && System.currentTimeMillis() < deadline) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    continue
                } catch (e: Exception) {
                    break
                }
                try {
                    parseResponse(packet.data, packet.length, sink)
                } catch (e: Exception) {
                    // 单个畸形报文不影响后续
                }
            }
        } catch (e: Exception) {
            // mDNS 不可用（端口占用 / 无组播 / 权限），静默降级
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // ignore
            }
            try {
                if (lock?.isHeld == true) lock.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun buildPtrQuery(qname: String): ByteArray {
        val out = ByteArrayOutputStream()
        // Header: ID=0, Flags=0, QD=1, AN=0, NS=0, AR=0
        writeU16(out, 0)
        writeU16(out, 0)
        writeU16(out, 1)
        writeU16(out, 0)
        writeU16(out, 0)
        writeU16(out, 0)
        for (label in qname.split(".")) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes, 0, bytes.size)
        }
        out.write(0) // 根标签
        writeU16(out, 12) // QTYPE = PTR
        writeU16(out, 1)  // QCLASS = IN
        return out.toByteArray()
    }

    private fun writeU16(out: ByteArrayOutputStream, value: Int) {
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun u16(data: ByteArray, offset: Int): Int {
        if (offset + 1 >= data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    /** 读取 DNS name，支持 0xC0 压缩指针，返回 (名称, 该字段结束偏移)。 */
    private fun readName(data: ByteArray, start: Int, limit: Int): Pair<String, Int> {
        val parts = mutableListOf<String>()
        var pos = start
        var endOffset = -1
        var jumps = 0
        while (pos < limit && jumps < 24) {
            val len = data[pos].toInt() and 0xFF
            if (len == 0) {
                pos++
                break
            }
            if ((len and 0xC0) == 0xC0) {
                if (pos + 1 >= limit) break
                val ptr = ((len and 0x3F) shl 8) or (data[pos + 1].toInt() and 0xFF)
                if (ptr >= limit) break
                if (endOffset < 0) endOffset = pos + 2
                pos = ptr
                jumps++
                continue
            }
            pos++
            if (pos + len > limit) break
            parts.add(String(data, pos, len, Charsets.UTF_8))
            pos += len
        }
        return parts.joinToString(".") to (if (endOffset >= 0) endOffset else pos)
    }

    private fun parseResponse(data: ByteArray, length: Int, sink: MutableMap<String, String>) {
        if (length < 12) return
        val flags = u16(data, 2)
        if ((flags and 0x8000) == 0) return // 不是应答
        val qdCount = u16(data, 4)
        val anCount = u16(data, 6)
        val nsCount = u16(data, 8)
        val arCount = u16(data, 10)

        var pos = 12
        // 跳过 Question 段
        repeat(qdCount) {
            if (pos >= length) return
            val (_, next) = readName(data, pos, length)
            pos = next + 4 // QTYPE + QCLASS
        }

        val addressNames = mutableMapOf<String, String>()  // IP -> owner name
        val friendlyNames = mutableListOf<String>()         // 候选主机名

        val rrCount = anCount + nsCount + arCount
        repeat(rrCount) {
            if (pos >= length) return
            val (owner, next) = readName(data, pos, length)
            pos = next
            if (pos + 10 > length) return
            val type = u16(data, pos)
            pos += 2 // TYPE
            pos += 2 // CLASS
            pos += 4 // TTL
            val rdLen = u16(data, pos)
            pos += 2
            if (pos + rdLen > length) return
            val rdataEnd = pos + rdLen

            when (type) {
                1 -> { // A 记录
                    if (rdLen == 4) {
                        val ip = buildString {
                            append(data[pos].toInt() and 0xFF).append('.')
                            append(data[pos + 1].toInt() and 0xFF).append('.')
                            append(data[pos + 2].toInt() and 0xFF).append('.')
                            append(data[pos + 3].toInt() and 0xFF)
                        }
                        if (isLanAddress(ip)) addressNames[ip] = owner
                        owner.firstLabel()?.let { if (!it.startsWith("_")) friendlyNames.add(it) }
                    }
                }
                12 -> { // PTR 记录：实例名 / 服务名
                    val (target, _) = readName(data, pos, rdataEnd)
                    target.firstLabel()?.let { if (!it.startsWith("_")) friendlyNames.add(it) }
                }
                33 -> { // SRV 记录：末尾是 target 主机名
                    if (rdLen >= 7) {
                        val (target, _) = readName(data, pos + 6, rdataEnd)
                        target.firstLabel()?.let { if (!it.startsWith("_")) friendlyNames.add(it) }
                    }
                }
            }
            pos = rdataEnd
        }

        val preferred = friendlyNames.firstOrNull { it.isNotBlank() && !it.equals("local", true) }
        for ((ip, ownerName) in addressNames) {
            val name = preferred
                ?: ownerName.firstLabel()?.takeIf { it.isNotBlank() && !it.startsWith("_") }
                ?: "mDNS 设备"
            if (!sink.containsKey(ip) || sink[ip].isNullOrBlank()) {
                sink[ip] = name
            }
        }
    }

    private fun String.firstLabel(): String? =
        split(".").firstOrNull()?.takeIf { it.isNotBlank() }?.trimEnd('.')

    private fun isLanAddress(ip: String): Boolean {
        val p = ip.split(".")
        if (p.size != 4) return false
        val a = p[0].toIntOrNull() ?: return false
        val b = p[1].toIntOrNull() ?: return false
        return a == 10 ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) ||
            (a == 169 && b == 254)
    }
}
