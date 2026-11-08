package com.example.lan_scanner.scan

import com.example.lan_scanner.data.DeviceInfo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket

class NioScannerTest {

    @Test
    fun openPortIsReported() {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        try {
            val port = server.localPort
            var device: DeviceInfo? = null
            NioScanner(hostConcurrency = 4, timeoutMs = 300).scan(
                ips = listOf("127.0.0.1"),
                ports = listOf(port),
                onHostDone = { _, d -> device = d },
            )
            assertNotNull("已监听端口应判定在线", device)
            assertTrue("应列出开放端口 $port", device!!.openPorts.contains(port))
        } finally {
            server.close()
        }
    }

    @Test
    fun refusedPortCountsAliveButHasNoOpenPorts() {
        // 取一个刚释放的本地端口：连接会收到 RST -> 在线但无开放端口
        val closedPort = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
            .use { it.localPort }
        var device: DeviceInfo? = null
        NioScanner(hostConcurrency = 4, timeoutMs = 300).scan(
            ips = listOf("127.0.0.1"),
            ports = listOf(closedPort),
            onHostDone = { _, d -> device = d },
        )
        assertNotNull("RST 应表示主机在线", device)
        assertTrue("不应列出开放端口", device!!.openPorts.isEmpty())
    }

    @Test
    fun unreachableHostIsNotReported() {
        // 192.0.2.1 属 TEST-NET-1：通常返回 Network is unreachable / 超时。
        // 关键回归：不能像旧版那样把任意 ConnectException 当成在线。
        var device: DeviceInfo? = null
        NioScanner(hostConcurrency = 4, timeoutMs = 250).scan(
            ips = listOf("192.0.2.1"),
            ports = listOf(80),
            onHostDone = { _, d -> device = d },
        )
        assertNull("不可达主机必须判为离线", device)
    }

    @Test
    fun allHostsAreReportedExactlyOnce() {
        val seen = mutableListOf<String>()
        NioScanner(hostConcurrency = 2, timeoutMs = 200).scan(
            ips = listOf("192.0.2.1", "192.0.2.2", "192.0.2.3"),
            ports = listOf(80),
            onHostDone = { ip, _ -> seen.add(ip) },
        )
        assertTrue("每个 IP 都应回调一次，实际 $seen", seen.size == 3)
        assertTrue("回调 IP 应完整，实际 $seen",
            seen.toSet() == setOf("192.0.2.1", "192.0.2.2", "192.0.2.3"))
    }
}
