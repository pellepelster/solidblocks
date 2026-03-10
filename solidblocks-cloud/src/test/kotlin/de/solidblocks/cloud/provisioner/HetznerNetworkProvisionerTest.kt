package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetwork
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkProvisioner
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

class HetznerNetworkProvisionerTest {

    @Test
    fun testFlow() {

        val name = UUID.randomUUID().toString()
        val resource = HetznerNetwork(name, "10.0.0.0/24")
        val provisioner = HetznerNetworkProvisioner(System.getenv("HCLOUD_TOKEN"))

        runBlocking {
            // before create
            provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)?.name shouldBe null
            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)!!) {
                it.status shouldBe ResourceDiffStatus.missing
                it.changes.shouldBeEmpty()
            }

            // create
            provisioner.apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).runtime?.name shouldBe name
            assertSoftly(provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)!!) {
                it.deleteProtected shouldBe true
            }
            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)!!) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            // create new label
            val resourceWithNewLabel = HetznerNetwork(name, "10.0.0.0/24", mapOf("foo" to "bar"))
            assertSoftly(provisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT)!!) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].missing shouldBe true
                it.changes[0].name shouldBe "label 'foo'"
            }
            provisioner.apply(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).runtime?.name shouldBe name
            assertSoftly(provisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT)!!) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)!!) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].changed shouldBe true
                it.changes[0].name shouldBe "label 'foo'"
            }
            provisioner.apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).runtime?.name shouldBe name


            // update delete protection
            val resourceWithNewDeleteProtection = HetznerNetwork(name, "10.0.0.0/24", protected = false)
            assertSoftly(provisioner.diff(resourceWithNewDeleteProtection, TEST_PROVISIONER_CONTEXT)!!) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].name shouldBe "delete protection"
                it.changes[0].expectedValue shouldBe "false"
                it.changes[0].actualValue shouldBe "true"
            }

            provisioner.apply(resourceWithNewDeleteProtection, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).runtime?.name shouldBe name
            assertSoftly(provisioner.diff(resourceWithNewDeleteProtection, TEST_PROVISIONER_CONTEXT)!!) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }
        }
    }
}
