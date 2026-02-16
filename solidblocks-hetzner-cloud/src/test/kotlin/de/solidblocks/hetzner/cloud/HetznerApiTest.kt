package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.FilterValue
import de.solidblocks.hetzner.cloud.model.HetznerApiException
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue.Equals
import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.DnsRRSetRecord
import de.solidblocks.hetzner.cloud.resources.DnsRRSetsCreateRequest
import de.solidblocks.hetzner.cloud.resources.ImageType
import de.solidblocks.hetzner.cloud.resources.LoadBalancerCreateRequest
import de.solidblocks.hetzner.cloud.resources.LoadBalancerType
import de.solidblocks.hetzner.cloud.resources.NetworkCreateRequest
import de.solidblocks.hetzner.cloud.resources.PublicNet
import de.solidblocks.hetzner.cloud.resources.RRType
import de.solidblocks.hetzner.cloud.resources.SSHKeysCreateRequest
import de.solidblocks.hetzner.cloud.resources.SSHKeysUpdateRequest
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import de.solidblocks.hetzner.cloud.resources.ServerUpdateRequest
import de.solidblocks.hetzner.cloud.resources.VolumeCreateRequest
import de.solidblocks.hetzner.cloud.resources.VolumeFormat
import de.solidblocks.hetzner.cloud.resources.VolumeUpdateRequest
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HetznerApiTest {

  val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
  val api = HetznerApi(hcloudToken)

  val testLabels = mapOf("blcks.de/managed-by" to "test")

  fun cleanup() {
    runBlocking {
      mapOf("type" to LabelSelectorValue.NotEquals(ImageType.SNAPSHOT.name.lowercase()))
      api.servers.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
        val delete = api.servers.delete(it.id)
        api.servers.waitForAction(delete) shouldBe true
      }

      api.sshKeys.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
        api.sshKeys.delete(it.id)
      }

      api.volumes.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
        val result = api.volumes.changeDeleteProtection(it.id, false)
        api.volumes.waitForAction(result) shouldBe true
        api.volumes.delete(it.id)
      }
    }
  }

  @BeforeAll
  fun beforeAll() {
    cleanup()
  }

  @AfterAll
  fun afterAll() {
    cleanup()
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
          api.servers.list(labelSelectors = mapOf("test" to Equals("true"))).size

      val createdServer =
          api.servers.create(
              ServerCreateRequest(
                  name,
                  "nbg1",
                  "cx23",
                  image = "debian-12",
                  userData = "",
                  labels = mapOf("test" to "true"),
              ),
          )

      createdServer shouldNotBe null
      createdServer!!.server.name shouldBe name

      createdServer.action shouldNotBe null
      createdServer.action.command shouldBe "create_server"

      assertSoftly(api.servers.actions(createdServer.server.id)) {
        it.actions shouldHaveSize 2
        it.actions.count { it.command == "create_server" } shouldBe 1
        it.actions.count { it.command == "start_server" } shouldBe 1
      }

      api.servers.list().size shouldBe oldServerCount + 1
      api.servers.list(labelSelectors = mapOf("test" to Equals("true"))).size shouldBe
          oldServerCountFiltered + 1
      api.servers.waitForAction(createdServer.action.id) shouldBe true

      val byName = api.servers.get(createdServer.server.name)!!
      val byId = api.servers.get(createdServer.server.id)!!

      byId.name shouldBe createdServer.server.name
      byId.type.name shouldBe "cx23"
      byId.image.name shouldBe "debian-12"
      byName.name shouldBe createdServer.server.name

      api.servers.update(
          byId.id,
          ServerUpdateRequest(labels = mapOf("test" to "true", "foo" to "bar")),
      )
      api.servers.list(labelSelectors = mapOf("foo" to Equals("bar"))).size shouldBe
          oldServerCountFiltered + 1

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
  fun testNetworksFlow() {
    runBlocking {
      val name = UUID.randomUUID().toString()
      api.networks.create(NetworkCreateRequest(name, "10.0.0.0/24")) shouldNotBe null

      val networkByName = api.networks.get(name)!!
      val networkById = api.networks.get(networkByName.id)!!
      networkById.name shouldBe networkByName.name

      api.networks.delete(networkById.id) shouldBe true
    }
  }

  @Test
  fun testLoadBalancersFlow() {
    runBlocking {
      val name = UUID.randomUUID().toString()
      api.loadBalancers.create(
          LoadBalancerCreateRequest(LoadBalancerType.lb11, name, "fsn1"),
      ) shouldNotBe null
      api.loadBalancers.list() shouldHaveAtLeastSize 1

      val byName = api.loadBalancers.get(name)!!
      byName.name shouldBe name

      val byId = api.loadBalancers.get(byName.id)!!
      byId.name shouldBe name
    }
  }

  @Test
  fun testVolumesFlow() {
    runBlocking {
      api.volumes.list()

      api.volumes.list(labelSelectors = testLabels.toLabelSelectors()) shouldHaveSize 0
      val result =
          api.volumes.create(
              VolumeCreateRequest(
                  "volume1",
                  16,
                  "nbg1",
                  labels = testLabels,
                  format = VolumeFormat.ext4,
              ),
          )
      result!!.volume.linuxDevice shouldNotBe null
      result.volume.format shouldBe VolumeFormat.ext4
      api.volumes.list(labelSelectors = testLabels.toLabelSelectors()) shouldHaveSize 1

      val byName = api.volumes.get(result.volume.name)!!
      val byId = api.volumes.get(result.volume.id)!!

      byId.name shouldBe result.volume.name
      byName.name shouldBe result.volume.name

      val random = UUID.randomUUID().toString()
      api.volumes.update(
          byId.id,
          VolumeUpdateRequest(labels = mapOf("test" to "true", "foo" to random)),
      )
      api.volumes.list(labelSelectors = mapOf("foo" to Equals(random))).size shouldBe 1

      val enableDeleteProtection = api.volumes.changeDeleteProtection(byId.id, true)
      api.volumes.waitForAction(enableDeleteProtection)

      val disableDeleteProtection = api.volumes.changeDeleteProtection(byId.id, false)
      api.volumes.waitForAction(disableDeleteProtection)

      api.volumes.delete(byId.id) shouldBe true
    }
  }

  @Test
  fun testWaitForAction() {
    runBlocking {
      val name = UUID.randomUUID().toString()
      val volume = api.volumes.create(VolumeCreateRequest(name, 12, "nbg1", VolumeFormat.ext4))

      val result =
          api.waitForAction(
              { api.volumes.changeDeleteProtection(volume!!.volume.id, true) },
              { api.volumes.action(it) },
          )

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

      api.sshKeys.update(
          keys[0].id,
          SSHKeysUpdateRequest(labels = mapOf("label1" to "foo", "label2" to "bar")),
      )

      val byId = api.sshKeys.get(keys[0].id)!!
      byId.labels shouldHaveSize 2
      byId.labels shouldContain ("label1" to "foo")
      byId.labels shouldContain ("label2" to "bar")

      api.sshKeys.list(labelSelectors = mapOf("label1" to Equals("foo"))) shouldHaveSize 1
      api.sshKeys.list(labelSelectors = mapOf("label1" to Equals("invalid"))) shouldHaveSize 0
      api.sshKeys.list(
          labelSelectors =
              mapOf(
                  "label1" to Equals("foo"),
                  "label2" to Equals("bar"),
              ),
      ) shouldHaveSize 1

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

      api.serverTypes.get("cx23") shouldNotBe null
    }
  }

  @Test
  fun testDnsZones() {
    runBlocking {
      api.dnsZones.get("invalid") shouldBe null

      val byName = api.dnsZones.get("blcks-test.de")!!

      assertSoftly(byName) { it.zone.name shouldBe "blcks-test.de" }

      assertSoftly(api.dnsZones.get(byName.zone.id)!!) { it.zone.name shouldBe "blcks-test.de" }

      val dnsZones = api.dnsZones.list()
      dnsZones shouldHaveSize 1
      dnsZones[0].name shouldBe "blcks-test.de"
    }
  }

  @Test
  fun testListDnsRrSets() {
    runBlocking {
      val dnsZones = api.dnsZones.list()
      dnsZones shouldHaveSize 1

      val rrSetsApi = api.dnsRrSets(dnsZones[0].name)
      rrSetsApi.list() shouldHaveAtLeastSize 1
    }
  }

  @Test
  fun testDnsRRSetFlow() {
    runBlocking {
      val dnsZones = api.dnsZones.list()
      dnsZones shouldHaveSize 1

      val rrSetsApi = api.dnsRrSets(dnsZones[0].name)

      val name = UUID.randomUUID().toString()

      rrSetsApi.get(name, RRType.TXT) shouldBe null
      val response =
          rrSetsApi.create(
              DnsRRSetsCreateRequest(name, RRType.A, listOf(DnsRRSetRecord("127.0.0.1"))),
          )
      response?.rrset?.name shouldBe name

      rrSetsApi.get(name, RRType.TXT) shouldBe null
      assertSoftly(rrSetsApi.get(name, RRType.A)!!) { it.rrset.name shouldBe name }

      rrSetsApi.list().map { it.name } shouldContain name

      rrSetsApi.delete(response!!.rrset.name, response.rrset.type) shouldBe true

      rrSetsApi.list().map { it.name } shouldNotContain name
    }
  }
}
