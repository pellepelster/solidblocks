package de.solidblocks.systemd

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SystemDConfigTest {

  @Test
  fun testRender() {
    val config =
        SystemdConfig(
            Unit("foo-bar"),
            Service(
                listOf("/usr/local/bin/service1", "arg1"),
                environment = mapOf("foo" to "bar"),
                limitNOFILE = 12,
            ),
        )

    config.render() shouldBe
        """
        [Unit]
        Description=foo-bar
        After=network-online.target
        Requires=network-online.target

        [Service]
        Environment="foo=bar"
        ExecStart=/usr/local/bin/service1 arg1
        LimitNOFILE=12

        [Install]
        WantedBy=multi-user.target

        """
            .trimIndent()
  }
}
