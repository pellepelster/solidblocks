package de.solidblocks.cloud.model

import de.solidblocks.base.ServiceReference
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

        val service1Ref = ServiceReference("cloud1", "env1", "service1")

        cloudRepository.createCloud(service1Ref.toCloud(), "domain1")
        assertThat(environmentRepository.createEnvironment(service1Ref.toEnvironment())).isNotNull

        val serviceRepository = ServiceRepository(solidblocksDatabase.dsl, environmentRepository)

        assertThat(serviceRepository.hasService(service1Ref)).isFalse
        serviceRepository.createService(service1Ref)
        assertThat(serviceRepository.hasService(service1Ref)).isTrue

        val service1 = serviceRepository.getService(service1Ref)
        assertThat(service1!!.configValues).isEmpty()

        val service2Ref = ServiceReference("cloud1", "env1", "service2")

        serviceRepository.createService(service2Ref, mapOf("foo" to "bar"))
        val service2 = serviceRepository.getService(service2Ref)
        assertThat(service2!!.configValues).hasSize(1)
        assertThat(service2.configValues.first { it.name == "foo" }.value).isEqualTo("bar")
    }
}
