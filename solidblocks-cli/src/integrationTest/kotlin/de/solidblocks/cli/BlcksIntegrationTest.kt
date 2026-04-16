package de.solidblocks.cli

import com.jayway.jsonpath.JsonPath
import de.solidblocks.cloud.DnsService
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stderrShouldContain
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import de.solidblocks.utils.logInfo
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@ExtendWith(SolidblocksTest::class)
class BlcksIntegrationTest {
    val blcksCommand = Path.of(".").resolve("blcks")

    @Test
    fun testBinaryIsValid(context: SolidblocksTestContext) {
        assertSoftly(context.local().command(blcksCommand).runResult()) { result ->
            result shouldHaveExitCode 0
            listOf("cloud", "hetzner", "github", "docs", "terraform", "tofu").forEach {
                result stdoutShouldMatch ".*$it.*"
            }
        }
    }

    @Test
    fun testNuke(context: SolidblocksTestContext) {
        assertSoftly(context.local().command("$blcksCommand", "hetzner", "nuke", "--do-nuke").timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult()) { result ->
            result shouldHaveExitCode 0
        }
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
    fun testMinimalAndDnsCloudConfig(context: SolidblocksTestContext) {
        val test1CloudConfig = Path.of(ClassLoader.getSystemResource("test1.yaml").toURI())
        val test2CloudConfig = Path.of(ClassLoader.getSystemResource("test2.yaml").toURI())
        val cloud1Key = Path.of(ClassLoader.getSystemResource("cloud1.key").toURI())

        assertSoftly(context.local().command("$blcksCommand", "hetzner", "nuke", "--do-nuke").timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult()) { result ->
            result shouldHaveExitCode 0
        }

        val permissions = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
        )
        Files.setPosixFilePermissions(cloud1Key, permissions)

        /**
         * plan minimal cloud
         */
        assertSoftly(
            context.local().command("$blcksCommand", "cloud", "plan", test1CloudConfig.absolutePathString()).timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult(),
        ) { result ->
            result shouldHaveExitCode 0
            listOf(
                "will create network 'cloud1-default' (10.0.0.0/8)",
                "will create subnet '10.0.1.0/24'",
                "will create firewall 'cloud1-default-ssh'",
                "will create SSH key 'cloud1-default'",
                "will create volume 'cloud1-default-database1-0-backup'",
                "will create volume 'cloud1-default-database1-0-data'",
                "will create server 'cloud1-default-database1-0' (10.0.1.1)",
                "will create volume 'cloud1-default-service1-0-backup'",
                "will create volume 'cloud1-default-service1-0-data'",
                "will create firewall 'cloud1-default-service1'",
                "will create server 'cloud1-default-service1-0' (10.0.1.2)",
            ).forEach {
                result stderrShouldContain it
            }
        }

        /**
         * apply minimal cloud
         */
        assertSoftly(
            context.local().command("$blcksCommand", "cloud", "apply", test1CloudConfig.absolutePathString()).timeout(10.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult(),
        ) { result ->
            result shouldHaveExitCode 0
            result stderrShouldContain "ssh config file for cloud 'cloud1' written to"
        }

        /**
         * assert endpoint can be called and ssh connect command works
         */
        assertSoftly(
            context.local().command(
                "$blcksCommand",
                "cloud",
                "info",
                "--format",
                "json",
                test1CloudConfig.absolutePathString(),
            ).timeout(10.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult(),
        ) { result ->
            result shouldHaveExitCode 0

            val webEndpoint = JsonPath.read<List<String>>(
                result.stdout,
                "$['services'][?(@.name == 'service1')]['endpoints'][?(@.type == 'web')].url",
            ).first()

            val sshConnectCommand = JsonPath.read<List<String>>(
                result.stdout,
                "$['services'][?(@.name == 'service1')]['servers'][0].sshConnectCommand",
            ).first()

            val whoAmI = context.local().command(*"$sshConnectCommand whoami".split(" ").toTypedArray()).runResult()
            whoAmI.stdout.trim() shouldBe "root"

            /**
             * wait until http endpoint is alive
             */
            await().pollDelay(30.seconds.toJavaDuration()).pollInterval(1.minutes.toJavaDuration()).atMost(5.minutes.toJavaDuration()).until {
                try {
                    logInfo("waiting for https endpoint")
                    callEndpoint(webEndpoint).contains("visitor")
                    true
                } catch (e: Exception) {
                    false
                }
            }

            val oldVisitorCounter: Int = JsonPath.read(
                callEndpoint(webEndpoint),
                "$['visitor']",
            )

            val newVisitorCounter: Int = JsonPath.read(
                callEndpoint(webEndpoint),
                "$['visitor']",
            )

            newVisitorCounter shouldBe oldVisitorCounter + 1
        }

        /**
         * plan minimal cloud with dns
         */
        assertSoftly(
            context.local().command("$blcksCommand", "cloud", "plan", test2CloudConfig.absolutePathString()).timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult(),
        ) { result ->
            result shouldHaveExitCode 0
            listOf(
                "firewall 'cloud1-default-service1' has pending changes",
                "will create DNS record 'cloud1-default-service1-0.blcks-test.de/A'",
                "server 'cloud1-default-service1-0' (10.0.1.2) has breaking changes and needs to be re-created",
            ).forEach {
                result stderrShouldContain it
            }
        }

        /**
         * apply minimal cloud with dns
         */
        assertSoftly(
            context.local().command("$blcksCommand", "cloud", "apply", test2CloudConfig.absolutePathString()).timeout(10.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult(),
        ) { result ->
            result shouldHaveExitCode 0
            result stderrShouldContain "ssh config file for cloud 'cloud1' written to"
        }

        val dnsService = DnsService()

        /**
         * wait until at least 6 resolver have the DNS record to ensure caddy can retrieve certificates
         */
        await().atMost(5.minutes.toJavaDuration()).until {
            dnsService.tryResolveARecords("cloud1-default-service1-0.blcks-test.de.").filter { it.values.isNotEmpty() }.count() >= 6
        }

        val infoResult =
            context.local().command("$blcksCommand", "cloud", "info", "--format", "json", test2CloudConfig.absolutePathString()).timeout(10.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN"))
                .runResult()
        val sshConnectCommand = JsonPath.read<List<String>>(
            infoResult.stdout,
            "$['services'][?(@.name == 'service1')]['servers'][0].sshConnectCommand",
        ).first()

        /**
         * verify new endpoint starts with https
         */
        val sslEndpoint = JsonPath.read<List<String>>(
            infoResult.stdout,
            "$['services'][?(@.name == 'service1')]['endpoints'][?(@.type == 'web')].url",
        ).first()
        sslEndpoint shouldStartWith "https://"

        /**
         * wait until https endpoint is alive
         */
        await().pollDelay(30.seconds.toJavaDuration()).pollInterval(1.minutes.toJavaDuration()).atMost(5.minutes.toJavaDuration()).until {
            try {
                logInfo("waiting for https endpoint")
                callEndpoint(sslEndpoint).contains("visitor")
                true
            } catch (e: Exception) {
                /**
                 * restart caddy to force cert creation
                 */
                context.local().command(*"$sshConnectCommand systemctl restart caddy".split(" ").toTypedArray()).runResult()
                false
            }
        }

        val oldVisitorCounter: Int = JsonPath.read(
            callEndpoint(sslEndpoint),
            "$['visitor']",
        )

        /**
         * call new endpoint to ensure everything still works
         */
        val newVisitorCounter: Int = JsonPath.read(
            callEndpoint(sslEndpoint),
            "$['visitor']",
        )

        /**
         * verify old data is still there
         */
        newVisitorCounter shouldBeGreaterThan 1
        newVisitorCounter shouldBe oldVisitorCounter + 1

        assertSoftly(context.local().command("$blcksCommand", "hetzner", "nuke", "--do-nuke").timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult()) { result ->
            result shouldHaveExitCode 0
        }
    }

    private fun callEndpoint(endpoint: String): String {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .header("Accept", "application/json")
            .uri(URI.create(endpoint))
            .build()

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }
}
