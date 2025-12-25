package de.solidblocks.infra.test

import de.solidblocks.infra.test.assertions.portShouldBeClosed
import de.solidblocks.infra.test.assertions.portShouldBeOpen
import de.solidblocks.infra.test.assertions.shouldBeSuccess
import de.solidblocks.infra.test.cloudinit.CloudInitResultWrapper
import de.solidblocks.infra.test.cloudinit.CloudInitTestContext
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CloudInitTest {

  @Test
  fun testCloudInitSerialization() {
    val statusJson =
        """
            {
             "v1": {
              "datasource": "DataSourceHetzner",
              "init": {
               "errors": [],
               "finished": 9.23,
               "recoverable_errors": {},
               "start": 8.37
              },
              "init-local": {
               "errors": [],
               "finished": 7.97,
               "recoverable_errors": {},
               "start": 6.76
              },
              "modules-config": {
               "errors": [],
               "finished": 9.5,
               "recoverable_errors": {},
               "start": 9.35
              },
              "modules-final": {
               "errors": [],
               "finished": 43.29,
               "recoverable_errors": {},
               "start": 9.55
              },
              "stage": null
             }
            }
        """
            .trimIndent()

    val cloudInitResult =
        CloudInitTestContext.json.decodeFromString<CloudInitResultWrapper>(statusJson)
    cloudInitResult.hasErrors shouldBe false
  }

  @Test
  fun testCloudInitSuccess(context: SolidblocksTestContext) {
    val terraform1 = CloudInitTest::class.java.getResource("/terraformCloudInitTestBed1").path

    context.cleanupAfterTestFailure(false)

    val terraform = context.terraform(terraform1)
    terraform.init()
    terraform.apply()
    val output = terraform.output()

    val ipv4Address = output.getString("ipv4_address")

    val host = context.host(ipv4Address)

    await atMost (30.seconds.toJavaDuration()) until { host.portIsOpen(22) }

    host portShouldBeOpen 22
    host portShouldBeClosed 23

    val cloudInit = context.cloudInit(ipv4Address, output.getString("private_key_openssh_ed25519"))

    await atMost (20.seconds.toJavaDuration()) until { cloudInit.isFinished() }

    cloudInit.result().shouldBeSuccess()

    cloudInit.outputLog() shouldNotBe null
    cloudInit.printOutputLog()
  }
}
