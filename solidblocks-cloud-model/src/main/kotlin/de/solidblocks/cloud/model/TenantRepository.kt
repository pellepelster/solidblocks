package de.solidblocks.cloud.model

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.TenantReference
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.TENANTS
import org.jooq.DSLContext
import java.util.*

class TenantRepository(dsl: DSLContext, val environmentRepository: EnvironmentRepository) : BaseRepository(dsl) {

    fun getTenant(reference: TenantReference): TenantEntity {
        val environment = environmentRepository.getEnvironment(reference)
        return listTenants(reference.tenant, environment).first()
    }

    fun getOptional(reference: TenantReference): TenantEntity? {
        val environment = environmentRepository.getEnvironment(reference)
        return listTenants(reference.tenant, environment).firstOrNull()
    }

    fun listTenants(name: String? = null, environment: EnvironmentEntity): List<TenantEntity> {

        val latest = latestConfigurationValues(CONFIGURATION_VALUES.TENANT)

        val tenants = TENANTS.`as`("tenants")

        var condition = tenants.DELETED.isFalse
        condition = condition.and(tenants.ENVRIONMENT.eq(environment.id))

        if (name != null) {
            condition = condition.and(tenants.NAME.eq(name))
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

    fun createTenant(reference: EnvironmentReference, name: String, networkCidr: String): TenantEntity {

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

    fun hasTenant(reference: TenantReference): Boolean {
        val environment = environmentRepository.getEnvironment(reference)
        return listTenants(reference.tenant, environment).isNotEmpty()
    }
}
