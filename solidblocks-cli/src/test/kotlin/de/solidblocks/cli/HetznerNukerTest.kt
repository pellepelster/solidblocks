package de.solidblocks.cli

import de.solidblocks.cli.hetzner.nuke.HetznerNuker
import de.solidblocks.hetzner.cloud.HetznerApi
import org.junit.jupiter.api.Test

class HetznerNukerTest {

    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
    val api = HetznerApi(hcloudToken)

    @Test
    fun testNuke() {
        HetznerNuker(hcloudToken).nuke()
    }
}
