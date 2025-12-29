package de.solidblocks.ssh.test

import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemdConfig
import de.solidblocks.systemd.Unit
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * [Unit]
 * Description=Garage Data Store
 * After=network-online.target
 * Wants=network-online.target
 *
 * [Service]
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
        val config = SystemdConfig(Unit("foo-bar"), Service(listOf("/usr/local/bin/service1", "arg1")))

        config.render() shouldBe """
            [Unit]
            After=network-online.target
            Wants=network-online.target

            [Service]
            ExecStart=/usr/local/bin/service1 arg1

            [Install]
            WantedBy=multi-user.target
            
        """.trimIndent()
    }
}