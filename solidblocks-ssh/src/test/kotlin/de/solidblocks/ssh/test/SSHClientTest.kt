package de.solidblocks.ssh.test

import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

class SSHClientTest {

    val sshServer = GenericContainer(
        ImageFromDockerfile()
            .withFileFromClasspath("Dockerfile", "Dockerfile")
            .withFileFromClasspath("test_ed25519.key.pub", "test_ed25519.key.pub")
            .withFileFromClasspath("sshd_config", "sshd_config")
    ).also {
        it.addExposedPort(22)
        it.start()
    }

    @Test
    fun testSSHCommand() {

        val ed25519Key = SSHClientTest::class.java.getResource("/test_ed25519.key").readText()
        val key = SSHKeyUtils.tryLoadKey(ed25519Key)

        val client = SSHClient(sshServer.host, key, port = sshServer.getMappedPort(22))
        client.sshCommand("whoami") shouldBe "root\n"
    }

}