package de.solidblocks.systemd

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SystemDConfigTest {

  @Test
  fun testService() {
    val config =
        SystemDService(
            "foo-bar",
            Unit("foo-bar"),
            Service(
                listOf("/usr/local/bin/service1", "arg1"),
                environment = mapOf("foo" to "bar"),
                limitNOFILE = 12,
            ),
            Install(),
        )

    config.fullUnitName() shouldBe "foo-bar.service"

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

  @Test
  fun testTimer() {
    val config =
        SystemDTimer(
            "foo-bar",
            Unit("foo-bar"),
            Timer(
                Daily(),
                unit = "unit-name.service",
            ),
            Install(),
        )
    config.fullUnitName() shouldBe "foo-bar.timer"

    config.render() shouldBe
        """
        [Unit]
        Description=foo-bar
        After=network-online.target
        Requires=network-online.target

        [Timer]
        OnCalendar=daily
        Unit=unit-name.service

        [Install]
        WantedBy=multi-user.target

        """
            .trimIndent()
  }
}
