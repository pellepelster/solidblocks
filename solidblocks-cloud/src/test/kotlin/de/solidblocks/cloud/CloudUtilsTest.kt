package de.solidblocks.cloud

import de.solidblocks.cloud.mocks.MockResource1
import de.solidblocks.cloud.mocks.MockResource1Lookup
import de.solidblocks.cloud.mocks.MockResource2Lookup
import de.solidblocks.cloud.utils.ByteSize
import de.solidblocks.cloud.utils.commandExists
import de.solidblocks.cloud.utils.equalsIgnoreOrder
import de.solidblocks.cloud.utils.formatBytes
import de.solidblocks.cloud.utils.formatLocale
import de.solidblocks.cloud.utils.getEnvOrProperty
import de.solidblocks.cloud.utils.joinToStringOrEmpty
import de.solidblocks.cloud.utils.runCommand
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class CloudUtilsTest {

    @Test
    fun testLogText() {
        MockResource1("resource1").logText() shouldBe "mockresource1 'resource1'"
        MockResource1Lookup("resource1").logText() shouldBe "mockresource1 'resource1'"
        MockResource2Lookup("resource2").logText() shouldBe "custom log text 'resource2'"
    }

    @Test
    fun testFormatBytes() {
        0L.formatBytes() shouldBe "0 B"
        512L.formatBytes() shouldBe "512 B"
        1024L.formatBytes() shouldBe "1.00 KB"
        1_536_000L.formatBytes() shouldBe "1.46 MB"
        2_147_483_648.formatBytes() shouldBe "2.00 GB"
    }

    @Test
    fun testCommandExists() {
        commandExists("whoami") shouldBe true
        commandExists("invalid") shouldBe false
    }

    @Test
    fun testCommand() {
        assertSoftly(runCommand(listOf("whoami"))) {
            it?.exitCode shouldBe 0
            it?.stdout shouldBe "pelle\n"
            it?.stderr shouldBe ""
        }
    }

    @Test
    fun testInvalid() {
        runCommand(listOf("invalid")) shouldBe null
    }

    @Test
    fun testGetEnvOrProperty() {
        System.setProperty("test.solidblocks.property", "propertyValue")
        getEnvOrProperty("test.solidblocks.property") shouldBe "propertyValue"
        getEnvOrProperty("PATH") shouldBe System.getenv("PATH")
        getEnvOrProperty("nonexistent.solidblocks.property") shouldBe null
    }

    @Test
    fun testByteSize() {
        ByteSize.fromGigabytes(1).bytes shouldBe 1_000_000_000L
        ByteSize.fromGigabytes(2).bytes shouldBe 2_000_000_000L
        ByteSize(1_000_000_000L).gigabytes() shouldBe 1
        ByteSize(2_500_000_000L).gigabytes() shouldBe 2
        ByteSize(500_000_000L).gigabytes() shouldBe 0
    }

    @Test
    fun testDurationFormatLocale() {
        Duration.ofSeconds(30).formatLocale() shouldBe "30s"
        Duration.ofSeconds(120).formatLocale() shouldBe "120s"
        Duration.ofSeconds(180).formatLocale() shouldBe "3m"
        Duration.ofMinutes(5).formatLocale() shouldBe "5m"
    }

    @Test
    fun testEqualsIgnoreOrder() {
        listOf(1, 2, 3) equalsIgnoreOrder listOf(3, 1, 2) shouldBe true
        listOf(1, 2, 3) equalsIgnoreOrder listOf(1, 2, 3) shouldBe true
        listOf(1, 2, 3) equalsIgnoreOrder listOf(1, 2, 4) shouldBe false
        listOf(1, 2, 3) equalsIgnoreOrder listOf(1, 2) shouldBe false
        emptyList<Int>() equalsIgnoreOrder emptyList() shouldBe true
    }

    @Test
    fun testJoinToStringOrEmpty() {
        emptyList<String>().joinToStringOrEmpty { it } shouldBe "<none>"
        emptyList<String>().joinToStringOrEmpty(empty = "nothing") { it } shouldBe "nothing"
        listOf("a", "b", "c").joinToStringOrEmpty { it } shouldBe "a, b, c"
        listOf("a", "b").joinToStringOrEmpty(separator = " | ") { it.uppercase() } shouldBe "A | B"
        listOf("x").joinToStringOrEmpty { it } shouldBe "x"
    }

    @Test
    @Disabled
    fun testPassShow() {
        println(runCommand(listOf("pass", "test"))?.stdout)
    }

    @Test
    @Disabled
    fun testPassInsert() {
        val random = UUID.randomUUID().toString()
        runCommand(listOf("pass", "insert", "--multiline", "--force", "test"), random)
        runCommand(listOf("pass", "test"))?.stdout shouldBe random
    }
}
