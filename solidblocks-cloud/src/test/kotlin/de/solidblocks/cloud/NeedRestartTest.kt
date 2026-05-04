package de.solidblocks.cloud

import de.solidblocks.cloud.status.parseNeedRestart
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NeedRestartTest {

    @Test
    fun testParse() {
        val output = """
            NEEDRESTART-KCUR: 6.1.0-41-amd64
            NEEDRESTART-KEXP: 6.1.0-45-amd64
            NEEDRESTART-KSTA: 3
            NEEDRESTART-SVC: atd.service
            NEEDRESTART-SVC: cron.service
            NEEDRESTART-SVC: dbus.service
            NEEDRESTART-SVC: systemd-journald.service
            NEEDRESTART-SVC: systemd-logind.service
            NEEDRESTART-SVC: systemd-manager
            NEEDRESTART-SVC: systemd-timesyncd.service
            NEEDRESTART-SVC: systemd-udevd.service
            NEEDRESTART-SVC: unattended-upgrades.service
            NEEDRESTART-SESS: root @ user manager service
        """.trimIndent()

        assertSoftly(output.parseNeedRestart()) {
            it.expectedKernel shouldBe "6.1.0-45-amd64"
            it.currentKernel shouldBe "6.1.0-41-amd64"
        }
    }
}
