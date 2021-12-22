package de.solidblocks.cloud.model

import de.solidblocks.test.SolidblocksTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTestDatabaseExtension::class)
class TenantRepositoryTest {

    @Test
    fun testCreateAndUpdateTenant(solidblocksDatabase: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(solidblocksDatabase.dsl)
        val environmentRepository = EnvironmentRepository(solidblocksDatabase.dsl, cloudRepository)
        val tenantRepository = TenantRepository(solidblocksDatabase.dsl, environmentRepository)

        val cloud = cloudRepository.createCloud("cloud1", "domain1")
        val envrionment = environmentRepository.createEnvironment("cloud1", "env1")

        assertThat(tenantRepository.hasTenant("cloud1", "env1", "tenant1")).isFalse

        assertThat(tenantRepository.createTenant("cloud1", "env1", "tenant1")).isNotNull
        assertThat(tenantRepository.hasTenant("cloud1", "env1", "tenant1")).isTrue

        val tenant = tenantRepository.getTenant("cloud1", "env1", "tenant1")
        assertThat(tenant.name).isEqualTo("tenant1")
    }
}
