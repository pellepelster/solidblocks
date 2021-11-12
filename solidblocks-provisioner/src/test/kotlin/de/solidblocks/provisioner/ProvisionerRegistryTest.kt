package de.solidblocks.provisioner

import de.solidblocks.provisioner.fixtures.TestConfiguration
import de.solidblocks.provisioner.fixtures.TestResource
import de.solidblocks.provisioner.fixtures.TestResourceProvisioner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestConfiguration::class])
class ProvisionerRegistryTest {

    @Autowired
    private lateinit var provisionerRegistry: ProvisionerRegistry

    @Autowired
    private lateinit var testResourceProvisioner: TestResourceProvisioner

    @Test
    fun testSupportsResource() {
        assertTrue(provisionerRegistry.supportsResource(TestResource::class))
        assertFalse(provisionerRegistry.supportsResource(String::class))
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
