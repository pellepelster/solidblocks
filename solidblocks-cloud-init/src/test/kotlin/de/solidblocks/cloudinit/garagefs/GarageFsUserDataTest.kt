package de.solidblocks.cloudinit.garagefs

import de.solidblocks.cloudinit.BackupConfiguration
import de.solidblocks.cloudinit.LocalBackupTarget
import de.solidblocks.cloudinit.waitForSuccessfulProvisioning
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTest::class)
class GarageFsUserDataTest {
    fun getRandomString(length: Int): String {
        val allowedChars = ('a'..'f') + ('0'..'9')
        return (1..length).map { allowedChars.random() }.joinToString("")
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
    fun testIntegration(context: SolidblocksTestContext) {
        val hetznerContext = context.hetzner(System.getenv("HCLOUD_TOKEN").toString())

        val dataVolume = hetznerContext.createVolume("${context.testId}-data")
        val backupVolume = hetznerContext.createVolume("${context.testId}-backup")
        val sshKey = hetznerContext.createSSHKey()

        val rpcSecret = getRandomString(64)
        val adminToken = getRandomString(64)
        val metricsToken = getRandomString(64)
        val userData =
            GarageFsUserData(
                "service1",
                dataVolume.linuxDevice,
                BackupConfiguration("some-password", LocalBackupTarget(backupVolume.linuxDevice)),
                "yolo.de",
                rpcSecret,
                adminToken,
                metricsToken,
                emptyList(),
            )

        val serverContext =
            hetznerContext.createServer(
                userData.render(),
                sshKey,
                volumes = listOf(dataVolume.id, backupVolume.id),
            )

        serverContext.waitForSuccessfulProvisioning()
    }

    @Test
    fun testRender() {
        println(
            GarageFsUserData(
                "service1",
                "/dev/sdb",
                BackupConfiguration("some-password", LocalBackupTarget("/dev/sdc")),
                "yolo.de",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                emptyList(),
            )
                .render(),
        )
    }
}
