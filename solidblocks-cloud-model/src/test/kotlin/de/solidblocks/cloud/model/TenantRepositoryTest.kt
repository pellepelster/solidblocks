package de.solidblocks.cloud.model

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.base.resources.parsePermissions
import de.solidblocks.test.SolidblocksTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTestDatabaseExtension::class)
class TenantRepositoryTest {

    @Test
    fun testPermissions(database: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(database.dsl)
        val environmentRepository = EnvironmentRepository(database.dsl, cloudRepository)
        val repository = TenantRepository(database.dsl, environmentRepository)

        val cloud1 = UUID.randomUUID().toString()
        val env1 = UUID.randomUUID().toString()
        val tenant1 = UUID.randomUUID().toString()

        val cloud2 = UUID.randomUUID().toString()
        val env2 = UUID.randomUUID().toString()
        val tenant2 = UUID.randomUUID().toString()

        cloudRepository.createCloud(cloud1, "domain1")
        cloudRepository.createCloud(cloud2, "domain1")

        environmentRepository.createEnvironment(CloudReference(cloud1), env1)
        environmentRepository.createEnvironment(CloudReference(cloud1), env2)
        environmentRepository.createEnvironment(CloudReference(cloud2), env1)
        environmentRepository.createEnvironment(CloudReference(cloud2), env2)

        assertThat(repository.createTenant(EnvironmentReference(cloud1, env1), tenant1, "<none>")).isNotNull
        assertThat(repository.createTenant(EnvironmentReference(cloud1, env1), tenant2, "<none>")).isNotNull
        assertThat(repository.createTenant(EnvironmentReference(cloud1, env2), tenant1, "<none>")).isNotNull
        assertThat(repository.createTenant(EnvironmentReference(cloud1, env2), tenant2, "<none>")).isNotNull

        assertThat(repository.createTenant(EnvironmentReference(cloud2, env1), tenant1, "<none>")).isNotNull
        assertThat(repository.createTenant(EnvironmentReference(cloud2, env1), tenant2, "<none>")).isNotNull
        assertThat(repository.createTenant(EnvironmentReference(cloud2, env2), tenant1, "<none>")).isNotNull
        assertThat(repository.createTenant(EnvironmentReference(cloud2, env2), tenant2, "<none>")).isNotNull

        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:::".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant2), "srn:::".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud1, env2, tenant1), "srn:::".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud1, env2, tenant2), "srn:::".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud2, env1, tenant1), "srn:::".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud2, env1, tenant2), "srn:::".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud2, env2, tenant1), "srn:::".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud2, env2, tenant2), "srn:::".parsePermissions())).isNotNull

        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud1::".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud1:$env1:".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud1:$env2:".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud1:$env1:$tenant1".parsePermissions())).isNotNull
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud1:$env1:$tenant2".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud1:$env2:$tenant1".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud1:$env2:$tenant2".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud2::".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud2:$env1:".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud2:$env1:$tenant1".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud2:$env1:$tenant2".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud2:$env2:".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud2:$env2:$tenant1".parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud1, env1, tenant1), "srn:$cloud2:$env2:$tenant2".parsePermissions())).isNull()

        assertThat(repository.getTenant(TenantReference(cloud2, env1, tenant1), listOf("srn:$cloud1:$env1:$tenant1","srn:$cloud1:$env2:$tenant2").parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud2, env1, tenant1), listOf("srn:$cloud1:$env2:$tenant1","srn:$cloud1:$env2:$tenant2").parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud2, env1, tenant1), listOf("srn:$cloud2:$env1:$tenant2").parsePermissions())).isNull()
        assertThat(repository.getTenant(TenantReference(cloud2, env1, tenant1), listOf("srn:$cloud2:$env1:$tenant1").parsePermissions())).isNotNull

    }

    @Test
    fun testCreateAndUpdateTenant(database: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(database.dsl)
        val environmentRepository = EnvironmentRepository(database.dsl, cloudRepository)
        val tenantRepository = TenantRepository(database.dsl, environmentRepository)

        val reference = TenantReference("cloud1", "env1", "tenant1")

        cloudRepository.createCloud(reference.cloud, "domain1")
        environmentRepository.createEnvironment(reference, "env1")

        assertThat(tenantRepository.hasTenant(reference)).isFalse

        assertThat(tenantRepository.createTenant(reference, "tenant1", "10.0.0.0/16")).isNotNull
        assertThat(tenantRepository.hasTenant(reference)).isTrue

        val tenant = tenantRepository.getTenant(reference)!!
        assertThat(tenant.name).isEqualTo("tenant1")
    }
}
