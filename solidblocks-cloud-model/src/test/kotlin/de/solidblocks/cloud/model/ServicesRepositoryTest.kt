package de.solidblocks.cloud.model

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.ServiceReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.base.resources.parsePermissions
import de.solidblocks.cloud.model.repositories.CloudsRepository
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.cloud.model.repositories.ServicesRepository
import de.solidblocks.cloud.model.repositories.TenantsRepository
import de.solidblocks.test.SolidblocksTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTestDatabaseExtension::class)
class ServicesRepositoryTest {

    @Test
    fun testPermissions(database: SolidblocksDatabase) {
        val cloudsRepository = CloudsRepository(database.dsl)
        val environmentsRepository = EnvironmentsRepository(database.dsl, cloudsRepository)
        val tenantsRepository = TenantsRepository(database.dsl, environmentsRepository)
        val repository = ServicesRepository(database.dsl, tenantsRepository)

        val cloud1 = UUID.randomUUID().toString()
        val env1 = UUID.randomUUID().toString()
        val tenant1 = UUID.randomUUID().toString()

        val cloud2 = UUID.randomUUID().toString()
        val env2 = UUID.randomUUID().toString()
        val tenant2 = UUID.randomUUID().toString()

        cloudsRepository.createCloud(cloud1, "domain1")
        cloudsRepository.createCloud(cloud2, "domain2")
        environmentsRepository.createEnvironment(CloudReference(cloud1), env1)
        environmentsRepository.createEnvironment(CloudReference(cloud2), env1)

        assertThat(tenantsRepository.createTenant(EnvironmentReference(cloud1, env1), tenant1, "<none>")).isNotNull
        assertThat(tenantsRepository.createTenant(EnvironmentReference(cloud1, env1), tenant2, "<none>")).isNotNull

        assertThat(repository.createService(TenantReference(cloud1, env1, tenant1), "service1", "<none>")).isNotNull
        assertThat(repository.createService(TenantReference(cloud2, env1, tenant2), "service2", "<none>")).isNotNull

        assertThat(repository.getService(ServiceReference(cloud1, env1, tenant1, "service1"), "srn:$cloud1::".parsePermissions())).isNotNull
        assertThat(repository.getService(ServiceReference(cloud2, env1, tenant1, "service2"), "srn:$cloud1::".parsePermissions())).isNull()
    }

    @Test
    fun testCreateService(database: SolidblocksDatabase) {

        val cloudsRepository = CloudsRepository(database.dsl)
        val environmentsRepository = EnvironmentsRepository(database.dsl, cloudsRepository)
        val tenantsRepository = TenantsRepository(database.dsl, environmentsRepository)
        val servicesRepository = ServicesRepository(database.dsl, tenantsRepository)

        val service1Ref = ServiceReference("cloud1", "env1", "tenant1", "service1")

        assertThat(cloudsRepository.createCloud(service1Ref.cloud, "domain1")).isNotNull
        assertThat(environmentsRepository.createEnvironment(service1Ref, "env1")).isNotNull
        assertThat(tenantsRepository.createTenant(service1Ref, "tenant1", "<none>")).isNotNull

        assertThat(servicesRepository.hasService(service1Ref)).isFalse
        assertThat(servicesRepository.createService(service1Ref, service1Ref.service, "type1")).isNotNull
        assertThat(servicesRepository.hasService(service1Ref)).isTrue

        val service1 = servicesRepository.getService(service1Ref)
        assertThat(service1!!.configValues).isEmpty()
    }

    @Test
    fun testCreateServiceWithConfigValues(database: SolidblocksDatabase) {

        val cloudsRepository = CloudsRepository(database.dsl)
        val environmentsRepository = EnvironmentsRepository(database.dsl, cloudsRepository)
        val tenantsRepository = TenantsRepository(database.dsl, environmentsRepository)
        val servicesRepository = ServicesRepository(database.dsl, tenantsRepository)

        val cloud = UUID.randomUUID().toString()
        assertThat(cloudsRepository.createCloud(cloud, "domain1")).isNotNull
        assertThat(environmentsRepository.createEnvironment(CloudReference(cloud), "env1")).isNotNull
        assertThat(tenantsRepository.createTenant(EnvironmentReference(cloud, "env1"), "tenant1", "<none>")).isNotNull

        assertThat(servicesRepository.hasService(ServiceReference(cloud, "env1", "tenant1", "service1"))).isFalse
        servicesRepository.createService(
            TenantReference(cloud, "env1", "tenant1"),
            "service1",
            "type1", mapOf("foo" to "bar")
        )
        assertThat(servicesRepository.hasService(ServiceReference(cloud, "env1", "tenant1", "service1"))).isTrue

        val service2 = servicesRepository.getService(ServiceReference(cloud, "env1", "tenant1", "service1"))
        assertThat(service2!!.configValues).hasSize(1)
        assertThat(service2.configValues.first { it.name == "foo" }.value).isEqualTo("bar")
    }
}
