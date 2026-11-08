package com.example.lan_scanner.scan

import android.content.Context
import com.example.lan_scanner.data.DeviceInfo
import com.example.lan_scanner.data.PortCatalog
import com.example.lan_scanner.data.ScanProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 局域网扫描服务：CIDR 展开 -> 并发 TCP 端口探测 -> 存活判定 -> 主机名解析。
 *
 * 判定规则与 Flutter 原版一致：
 *  - connect 成功            -> 端口开放，主机在线
 *  - Connection refused(RST) -> 端口关闭，但主机在线
 *  - 超时 / 不可达           -> 视为离线
 *
 * 使用 channelFlow：builder 返回后自动关闭通道，collect 能正常收到完成事件。
 */
class ScannerService {

    companion object {
        const val HOST_CONCURRENCY = 48
        const val MAX_SOCKETS = 180
        const val TIMEOUT_MS = 800
        const val MDNS_DURATION_MS = 6000L
    }

    fun scan(cidr: String, ports: List<Int>, context: Context): Flow<ScanProgress> = channelFlow {
        val appContext = context.applicationContext
        val effectivePorts = ports.ifEmpty { PortCatalog.defaults }
        val producer: ProducerScope<ScanProgress> = this

        // mDNS 在独立 scope 中并行收集，不阻塞 channelFlow 的生命周期
        val mdnsResults = ConcurrentHashMap<String, String>()
        val mdnsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val mdnsJob = mdnsScope.launch {
            MdnsDiscovery.discoverInto(appContext, mdnsResults, MDNS_DURATION_MS)
        }

        try {
            val ips = withContext(Dispatchers.IO) { CidrParser.parse(cidr) }
            val total = ips.size
            val hostSem = Semaphore(HOST_CONCURRENCY)
            val socketSem = Semaphore(MAX_SOCKETS)
            val scanned = AtomicInteger(0)
            val discovered = ConcurrentHashMap.newKeySet<String>()

            val jobs = ips.map { ip ->
                launch(Dispatchers.IO) {
                    hostSem.withPermit {
                        if (!isActive) return@withPermit
                        val device = probeHost(ip, effectivePorts, socketSem, mdnsResults)
                        if (device != null) discovered.add(ip)
                        val done = scanned.incrementAndGet()
                        producer.send(ScanProgress(total, done, device))
                    }
                }
            }
            jobs.joinAll()

            // 小网段扫描可能瞬间完成，给 mDNS 一个最多 1.2s 的收尾窗口收集组播应答
            withTimeoutOrNull(1200) { mdnsJob.join() }

            // 纯 mDNS 发现（TCP 未探到）的设备补充进来；不增加 scanned，避免进度越界
            val done = scanned.get()
            mdnsResults.forEach { (ip, name) ->
                if (ip !in discovered && ip.isNotBlank()) {
                    producer.send(
                        ScanProgress(total, done, DeviceInfo(ip = ip, hostname = name))
                    )
                }
            }
        } finally {
            mdnsScope.cancel()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun probeHost(
        ip: String,
        ports: List<Int>,
        socketSem: Semaphore,
        mdnsResults: Map<String, String>,
    ): DeviceInfo? = coroutineScope {
        val results = ports.map { port ->
            async(Dispatchers.IO) {
                socketSem.withPermit { port to probePort(ip, port) }
            }
        }.awaitAll()

        val alive = results.any { (_, state) -> state != PortState.UNKNOWN }
        if (!alive) return@coroutineScope null

        val openPorts = results
            .filter { (_, state) -> state == PortState.OPEN }
            .map { (port, _) -> port }
            .sorted()

        val reverse = resolveHostname(ip)
        val hostname = reverse.ifBlank { mdnsResults[ip].orEmpty() }

        DeviceInfo(ip = ip, hostname = hostname, openPorts = openPorts)
    }

    private fun probePort(ip: String, port: Int): PortState {
        val socket = Socket()
        return try {
            socket.connect(InetSocketAddress(ip, port), TIMEOUT_MS)
            PortState.OPEN
        } catch (e: IOException) {
            val message = (e.message ?: "").lowercase()
            val refused = e is ConnectException ||
                message.contains("refused") ||
                message.contains("econnrefused") ||
                message.contains("errno = 111") ||
                message.contains("errno = 61")
            if (refused) PortState.REFUSED else PortState.UNKNOWN
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /** 反向 DNS 解析主机名，超时兜底；Android 上反查常常很慢，故限制时长。 */
    private suspend fun resolveHostname(ip: String): String = withTimeoutOrNull(1000) {
        withContext(Dispatchers.IO) {
            try {
                val host = InetAddress.getByName(ip).hostName
                if (!host.isNullOrBlank() && host != ip) host else ""
            } catch (e: Exception) {
                ""
            }
        }
    } ?: ""

    private enum class PortState { OPEN, REFUSED, UNKNOWN }
}
