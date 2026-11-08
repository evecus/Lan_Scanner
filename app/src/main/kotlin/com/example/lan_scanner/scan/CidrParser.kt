package com.example.lan_scanner.scan

/**
 * CIDR 网段解析：把 "192.168.1.0/24" 展开为待扫描的主机 IP 列表。
 * 与原 Flutter 版一致，排除网络地址与广播地址。
 */
object CidrParser {

    fun parse(input: String): List<String> {
        val text = input.trim()
        val slash = text.indexOf('/')
        require(slash > 0) { "网段格式错误，应形如 192.168.1.0/24" }

        val ipPart = text.substring(0, slash).trim()
        val prefix = text.substring(slash + 1).trim().toIntOrNull()
            ?: throw IllegalArgumentException("前缀长度无效，应为 0~32 的数字")
        require(prefix in 1..30) { "前缀长度建议在 1~30 之间，网段过大请拆分扫描" }

        val octets = ipPart.split(".")
        require(octets.size == 4) { "IP 地址格式错误" }
        val ipLong = octets.fold(0L) { acc, part ->
            val v = part.toIntOrNull() ?: throw IllegalArgumentException("IP 地址格式错误")
            require(v in 0..255) { "IP 地址格式错误" }
            (acc shl 8) or v.toLong()
        }

        val mask = (-1L shl (32 - prefix)) and 0xFFFFFFFFL
        val network = ipLong and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        val count = broadcast - network - 1
        require(count in 1..65534) { "网段过大（超过 65534 个地址），请使用 /24 或更小的网段" }

        val result = ArrayList<String>(count.toInt())
        var addr = network + 1
        while (addr < broadcast) {
            result.add(intToIp(addr))
            addr++
        }
        return result
    }

    private fun intToIp(v: Long): String = buildString {
        append((v shr 24) and 0xFF).append('.')
        append((v shr 16) and 0xFF).append('.')
        append((v shr 8) and 0xFF).append('.')
        append(v and 0xFF)
    }
}
