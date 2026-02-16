package de.solidblocks.cloud.health

import de.solidblocks.cloud.api.health.HealthCheck
import de.solidblocks.cloud.api.health.HealthStatus
import de.solidblocks.ssh.SSHClient
import java.security.KeyPair

class SSHHealthCheck(val key: KeyPair) : HealthCheck {

  override fun check(address: String, port: Int) =
      try {
        SSHClient(address, key, port = port).use {
          if (it.command("whoami").exitCode == 0) {
            HealthStatus.healthy
          } else {
            HealthStatus.unhealthy
          }
        }
      } catch (e: Exception) {
        HealthStatus.unhealthy
      }
}
