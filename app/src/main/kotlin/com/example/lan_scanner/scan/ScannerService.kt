package com.example.lan_scanner.scan

import com.example.lan_scanner.data.PortCatalog
import com.example.lan_scanner.data.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

/**
 * 局域网扫描：CIDR 展开 -> 非阻塞 TCP 端口探测 -> 存活判定 -> 主机名解析。
 *
 * 行为与 Flutter 原版一致（原版 `_startMdns` 从不写入结果，是死代码，
 * 因此本版只做 TCP 扫描，不补充任何「仅组播发现」的设备）：
 *  - connect 成功 / Connection refused -> 主机在线
 *  - 超时 / 网络不可达 / 无路由      -> 离线，不进列表
 *  - 在线但无开放端口                -> 仍列出，但不显示端口标签
 */
class ScannerService {

    companion object {
        const val HOST_CONCURRENCY = 80
        const val TIMEOUT_MS = 800
        private const val REVERSE_DNS_TIMEOUT_MS = 1000L
    }

    fun scan(cidr: String, ports: List<Int>): Flow<ScanProgress> = channelFlow {
        val ips = withContext(Dispatchers.IO) { CidrParser.parse(cidr) }
        val total = ips.size
        val effectivePorts = ports.ifEmpty { PortCatalog.defaults }
        val scanned = AtomicInteger(0)

        // 立刻给出 total，消除「正在初始化扫描…」的空窗期
        send(ScanProgress(total, 0, null))

        // 反向 DNS 放到 IO 协程并发执行，不阻塞 NIO selector 循环
        val resolutions = mutableListOf<Job>()

        NioScanner(HOST_CONCURRENCY, TIMEOUT_MS).scan(
            ips = ips,
            ports = effectivePorts,
            cancelled = { !isActive },
        ) { _, device ->
            val done = scanned.incrementAndGet()
            if (device == null) {
                trySend(ScanProgress(total, done, null))
            } else {
                resolutions += launch(Dispatchers.IO) {
                    val hostname = resolveHostname(device.ip)
                    trySend(ScanProgress(total, done, device.copy(hostname = hostname)))
                }
            }
        }

        resolutions.joinAll()
    }.flowOn(Dispatchers.IO)

    /** 反向 DNS 解析主机名，超时兜底；Android 上反查常常很慢，故限制时长。 */
    private suspend fun resolveHostname(ip: String): String =
        withTimeoutOrNull(REVERSE_DNS_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                try {
                    val host = InetAddress.getByName(ip).hostName
                    if (!host.isNullOrBlank() && host != ip) host else ""
                } catch (e: Exception) {
                    ""
                }
            }
        } ?: ""
}
