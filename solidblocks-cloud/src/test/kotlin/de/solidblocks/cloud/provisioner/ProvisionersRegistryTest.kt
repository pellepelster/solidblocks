package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.mock.Resource1
import de.solidblocks.cloud.provisioner.mock.Resource1Generic
import de.solidblocks.cloud.provisioner.mock.Resource1GenericLookup
import de.solidblocks.cloud.provisioner.mock.Resource1Lookup
import de.solidblocks.cloud.provisioner.mock.Resource1Provisioner
import de.solidblocks.cloud.provisioner.mock.Resource1Runtime
import de.solidblocks.cloud.provisioner.mock.Resource2
import de.solidblocks.cloud.provisioner.mock.Resource2Lookup
import de.solidblocks.cloud.provisioner.mock.Resource2Runtime
import de.solidblocks.cloud.utils.Success
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ProvisionersRegistryTest {

    @Test
    fun testNoLookupProviderFound() {
        val registry = ProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))

        val exception = shouldThrow<RuntimeException> {
            registry.lookup(Resource2Lookup("name1"), mockk<SSHProvisionerContext>())
        }
        exception.message shouldBe "expected one lookup but found 0 for 'de.solidblocks.cloud.provisioner.mock.Resource2Lookup' (<none>)"
    }

    @Test
    fun testNoProvisionerFound() {
        runBlocking {
            val registry = ProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))

            val exception = shouldThrow<RuntimeException> {
                registry.apply<Resource2Runtime>(Resource2("name1"), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            }
            exception.message shouldBe "expected one provisioner but found 0 for 'de.solidblocks.cloud.provisioner.mock.Resource2' (<none>)"
        }
    }

    @Test
    fun testApplyGenericResource() {
        runBlocking {
            val registry = ProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))
            val runtime = registry.apply<Resource2Runtime>(Resource1Generic("name1"), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<Resource1Runtime>>()
            runtime.data.name shouldBe "name1"
        }
    }

    @Test
    fun testApply() {
        runBlocking {
            val registry = ProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))
            val runtime = registry.apply<Resource1Runtime>(Resource1("name1"), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeTypeOf<Success<Resource1Runtime>>()
            runtime.data.name shouldBe "name1"
        }
    }

    @Test
    fun testLookup() {
        runBlocking {
            val registry = ProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))
            registry.apply<Resource1Runtime>(Resource1("name1"), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<Resource1Runtime>>()
            val runtime = registry.lookup(Resource1Lookup("name1"), mockk<SSHProvisionerContext>())
            runtime!!.name shouldBe "name1"
        }
    }

    @Test
    fun testLookupGeneric() {
        runBlocking {
            val registry = ProvisionersRegistry(resourceProvisioners = listOf(Resource1Provisioner()))
            registry.apply<Resource1Runtime>(Resource1("name1"), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<Resource1Runtime>>()
            val runtime = registry.lookup(Resource1GenericLookup("name1"), mockk<SSHProvisionerContext>())
            runtime!!.name shouldBe "name1"
        }
    }
}
