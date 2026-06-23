package de.solidblocks.provisioner

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffStatus
import de.solidblocks.provisioner.mock.ApplyBehaviour
import de.solidblocks.provisioner.mock.DiffBehaviour
import de.solidblocks.provisioner.mock.Resource1
import de.solidblocks.provisioner.mock.Resource1ListProvisioner
import de.solidblocks.provisioner.mock.Resource1Lookup
import de.solidblocks.provisioner.mock.Resource1Provisioner
import de.solidblocks.provisioner.mock.Resource1Runtime
import de.solidblocks.provisioner.mock.Resource2
import de.solidblocks.provisioner.mock.Resource2Lookup
import de.solidblocks.provisioner.mock.Resource2Provisioner
import de.solidblocks.provisioner.mock.Resource2Runtime
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class BaseProvisionersRegistryTest {

    private val diffContext = TestDiffContext()
    private val applyContext = TestApplyContext()
    private val destroyContext = TestDestroyContext()
    private val lookupContext = TestLookupContext()

    // apply ----------------------------------------------------------------

    @Test
    fun `apply resolves the provisioner by resource type and returns the runtime`() = runBlocking<Unit> {
        val provisioner = Resource2Provisioner()
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(provisioner))

        val result = registry.apply(Resource2("a", DiffBehaviour.up_to_date_or_missing), applyContext)

        result.shouldBeTypeOf<Success<Resource2Runtime>>().data shouldBe Resource2Runtime("a")
        provisioner.isApplied("a") shouldBe true
    }

    @Test
    fun `apply returns the error from the provisioner`() = runBlocking<Unit> {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner()))

        val result = registry.apply(
            Resource2("a", DiffBehaviour.up_to_date_or_missing, applyBehaviour = ApplyBehaviour.error_on_apply),
            applyContext,
        )

        result.shouldBeTypeOf<Error<Resource2Runtime>>()
    }

    @Test
    fun `apply propagates exceptions thrown by the provisioner`() {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner()))

        shouldThrow<RuntimeException> {
            runBlocking {
                registry.apply(
                    Resource2("a", DiffBehaviour.up_to_date_or_missing, applyBehaviour = ApplyBehaviour.throw_exception_on_apply),
                    applyContext,
                )
            }
        }
    }

    @Test
    fun `apply throws when no provisioner is registered for the resource`() {
        val registry = TestProvisionersRegistry()

        shouldThrow<RuntimeException> {
            runBlocking { registry.apply(Resource2("a", DiffBehaviour.up_to_date_or_missing), applyContext) }
        }
    }

    // diff -----------------------------------------------------------------

    @Test
    fun `diff returns up_to_date when the resource exists and missing otherwise`() = runBlocking<Unit> {
        val provisioner = Resource2Provisioner()
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(provisioner))
        val resource = Resource2("a", DiffBehaviour.up_to_date_or_missing)

        (registry.diff(resource, diffContext).shouldBeTypeOf<Success<ResourceDiff>>().data).status shouldBe ResourceDiffStatus.missing

        registry.apply(resource, applyContext)

        (registry.diff(resource, diffContext).shouldBeTypeOf<Success<ResourceDiff>>().data).status shouldBe ResourceDiffStatus.up_to_date
    }

    @Test
    fun `diff returns the error from the provisioner`() = runBlocking<Unit> {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner()))

        registry.diff(Resource2("a", DiffBehaviour.error_on_diff), diffContext).shouldBeTypeOf<Error<*>>()
    }

    @Test
    fun `diff reports unknown status`() = runBlocking<Unit> {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner()))

        val diff = registry.diff(Resource2("a", DiffBehaviour.unknown_on_diff), diffContext).shouldBeTypeOf<Success<ResourceDiff>>().data
        diff.status shouldBe ResourceDiffStatus.unknown
    }

    @Test
    fun `diff reports duplicate status with message`() = runBlocking<Unit> {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner()))

        val diff = registry.diff(Resource2("a", DiffBehaviour.duplicate_on_diff), diffContext)
            .shouldBeTypeOf<Success<ResourceDiff>>().data
        diff.status shouldBe ResourceDiffStatus.duplicate
        (diff.duplicateErrorMessage != null) shouldBe true
    }

    @Test
    fun `diff reports changes that require recreate`() = runBlocking<Unit> {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner()))

        val diff = registry.diff(Resource2("a", DiffBehaviour.force_recreate_change), diffContext)
            .shouldBeTypeOf<Success<ResourceDiff>>().data
        diff.status shouldBe ResourceDiffStatus.has_changes
        diff.needsRecreate() shouldBe true
    }

    @Test
    fun `diff propagates exceptions thrown by the provisioner`() {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner()))

        shouldThrow<RuntimeException> {
            runBlocking { registry.diff(Resource2("a", DiffBehaviour.throw_exception_on_diff), diffContext) }
        }
    }

    @Test
    fun `diff throws when no provisioner is registered`() {
        val registry = TestProvisionersRegistry()

        shouldThrow<RuntimeException> {
            runBlocking { registry.diff(Resource2("a", DiffBehaviour.up_to_date_or_missing), diffContext) }
        }
    }

    // destroy --------------------------------------------------------------

    @Test
    fun `destroy delegates to a destroyable provisioner and returns its result`() = runBlocking<Unit> {
        val provisioner = Resource2Provisioner()
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(provisioner))

        registry.destroy(Resource2Lookup("a"), destroyContext) shouldBe true
        provisioner.isDestroyed("a") shouldBe true
    }

    @Test
    fun `destroy returns false when the provisioner reports failure`() = runBlocking<Unit> {
        val provisioner = Resource2Provisioner().apply { destroyResult = false }
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(provisioner))

        registry.destroy(Resource2Lookup("a"), destroyContext) shouldBe false
    }

    @Test
    fun `destroy returns false when the provisioner does not support destroy`() = runBlocking<Unit> {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))

        registry.destroy(Resource1Lookup("a"), destroyContext) shouldBe false
    }

    @Test
    fun `destroy throws when no provisioner is registered for the lookup`() {
        val registry = TestProvisionersRegistry()

        shouldThrow<RuntimeException> {
            runBlocking { registry.destroy(Resource2Lookup("a"), destroyContext) }
        }
    }

    // lookup ---------------------------------------------------------------

    @Test
    fun `lookup returns the runtime when the resource exists`() = runBlocking<Unit> {
        val provisioner = Resource2Provisioner()
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(provisioner))
        registry.apply(Resource2("a", DiffBehaviour.up_to_date_or_missing), applyContext)

        registry.lookup(Resource2Lookup("a"), lookupContext) shouldBe Resource2Runtime("a")
    }

    @Test
    fun `lookup returns null when the resource does not exist`() {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner()))

        registry.lookup(Resource2Lookup("a"), lookupContext) shouldBe null
    }

    @Test
    fun `lookup resolves a standalone lookup provider`() {
        val provider = Resource1Provisioner().apply { resources["a"] = Resource1("a") }
        val registry = TestProvisionersRegistry(resourceLookupProviders = listOf(provider))

        registry.lookup(Resource1Lookup("a"), lookupContext)?.name shouldBe "a"
        registry.lookup(Resource1Lookup("b"), lookupContext) shouldBe null
    }

    @Test
    fun `lookup throws when no lookup provider is registered`() {
        val registry = TestProvisionersRegistry()

        shouldThrow<RuntimeException> {
            registry.lookup(Resource2Lookup("a"), lookupContext)
        }
    }

    // list -----------------------------------------------------------------

    @Test
    fun `list throws when no lookup provider is registered for the type`() {
        val registry = TestProvisionersRegistry()

        shouldThrow<RuntimeException> {
            runBlocking { registry.list<Resource2Lookup, Resource2Runtime>(Resource2Lookup::class) }
        }
    }

    @Test
    fun `list throws when the lookup provider does not support list`() {
        val registry = TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner()))

        shouldThrow<RuntimeException> {
            runBlocking { registry.list<Resource2Lookup, Resource2Runtime>(Resource2Lookup::class) }
        }
    }

    @Test
    fun `list returns the items from a listable provider`() = runBlocking<Unit> {
        val provider = Resource1ListProvisioner(listOf(Resource1Lookup("a"), Resource1Lookup("b")))
        val registry = TestProvisionersRegistry(resourceLookupProviders = listOf(provider))

        val items = registry.list<Resource1Lookup, Resource1Runtime>(Resource1Lookup::class)
        items.map { it.name } shouldBe listOf("a", "b")
    }
}
