package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.NetworkCreateRequest
import de.solidblocks.hetzner.cloud.resources.NetworkNameFilter
import de.solidblocks.hetzner.cloud.resources.NetworkType
import de.solidblocks.hetzner.cloud.resources.NetworkUpdateRequest
import de.solidblocks.hetzner.cloud.resources.NetworkZone
import de.solidblocks.hetzner.cloud.resources.NetworksAddRouteRequest
import de.solidblocks.hetzner.cloud.resources.NetworksDeleteRouteRequest
import de.solidblocks.hetzner.cloud.resources.NetworksSubnetCreateRequest
import de.solidblocks.hetzner.cloud.resources.NetworksSubnetDeleteRequest
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
class NetworkApiTest : BaseTest() {

    fun cleanup() {
        runBlocking {
            api.networks.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                if (it.protection.delete) {
                    api.networks.changeDeleteProtection(it.id, false)
                }
                api.networks.delete(it.id)
            }
        }
    }

    @BeforeAll
    fun beforeAll() = cleanup()

    @AfterAll
    fun afterAll() = cleanup()

    @Test
    fun testNetworksFlow() {
        runBlocking {
            val name = "test-network"

            val created = api.networks.create(NetworkCreateRequest(name = name, ipRange = "10.0.0.0/16", labels = testLabels))
            created shouldNotBe null
            created.network.name shouldBe name
            created.network.ipRange shouldBe "10.0.0.0/16"
            created.network.labels shouldBe testLabels

            val byId = api.networks.get(created.network.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.networks.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.network.id

            api.networks.list(listOf(NetworkNameFilter(name))) shouldHaveSize 1

            val updated = api.networks.update(created.network.id, NetworkUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated.network.labels["extra"] shouldBe "value"

            val listed = api.networks.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1

            api.networks.delete(created.network.id) shouldBe true

            api.networks.get(created.network.id) shouldBe null
        }
    }

    @Test
    fun testNetworkRoutesAndSubnets() {
        runBlocking {
            val network = api.networks.create(NetworkCreateRequest(name = "test-network-routes", ipRange = "10.1.0.0/16", labels = testLabels))
            val networkId = network.network.id

            val subnetAction = api.networks.addSubnet(networkId, NetworksSubnetCreateRequest(NetworkType.cloud, "10.1.0.0/24", NetworkZone.`eu-central`))
            subnetAction shouldNotBe null
            api.networks.waitForAction(subnetAction)

            val afterSubnet = api.networks.get(networkId)
            afterSubnet!!.subnets shouldHaveSize 1

            val routeAction = api.networks.addRoute(networkId, NetworksAddRouteRequest("10.1.2.0/24", "10.1.0.2"))
            api.networks.waitForAction(routeAction)

            val deleteRouteAction = api.networks.deleteRoute(networkId, NetworksDeleteRouteRequest("10.1.2.0/24", "10.1.0.2"))
            api.networks.waitForAction(deleteRouteAction)

            val deleteSubnetAction = api.networks.deleteSubnet(networkId, NetworksSubnetDeleteRequest("10.1.0.0/24", NetworkZone.`eu-central`))
            api.networks.waitForAction(deleteSubnetAction)

            api.networks.delete(networkId) shouldBe true
        }
    }

    @Test
    fun testNetworkChangeDeleteProtection() {
        runBlocking {
            val network = api.networks.create(NetworkCreateRequest(name = "test-network-protection", ipRange = "10.2.0.0/16", labels = testLabels))
            val networkId = network.network.id

            val protectAction = api.networks.changeDeleteProtection(networkId, true)
            api.networks.waitForAction(protectAction)

            val protected = api.networks.get(networkId)
            protected!!.protection.delete shouldBe true

            val unprotectAction = api.networks.changeDeleteProtection(networkId, false)
            api.networks.waitForAction(unprotectAction)

            val unprotected = api.networks.get(networkId)
            unprotected!!.protection.delete shouldBe false

            api.networks.delete(networkId) shouldBe true
        }
    }
}
