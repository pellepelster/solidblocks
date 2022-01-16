package de.solidblocks.cloud.model

import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.base.resources.TenantResource
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.config.db.tables.references.CLOUDS
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.ENVIRONMENTS
import de.solidblocks.config.db.tables.references.TENANTS
import org.jooq.Condition
import org.jooq.DSLContext
import java.util.*

class TenantRepository(dsl: DSLContext, val environmentRepository: EnvironmentRepository) : BaseRepository(dsl) {

    val tenants = TENANTS.`as`("tenants")

    fun getTenant(id: UUID): TenantEntity? {
        val record = dsl.selectFrom(TENANTS.join(ENVIRONMENTS).on(ENVIRONMENTS.ID.eq(TENANTS.ID)).leftJoin(CLOUDS).on(ENVIRONMENTS.ID.eq(CLOUDS.ID))).where(TENANTS.ID.eq(id)).fetchOne()
            ?: return null

        val cloud = record.getValue(CLOUDS.NAME)
        val environment = record.getValue(ENVIRONMENTS.NAME)
        val tenant = record.getValue(TENANTS.NAME)

        return getTenant(TenantResource(cloud!!, environment!!, tenant!!))
    }

    fun getTenant(reference: TenantResource): TenantEntity {
        val environment = environmentRepository.getEnvironment(reference)
        return listTenants(tenants.NAME.eq(reference.tenant), environment).first()
    }

    fun getOptional(reference: TenantResource): TenantEntity? {
        val environment = environmentRepository.getEnvironment(reference)
        return listTenants(tenants.NAME.eq(reference.tenant), environment).firstOrNull()
    }

    fun listTenants(extraCondition: Condition? = null, environment: EnvironmentEntity): List<TenantEntity> {

        val latest = latestConfigurationValues(CONFIGURATION_VALUES.TENANT)

        var condition = tenants.DELETED.isFalse
        condition = condition.and(tenants.ENVRIONMENT.eq(environment.id))

        if (extraCondition != null) {
            condition.and(extraCondition)
        }

        return dsl.selectFrom(tenants.leftJoin(latest).on(tenants.ID.eq(latest.field(CONFIGURATION_VALUES.TENANT))))
            .where(condition)
            .fetchGroups(
                { it.into(tenants) }, { it.into(latest) }
            ).map {
                TenantEntity(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    environment = environment,
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

    fun createTenant(reference: EnvironmentResource, name: String, networkCidr: String): TenantEntity {

        val id = UUID.randomUUID()
        val environment = environmentRepository.getEnvironment(reference)

        dsl.insertInto(TENANTS)
            .columns(
                TENANTS.ID,
                TENANTS.NAME,
                TENANTS.DELETED,
                TENANTS.ENVRIONMENT,
            )
            .values(id, name, false, environment.id).execute()

        setConfiguration(TenantId(id), ModelConstants.TENANT_NETWORK_CIDR_KEY, networkCidr)

        return getTenant(reference.toTenant(name))
    }

    fun hasTenant(reference: TenantResource): Boolean {
        val environment = environmentRepository.getEnvironment(reference)
        return listTenants(tenants.NAME.eq(reference.tenant), environment).isNotEmpty()
    }
}
