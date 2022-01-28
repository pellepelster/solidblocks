package de.solidblocks.cloud.model

import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.base.resources.ResourcePermissions
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.config.db.tables.references.CLOUDS
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.ENVIRONMENTS
import de.solidblocks.config.db.tables.references.TENANTS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.util.*

class TenantsRepository(dsl: DSLContext, val environmentsRepository: EnvironmentsRepository) : BaseRepository(dsl) {

    val tenants = TENANTS.`as`("tenants")

    fun hasTenant(reference: TenantReference, permissions: ResourcePermissions? = null) =
        getTenant(reference, permissions) != null

    fun getTenant(id: UUID): TenantEntity? = listTenants(tenants.ID.eq(id)).firstOrNull()

    fun getTenant(reference: TenantReference, permissions: ResourcePermissions? = null): TenantEntity? {
        return listTenants(
            CLOUDS.NAME.eq(reference.cloud).and(ENVIRONMENTS.NAME.eq(reference.environment))
                .and(tenants.NAME.eq(reference.tenant)),
            permissions
        ).firstOrNull()
    }

    fun listTenants(filter: Condition? = null, permissions: ResourcePermissions? = null): List<TenantEntity> {

        val latest = latestConfigurationValuesQuery(CONFIGURATION_VALUES.TENANT)

        var filterConditions = tenants.DELETED.isFalse

        if (filter != null) {
            filterConditions = filterConditions.and(filter)
        }

        var permissionConditions: Condition = DSL.noCondition()
        if (permissions != null && !permissions.isCloudWildcard && permissions.clouds.isNotEmpty()) {
            if (!permissions.isCloudWildcard && permissions.clouds.isNotEmpty()) {
                for (cloud in permissions.clouds) {
                    permissionConditions = permissionConditions.and(CLOUDS.NAME.eq(cloud))
                }
            }

            if (!permissions.isEnvironmentWildcard && permissions.environments.isNotEmpty()) {
                for (environment in permissions.environments) {
                    permissionConditions = permissionConditions.and(ENVIRONMENTS.NAME.eq(environment))
                }
            }

            if (!permissions.isTenantWildcard && permissions.tenants.isNotEmpty()) {
                for (tenant in permissions.tenants) {
                    permissionConditions = permissionConditions.and(tenants.NAME.eq(tenant))
                }
            }
        }

        return dsl.selectFrom(
            tenants
                .leftJoin(ENVIRONMENTS).on(tenants.ENVIRONMENT.eq(ENVIRONMENTS.ID))
                .leftJoin(CLOUDS).on(ENVIRONMENTS.CLOUD.eq(CLOUDS.ID))
                .leftJoin(latest).on(tenants.ID.eq(latest.field(CONFIGURATION_VALUES.TENANT)))
        )
            .where(filterConditions).and(permissionConditions)
            .orderBy(tenants.NAME)
            .fetchGroups(
                { it.into(tenants) }, { it.into(latest) }
            ).map {
                TenantEntity(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    environment = environmentsRepository.getEnvironment(it.key.environment!!)!!,
                    configValues = it.value.map {
                        CloudConfigValue(
                            it.getValue(CONFIGURATION_VALUES.NAME)!!,
                            it.getValue(CONFIGURATION_VALUES.CONFIG_VALUE)!!,
                            it.getValue(CONFIGURATION_VALUES.VERSION)!!
                        )
                    }
                )
            }
    }

    fun createTenant(reference: EnvironmentReference, name: String, networkCidr: String): TenantEntity? {
        val id = UUID.randomUUID()
        val environment = environmentsRepository.getEnvironment(reference) ?: return null

        dsl.insertInto(TENANTS)
            .columns(
                TENANTS.ID,
                TENANTS.NAME,
                TENANTS.DELETED,
                TENANTS.ENVIRONMENT,
            )
            .values(id, name, false, environment.id).execute()

        setConfiguration(TenantId(id), ModelConstants.TENANT_NETWORK_CIDR_KEY, networkCidr)

        return getTenant(reference.toTenant(name))
    }
}
