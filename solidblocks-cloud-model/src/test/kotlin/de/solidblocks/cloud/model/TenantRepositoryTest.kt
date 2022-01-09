package de.solidblocks.cloud.model

import de.solidblocks.base.TenantReference
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

        val tenantRef = TenantReference("cloud1", "env1", "tenant1")

        cloudRepository.createCloud(tenantRef.toCloud(), "domain1")
        environmentRepository.createEnvironment(tenantRef.toEnvironment())

        assertThat(tenantRepository.hasTenant(tenantRef)).isFalse

        assertThat(tenantRepository.createTenant(tenantRef)).isNotNull
        assertThat(tenantRepository.hasTenant(tenantRef)).isTrue

        val tenant = tenantRepository.getTenant(tenantRef)
        assertThat(tenant.name).isEqualTo("tenant1")
    }
}
