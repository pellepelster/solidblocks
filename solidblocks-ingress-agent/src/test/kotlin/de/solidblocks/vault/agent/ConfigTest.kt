package de.solidblocks.vault.agent

import de.solidblocks.ingress.agent.config.*
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConfigTest {

    @Test
    fun testConfigDefaultConfig() {

        assertThat(
            CaddyConfig.serialize(
                CaddyConfig()
            )
        ).isEqualTo("{\"apps\":{},\"admin\":{\"disabled\":true}}")
    }

    @Test
    fun testHostBasedReverseProxy() {

        val expectedConfig = """
            {
              "admin": {
                "disabled": true
              },
              "apps": {
                "http": {
                  "servers": {
                    "server1": {
                      "automatic_https": {
                        "disable": true
                      },
                      "listen": [
                        ":80",
                        ":443"
                      ],
                      "routes": [
                        {
                          "match": [
                            {
                              "host": [
                                "localhost"
                              ]
                            }
                          ],
                          "handle": [
                            {
                              "handler": "reverse_proxy",
                              "transport": {
                                "protocol": "http",
                                "compression": true,
                                "tls": {
                                  "root_ca_pem_files": [
                                    "/tmp/ca.crt"
                                  ]
                                }
                              },
                              "upstreams": [
                                {
                                  "dial": "localhost:49298"
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
              }
            }            
        """.trimIndent()

        assertThatJson(
            CaddyConfig.serialize(
                CaddyConfig(
                    apps = mapOf(
                        "http" to
                            Http(
                                servers = mapOf(
                                    "server1" to Server(
                                        automaticHttps = AutomaticHttps(disable = true),
                                        routes = listOf(
                                            Route(
                                                match = listOf(
                                                    Match(host = listOf("localhost"))
                                                ),
                                                handle = listOf(
                                                    Handler(
                                                        transport = Transport(
                                                            tls = Tls(
                                                                rootCAPemFiles = listOf(
                                                                    "/tmp/ca.crt"
                                                                )
                                                            )
                                                        ),
                                                        upstreams = listOf(Upstream("localhost:49298"))
                                                    )
                                                )
                                            )
                                        )

                                    )
                                )
                            )
                    )
                )
            )
        ).`when`(IGNORING_EXTRA_FIELDS).isEqualTo(expectedConfig)
    }
}
