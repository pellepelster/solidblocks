package de.solidblocks.cli

import de.solidblocks.cli.hetzner.nuke.HetznerNuker
import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class HetznerNukerTest {

    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
    val api = HetznerApi(hcloudToken)

    @Test
    fun testNuke(context: SolidblocksTestContext) {
        val testbed = HetznerNukerTest::class.java.getResource("/nuke-testbed").path
        val terraform = context.terraform(testbed)
        terraform.init()
        terraform.apply()

        HetznerNuker(hcloudToken).nuke()
    }
}
