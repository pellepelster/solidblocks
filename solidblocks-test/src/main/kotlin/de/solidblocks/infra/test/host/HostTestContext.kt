package de.solidblocks.infra.test.host

import java.net.InetSocketAddress
import java.net.Socket

fun hostTestContext(host: String) = HostTestContext(host)

class HostTestContext(
    val host: String,
) {

  fun portIsOpen(port: Int, timeoutMs: Int = 5000) =
      try {
        Socket().use { socket ->
          socket.connect(InetSocketAddress(host, port), timeoutMs)
          true
        }
      } catch (e: Exception) {
        false
      }
}
