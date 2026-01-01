package de.solidblocks.ssh.test

import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.util.*
import kotlin.io.path.writeText
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

class SSHClientTest {

  val sshServer =
      GenericContainer(
              ImageFromDockerfile()
                  .withFileFromClasspath("Dockerfile", "Dockerfile")
                  .withFileFromClasspath("authorized_keys", "test_ed25519.key.pub"),
          )
          .also {
            it.addExposedPort(22)
            it.start()
          }

  val ed25519Key = SSHClientTest::class.java.getResource("/test_ed25519.key").readText()
  val key = SSHKeyUtils.tryLoadKey(ed25519Key)
  val client = SSHClient(sshServer.host, key, port = sshServer.getMappedPort(22))

  @Test
  fun testDownload() {
    val file = client.download("/root/.ssh/authorized_keys")
    val ed25519KeyPublic = SSHClientTest::class.java.getResource("/test_ed25519.key.pub").readText()
    file shouldBe ed25519KeyPublic.toByteArray()
  }

  @Test
  fun testUploadDownload() {
    val random = UUID.randomUUID().toString()

    val file = Files.createTempFile("upload", ".random")
    file.writeText(random)

    client.upload(file, "/tmp/$random")
    client.download("/tmp/$random") shouldBe random.toByteArray()
  }

  @Test
  fun testInvalidDownload() {
    client.download("invalid") shouldBe ByteArray(0)
  }

  @Test
  fun testCommandSuccess() {
    assertSoftly(client.command("whoami")) {
      it.exitCode shouldBe 0
      it.stdOut shouldBe "root\n"
      it.stdErr shouldBe ""
    }
  }

  @Test
  fun testCommandFailure() {
    assertSoftly(client.command("invalid")) {
      it.exitCode shouldBe 127
      it.stdOut shouldBe ""
      it.stdErr shouldContain "not found"
    }
  }
}
