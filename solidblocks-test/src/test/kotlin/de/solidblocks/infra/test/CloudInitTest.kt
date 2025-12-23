package de.solidblocks.infra.test

import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CloudInitTest {

    @Test
    fun testCloudInitSuccess(context: SolidblocksTestContext) {
        val terraform1 = CloudInitTest::class.java.getResource("/terraformCloudInitTestBed1").path

        val terraform = context.terraform(terraform1)
        terraform.init()
        terraform.apply()
        val output = terraform.output()

        val ipv4Address = output.getString("ipv4_address")

        val host = context.host(ipv4Address)

        await until {
            host.isPortOpen(22)
        }

        val ssh = context.ssh(ipv4Address, output.getString("private_key_openssh_ed25519"))
        ssh.command("whoami").stdOut shouldBe "root"
    }
}
