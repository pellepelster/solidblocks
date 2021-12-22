package de.solidblocks.service.vault

import de.solidblocks.ingress.config.AutomaticHttps
import de.solidblocks.ingress.config.CaddyConfig
import de.solidblocks.ingress.config.Http
import de.solidblocks.ingress.config.Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConfigTest {

    @Test
    fun testConfig() {

        assertThat(
            CaddyConfig.serialize(
                CaddyConfig(
                    apps = mapOf(
                        "http" to
                            Http(
                                servers = mapOf(
                                    "server1" to Server(
                                        automaticHttps = AutomaticHttps(disable = false)
                                    )
                                )
                            )
                    )
                )
            )
        ).isEqualTo("{\"apps\":{\"http\":{\"servers\":{\"server1\":{\"listen\":[\":80\",\":443\"],\"automatic_https\":{\"disable\":false}}}}}}")
    }
}
