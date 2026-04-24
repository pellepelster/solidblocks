package de.solidblocks.cloudinit

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.hetzner.HetznerServerTestContext
import de.solidblocks.shell.*
import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files
import java.time.Duration.ofSeconds
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText

@ExtendWith(SolidblocksTest::class)
class CloudInitUserDataTest {
    @Test
    fun testShellScriptIntegration(testContext: SolidblocksTestContext) {
        val hetznerTestContext = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val volume = hetznerTestContext.createVolume()

        val randomContent = UUID.randomUUID().toString()

        val shellScript = ShellScript()
        shellScript.addLibrary(StorageLibrary)
        shellScript.addCommand(StorageLibrary.Mount(volume.linuxDevice, "/storage/data"))
        shellScript.addCommand(WriteFile(randomContent.toByteArray(), "/tmp/foo-bar"))

        val serverTestContext =
            hetznerTestContext.createServer(
                shellScript.toCloudInit(RSA_KEY_PEM.privateKey, ED25519_PRIVATE_KEY).render(),
                volumes = listOf(volume.id),
            )
        serverTestContext.waitForSuccessfulProvisioning()
        var sshContext = serverTestContext.ssh()

        sshContext.fileExists("/tmp/foo-bar") shouldBe true
        sshContext.filePermissions("/tmp/foo-bar") shouldBe "-rw-------"
        sshContext.download("/tmp/foo-bar") shouldBe randomContent.toByteArray()

        val randomUUID = UUID.randomUUID().toString()

        val randomFile = Files.createTempFile("random", ".txt").also { it.writeText(randomUUID) }
        sshContext.upload(randomFile.toAbsolutePath(), "/storage/data/$randomUUID.txt")
        sshContext.download("/storage/data/$randomUUID.txt") shouldBe randomUUID.toByteArray()
        hetznerTestContext.destroyServer(serverTestContext)

        val recreatedServerTestContext =
            hetznerTestContext.createServer(
                shellScript.toCloudInit(RSA_KEY_PEM.privateKey, ED25519_PRIVATE_KEY).render(),
                volumes = listOf(volume.id),
            )
        recreatedServerTestContext.waitForSuccessfulProvisioning()

        sshContext = recreatedServerTestContext.ssh()
        sshContext.download("/storage/data/$randomUUID.txt") shouldBe randomUUID.toByteArray()
    }

    @Test
    fun testCloudInitIntegration(testContext: SolidblocksTestContext) {
        val hetznerTestContext = testContext.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val volume = hetznerTestContext.createVolume()

        val randomContent = UUID.randomUUID().toString()

        val shellScript = ShellScript()
        shellScript.addLibrary(StorageLibrary)
        shellScript.addCommand(StorageLibrary.Mount(volume.linuxDevice, "/storage/data"))
        shellScript.addCommand(WriteFile(randomContent.toByteArray(), "/tmp/foo-bar"))

        val serverTestContext =
            hetznerTestContext.createServer(
                shellScript.toCloudInit(RSA_KEY_PEM.privateKey, ED25519_PRIVATE_KEY).render(),
                volumes = listOf(volume.id),
            )
        serverTestContext.waitForSuccessfulProvisioning()
        var sshContext = serverTestContext.ssh()

        /**
         * ensure the public keys for the host keys could be derived without an issue
         */
        val outputLog = serverTestContext.cloudInit().outputLog()
        outputLog shouldContain "ssh-keygen -yf /etc/ssh/ssh_host_rsa_key"
        outputLog shouldContain "o=ssh-rsa"
        outputLog shouldContain "ssh-keygen -yf /etc/ssh/ssh_host_ed25519_key"
        outputLog shouldContain "o=ssh-ed25519"

        SSHClient(sshContext.host, SSHKeyUtils.loadKey(hetznerTestContext.ed25519KeyPem.privateKey), ED25519_KEY.public).command("whoami").stdOut.trim() shouldBe "root"
        SSHClient(sshContext.host, SSHKeyUtils.loadKey(hetznerTestContext.rsaKeyPem.privateKey), RSA_KEY.public).command("whoami").stdOut.trim() shouldBe "root"

        sshContext.fileExists("/tmp/foo-bar") shouldBe true
        sshContext.filePermissions("/tmp/foo-bar") shouldBe "-rw-------"
        sshContext.download("/tmp/foo-bar") shouldBe randomContent.toByteArray()

        val randomUUID = UUID.randomUUID().toString()

        val randomFile = Files.createTempFile("random", ".txt").also { it.writeText(randomUUID) }
        sshContext.upload(randomFile.toAbsolutePath(), "/storage/data/$randomUUID.txt")
        sshContext.download("/storage/data/$randomUUID.txt") shouldBe randomUUID.toByteArray()
        hetznerTestContext.destroyServer(serverTestContext)

        val recreatedServerTestContext =
            hetznerTestContext.createServer(
                shellScript.toCloudInit(RSA_KEY_PEM.privateKey, ED25519_PRIVATE_KEY).render(),
                volumes = listOf(volume.id),
            )
        recreatedServerTestContext.waitForSuccessfulProvisioning()

        sshContext = recreatedServerTestContext.ssh()
        sshContext.download("/storage/data/$randomUUID.txt") shouldBe randomUUID.toByteArray()
    }

    @Test
    fun testPlaceHolders() {
        val template =
            ShellScript::class.java.getResource("/blcks-cloud-init-bootstrap.sh.template").readText()
        template shouldContain "__CLOUD_INIT_VARIABLES__"
        template shouldContain "__CLOUD_INIT_SCRIPT__"
    }

    @Test
    fun testRender() {
        val cloudInit = CloudInit()

        cloudInit.privateKeyRsa = RSA_KEY_PEM.privateKey
        cloudInit.privateKeyEd25519 = ED25519_PRIVATE_KEY

        println(cloudInit.render())
    }
}

fun HetznerServerTestContext.waitForSuccessfulProvisioning() {
    val hostTestContext = this.host()

    await().atMost(1, TimeUnit.MINUTES).pollInterval(ofSeconds(5)).until {
        hostTestContext.portIsOpen(22)
    }

    val cloudInitContext = this.cloudInit()
    cloudInitContext.printOutputLogOnTestFailure()

    await().atMost(5, TimeUnit.MINUTES).pollInterval(ofSeconds(10)).until {
        cloudInitContext.isFinished()
    }

    cloudInitContext.result()?.hasErrors shouldBe false
}
