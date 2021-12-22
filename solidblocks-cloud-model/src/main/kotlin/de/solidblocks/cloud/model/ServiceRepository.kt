package de.solidblocks.cloud.model

import de.solidblocks.cloud.model.model.EnvironmentModel
import de.solidblocks.cloud.model.model.ServiceModel
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.SERVICES
import org.jooq.DSLContext
import java.util.*

class ServiceRepository(dsl: DSLContext, val environmentRepository: EnvironmentRepository) : BaseRepository(dsl) {

    fun createService(cloud: String, environment: String, name: String): Boolean {

        val id = UUID.randomUUID()
        val environment = environmentRepository.getEnvironment(cloud, environment) ?: return false

        dsl.insertInto(SERVICES)
            .columns(
                SERVICES.ID,
                SERVICES.NAME,
                SERVICES.DELETED,
                SERVICES.ENVIRONMENT,
            )
            .values(id, name, false, environment.id).execute()

        return true
    }

    fun listServices(
        cloudName: String? = null,
        environment: EnvironmentModel
    ): List<ServiceModel> {

        val latest = latestConfigurationValues(CONFIGURATION_VALUES.SERVICE)

        val services = SERVICES.`as`("services")

        var condition = services.DELETED.isFalse
        condition = condition.and(services.ENVIRONMENT.eq(environment.id))

        if (cloudName != null) {
            condition = condition.and(services.NAME.eq(cloudName))
        }

        return dsl.selectFrom(services.leftJoin(latest).on(services.ID.eq(latest.field(CONFIGURATION_VALUES.TENANT))))
            .where(condition)
            .fetchGroups(
                { it.into(services) }, { it.into(latest) }
            ).map {
                ServiceModel(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    environment = environment
                )
            }
    }
}
