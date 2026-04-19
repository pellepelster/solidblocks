package de.solidblocks.ssh.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyFactory
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.ssh.toPem
import io.kotest.assertions.assertSoftly
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.nio.file.Files
import java.util.*
import kotlin.io.path.writeText

@ExtendWith(SolidblocksTest::class)
class SSHClientTest {

    val rsaHostKey = SSHKeyUtils.RSA.generate()
    val rsHostKeyPem = rsaHostKey.toPem()

    val ed25519HostKey = SSHKeyUtils.ED25519.generate()
    val ed25519HostKeyPem = ed25519HostKey.toPem()

    val sshServer =
        GenericContainer(
            ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "Dockerfile")
                .withFileFromClasspath("authorized_keys", "test_ed25519.key.pub")
                .withFileFromString("ssh_host_rsa_key", rsHostKeyPem.privateKey)
                .withFileFromString("ssh_host_ed25519_key", SSHKeyUtils.privateKeyToOpenSsh(ed25519HostKey.private)),
        )
            .also {
                it.addExposedPort(22)
                it.start()
            }

    val ed25519Key = SSHClientTest::class.java.getResource("/test_ed25519.key")!!.readText()
    val invalidEd25519Key = SSHClientTest::class.java.getResource("/test_ed25519_invalid.key")!!.readText()
    val key = SSHKeyUtils.loadKey(ed25519Key)
    val invalidKey = SSHKeyUtils.loadKey(invalidEd25519Key)
    val client = SSHClient(sshServer.host, key, null, port = sshServer.getMappedPort(22))

    @Test
    fun testDownload() {
        val file = client.download("/root/.ssh/authorized_keys")
        val ed25519KeyPublic =
            SSHClientTest::class.java.getResource("/test_ed25519.key.pub")!!.readText()
        file shouldBe ed25519KeyPublic.toByteArray()
    }

    @Test
    fun testInvalid() {
        val exception = assertThrows<Exception> {
            SSHClient(sshServer.host, invalidKey, null, port = sshServer.getMappedPort(22))
        }
        exception.message shouldContain "No more authentication methods available"
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
    fun testPortForwarding(testContext: SolidblocksTestContext) {
        runBlocking {
            client.portForward(22, 1234) { testContext.host("localhost").portIsOpen(it) shouldBe true }
            client.portForward(22) { testContext.host("localhost").portIsOpen(it) shouldBe true }
        }
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
    fun testCommandWithArguments() {
        assertSoftly(client.command("echo foo-bar")) {
            it.exitCode shouldBe 0
            it.stdOut shouldBe "foo-bar\n"
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
