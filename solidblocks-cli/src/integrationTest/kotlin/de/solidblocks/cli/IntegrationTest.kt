package de.solidblocks.cli

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stderrShouldBe
import de.solidblocks.infra.test.assertions.stderrShouldContain
import de.solidblocks.infra.test.assertions.stderrShouldMatch
import de.solidblocks.infra.test.assertions.stdoutShouldContain
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.minutes

@ExtendWith(SolidblocksTest::class)
@DisabledIfEnvironmentVariable(named = "SKIP_TESTS", matches = ".*integration.*")
class IntegrationTest {
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
    fun testCloudLocalBackup(context: SolidblocksTestContext) {
        val cloudConfig = Path.of(ClassLoader.getSystemResource("test1.yaml").toURI());

        assertSoftly(context.local().command("$blcksCommand", "hetzner", "nuke", "--do-nuke").timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult()) { result ->
            result shouldHaveExitCode 0
        }

        assertSoftly(
            context.local().command("$blcksCommand", "cloud", "plan", cloudConfig.absolutePathString()).timeout(5.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult()
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

        assertSoftly(
            context.local().command("$blcksCommand", "cloud", "apply", cloudConfig.absolutePathString()).timeout(10.minutes).env("HCLOUD_TOKEN", System.getenv("HCLOUD_TOKEN")).runResult()
        ) { result ->
            result shouldHaveExitCode 0
            result stdoutShouldContain "ssh config file for cloud 'cloud1' written to"
        }
    }
}
