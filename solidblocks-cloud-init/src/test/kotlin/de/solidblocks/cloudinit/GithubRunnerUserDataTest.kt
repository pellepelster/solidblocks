package de.solidblocks.cloudinit

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.shell.toCloudInit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class GithubRunnerUserDataTest {
    @Test
    fun testIntegration(context: SolidblocksTestContext) {
        val hetznerContext = context.hetzner(System.getenv("HCLOUD_TOKEN").toString())
        val dataVolume = hetznerContext.createVolume("${context.testId}-data")

        val userData = GithubRunnerUserData(
            "runner1",
            "https://github.com/pellepelster/solidblocks",
            "AAEYLRLYTXITHVJNQLXFKCDJ5IDYS",
            listOf("label1", "label2"),
            listOf("zip"),
            false,
        )

        val serverContext =
            hetznerContext.createServer(
                userData.shellScript().toCloudInit(RSA_KEY_PEM.privateKey, ED25519_PRIVATE_KEY).render(),
                volumes = listOf(dataVolume.id),
            )

        serverContext.waitForSuccessfulProvisioning()
    }
}
