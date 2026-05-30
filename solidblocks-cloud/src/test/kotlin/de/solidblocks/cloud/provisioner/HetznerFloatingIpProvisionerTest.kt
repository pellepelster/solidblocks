package de.solidblocks.cloud.provisioner
import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.diffData
import de.solidblocks.cloud.provisioner.hetzner.cloud.floatingip.HetznerFloatingIp
import de.solidblocks.cloud.provisioner.hetzner.cloud.floatingip.HetznerFloatingIpProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.floatingip.HetznerFloatingIpRuntime
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.resources.FloatingIpType
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTest::class)
class HetznerFloatingIpProvisionerTest {

    @Test
    fun testFlow(context: SolidblocksTestContext) {
        val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN"))

        val name = UUID.randomUUID().toString()
        val resource =
            HetznerFloatingIp(name, FloatingIpType.ipv4, HetznerLocation.nbg1, hetzner.defaultLabels)
        val provisioner = HetznerFloatingIpProvisioner(System.getenv("HCLOUD_TOKEN"))

        runBlocking {
            // before create
            provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)?.name shouldBe null
            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.missing
                it.changes.shouldBeEmpty()
            }

            // create
            provisioner
                .apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<HetznerFloatingIpRuntime>>()
                .data
                .name shouldBe name
            assertSoftly(provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)!!) {
                it.type shouldBe FloatingIpType.ipv4
                it.deleteProtected shouldBe true
                it.ip shouldNotBe null
            }
            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            // changing the type should trigger recreate
            val resourceWithDifferentType =
                HetznerFloatingIp(name, FloatingIpType.ipv6, HetznerLocation.nbg1, hetzner.defaultLabels)
            assertSoftly(provisioner.diff(resourceWithDifferentType, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].name shouldBe "type"
                it.changes[0].triggersRecreate shouldBe true
                it.changes[0].expectedValue shouldBe FloatingIpType.ipv6.toString()
                it.changes[0].actualValue shouldBe FloatingIpType.ipv4.toString()
            }

            // create new label
            val resourceWithNewLabel =
                HetznerFloatingIp(
                    name,
                    FloatingIpType.ipv4,
                    HetznerLocation.nbg1,
                    hetzner.defaultLabels + mapOf("foo" to "bar"),
                )
            assertSoftly(provisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].missing shouldBe true
                it.changes[0].name shouldBe "label 'foo'"
            }
            provisioner
                .apply(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<HetznerFloatingIpRuntime>>()
                .data
                .name shouldBe name
            assertSoftly(provisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            // update labels
            val resourceWithUpdatedLabel =
                HetznerFloatingIp(
                    name,
                    FloatingIpType.ipv4,
                    HetznerLocation.nbg1,
                    hetzner.defaultLabels + mapOf("foo" to "bar2"),
                )
            assertSoftly(provisioner.diff(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].name shouldBe "label 'foo'"
                it.changes[0].expectedValue shouldBe "bar2"
                it.changes[0].actualValue shouldBe "bar"
            }
            provisioner
                .apply(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<HetznerFloatingIpRuntime>>()
                .data
                .name shouldBe name
            assertSoftly(provisioner.diff(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            // update delete protection
            val resourceWithNewDeleteProtection =
                HetznerFloatingIp(
                    name,
                    FloatingIpType.ipv4,
                    HetznerLocation.nbg1,
                    hetzner.defaultLabels + mapOf("foo" to "bar2"),
                    protected = false,
                )
            assertSoftly(provisioner.diff(resourceWithNewDeleteProtection, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].name shouldBe "delete protection"
                it.changes[0].expectedValue shouldBe "false"
                it.changes[0].actualValue shouldBe "true"
            }
            provisioner
                .apply(resourceWithNewDeleteProtection, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<HetznerFloatingIpRuntime>>()
                .data
                .name shouldBe name
            assertSoftly(provisioner.diff(resourceWithNewDeleteProtection, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            // delete
            provisioner.destroy(resourceWithNewDeleteProtection.asLookup(), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
        }
    }
}
