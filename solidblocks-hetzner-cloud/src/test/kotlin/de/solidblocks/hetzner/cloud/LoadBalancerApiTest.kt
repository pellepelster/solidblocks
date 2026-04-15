package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.LoadBalancerAddServiceRequest
import de.solidblocks.hetzner.cloud.resources.LoadBalancerAlgorithmType
import de.solidblocks.hetzner.cloud.resources.LoadBalancerAttachServerRequest
import de.solidblocks.hetzner.cloud.resources.LoadBalancerAttachToNetworkRequest
import de.solidblocks.hetzner.cloud.resources.LoadBalancerCreateRequest
import de.solidblocks.hetzner.cloud.resources.LoadBalancerHealthCheckRequest
import de.solidblocks.hetzner.cloud.resources.LoadBalancerNameFilter
import de.solidblocks.hetzner.cloud.resources.LoadBalancerProtocol
import de.solidblocks.hetzner.cloud.resources.LoadBalancerRemoveTargetRequest
import de.solidblocks.hetzner.cloud.resources.LoadBalancerTargetType
import de.solidblocks.hetzner.cloud.resources.LoadBalancerType
import de.solidblocks.hetzner.cloud.resources.LoadBalancerUpdateRequest
import de.solidblocks.hetzner.cloud.resources.NetworkCreateRequest
import de.solidblocks.hetzner.cloud.resources.NetworkType
import de.solidblocks.hetzner.cloud.resources.NetworkZone
import de.solidblocks.hetzner.cloud.resources.NetworksSubnetCreateRequest
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoadBalancerApiTest {
    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
    val api = HetznerApi(hcloudToken)
    val testLabels = mapOf("blcks.de/managed-by" to "test")

    fun cleanup() {
        runBlocking {
            api.loadBalancers.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                if (it.protection.delete) {
                    api.loadBalancers.changeDeleteProtection(it.id, false)
                }
                if (it.privateNetworks.isNotEmpty()) {
                    it.privateNetworks.forEach { net ->
                        runCatching { api.loadBalancers.detachFromNetwork(it.id, net.network) }
                    }
                }
                api.loadBalancers.delete(it.id)
            }
            api.networks.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                if (it.protection.delete) {
                    api.networks.changeDeleteProtection(it.id, false)
                }
                api.networks.delete(it.id)
            }
            api.servers.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                api.servers.delete(it.id)
            }
        }
    }

    @BeforeAll
    fun beforeAll() = cleanup()

    @AfterAll
    fun afterAll() = cleanup()

    @Test
    fun testGetInvalidLoadBalancerById() {
        runBlocking {
            val result = api.loadBalancers.get(999999999L)
            result shouldBe null
        }
    }

    @Test
    fun testGetInvalidLoadBalancerByName() {
        runBlocking {
            api.loadBalancers.get("non-existent-load-balancer-xyz") shouldBe null
        }
    }

    @Test
    fun testLoadBalancersFlow() {
        runBlocking {
            val name = "test-load-balancer"

            val created = api.loadBalancers.create(
                LoadBalancerCreateRequest(
                    loadBalancerType = LoadBalancerType.lb11,
                    name = name,
                    location = HetznerLocation.fsn1,
                    labels = testLabels,
                ),
            )
            created shouldNotBe null
            created.loadBalancer.name shouldBe name
            created.loadBalancer.labels shouldBe testLabels

            api.loadBalancers.waitForAction(created.action)

            val byId = api.loadBalancers.get(created.loadBalancer.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.loadBalancers.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.loadBalancer.id

            api.loadBalancers.list(listOf(LoadBalancerNameFilter(name))) shouldHaveSize 1

            val updated = api.loadBalancers.update(created.loadBalancer.id, LoadBalancerUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated.loadBalancer.labels["extra"] shouldBe "value"

            val listed = api.loadBalancers.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1
            listed.map { it.name } shouldContain name

            api.loadBalancers.delete(created.loadBalancer.id) shouldBe true
            api.loadBalancers.get(created.loadBalancer.id) shouldBe null
        }
    }

    @Test
    fun testLoadBalancerServices() {
        runBlocking {
            val lb = api.loadBalancers.create(
                LoadBalancerCreateRequest(
                    loadBalancerType = LoadBalancerType.lb11,
                    name = "test-lb-services",
                    location = HetznerLocation.fsn1,
                    labels = testLabels,
                ),
            )
            api.loadBalancers.waitForAction(lb.action)

            val addServiceAction = api.loadBalancers.addService(
                lb.loadBalancer.id,
                LoadBalancerAddServiceRequest(
                    protocol = LoadBalancerProtocol.http,
                    listenPort = 80,
                    destinationPort = 8080,
                    healthCheck = LoadBalancerHealthCheckRequest(
                        protocol = LoadBalancerProtocol.http,
                        port = 8080,
                        interval = 15,
                        timeout = 10,
                        retries = 3,
                    ),
                    proxyProtocol = false,
                ),
            )
            api.loadBalancers.waitForAction(addServiceAction)

            val afterAdd = api.loadBalancers.get(lb.loadBalancer.id)
            afterAdd!!.services shouldHaveSize 1

            val changeAlgoAction = api.loadBalancers.changeAlgorithm(lb.loadBalancer.id, LoadBalancerAlgorithmType.least_connections)
            api.loadBalancers.waitForAction(changeAlgoAction)

            val deleteServiceAction = api.loadBalancers.deleteService(lb.loadBalancer.id, 80)
            api.loadBalancers.waitForAction(deleteServiceAction)

            api.loadBalancers.delete(lb.loadBalancer.id) shouldBe true
        }
    }

    @Test
    fun testLoadBalancerTargets() {
        runBlocking {
            val server = api.servers.create(
                ServerCreateRequest(
                    name = "test-server-for-lb",
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            api.servers.waitForAction(server.action)

            val lb = api.loadBalancers.create(
                LoadBalancerCreateRequest(
                    loadBalancerType = LoadBalancerType.lb11,
                    name = "test-lb-targets",
                    location = HetznerLocation.fsn1,
                    labels = testLabels,
                ),
            )
            api.loadBalancers.waitForAction(lb.action)

            val attachAction = api.loadBalancers.attachServer(lb.loadBalancer.id, server.server.id)
            api.loadBalancers.waitForAction(attachAction)

            val afterAttach = api.loadBalancers.get(lb.loadBalancer.id)
            afterAttach!!.targets shouldHaveAtLeastSize 1

            val removeAction = api.loadBalancers.removeTarget(
                lb.loadBalancer.id,
                LoadBalancerRemoveTargetRequest(
                    type = LoadBalancerTargetType.server,
                    server = LoadBalancerAttachServerRequest(server.server.id),
                ),
            )
            api.loadBalancers.waitForAction(removeAction)

            api.loadBalancers.delete(lb.loadBalancer.id) shouldBe true
            api.servers.delete(server.server.id)
        }
    }

    @Test
    @Disabled
    fun testLoadBalancerProtectionAndNetworkAttach() {
        runBlocking {
            val network = api.networks.create(NetworkCreateRequest(name = "test-network-for-lb", ipRange = "10.3.0.0/16", labels = testLabels))
            val subnetAction = api.networks.addSubnet(network.network.id, NetworksSubnetCreateRequest(NetworkType.cloud, "10.3.0.0/24", NetworkZone.`eu-central`))
            api.networks.waitForAction(subnetAction)

            val lb = api.loadBalancers.create(
                LoadBalancerCreateRequest(
                    loadBalancerType = LoadBalancerType.lb11,
                    name = "test-lb-protection",
                    location = HetznerLocation.fsn1,
                    labels = testLabels,
                ),
            )
            api.loadBalancers.waitForAction(lb.action)

            val protectAction = api.loadBalancers.changeDeleteProtection(lb.loadBalancer.id, true)
            api.loadBalancers.waitForAction(protectAction)

            val protected = api.loadBalancers.get(lb.loadBalancer.id)
            protected!!.protection.delete shouldBe true

            val unprotectAction = api.loadBalancers.changeDeleteProtection(lb.loadBalancer.id, false)
            api.loadBalancers.waitForAction(unprotectAction)

            val attachNetworkAction = api.loadBalancers.attachToNetwork(lb.loadBalancer.id, LoadBalancerAttachToNetworkRequest(network.network.id))
            api.loadBalancers.waitForAction(attachNetworkAction)

            val attachedLb = api.loadBalancers.get(lb.loadBalancer.id)
            attachedLb!!.privateNetworks shouldHaveAtLeastSize 1

            val detachNetworkAction = api.loadBalancers.detachFromNetwork(lb.loadBalancer.id, network.network.id)
            api.loadBalancers.waitForAction(detachNetworkAction)

            api.loadBalancers.delete(lb.loadBalancer.id) shouldBe true
            api.networks.delete(network.network.id) shouldBe true
        }
    }
}
