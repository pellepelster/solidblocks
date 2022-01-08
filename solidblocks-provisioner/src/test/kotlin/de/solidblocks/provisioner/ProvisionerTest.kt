package de.solidblocks.provisioner

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.fixtures.TestResource
import de.solidblocks.provisioner.fixtures.TestResourceProvisioner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class ProvisionerTest {

    @Test
    fun handlesFailingResourceLookups() {

        val testResourceProvisioner = TestResourceProvisioner()

        val provisionerRegistry = ProvisionerRegistry()
        provisionerRegistry.addProvisioner(testResourceProvisioner as IInfrastructureResourceProvisioner<Any, Any>)
        provisionerRegistry.addLookupProvider(testResourceProvisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)

        val provisioner = Provisioner(provisionerRegistry)

        val resource = TestResource(UUID.randomUUID())

        testResourceProvisioner.reset()
        testResourceProvisioner.mockFailOnLookupFor(resource)

        val result = provisioner.lookup(resource)

        assertThat(result.isEmptyOrFailed()).isTrue
        assertThat(testResourceProvisioner.lookupCount(resource)).isEqualTo(1)
    }

    @Test
    fun resourcesAreAppliedWhenParentChanges() {
        val testResourceProvisioner = TestResourceProvisioner()

        val provisionerRegistry = ProvisionerRegistry()
        provisionerRegistry.addProvisioner(testResourceProvisioner as IInfrastructureResourceProvisioner<Any, Any>)
        provisionerRegistry.addLookupProvider(testResourceProvisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)

        val provisioner = Provisioner(provisionerRegistry)

        val resource1 = TestResource(UUID.randomUUID())
        val resource2 = TestResource(UUID.randomUUID(), setOf(resource1))

        testResourceProvisioner.reset()
        testResourceProvisioner.mockMissingDiffFor(resource1)

        val layer = provisioner.createResourceGroup("layer1")
        layer.addResource(resource1)
        layer.addResource(resource2)

        assertThat(provisioner.apply()).isTrue
        assertThat(testResourceProvisioner.applyCount(resource2)).isEqualTo(1)
    }

    @Test
    fun resourcesAreNotTouchedIfPreviousLayerHasUnhealthyResources() {
        val testResourceProvisioner = TestResourceProvisioner()

        val provisionerRegistry = ProvisionerRegistry()
        provisionerRegistry.addProvisioner(testResourceProvisioner as IInfrastructureResourceProvisioner<Any, Any>)
        provisionerRegistry.addLookupProvider(testResourceProvisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)

        val provisioner = Provisioner(provisionerRegistry, healthCheckWait = Duration.ofMillis(10))

        val resource1 = TestResource(UUID.randomUUID(), healthy = false, hasChanges = true)
        val resource2 = TestResource(UUID.randomUUID())

        provisioner.createResourceGroup("layer1").addResource(resource1)
        provisioner.createResourceGroup("layer2").addResource(resource2)

        assertThat(provisioner.apply()).isFalse
        assertThat(testResourceProvisioner.applyCount(resource1)).isEqualTo(1)
        assertThat(testResourceProvisioner.noInteractions(resource2)).isTrue
    }

    @Test
    fun applyFailsWhenHealthCheckFails() {
        val testResourceProvisioner = TestResourceProvisioner()

        val provisionerRegistry = ProvisionerRegistry()
        provisionerRegistry.addProvisioner(testResourceProvisioner as IInfrastructureResourceProvisioner<Any, Any>)
        provisionerRegistry.addLookupProvider(testResourceProvisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)

        val provisioner = Provisioner(provisionerRegistry, healthCheckWait = Duration.ofMillis(10))

        val resource1 = TestResource(UUID.randomUUID(), healthy = false, hasChanges = true)
        provisioner.createResourceGroup("layer1").addResource(resource1)

        assertThat(provisioner.apply()).isFalse
    }

    @Test
    fun resourcesForGroupAreNotTouchedWhenParentsIsUnhealthy() {
        val testResourceProvisioner = TestResourceProvisioner()

        val provisionerRegistry = ProvisionerRegistry()
        provisionerRegistry.addProvisioner(testResourceProvisioner as IInfrastructureResourceProvisioner<Any, Any>)
        provisionerRegistry.addLookupProvider(testResourceProvisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)

        val provisioner = Provisioner(provisionerRegistry, healthCheckWait = Duration.ofMillis(10))

        val resource1 = TestResource(UUID.randomUUID(), healthy = false)
        val resource2 = TestResource(UUID.randomUUID(), setOf(resource1))

        testResourceProvisioner.reset()
        testResourceProvisioner.mockMissingDiffFor(resource1)

        provisioner.createResourceGroup("layer1").addResource(resource1)
        provisioner.createResourceGroup("layer2").addResource(resource2)

        assertThat(provisioner.apply()).isFalse
        assertThat(testResourceProvisioner.diffCount(resource1)).isEqualTo(1)
        assertThat(testResourceProvisioner.noInteractions(resource2)).isTrue
    }

    @Test
    fun handlesFailingResourceDiffs() {
        val testResourceProvisioner = TestResourceProvisioner()

        val provisionerRegistry = ProvisionerRegistry()
        provisionerRegistry.addProvisioner(testResourceProvisioner as IInfrastructureResourceProvisioner<Any, Any>)
        provisionerRegistry.addLookupProvider(testResourceProvisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)

        val provisioner = Provisioner(provisionerRegistry)

        val resource = TestResource(UUID.randomUUID())

        testResourceProvisioner.reset()
        testResourceProvisioner.mockFailOnDiffFor(resource)

        provisioner.createResourceGroup("layer1").addResource(resource)

        val result = provisioner.apply()

        assertThat(result).isFalse
        assertThat(testResourceProvisioner.diffCount(resource)).isEqualTo(1)
    }

    @Test
    fun handlesFailingResourceApply() {
        val testResourceProvisioner = TestResourceProvisioner()

        val provisionerRegistry = ProvisionerRegistry()
        provisionerRegistry.addProvisioner(testResourceProvisioner as IInfrastructureResourceProvisioner<Any, Any>)
        provisionerRegistry.addLookupProvider(testResourceProvisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)

        val provisioner = Provisioner(provisionerRegistry)

        val resource = TestResource(UUID.randomUUID())

        testResourceProvisioner.reset()
        testResourceProvisioner.mockFailOnApplyFor(resource)
        testResourceProvisioner.mockMissingDiffFor(resource)

        provisioner.createResourceGroup("layer1").addResource(resource)

        val result = provisioner.apply()

        assertThat(result).isFalse
        assertThat(testResourceProvisioner.applyCount(resource)).isEqualTo(1)
    }
}
