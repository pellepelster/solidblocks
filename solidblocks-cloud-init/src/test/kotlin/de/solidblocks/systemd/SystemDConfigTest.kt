package de.solidblocks.systemd

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * [de.solidblocks.systemd.Unit]
 * Description=Garage Data Store
 * After=network-online.target
 * Wants=network-online.target
 *
 * [de.solidblocks.systemd.Service]
 * Environment='RUST_LOG=garage=info' 'RUST_BACKTRACE=1'
 * ExecStart=/usr/local/bin/garage server
 * StateDirectory=garage
 * #DynamicUser=true
 * #ProtectHome=true
 * #NoNewPrivileges=true
 * LimitNOFILE=42000
 *
 * [Install]
 * WantedBy=multi-user.target
 */

class SystemDConfigTest {

    @Test
    fun testRender() {
        val config =
            SystemdConfig(
                Unit("foo-bar"),
                Service(
                    listOf("/usr/local/bin/service1", "arg1"),
                    environment = mapOf("foo" to "bar"),
                    limitNOFILE = 12
                )
            )

        config.render() shouldBe """
            [Unit]
            Description=foo-bar
            After=network-online.target
            Wants=network-online.target

            [Service]
            Environment="foo=bar"
            ExecStart=/usr/local/bin/service1 arg1
            LimitNOFILE=12

            [Install]
            WantedBy=multi-user.target
            
        """.trimIndent()
    }
}