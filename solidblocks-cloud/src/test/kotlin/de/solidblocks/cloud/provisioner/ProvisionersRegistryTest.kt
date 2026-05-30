package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.provisioner.mock.Resource1
import de.solidblocks.cloud.provisioner.mock.Resource1Provisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class ProvisionersRegistryTest {

    private val resourceGroups = listOf(ResourceGroup("common", listOf(Resource1("test1"))))

    @Test
    fun testValidateWiringFailsWhenNoProvisionerIsRegistered() {
        ProvisionersRegistry(emptyList(), emptyList())
            .validateWiring(resourceGroups)
            .shouldBeTypeOf<Error<Unit>>()
    }

    @Test
    fun testValidateWiringSucceedsWhenProvisionerIsRegistered() {
        ProvisionersRegistry(emptyList(), listOf(Resource1Provisioner()))
            .validateWiring(resourceGroups)
            .shouldBeTypeOf<Success<Unit>>()
    }
}
