package de.solidblocks.provisioner

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.fixtures.TestResource
import de.solidblocks.provisioner.fixtures.TestResourceProvisioner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
        provisioner.clear()
        testResourceProvisioner.failOnLookup(resource)

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
        val resource2 = TestResource(UUID.randomUUID(), listOf(resource1))

        testResourceProvisioner.reset()
        provisioner.clear()
        testResourceProvisioner.diffIsMissing(resource1)

        val layer = provisioner.createResourceGroup("layer1")
        layer.addResource(resource1)
        layer.addResource(resource2)

        assertThat(provisioner.apply()).isTrue
        assertThat(testResourceProvisioner.applyCount(resource2)).isEqualTo(1)
    }

    @Test
    fun diffIsOmittedWhenParentsAreMissing() {
        val testResourceProvisioner = TestResourceProvisioner()

        val provisionerRegistry = ProvisionerRegistry()
        provisionerRegistry.addProvisioner(testResourceProvisioner as IInfrastructureResourceProvisioner<Any, Any>)
        provisionerRegistry.addLookupProvider(testResourceProvisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)

        val provisioner = Provisioner(provisionerRegistry)

        val resource1 = TestResource(UUID.randomUUID())
        val resource2 = TestResource(UUID.randomUUID(), listOf(resource1))

        testResourceProvisioner.reset()
        provisioner.clear()
        testResourceProvisioner.diffIsMissing(resource1)

        val layer = provisioner.createResourceGroup("layer1")
        layer.addResource(resource1)
        layer.addResource(resource2)

        assertThat(provisioner.apply()).isTrue
        assertThat(testResourceProvisioner.diffCount(resource2)).isEqualTo(0)
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
        provisioner.clear()
        testResourceProvisioner.failOnDiff(resource)

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
        provisioner.clear()
        testResourceProvisioner.failOnApply(resource)
        testResourceProvisioner.diffIsMissing(resource)

        provisioner.createResourceGroup("layer1").addResource(resource)

        val result = provisioner.apply()

        assertThat(result).isFalse
        assertThat(testResourceProvisioner.applyCount(resource)).isEqualTo(1)
    }
}
