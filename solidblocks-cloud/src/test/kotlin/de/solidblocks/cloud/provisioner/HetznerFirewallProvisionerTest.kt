package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.hetzner.cloud.firewall.HetznerFirewall
import de.solidblocks.cloud.provisioner.hetzner.cloud.firewall.HetznerFirewallProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.firewall.HetznerFirewallRuntime
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.resources.HetznerFirewallRule
import de.solidblocks.hetzner.cloud.resources.FirewallRuleDirection
import de.solidblocks.hetzner.cloud.resources.FirewallRuleProtocol
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTest::class)
class HetznerFirewallProvisionerTest {

    private val httpRule = HetznerFirewallRule(
        direction = FirewallRuleDirection.IN,
        protocol = FirewallRuleProtocol.TCP,
        port = "80",
        sourceIps = listOf("0.0.0.0/0", "::/0"),
        description = "allow http",
    )

    private val httpsRule = HetznerFirewallRule(
        direction = FirewallRuleDirection.IN,
        protocol = FirewallRuleProtocol.TCP,
        port = "443",
        sourceIps = listOf("0.0.0.0/0", "::/0"),
        description = "allow https",
    )

    private val icmpRule = HetznerFirewallRule(
        direction = FirewallRuleDirection.IN,
        protocol = FirewallRuleProtocol.ICMP,
        sourceIps = listOf("0.0.0.0/0", "::/0"),
        description = "allow icmp",
    )

    @Test
    fun testFlow(context: SolidblocksTestContext) {
        val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN"))
        val name = UUID.randomUUID().toString()

        val provisioner = HetznerFirewallProvisioner(System.getenv("HCLOUD_TOKEN"))

        val resource = HetznerFirewall(name, listOf(httpRule, httpsRule), hetzner.defaultLabels, emptyMap())

        runBlocking {
            provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.missing
                it.changes.shouldBeEmpty()
            }

            provisioner
                .apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<HetznerFirewallRuntime>>()
                .data
                .name shouldBe name

            assertSoftly(provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)!!) {
                it.name shouldBe name
                it.rules shouldHaveSize 2
            }
            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            val resourceWithNewRules = HetznerFirewall(name, listOf(icmpRule), hetzner.defaultLabels, emptyMap())

            assertSoftly(provisioner.diff(resourceWithNewRules, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].name shouldBe "rules"
                it.changes[0].expectedValue shouldBe 1
                it.changes[0].actualValue shouldBe 2
            }

            provisioner
                .apply(resourceWithNewRules, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<HetznerFirewallRuntime>>()
                .data
                .name shouldBe name

            assertSoftly(provisioner.lookup(resourceWithNewRules.asLookup(), TEST_PROVISIONER_CONTEXT)!!) {
                it.rules shouldHaveSize 1
                it.rules[0].protocol shouldBe FirewallRuleProtocol.ICMP
            }
            assertSoftly(provisioner.diff(resourceWithNewRules, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            val resourceWithNewLabel = HetznerFirewall(name, listOf(icmpRule), hetzner.defaultLabels + mapOf("foo" to "bar"), emptyMap())

            assertSoftly(provisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].missing shouldBe true
                it.changes[0].name shouldBe "label 'foo'"
            }

            provisioner.apply(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)

            assertSoftly(provisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            val resourceWithUpdatedLabel = HetznerFirewall(name, listOf(icmpRule), hetzner.defaultLabels + mapOf("foo" to "bar2"), emptyMap())

            assertSoftly(provisioner.diff(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].name shouldBe "label 'foo'"
                it.changes[0].expectedValue shouldBe "bar2"
                it.changes[0].actualValue shouldBe "bar"
            }

            provisioner.apply(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)

            assertSoftly(provisioner.diff(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            provisioner.destroy(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
        }
    }
}
