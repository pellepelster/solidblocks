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

        val reference = TenantReference("cloud1", "env1", "tenant1")

        cloudRepository.createCloud(reference.cloud, "domain1")
        environmentRepository.createEnvironment(reference)

        assertThat(tenantRepository.hasTenant(reference)).isFalse

        assertThat(tenantRepository.createTenant(reference, "tenant1", "10.0.0.0/16")).isNotNull
        assertThat(tenantRepository.hasTenant(reference)).isTrue

        val tenant = tenantRepository.getTenant(reference)
        assertThat(tenant.name).isEqualTo("tenant1")
    }
}
