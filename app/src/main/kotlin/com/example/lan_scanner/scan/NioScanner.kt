package com.example.lan_scanner.scan

import com.example.lan_scanner.data.DeviceInfo
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

/**
 * 非阻塞 TCP 端口扫描器。
 *
 * 对齐 Flutter 原版 `Socket.connect` 的事件驱动模型：由单个 Selector 管理
 * 最多 (hostConcurrency × ports) 个连接，而不是为每个连接占用一个线程。
 * 因此 /24 网段的 254 × 13 次探测能在数个 timeout 周期内完成，不会排队等线程池。
 *
 * 存活判定严格对齐原版 `_probeHost`：
 *  - connect 成功                 -> OPEN（端口开放，主机在线）
 *  - "Connection refused" 等      -> REFUSED（端口关闭，主机在线）
 *  - 其它（超时 / 不可达 / 无路由）-> UNKNOWN（离线）
 *
 * 特别注意：**不能**把任意 `ConnectException` 当作在线。热点 / 蜂窝网段里
 * "Network is unreachable"、"No route to host" 会成片出现，那正是旧版
 * 把一堆根本不在线的地址列进结果的根因。
 */
class NioScanner(
    private val hostConcurrency: Int = 80,
    private val timeoutMs: Int = 800,
) {

    /** 主机探测完成回调；device 为 null 表示该主机离线，不进入列表。 */
    fun scan(
        ips: List<String>,
        ports: List<Int>,
        cancelled: () -> Boolean = { false },
        onHostDone: (ip: String, device: DeviceInfo?) -> Unit,
    ) {
        if (ips.isEmpty() || ports.isEmpty()) return

        val selector = Selector.open()
        val waiting = ArrayDeque(ips)
        val active = LinkedHashMap<String, HostState>()

        fun startHost(ip: String) {
            val state = HostState(ip, ports.size)
            active[ip] = state
            for (port in ports) {
                val channel = try {
                    SocketChannel.open().apply { configureBlocking(false) }
                } catch (e: Exception) {
                    state.record(port, PortState.UNKNOWN)
                    continue
                }
                try {
                    val key = channel.register(
                        selector,
                        SelectionKey.OP_CONNECT,
                        Conn(ip, port, System.currentTimeMillis() + timeoutMs),
                    )
                    if (channel.connect(InetSocketAddress(ip, port))) {
                        // 本机等极快路径：立即连上
                        state.record(port, PortState.OPEN)
                        key.cancel()
                        channel.close()
                    }
                } catch (e: Exception) {
                    state.record(port, classify(e))
                    runCatching { channel.close() }
                }
            }
        }

        try {
            repeat(minOf(hostConcurrency, waiting.size)) {
                startHost(waiting.removeFirst())
            }

            while (active.isNotEmpty() && !cancelled()) {
                val now = System.currentTimeMillis()

                // 1) 统一超时：到点仍未就绪即视为离线
                for (key in selector.keys().toList()) {
                    if (!key.isValid) continue
                    val conn = key.attachment() as? Conn ?: continue
                    if (now >= conn.deadline) {
                        active[conn.ip]?.record(conn.port, PortState.UNKNOWN)
                        key.cancel()
                        runCatching { key.channel().close() }
                    }
                }

                // 2) 处理就绪连接
                val ready = selector.select(SELECT_TICK_MS)
                if (ready > 0) {
                    val iterator = selector.selectedKeys().iterator()
                    while (iterator.hasNext()) {
                        val key = iterator.next()
                        iterator.remove()
                        if (!key.isValid) continue
                        val conn = key.attachment() as? Conn ?: continue
                        val channel = key.channel() as? SocketChannel
                        val result = if (channel == null) {
                            PortState.UNKNOWN
                        } else {
                            try {
                                if (channel.finishConnect()) PortState.OPEN else PortState.UNKNOWN
                            } catch (e: Exception) {
                                classify(e)
                            }
                        }
                        active[conn.ip]?.record(conn.port, result)
                        key.cancel()
                        if (channel != null) runCatching { channel.close() }
                    }
                }

                // 3) 回收已完成的主机并补位（与原版 80 并发信号量一致）
                val finished = active.values.filter { it.remaining <= 0 }
                for (state in finished) {
                    active.remove(state.ip)
                    onHostDone(state.ip, state.toDevice())
                    if (waiting.isNotEmpty() && !cancelled()) {
                        startHost(waiting.removeFirst())
                    }
                }
            }
        } finally {
            for (key in selector.keys()) {
                key.cancel()
                runCatching { key.channel().close() }
            }
            selector.close()
        }
    }

    /** 只认明确的 RST 语义，其余一律离线。 */
    private fun classify(e: Exception): PortState {
        val message = (e.message ?: "").lowercase()
        val refused = message.contains("refused") ||
            message.contains("econnrefused") ||
            message.contains("errno = 111") ||
            message.contains("errno = 61")
        return if (refused) PortState.REFUSED else PortState.UNKNOWN
    }

    private data class Conn(val ip: String, val port: Int, val deadline: Long)

    private class HostState(val ip: String, count: Int) {
        var remaining = count
            private set
        private val openPorts = mutableListOf<Int>()
        private var alive = false

        fun record(port: Int, state: PortState) {
            when (state) {
                PortState.OPEN -> {
                    openPorts.add(port)
                    alive = true
                }
                PortState.REFUSED -> alive = true
                PortState.UNKNOWN -> Unit
            }
            remaining--
        }

        fun toDevice(): DeviceInfo? =
            if (alive) DeviceInfo(ip = ip, openPorts = openPorts.sorted()) else null
    }

    private enum class PortState { OPEN, REFUSED, UNKNOWN }

    companion object {
        private const val SELECT_TICK_MS = 40L
    }
}
