package de.solidblocks.cloud.model

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.ServiceReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.base.resources.parsePermissions
import de.solidblocks.test.SolidblocksTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTestDatabaseExtension::class)
class ServiceRepositoryTest {

    @Test
    fun testPermissions(database: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(database.dsl)
        val environmentRepository = EnvironmentRepository(database.dsl, cloudRepository)
        val tenantRepository = TenantRepository(database.dsl, environmentRepository)
        val repository = ServiceRepository(database.dsl, tenantRepository)

        val cloud1 = UUID.randomUUID().toString()
        val env1 = UUID.randomUUID().toString()
        val tenant1 = UUID.randomUUID().toString()

        val cloud2 = UUID.randomUUID().toString()
        val env2 = UUID.randomUUID().toString()
        val tenant2 = UUID.randomUUID().toString()

        cloudRepository.createCloud(cloud1, "domain1")
        cloudRepository.createCloud(cloud2, "domain2")
        environmentRepository.createEnvironment(CloudReference(cloud1), env1)
        environmentRepository.createEnvironment(CloudReference(cloud2), env1)

        assertThat(tenantRepository.createTenant(EnvironmentReference(cloud1, env1), tenant1, "<none>")).isNotNull
        assertThat(tenantRepository.createTenant(EnvironmentReference(cloud1, env1), tenant2, "<none>")).isNotNull

        assertThat(repository.createService(TenantReference(cloud1, env1, tenant1), "service1", "<none>")).isNotNull
        assertThat(repository.createService(TenantReference(cloud2, env1, tenant2), "service2", "<none>")).isNotNull


        assertThat(repository.getService(ServiceReference(cloud1, env1, tenant1, "service1"), "srn:$cloud1::".parsePermissions())).isNotNull
        assertThat(repository.getService(ServiceReference(cloud2, env1, tenant1, "service2"), "srn:$cloud1::".parsePermissions())).isNull()
    }


    @Test
    fun testCreateService(database: SolidblocksDatabase) {

        val cloudRepository = CloudRepository(database.dsl)
        val environmentRepository = EnvironmentRepository(database.dsl, cloudRepository)
        val tenantRepository = TenantRepository(database.dsl, environmentRepository)
        val serviceRepository = ServiceRepository(database.dsl, tenantRepository)

        val service1Ref = ServiceReference("cloud1", "env1", "tenant1", "service1")

        assertThat(cloudRepository.createCloud(service1Ref.cloud, "domain1")).isNotNull
        assertThat(environmentRepository.createEnvironment(service1Ref, "env1")).isNotNull
        assertThat(tenantRepository.createTenant(service1Ref, "tenant1", "<none>")).isNotNull

        assertThat(serviceRepository.hasService(service1Ref)).isFalse
        assertThat(serviceRepository.createService(service1Ref, service1Ref.service, "type1")).isTrue
        assertThat(serviceRepository.hasService(service1Ref)).isTrue

        val service1 = serviceRepository.getService(service1Ref)
        assertThat(service1!!.configValues).isEmpty()
    }

    @Test
    fun testCreateServiceWithConfigValues(database: SolidblocksDatabase) {

        val cloudRepository = CloudRepository(database.dsl)
        val environmentRepository = EnvironmentRepository(database.dsl, cloudRepository)
        val tenantRepository = TenantRepository(database.dsl, environmentRepository)
        val serviceRepository = ServiceRepository(database.dsl, tenantRepository)

        val cloud = UUID.randomUUID().toString()
        assertThat(cloudRepository.createCloud(cloud, "domain1")).isNotNull
        assertThat(environmentRepository.createEnvironment(CloudReference(cloud), "env1")).isNotNull
        assertThat(tenantRepository.createTenant(EnvironmentReference(cloud, "env1"), "tenant1", "<none>")).isNotNull

        assertThat(serviceRepository.hasService(ServiceReference(cloud, "env1", "tenant1", "service1"))).isFalse
        serviceRepository.createService(
            TenantReference(cloud, "env1", "tenant1"),
            "service1",
            "type1", mapOf("foo" to "bar")
        )
        assertThat(serviceRepository.hasService(ServiceReference(cloud, "env1", "tenant1", "service1"))).isTrue

        val service2 = serviceRepository.getService(ServiceReference(cloud, "env1", "tenant1", "service1"))
        assertThat(service2!!.configValues).hasSize(1)
        assertThat(service2.configValues.first { it.name == "foo" }.value).isEqualTo("bar")
    }
}
