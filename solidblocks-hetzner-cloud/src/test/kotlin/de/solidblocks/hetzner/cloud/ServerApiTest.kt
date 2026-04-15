package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.HetznerApiException
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.NetworkCreateRequest
import de.solidblocks.hetzner.cloud.resources.NetworkType
import de.solidblocks.hetzner.cloud.resources.NetworkZone
import de.solidblocks.hetzner.cloud.resources.NetworksSubnetCreateRequest
import de.solidblocks.hetzner.cloud.resources.PlacementGroupCreateRequest
import de.solidblocks.hetzner.cloud.resources.PlacementGroupType
import de.solidblocks.hetzner.cloud.resources.ServerChangeTypeRequest
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import de.solidblocks.hetzner.cloud.resources.ServerNameFilter
import de.solidblocks.hetzner.cloud.resources.ServerNetworkAttachRequest
import de.solidblocks.hetzner.cloud.resources.ServerStatus
import de.solidblocks.hetzner.cloud.resources.ServerStatusFilter
import de.solidblocks.hetzner.cloud.resources.ServerUpdateRequest
import io.kotest.assertions.throwables.shouldThrow
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
class ServerApiTest : BaseTest() {

    fun cleanup() {
        runBlocking {
            api.servers.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                if (it.protection.delete) {
                    api.servers.changeDeleteProtection(it.id, false)
                }
                api.servers.delete(it.id)
            }
            api.networks.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                if (it.protection.delete) {
                    api.networks.changeDeleteProtection(it.id, false)
                }
                api.networks.delete(it.id)
            }
            api.placementGroups.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                api.placementGroups.delete(it.id)
            }
        }
    }

    @BeforeAll
    fun beforeAll() = cleanup()

    @AfterAll
    fun afterAll() = cleanup()

    @Test
    fun testPermissionDenied() {
        runBlocking {
            val badApi = HetznerApi("invalid-token")
            shouldThrow<HetznerApiException> {
                badApi.servers.list()
            }
        }
    }

    @Test
    fun testCreateServerInvalidRequest() {
        runBlocking {
            shouldThrow<HetznerApiException> {
                api.servers.create(
                    ServerCreateRequest(
                        name = "invalid-server",
                        location = HetznerLocation.fsn1,
                        type = HetznerServerType.cx23,
                        image = "non-existent-image-xyz",
                        labels = testLabels,
                    ),
                )
            }
        }
    }

    @Test
    fun testServerFlow() {
        runBlocking {
            val name = "test-server-flow"

            val created = api.servers.create(
                ServerCreateRequest(
                    name = name,
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            created shouldNotBe null
            created.server.name shouldBe name
            created.server.labels shouldBe testLabels

            api.servers.waitForAction(created.action)

            val byId = api.servers.get(created.server.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.servers.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.server.id

            api.servers.list(listOf(ServerNameFilter(name))) shouldHaveSize 1

            val updated = api.servers.update(created.server.id, ServerUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated.server.labels["extra"] shouldBe "value"

            val listed = api.servers.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1

            api.servers.delete(created.server.id)

            api.servers.get(created.server.id) shouldBe null
        }
    }

    @Test
    fun testGetServerByInvalidName() {
        runBlocking {
            val result = api.servers.get("non-existent-server-xyz")
            result shouldBe null
        }
    }

    @Test
    fun testServerPowerOperations() {
        runBlocking {
            val server = api.servers.create(
                ServerCreateRequest(
                    name = "test-server-power",
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            api.servers.waitForAction(server.action)

            val powerOffAction = api.servers.powerOff(server.server.id)
            api.servers.waitForAction(powerOffAction)

            val offServer = api.servers.get(server.server.id)
            offServer!!.status shouldBe ServerStatus.off

            val offServers = api.servers.list(listOf(ServerStatusFilter(ServerStatus.off)), labelSelectors = testLabels.toLabelSelectors())
            offServers.any { it.id == server.server.id } shouldBe true
            offServers.all { it.status == ServerStatus.off } shouldBe true

            val powerOnAction = api.servers.powerOn(server.server.id)
            api.servers.waitForAction(powerOnAction)

            val onServer = api.servers.get(server.server.id)
            onServer!!.status shouldBe ServerStatus.running

            val rebootAction = api.servers.reboot(server.server.id)
            api.servers.waitForAction(rebootAction)

            api.servers.delete(server.server.id)
        }
    }

    @Test
    fun testServerShutdown() {
        runBlocking {
            val server = api.servers.create(
                ServerCreateRequest(
                    name = "test-server-shutdown",
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            api.servers.waitForAction(server.action)

            val shutdownAction = api.servers.shutdown(server.server.id)
            api.servers.waitForAction(shutdownAction)

            api.servers.delete(server.server.id)
        }
    }

    @Test
    fun testServerReset() {
        runBlocking {
            val server = api.servers.create(
                ServerCreateRequest(
                    name = "test-server-reset",
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            api.servers.waitForAction(server.action)

            val resetAction = api.servers.reset(server.server.id)
            api.servers.waitForAction(resetAction)

            api.servers.delete(server.server.id)
        }
    }

    @Test
    fun testServerChangeType() {
        runBlocking {
            val server = api.servers.create(
                ServerCreateRequest(
                    name = "test-server-change-type",
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            api.servers.waitForAction(server.action)

            val powerOffAction = api.servers.powerOff(server.server.id)
            api.servers.waitForAction(powerOffAction)

            val changeTypeAction = api.servers.changeType(server.server.id, ServerChangeTypeRequest(HetznerServerType.cx33, false))
            api.servers.waitForAction(changeTypeAction)

            val upgraded = api.servers.get(server.server.id)
            upgraded!!.type.name shouldBe "cx33"

            api.servers.delete(server.server.id)
        }
    }

    @Test
    @Disabled
    fun testServerChangeDnsPtr() {
        runBlocking {
            val server = api.servers.create(
                ServerCreateRequest(
                    name = "test-server-dns-ptr",
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            api.servers.waitForAction(server.action)

            val publicIp = server.server.publicNetwork?.ipv4?.ip
            publicIp shouldNotBe null

            val dnsPtrAction = api.servers.changeDnsPtr(server.server.id, publicIp!!, "test.example.com")
            api.servers.waitForAction(dnsPtrAction)

            val clearDnsPtrAction = api.servers.changeDnsPtr(server.server.id, publicIp, null)
            api.servers.waitForAction(clearDnsPtrAction)

            api.servers.delete(server.server.id)
        }
    }

    @Test
    @Disabled
    fun testServerNetworkDetach() {
        runBlocking {
            val network = api.networks.create(NetworkCreateRequest(name = "test-network-server", ipRange = "10.4.0.0/16", labels = testLabels))
            val subnetAction = api.networks.addSubnet(network.network.id, NetworksSubnetCreateRequest(NetworkType.cloud, "10.4.0.0/24", NetworkZone.`eu-central`))
            api.networks.waitForAction(subnetAction)

            val server = api.servers.create(
                ServerCreateRequest(
                    name = "test-server-network",
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            api.servers.waitForAction(server.action)

            val attachAction = api.servers.attachToNetwork(server.server.id, ServerNetworkAttachRequest(network.network.id))
            api.servers.waitForAction(attachAction)

            val attached = api.servers.get(server.server.id)
            attached!!.privateNetwork shouldHaveAtLeastSize 1

            val detachAction = api.servers.detachFromNetwork(server.server.id, network.network.id)
            api.servers.waitForAction(detachAction)

            val detached = api.servers.get(server.server.id)
            detached!!.privateNetwork.none { it.network == network.network.id } shouldBe true

            api.servers.delete(server.server.id)
            api.networks.delete(network.network.id) shouldBe true
        }
    }

    @Test
    fun testServerPlacementGroupOperations() {
        runBlocking {
            val pg = api.placementGroups.create(PlacementGroupCreateRequest("test-pg-server", PlacementGroupType.spread, testLabels))

            val server = api.servers.create(
                ServerCreateRequest(
                    name = "test-server-pg",
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            api.servers.waitForAction(server.action)

            val powerOffAction = api.servers.powerOff(server.server.id)
            api.servers.waitForAction(powerOffAction)

            val addAction = api.servers.addToPlacementGroup(server.server.id, pg.placementGroup.id)
            api.servers.waitForAction(addAction)

            val removeAction = api.servers.removeFromPlacementGroup(server.server.id)
            api.servers.waitForAction(removeAction)

            api.servers.delete(server.server.id)
            api.placementGroups.delete(pg.placementGroup.id) shouldBe true
        }
    }
}
