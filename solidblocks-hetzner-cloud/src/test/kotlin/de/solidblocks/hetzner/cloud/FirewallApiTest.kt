package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.FirewallApplyToResourcesRequest
import de.solidblocks.hetzner.cloud.resources.FirewallCreateRequest
import de.solidblocks.hetzner.cloud.resources.FirewallLabelSelector
import de.solidblocks.hetzner.cloud.resources.FirewallNameFilter
import de.solidblocks.hetzner.cloud.resources.FirewallRemoveFromResourcesRequest
import de.solidblocks.hetzner.cloud.resources.FirewallResource
import de.solidblocks.hetzner.cloud.resources.FirewallResourceType
import de.solidblocks.hetzner.cloud.resources.FirewallRuleDirection
import de.solidblocks.hetzner.cloud.resources.FirewallRuleProtocol
import de.solidblocks.hetzner.cloud.resources.FirewallSetRulesRequest
import de.solidblocks.hetzner.cloud.resources.FirewallUpdateRequest
import de.solidblocks.hetzner.cloud.resources.HetznerFirewallRule
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirewallApiTest : BaseTest() {

    fun cleanup() {
        runBlocking {
            api.firewalls.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                api.firewalls.delete(it.id)
            }
        }
    }

    @BeforeAll
    fun beforeAll() = cleanup()

    @AfterAll
    fun afterAll() = cleanup()

    @Test
    fun testFirewallFlow() {
        runBlocking {
            val name = "test-firewall"
            val inboundRule = HetznerFirewallRule(
                direction = FirewallRuleDirection.IN,
                protocol = FirewallRuleProtocol.TCP,
                port = "22",
                sourceIps = listOf("0.0.0.0/0", "::/0"),
            )

            val created = api.firewalls.create(FirewallCreateRequest(name, rules = listOf(inboundRule), labels = testLabels))
            created shouldNotBe null
            created.firewall.name shouldBe name
            created.firewall.labels shouldBe testLabels
            created.firewall.rules shouldHaveSize 1

            val byId = api.firewalls.get(created.firewall.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.firewalls.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.firewall.id

            api.firewalls.list(listOf(FirewallNameFilter(name))) shouldHaveSize 1

            val updated = api.firewalls.update(created.firewall.id, FirewallUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated.firewall.labels["extra"] shouldBe "value"

            val httpsRule = HetznerFirewallRule(
                direction = FirewallRuleDirection.IN,
                protocol = FirewallRuleProtocol.TCP,
                port = "443",
                sourceIps = listOf("0.0.0.0/0", "::/0"),
            )
            val setRulesActions = api.firewalls.setRules(created.firewall.id, FirewallSetRulesRequest(listOf(inboundRule, httpsRule)))
            api.firewalls.waitForAction(setRulesActions)

            val afterRules = api.firewalls.get(created.firewall.id)
            afterRules!!.rules shouldHaveSize 2

            val labelSelector = FirewallResource(
                type = FirewallResourceType.LABEL_SELECTOR,
                labelSelector = FirewallLabelSelector("blcks.de/managed-by==test"),
            )
            val applyActions = api.firewalls.applyToResources(created.firewall.id, FirewallApplyToResourcesRequest(listOf(labelSelector)))
            api.firewalls.waitForAction(applyActions)

            val removeActions = api.firewalls.removeFromResources(created.firewall.id, FirewallRemoveFromResourcesRequest(listOf(labelSelector)))
            api.firewalls.waitForAction(removeActions)

            val listed = api.firewalls.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1

            api.firewalls.delete(created.firewall.id) shouldBe true

            api.firewalls.get(created.firewall.id) shouldBe null
        }
    }
}
