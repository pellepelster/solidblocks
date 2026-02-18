package de.solidblocks.cloud.healthcheck

import de.solidblocks.cloud.api.health.HealthStatus
import de.solidblocks.cloud.health.SSHHealthCheck
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.ssh.SSHKeyUtils
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

@ExtendWith(SolidblocksTest::class)
class SSHClientTest {

  val sshServer =
      GenericContainer(
              ImageFromDockerfile()
                  .withFileFromClasspath("Dockerfile", "ssh/Dockerfile")
                  .withFileFromClasspath("authorized_keys", "test_ed25519.key.pub"),
      )
          .also {
            it.addExposedPort(22)
            it.start()
          }

  val ed25519Key = SSHClientTest::class.java.getResource("/test_ed25519.key").readText()
  val key = SSHKeyUtils.loadKey(ed25519Key)

  @Test
  fun testHealthy() {
    val healthCheck = SSHHealthCheck(key)
    healthCheck.check(sshServer.host, sshServer.getMappedPort(22)) shouldBe HealthStatus.healthy
  }

  @Test
  fun testUnhealthy() {
    val healthCheck = SSHHealthCheck(key)
    healthCheck.check(sshServer.host, 2222) shouldBe HealthStatus.unhealthy
  }
}
