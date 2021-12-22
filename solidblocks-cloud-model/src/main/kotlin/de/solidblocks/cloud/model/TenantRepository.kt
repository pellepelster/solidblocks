package de.solidblocks.cloud.model

import de.solidblocks.cloud.model.model.EnvironmentModel
import de.solidblocks.cloud.model.model.TenantModel
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.TENANTS
import org.jooq.DSLContext
import java.util.*

class TenantRepository(dsl: DSLContext, val environmentRepository: EnvironmentRepository) : BaseRepository(dsl) {

    fun getTenant(cloudName: String, environmentName: String, name: String): TenantModel {
        val environment = environmentRepository.getEnvironment(cloudName, environmentName)
        return listTenants(name, environment).first()
    }

    fun listTenants(name: String? = null, environment: EnvironmentModel): List<TenantModel> {

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
                TenantModel(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    environment = environment
                )
            }
    }

    fun createTenant(cloudName: String, environmentName: String, name: String): TenantModel? {

        val id = UUID.randomUUID()
        val environment = environmentRepository.getEnvironment(cloudName, environmentName) ?: return null

        dsl.insertInto(TENANTS)
            .columns(
                TENANTS.ID,
                TENANTS.NAME,
                TENANTS.DELETED,
                TENANTS.ENVRIONMENT,
            )
            .values(id, name, false, environment.id).execute()

        return getTenant(cloudName, environmentName, name)
    }

    fun hasTenant(cloudName: String, environmentName: String, name: String): Boolean {
        val environment = environmentRepository.getEnvironment(cloudName, environmentName) ?: return false
        return listTenants(name, environment).isNotEmpty()
    }
}
