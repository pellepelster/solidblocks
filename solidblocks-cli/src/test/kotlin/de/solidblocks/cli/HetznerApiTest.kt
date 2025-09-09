package de.solidblocks.cli

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerApiException
import de.solidblocks.cli.hetzner.api.resources.LoadBalancerTargetType
import de.solidblocks.cli.hetzner.api.resources.ServerCreateRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class HetznerApiTest {

    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
    val api = HetznerApi(hcloudToken)

    @Test
    fun testPermissionDenied() {
        runBlocking {
            shouldThrow<RuntimeException> {
                HetznerApi("invalid").servers.list()
            }
        }
    }

    @Test
    fun testCreateServerInvalidRequest() {
        runBlocking {
            val exception = shouldThrow<HetznerApiException> {
                api.servers.create(ServerCreateRequest("", "", ""))
            }
            exception.error.message shouldBe "invalid input in field 'name'"
        }
    }

    @Test
    fun testCreateServer() {
        runBlocking {
            val name = UUID.randomUUID().toString()
            val server = api.servers.create(ServerCreateRequest(name, "cx22", "debian-12"))
            server!!.server.name shouldBe name
        }
    }

    @Test
    fun testListServersPaged() {
        runBlocking {
            api.servers.listPaged().servers shouldHaveAtLeastSize 3
        }
    }

    @Test
    fun testListServers() {
        runBlocking {
            api.servers.list() shouldHaveAtLeastSize 3
        }
    }

    @Test
    fun testGetServerByName() {
        runBlocking {
            val server1 = api.servers.get("hcloud-server1")!!
            server1.server.name shouldBe "hcloud-server1"
            server1.server.labels shouldHaveSize 1
        }
    }

    @Test
    fun testGetServerByInvalidName() {
        runBlocking {
            api.servers.get("invalid") shouldBe null
        }
    }

    @Test
    fun testGetInvalidLoadBalancerById() {
        runBlocking {
            api.loadBalancers.get(0) shouldBe null
        }
    }

    @Test
    fun testGetInvalidLoadBalancerByName() {
        runBlocking {
            api.loadBalancers.get("invalid") shouldBe null
        }
    }

    @Test
    fun testGetLoadBalancerByName() {
        runBlocking {
            api.loadBalancers.get("hcloud-load-balancer-asg2")!!.loadbalancer.name shouldBe "hcloud-load-balancer-asg2"
        }
    }

    @Test
    fun testListLoadBalancers() {
        runBlocking {
            val loadBalancers = api.loadBalancers.list()
            assertEquals(3, loadBalancers.size)

            val asg1 = api.loadBalancers.get(loadBalancers[0].id)!!
            asg1.loadbalancer.name shouldBe "hcloud-load-balancer-asg1"
            asg1.loadbalancer.targets shouldHaveSize 1
            asg1.loadbalancer.targets[0].type shouldBe LoadBalancerTargetType.server
            asg1.loadbalancer.targets[0].labelSelector shouldBe null

            val asg2 = api.loadBalancers.get(loadBalancers[1].id)!!
            asg2.loadbalancer.name shouldBe "hcloud-load-balancer-asg2"
            asg2.loadbalancer.targets shouldHaveSize 1
            asg2.loadbalancer.targets[0].type shouldBe LoadBalancerTargetType.label_selector
            asg2.loadbalancer.targets[0].labelSelector!!.selector shouldBe "foo=bar"
        }
    }

    @Test
    fun testListVolumesPaged() {
        runBlocking { assertEquals(13, api.volumes.listPaged().volumes.size) }
    }

    @Test
    fun testListVolumes() {
        runBlocking { assertEquals(13, api.volumes.list().size) }
    }

    @Test
    fun testWaitFor() {
        runBlocking {
            val volume = api.volumes.list().first()

            val result =
                api.waitFor({ api.volumes.changeProtection(volume.id, true) }, { api.volumes.action(it) })

            assertTrue(result)
        }
    }
}
