package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.FilterValue
import de.solidblocks.hetzner.cloud.model.HetznerApiException
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.resources.ImageType
import de.solidblocks.hetzner.cloud.resources.LoadBalancerTargetType
import de.solidblocks.hetzner.cloud.resources.PublicNet
import de.solidblocks.hetzner.cloud.resources.SSHKeysCreateRequest
import de.solidblocks.hetzner.cloud.resources.SSHKeysUpdateRequest
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HetznerApiTest {

  val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
  val api = HetznerApi(hcloudToken)

  @BeforeAll
  fun beforeAll() {
    runBlocking {
      mapOf("type" to LabelSelectorValue.NotEquals(ImageType.SNAPSHOT.name.lowercase()))
      api.servers
          .list(labelSelectors = mapOf("test" to LabelSelectorValue.Equals("true")))
          .forEach {
            val delete = api.servers.delete(it.id)
            api.servers.waitForAction(delete) shouldBe true
          }
    }
  }

  @Test
  fun testPermissionDenied() {
    runBlocking { shouldThrow<RuntimeException> { HetznerApi("invalid").servers.list() } }
  }

  @Test
  fun testCreateServerInvalidRequest() {
    runBlocking {
      val exception =
          shouldThrow<HetznerApiException> {
            api.servers.create(
                ServerCreateRequest(
                    "",
                    "",
                    "",
                    "",
                    "",
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    "",
                    emptyMap(),
                    PublicNet(true, false),
                ),
            )
          }
      exception.error.message shouldBe "failed to parse json"
    }
  }

  @Test
  fun testLocationsFlow() {
    runBlocking {
      val locations = api.locations.list()
      locations shouldHaveAtLeastSize 1

      val byId = api.locations.get(locations[0].id)!!
      val byName = api.locations.get(locations[0].name)!!
      byId.name shouldBe byName.name
    }
  }

  @Test
  fun testImagesFlow() {
    runBlocking {
      val allImages = api.images.list()

      allImages shouldHaveAtLeastSize 1

      api.images.list().any { it.type != ImageType.APP } shouldBe true
      api.images.list(mapOf("type" to FilterValue.Equals(ImageType.APP.name.lowercase()))).all {
        it.type == ImageType.APP
      } shouldBe true

      val byId = api.images.get(allImages[0].id)!!
      val byName =
          api.images.get(allImages[0].name!!, mapOf("architecture" to FilterValue.Equals("x86")))!!
      byId.name!! shouldBe byName.name!!

      api.images.get("debian-12", mapOf("architecture" to FilterValue.Equals("x86"))) shouldNotBe
          null
    }
  }

  @Test
  fun testServerFlow() {
    runBlocking {
      val name = UUID.randomUUID().toString()
      val oldServerCount = api.servers.list().size
      val oldServerCountFiltered =
          api.servers.list(labelSelectors = mapOf("test" to LabelSelectorValue.Equals("true"))).size

      val createdServer =
          api.servers.create(
              ServerCreateRequest(
                  name,
                  "nbg1",
                  "cx22",
                  image = "debian-12",
                  labels = mapOf("test" to "true"),
                  userData = "",
              ),
          )

      createdServer shouldNotBe null
      createdServer!!.server.name shouldBe name

      createdServer.action shouldNotBe null
      createdServer.action!!.command shouldBe "create_server"

      assertSoftly(api.servers.actions(createdServer.server.id)!!) {
        it.actions shouldHaveSize 2
        it.actions.count { it.command == "create_server" } shouldBe 1
        it.actions.count { it.command == "start_server" } shouldBe 1
      }

      api.servers.list().size shouldBe oldServerCount + 1
      api.servers
          .list(labelSelectors = mapOf("test" to LabelSelectorValue.Equals("true")))
          .size shouldBe oldServerCountFiltered + 1
      api.servers.waitForAction(createdServer.action.id) shouldBe true

      val serverByName = api.servers.get(createdServer.server.name)
      val serverById = api.servers.get(createdServer.server.id)

      serverById?.name shouldBe createdServer.server.name
      serverByName?.name shouldBe createdServer.server.name

      val delete = api.servers.delete(createdServer.server.id)
      api.servers.waitForAction(delete) shouldBe true
    }
  }

  @Test
  fun testGetServerByInvalidName() {
    runBlocking { api.servers.get("invalid") shouldBe null }
  }

  @Test
  fun testGetInvalidLoadBalancerById() {
    runBlocking { api.loadBalancers.get(0) shouldBe null }
  }

  @Test
  fun testGetInvalidLoadBalancerByName() {
    runBlocking { api.loadBalancers.get("invalid") shouldBe null }
  }

  @Test
  fun testGetLoadBalancerByName() {
    runBlocking { api.loadBalancers.get("application2")!!.name shouldBe "application2" }
  }

  @Test
  fun testLoadBalancerAsg1() {
    runBlocking {
      val loadbalancer = api.loadBalancers.get("application1")!!
      loadbalancer.privateNetworks shouldHaveSize 1
    }
  }

  @Test
  fun testGetNetwork() {
    runBlocking {
      val networkByName = api.networks.get("hcloud-network1")!!
      val networkById = api.networks.get(networkByName.id)!!
      networkById.name shouldBe networkByName.name
    }
  }

  @Test
  fun testLoadBalancerAsg2() {
    runBlocking {
      val loadbalancer = api.loadBalancers.get("application2")!!
      loadbalancer.privateNetworks shouldHaveSize 0
    }
  }

  @Test
  fun testListLoadBalancers() {
    runBlocking {
      val loadBalancers = api.loadBalancers.list()
      assertEquals(3, loadBalancers.size)

      val asg1 = api.loadBalancers.get(loadBalancers[0].id)!!
      asg1.name shouldBe "application1"
      asg1.targets shouldHaveAtLeastSize 1
      asg1.targets[0].type shouldBe LoadBalancerTargetType.server
      asg1.targets[0].labelSelector shouldBe null

      val asg2 = api.loadBalancers.get(loadBalancers[1].id)!!
      asg2.name shouldBe "application2"
      asg2.targets shouldHaveAtLeastSize 1
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

  @Test
  fun testSSHFlow() {
    runBlocking {
      api.sshKeys.list().filter { it.name.startsWith("test") }.forEach { api.sshKeys.delete(it.id) }

      api.sshKeys.list() shouldHaveAtLeastSize 0

      val name = "test" + UUID.randomUUID().toString()
      api.sshKeys.create(
          SSHKeysCreateRequest(
              name,
              "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBMZI38/RuEdy9l4PAMREgeC4LpnIIW27Hd5J44t/yqVjWq7GxRGNimtIALncIb2HuVpoe0d4Ot9vNWzvPEYijaI= pelle@fry",
          ),
      )

      val keys = api.sshKeys.list()
      keys shouldHaveAtLeastSize 1
      keys[0].labels.shouldBeEmpty()

      api.sshKeys.update(keys[0].id, SSHKeysUpdateRequest(labels = mapOf("foo" to "bar")))

      val byId = api.sshKeys.get(keys[0].id)!!
      byId.labels shouldHaveSize 1
      byId.labels shouldContain ("foo" to "bar")

      val byName = api.sshKeys.get(keys[0].name)!!
      byId.name shouldBe byName.name

      api.sshKeys.list().filter { it.name.startsWith("test") }.forEach { api.sshKeys.delete(it.id) }
    }
  }

  @Test
  fun testServerTypeFlow() {
    runBlocking {
      val serverTypes = api.serverTypes.list()
      serverTypes shouldHaveAtLeastSize 1

      val byId = api.serverTypes.get(serverTypes[0].id)!!
      val byName = api.serverTypes.get(serverTypes[0].name)!!
      byId.name shouldBe byName.name

      api.serverTypes.get("cx22") shouldNotBe null
    }
  }
}
