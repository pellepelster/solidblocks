package de.solidblocks.infra.test.host

import de.solidblocks.infra.test.TestContext
import java.net.InetSocketAddress
import java.net.Socket

fun hostTestContext(host: String, testId: String? = null) = HostTestContext(host, testId)

class HostTestContext(val host: String, testId: String? = null) : TestContext(testId) {
    fun portIsOpen(port: Int, timeoutMs: Int = 5000) = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            true
        }
    } catch (e: Exception) {
        false
    }
}
