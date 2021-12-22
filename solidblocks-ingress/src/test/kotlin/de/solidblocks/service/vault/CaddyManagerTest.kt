package de.solidblocks.service.vault

import de.solidblocks.ingress.CaddyManager
import de.solidblocks.test.SolidblocksLocalEnv
import de.solidblocks.test.SolidblocksLocalEnvExtension
import de.solidblocks.test.TestUtils.initWorldReadableTempDir
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksLocalEnvExtension::class)
class CaddyManagerTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testProvisionIngressForService(solidblocksLocalEnv: SolidblocksLocalEnv) {

        val service = "ingress-${UUID.randomUUID()}"
        val tempDir = initWorldReadableTempDir(service)

        val reference = solidblocksLocalEnv.reference.toService(service)

        val caddyManager = CaddyManager(
            reference,
            tempDir,
        )

        assertThat(caddyManager.start()).isTrue

        val certificate = solidblocksLocalEnv.createCertificate(reference)
        certificate.toString()
        // caddyManager.test()

        assertThat(caddyManager.stop()).isTrue
    }
}
