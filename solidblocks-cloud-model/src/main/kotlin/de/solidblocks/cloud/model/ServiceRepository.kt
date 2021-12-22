package de.solidblocks.cloud.model

import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.ServiceEntity
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.SERVICES
import org.jooq.DSLContext
import java.util.*

class ServiceRepository(dsl: DSLContext, val environmentRepository: EnvironmentRepository) : BaseRepository(dsl) {

    fun createService(
        cloud: String,
        environment: String,
        name: String,
        configValues: Map<String, String> = emptyMap()
    ): Boolean {

        val id = UUID.randomUUID()
        val environment = environmentRepository.getEnvironment(cloud, environment)

        dsl.insertInto(SERVICES)
            .columns(
                SERVICES.ID,
                SERVICES.NAME,
                SERVICES.DELETED,
                SERVICES.ENVIRONMENT,
            )
            .values(id, name, false, environment.id).execute()

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

    fun getService(cloud: String, environment: String, service: String) =
        environmentRepository.getEnvironment(cloud, environment).let {
            listServices(service, it).firstOrNull()
        }

    fun getService(reference: ServiceReference) =
        environmentRepository.getEnvironment(reference.cloud, reference.environment).let {
            listServices(reference.service, it).firstOrNull()
        }

    fun hasService(cloud: String, environment: String, service: String) =
        getService(cloud, environment, service) != null
}
