package de.solidblocks.provisioner

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.fixtures.TestResource
import de.solidblocks.provisioner.fixtures.TestResourceProvisioner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProvisionerRegistryTest {

    @Test
    fun testSupportsResource() {
        val testResourceProvisioner = TestResourceProvisioner()
        val provisionerRegistry = ProvisionerRegistry()

        provisionerRegistry.addProvisioner(testResourceProvisioner as IInfrastructureResourceProvisioner<Any, Any>)

        setOf(testResourceProvisioner as IResourceLookupProvider<IResourceLookup<Any>, Any>)

        assertThat(provisionerRegistry.supportsResource(TestResource::class)).isTrue
        assertThat(provisionerRegistry.supportsResource(String::class)).isFalse
    }

    @Test
    fun testProvisioner() {
        // assertNotNull(provisionerRegistry.provisioner(TestResource()))
    }

    /*
    @Test
    fun testDiff() {
        val resource = TestResource()
        this.provisionerRegistry.addResource(resource)
        this.provisionerRegistry.diff()
        assertThat(testResourceProvisioner.diffs[0], Is.`is`(resource))
    }*/
}
