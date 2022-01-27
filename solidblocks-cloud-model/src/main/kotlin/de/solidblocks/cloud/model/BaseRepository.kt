package de.solidblocks.cloud.model

import de.solidblocks.base.resources.ResourcePermission
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.config.db.tables.Clouds
import de.solidblocks.config.db.tables.Environments
import de.solidblocks.config.db.tables.Services
import de.solidblocks.config.db.tables.Tenants
import de.solidblocks.config.db.tables.records.ConfigurationValuesRecord
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import mu.KotlinLogging
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DSL.max
import java.util.*

abstract class BaseRepository(val dsl: DSLContext) {

    sealed class IdType(private val id: UUID)

    protected class CloudId(val id: UUID) : IdType(id)

    protected class EnvironmentId(val id: UUID) : IdType(id)

    protected class TenantId(val id: UUID) : IdType(id)

    protected class ServiceId(val id: UUID) : IdType(id)

    protected val logger = KotlinLogging.logger {}

    protected fun createPermissionConditions(
            permissions: List<ResourcePermission>,
            clouds: Clouds,
            environments: Environments? = null,
            tenants: Tenants? = null,
            services: Services? = null
    ): Condition {

        var permissionConditions: Condition = DSL.noCondition()

        for (permission in permissions) {

            var permissionCondition: Condition = DSL.noCondition()

            if (!permission.cloud.wildcard && permission.cloud.cloud != null) {
                permissionCondition = permissionCondition.and(clouds.NAME.eq(permission.cloud.cloud))
            }

            if (environments != null && !permission.environment.wildcard && permission.environment.environment != null) {
                permissionCondition = permissionCondition.and(environments.NAME.eq(permission.environment.environment))
            }

            if (tenants != null && !permission.tenant.wildcard && permission.tenant.tenant != null) {
                permissionCondition = permissionCondition.and(tenants.NAME.eq(permission.tenant.tenant))
            }

            permissionConditions = permissionConditions.or(permissionCondition)
        }

        return permissionConditions
    }

    protected fun latestConfigurationValuesQuery(referenceColumn: TableField<ConfigurationValuesRecord, UUID?>): Table<Record5<UUID?, UUID?, String?, String?, Int?>> {

        val latestVersions =
                dsl.select(
                        referenceColumn,
                        CONFIGURATION_VALUES.NAME,
                        max(CONFIGURATION_VALUES.VERSION).`as`(CONFIGURATION_VALUES.VERSION)
                )
                        .from(CONFIGURATION_VALUES).groupBy(referenceColumn, CONFIGURATION_VALUES.NAME)
                        .asTable("latest_versions")

        val latest = dsl.select(
                referenceColumn,
                CONFIGURATION_VALUES.ID,
                CONFIGURATION_VALUES.NAME,
                CONFIGURATION_VALUES.CONFIG_VALUE,
                CONFIGURATION_VALUES.VERSION
        ).from(
                CONFIGURATION_VALUES.rightJoin(latestVersions).on(
                        CONFIGURATION_VALUES.NAME.eq(latestVersions.field(CONFIGURATION_VALUES.NAME))
                                .and(
                                        CONFIGURATION_VALUES.VERSION.eq(latestVersions.field(CONFIGURATION_VALUES.VERSION))
                                                .and(referenceColumn.eq(latestVersions.field(referenceColumn)))
                                )
                )
        ).where(referenceColumn.isNotNull).asTable("latest_configurations")

        return latest
    }

    protected fun setConfiguration(id: IdType, name: String, value: String?) {

        var tenantId: UUID? = null
        var cloudId: UUID? = null
        var serviceId: UUID? = null
        var environmentId: UUID? = null

        when (id) {
            is CloudId -> cloudId = id.id
            is EnvironmentId -> environmentId = id.id
            is TenantId -> tenantId = id.id
            is ServiceId -> serviceId = id.id
        }

        val condition = when (id) {
            is CloudId -> CONFIGURATION_VALUES.CLOUD.eq(id.id)
            is EnvironmentId -> CONFIGURATION_VALUES.ENVIRONMENT.eq(id.id)
            is TenantId -> CONFIGURATION_VALUES.TENANT.eq(id.id)
            is ServiceId -> CONFIGURATION_VALUES.SERVICE.eq(id.id)
        }

        if (value != null) {
            // unfortunately derby does not support limits .limit(1).offset(0)
            val current = dsl.selectFrom(CONFIGURATION_VALUES)
                    .where(CONFIGURATION_VALUES.NAME.eq(name).and(condition))
                    .orderBy(CONFIGURATION_VALUES.VERSION.desc()).fetch()

            dsl.insertInto(CONFIGURATION_VALUES).columns(
                    CONFIGURATION_VALUES.ID,
                    CONFIGURATION_VALUES.VERSION,
                    CONFIGURATION_VALUES.CLOUD,
                    CONFIGURATION_VALUES.ENVIRONMENT,
                    CONFIGURATION_VALUES.TENANT,
                    CONFIGURATION_VALUES.SERVICE,
                    CONFIGURATION_VALUES.NAME,
                    CONFIGURATION_VALUES.CONFIG_VALUE
            ).values(
                    UUID.randomUUID(),
                    current.firstOrNull()?.let { it.version!! + 1 }
                            ?: 0,
                    cloudId, environmentId, tenantId, serviceId, name, value
            ).execute()
        }
    }

    protected fun List<Record5<UUID?, UUID?, String?, String?, Int?>>.configValue(name: String): CloudConfigValue {
        return this.firstOrNull { it.getValue(CONFIGURATION_VALUES.NAME) == name }?.map {
            CloudConfigValue(
                    it.getValue(CONFIGURATION_VALUES.NAME)!!,
                    it.getValue(CONFIGURATION_VALUES.CONFIG_VALUE)!!,
                    it.getValue(CONFIGURATION_VALUES.VERSION)!!
            )
        }!!
    }
}
