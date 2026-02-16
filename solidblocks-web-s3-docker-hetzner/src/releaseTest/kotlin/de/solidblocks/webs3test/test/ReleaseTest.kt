package de.solidblocks.webs3test.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldBeSuccess
import java.nio.file.Path
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class ReleaseTest {

  @Test
  fun testSnippets(context: SolidblocksTestContext) {
    val terraform = context.terraform(Path.of("").resolve("snippets/web-s3-docker-kitchen-sink"))

    terraform.addEnvironmentVariable("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN"))

    terraform.init()
    terraform.apply()

    val output = terraform.output()

    val ipv4Address = output.getString("ipv4_address")
    val privateKey = output.getString("private_key")

    val host = context.host(ipv4Address)
    await atMost (30.seconds.toJavaDuration()) until { host.portIsOpen(22) }

    val cloudInit = context.cloudInit(ipv4Address, privateKey)
    cloudInit.printOutputLogOnTestFailure()

    await.atMost(1, TimeUnit.MINUTES).withPollInterval(ofSeconds(5)) until
        {
          cloudInit.isFinished()
        }

    cloudInit.result().shouldBeSuccess()
  }
}
