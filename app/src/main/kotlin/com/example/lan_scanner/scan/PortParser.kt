package com.example.lan_scanner.scan

sealed interface PortParseResult {
    /** 输入为空，使用默认端口 */
    data object UseDefault : PortParseResult

    data class Ok(val ports: List<Int>) : PortParseResult

    /** 非空但格式不合法 */
    data object Invalid : PortParseResult
}

object PortParser {
    private val separator = Regex("[,\\s，、]+")

    fun parse(input: String): PortParseResult {
        val text = input.trim()
        if (text.isEmpty()) return PortParseResult.UseDefault
        return try {
            val ports = text.split(separator)
                .filter { it.isNotBlank() }
                .map { it.trim().toInt() }
                .filter { it in 1..65535 }
                .distinct()
            if (ports.isEmpty()) PortParseResult.Invalid else PortParseResult.Ok(ports)
        } catch (e: Exception) {
            PortParseResult.Invalid
        }
    }
}
