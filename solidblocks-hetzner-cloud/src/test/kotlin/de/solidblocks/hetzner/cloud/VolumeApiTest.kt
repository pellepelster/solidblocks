package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import de.solidblocks.hetzner.cloud.resources.VolumeCreateRequest
import de.solidblocks.hetzner.cloud.resources.VolumeFormat
import de.solidblocks.hetzner.cloud.resources.VolumeNameFilter
import de.solidblocks.hetzner.cloud.resources.VolumeStatus
import de.solidblocks.hetzner.cloud.resources.VolumeStatusFilter
import de.solidblocks.hetzner.cloud.resources.VolumeUpdateRequest
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeOneOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VolumeApiTest {
    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
    val api = HetznerApi(hcloudToken)
    val testLabels = mapOf("blcks.de/managed-by" to "test")

    fun cleanup() {
        runBlocking {
            api.volumes.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                if (it.protection.delete) {
                    val action = api.volumes.changeDeleteProtection(it.id, false)
                    api.volumes.waitForAction(action)
                }
                if (it.server != null) {
                    val action = api.volumes.detach(it.id)
                    api.volumes.waitForAction(action)
                }
                api.volumes.delete(it.id)
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
    fun testVolumesFlow() {
        runBlocking {
            val name = "test-volume"

            val created = api.volumes.create(
                VolumeCreateRequest(
                    name = name,
                    size = 10,
                    location = HetznerLocation.fsn1,
                    format = VolumeFormat.ext4,
                    labels = testLabels,
                    automount = false,
                ),
            )
            created.volume shouldNotBe null
            created.volume.name shouldBe name
            created.volume.size shouldBe 10
            created.volume.status shouldBeOneOf listOf(VolumeStatus.creating, VolumeStatus.creating)
            created.volume.labels shouldBe testLabels

            val byId = api.volumes.get(created.volume.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.volumes.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.volume.id

            api.volumes.list(listOf(VolumeNameFilter(name))) shouldHaveSize 1

            val availableVolumes = api.volumes.list(listOf(VolumeStatusFilter(VolumeStatus.available)), labelSelectors = testLabels.toLabelSelectors())
            availableVolumes.any { it.id == created.volume.id } shouldBe true

            val updated = api.volumes.update(created.volume.id, VolumeUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated.volume.labels["extra"] shouldBe "value"

            val listed = api.volumes.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1

            api.volumes.delete(created.volume.id) shouldBe true

            api.volumes.get(created.volume.id) shouldBe null
        }
    }

    @Test
    fun testVolumeAttachAndResize() {
        runBlocking {
            val server = api.servers.create(
                ServerCreateRequest(
                    name = "test-server-for-volume",
                    location = HetznerLocation.fsn1,
                    type = HetznerServerType.cx23,
                    image = "ubuntu-22.04",
                    labels = testLabels,
                ),
            )
            api.servers.waitForAction(server.action)

            val volume = api.volumes.create(
                VolumeCreateRequest(
                    name = "test-volume-attach",
                    size = 10,
                    location = HetznerLocation.fsn1,
                    format = VolumeFormat.ext4,
                    labels = testLabels,
                    automount = false,
                ),
            )

            val attachAction = api.volumes.attach(volume.volume.id, server.server.id)
            api.volumes.waitForAction(attachAction)

            val attached = api.volumes.get(volume.volume.id)
            attached!!.server shouldBe server.server.id

            val resizeAction = api.volumes.resize(volume.volume.id, 20)
            api.volumes.waitForAction(resizeAction)

            val resized = api.volumes.get(volume.volume.id)
            resized!!.size shouldBe 20

            val detachAction = api.volumes.detach(volume.volume.id)
            api.volumes.waitForAction(detachAction)

            val detached = api.volumes.get(volume.volume.id)
            detached!!.server shouldBe null

            api.volumes.delete(volume.volume.id) shouldBe true
            api.servers.delete(server.server.id)
        }
    }

    @Test
    fun testWaitForAction() {
        runBlocking {
            val volume = api.volumes.create(
                VolumeCreateRequest(
                    name = "test-volume-wait-for-action",
                    size = 10,
                    location = HetznerLocation.fsn1,
                    format = VolumeFormat.ext4,
                    labels = testLabels,
                    automount = false,
                ),
            )

            val protectAction = api.volumes.changeDeleteProtection(volume.volume.id, true)
            api.volumes.waitForAction(protectAction)

            val protected = api.volumes.get(volume.volume.id)
            protected!!.protection.delete shouldBe true

            val unprotectAction = api.volumes.changeDeleteProtection(volume.volume.id, false)
            api.volumes.waitForAction(unprotectAction)

            api.volumes.delete(volume.volume.id) shouldBe true
        }
    }
}
