package de.solidblocks.cloud.model

import de.solidblocks.test.SolidblocksTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTestDatabaseExtension::class)
class ServiceRepositoryTest {

    @Test
    fun testCreateService(solidblocksDatabase: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(solidblocksDatabase.dsl)
        val environmentRepository = EnvironmentRepository(solidblocksDatabase.dsl, cloudRepository)

        val cloud = cloudRepository.createCloud("cloud1", "domain1")
        assertThat(environmentRepository.createEnvironment(cloud.name, "env1")).isNotNull

        val serviceRepository = ServiceRepository(solidblocksDatabase.dsl, environmentRepository)

        assertThat(serviceRepository.hasService("cloud1", "env1", "service1")).isFalse
        serviceRepository.createService("cloud1", "env1", "service1")
        assertThat(serviceRepository.hasService("cloud1", "env1", "service1")).isTrue

        val service1 = serviceRepository.getService("cloud1", "env1", "service1")
        assertThat(service1!!.configValues).isEmpty()

        serviceRepository.createService("cloud1", "env1", "service2", mapOf("foo" to "bar"))
        val service2 = serviceRepository.getService("cloud1", "env1", "service2")
        assertThat(service2!!.configValues).hasSize(1)
        assertThat(service2.configValues.first { it.name == "foo" }.value).isEqualTo("bar")
    }
}
