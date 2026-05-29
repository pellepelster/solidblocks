package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.mock.Resource1
import de.solidblocks.cloud.provisioner.mock.Resource1Provisioner
import de.solidblocks.cloud.provisioner.mock.Resource1Runtime
import de.solidblocks.cloud.provisioner.mock.Resource2
import de.solidblocks.cloud.provisioner.mock.Resource2Lookup
import de.solidblocks.cloud.provisioner.mock.Resource2Runtime
import de.solidblocks.cloud.utils.Success
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ProvisionersRegistryTest {

    @Test
    fun testNoLookupProvider() {
        val registry = ProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))

        val exception = shouldThrow<RuntimeException> {
            registry.lookup(Resource2Lookup("name1"), mockk<SSHProvisionerContext>())
        }
        exception.message shouldBe "no lookup provider found for 'de.solidblocks.cloud.provisioner.mock.Resource2Lookup'"
    }

    @Test
    fun testNoProvisioner() {
        runBlocking {
            val registry = ProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))

            val exception = shouldThrow<RuntimeException> {
                registry.apply<Resource2, Resource2Runtime>(Resource2("name1"), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            }
            exception.message shouldBe "no or more than one (0) provisioner found for 'de.solidblocks.cloud.provisioner.mock.Resource2'"
        }
    }

    @Test
    fun testProvisionerApply() {
        runBlocking {
            val registry = ProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))
            val runtime =   registry.apply<Resource1, Resource1Runtime>(Resource1("name1"), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeTypeOf<Success<Resource1Runtime>>()
            runtime.data.name shouldBe "name1"
        }
    }
}