package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.HetznerTestContext
import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetwork
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnet
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKey
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.utils.ByteSize
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTest::class)
class HetznerServerProvisionerTest {

    @Test
    fun testFlow(context: SolidblocksTestContext) {
        val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN"))

        runBlocking {
            val name = UUID.randomUUID().toString()

            val testContext = HetznerTestContext.create(System.getenv("HCLOUD_TOKEN"))

            val sshKey1 = HetznerSSHKey(
                "test-key1", "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIE+u0dEVRZDzzp4E1teCqF49r8ig3YEk8eaPqNWfDcPb pelle@fry", hetzner.defaultLabels
            )
            val sshKey2 = HetznerSSHKey(
                "test-key2", "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIE+u0dEVRZDzzp4E1teCqF49r8ig3YEk8eaPqNWfDcPa pelle@fry", hetzner.defaultLabels
            )
            val volume = HetznerVolume(
                name, HetznerLocation.nbg1, ByteSize.fromGigabytes(16), protected = false, labels = hetzner.defaultLabels
            )

            val network = HetznerNetwork(name, "10.0.0.0/8")
            val subnet = HetznerSubnet("10.0.1.0/24", network.asLookup())

            testContext.provisioner.apply(listOf(sshKey1, sshKey2, volume, network, subnet), testContext.context, TEST_LOG_CONTEXT).shouldBeTypeOf<Success<Unit>>()

            val server = HetznerServer(
                name,
                HetznerLocation.nbg1,
                HetznerServerType.cx23,
                sshKeys = setOf(sshKey1.asLookup()),
                volumes = setOf(volume.asLookup()),
                userData = UserData(emptySet(), { "" }),
                labels = hetzner.defaultLabels
            )

            runBlocking {
                testContext.serverProvisioner.lookup(server.asLookup(), testContext.context) shouldBe null
                assertSoftly(testContext.serverProvisioner.diff(server, testContext.context)!!) {
                    it.status shouldBe ResourceDiffStatus.missing
                }

                testContext.serverProvisioner.apply(server, testContext.context, TEST_LOG_CONTEXT) shouldNotBe null

                assertSoftly(testContext.serverProvisioner.diff(server, testContext.context)!!) {
                    it.status shouldBe ResourceDiffStatus.up_to_date
                }

                assertSoftly(
                    testContext.serverProvisioner.diff(
                        HetznerServer(
                            name,
                            HetznerLocation.nbg1,
                            HetznerServerType.cx23,
                            sshKeys = setOf(sshKey1.asLookup()),
                            volumes = setOf(volume.asLookup()),
                            userData = UserData(emptySet(), { "" }),
                            image = "debian-99",
                            labels = hetzner.defaultLabels
                        ),
                        testContext.context,
                    )!!,
                ) {
                    it.status shouldBe ResourceDiffStatus.has_changes
                    it.needsRecreate() shouldBe true
                    it.changes shouldHaveSize 1
                    it.changes[0].name shouldBe "image"
                    it.changes[0].expectedValue shouldBe "debian-99"
                    it.changes[0].actualValue shouldBe "debian-12"
                }

                assertSoftly(
                    testContext.serverProvisioner.diff(
                        HetznerServer(
                            name,
                            HetznerLocation.nbg1,
                            HetznerServerType.cx23,
                            sshKeys = setOf(sshKey1.asLookup(), sshKey2.asLookup()),
                            volumes = setOf(volume.asLookup()),
                            userData = UserData(emptySet(), { "" }),
                            labels = hetzner.defaultLabels
                        ),
                        testContext.context,
                    )!!,
                ) {
                    it.status shouldBe ResourceDiffStatus.has_changes
                    it.needsRecreate() shouldBe true
                    it.changes shouldHaveSize 1
                    it.changes[0].name shouldBe "sshKeys"
                    it.changes[0].expectedValue shouldBe "c5383689b3a31bbd0fafbe5c62865ae53a9da27adb1f96b1d94e358f83c9bab6"
                    it.changes[0].actualValue shouldBe "b500b5a03c1ddbd8b64d477d406ab9a11f985ad382d14fefa45541094599fcb9"
                }

                assertSoftly(testContext.serverProvisioner.lookup(server.asLookup(), testContext.context)) {
                    it!!.name shouldBe name
                }

                val serverWithPrivateNetwork = HetznerServer(
                    name,
                    HetznerLocation.nbg1,
                    HetznerServerType.cx23,
                    sshKeys = setOf(sshKey1.asLookup()),
                    volumes = setOf(volume.asLookup()),
                    userData = UserData(emptySet(), { "" }),
                    labels = hetzner.defaultLabels,
                    subnet = subnet.asLookup(),
                    privateIp = "10.0.1.1"
                )

                assertSoftly(
                    testContext.serverProvisioner.diff(serverWithPrivateNetwork, testContext.context)!!,
                ) {
                    it.status shouldBe ResourceDiffStatus.has_changes
                    it.changes shouldHaveSize 1
                    it.changes[0].name shouldBe "private ip address"
                    it.changes[0].expectedValue shouldBe "10.0.1.1"
                    it.changes[0].actualValue shouldBe null
                }

                testContext.serverProvisioner.apply(serverWithPrivateNetwork, testContext.context, TEST_LOG_CONTEXT) shouldNotBe null

                assertSoftly(testContext.serverProvisioner.lookup(serverWithPrivateNetwork.asLookup(), testContext.context)!!) {
                    it.privateIpv4 shouldBe "10.0.1.1"
                }

                testContext.serverProvisioner.destroy(server, testContext.context, TEST_LOG_CONTEXT)
            }
        }
    }
}
