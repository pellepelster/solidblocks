package de.solidblocks.cloud.model

import de.solidblocks.base.resources.ServiceResource
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.ServiceEntity
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.SERVICES
import org.jooq.DSLContext
import java.util.*

class ServiceRepository(dsl: DSLContext, val environmentRepository: EnvironmentRepository) : BaseRepository(dsl) {

    fun createService(
            reference: ServiceResource,
            configValues: Map<String, String> = emptyMap()
    ): Boolean {

        val id = UUID.randomUUID()
        val environment = environmentRepository.getEnvironment(reference)

        dsl.insertInto(SERVICES)
            .columns(
                SERVICES.ID,
                SERVICES.NAME,
                SERVICES.DELETED,
                SERVICES.ENVIRONMENT,
            )
            .values(id, reference.service, false, environment.id).execute()

        configValues.forEach {
            setConfiguration(ServiceId(id), it.key, it.value)
        }

        return true
    }

    private fun listServices(
        name: String? = null,
        environment: EnvironmentEntity
    ): List<ServiceEntity> {

        val latest = latestConfigurationValues(CONFIGURATION_VALUES.SERVICE)

        val services = SERVICES.`as`("services")

        var condition = services.DELETED.isFalse
        condition = condition.and(services.ENVIRONMENT.eq(environment.id))

        if (name != null) {
            condition = condition.and(services.NAME.eq(name))
        }

        dsl.selectFrom(latest).fetch().forEach {
            it.toString()
        }

        return dsl.selectFrom(services.leftJoin(latest).on(services.ID.eq(latest.field(CONFIGURATION_VALUES.SERVICE))))
            .where(condition)
            .fetchGroups(
                { it.into(services) }, { it.into(latest) }
            ).map {
                ServiceEntity(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    environment = environment,
                    configValues = it.value.filter { it.value1() != null }.map {
                        CloudConfigValue(
                            it.getValue(CONFIGURATION_VALUES.NAME)!!,
                            it.getValue(CONFIGURATION_VALUES.CONFIG_VALUE)!!,
                            it.getValue(CONFIGURATION_VALUES.VERSION)!!
                        )
                    }
                )
            }
    }

    fun getService(reference: ServiceResource) =
        environmentRepository.getEnvironment(reference).let {
            listServices(reference.service, it).firstOrNull()
        }

    fun hasService(reference: ServiceResource) =
        getService(reference) != null
}
