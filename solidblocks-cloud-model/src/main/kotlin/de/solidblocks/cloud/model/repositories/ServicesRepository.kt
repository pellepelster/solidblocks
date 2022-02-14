package de.solidblocks.cloud.model.repositories

import de.solidblocks.base.reference.ServiceReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.base.resources.ResourcePermissions
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.ServiceEntity
import de.solidblocks.config.db.tables.references.*
import org.jooq.Condition
import org.jooq.DSLContext
import java.util.*

class ServicesRepository(dsl: DSLContext, val tenantsRepository: TenantsRepository) : BaseRepository(dsl) {

    private val services = SERVICES.`as`("services")

    fun createService(
        reference: TenantReference,
        service: String,
        type: String,
        configValues: Map<String, String> = emptyMap()
    ): ServiceEntity? {
        val id = UUID.randomUUID()
        val tenant = tenantsRepository.getTenant(reference)
            ?: return null

        dsl.insertInto(SERVICES)
            .columns(
                SERVICES.ID,
                SERVICES.NAME,
                SERVICES.TYPE,
                SERVICES.DELETED,
                SERVICES.TENANT,
            )
            .values(id, service, type, false, tenant.id).execute()

        configValues.forEach {
            setConfiguration(ServiceId(id), it.key, it.value)
        }

        return getService(id)
    }

    private fun listServices(
        filter: Condition? = null,
        permissions: ResourcePermissions? = null
    ): List<ServiceEntity> {

        var filterConditions = services.DELETED.isFalse
        if (filter != null) {
            filterConditions = filterConditions.and(filter)
        }

        val permissionConditions =
            createPermissionConditions(permissions?.permissions.orEmpty(), CLOUDS, ENVIRONMENTS, TENANTS)

        val latest = latestConfigurationValuesQuery(CONFIGURATION_VALUES.SERVICE)

        return dsl.selectFrom(
            services
                .leftJoin(TENANTS).on(services.TENANT.eq(TENANTS.ID))
                .leftJoin(ENVIRONMENTS).on(TENANTS.ENVIRONMENT.eq(ENVIRONMENTS.ID))
                .leftJoin(CLOUDS).on(ENVIRONMENTS.CLOUD.eq(CLOUDS.ID))
                .leftJoin(latest).on(services.ID.eq(latest.field(CONFIGURATION_VALUES.SERVICE)))
        ).where(filterConditions).and(permissionConditions)
            .fetchGroups(
                { it.into(services) }, { it.into(latest) }
            ).map {
                ServiceEntity(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    type = it.key.type!!,
                    tenant = tenantsRepository.getTenant(it.key.tenant!!)!!,
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

    fun getService(reference: ServiceReference, permissions: ResourcePermissions? = null): ServiceEntity? {
        val tenant = tenantsRepository.getTenant(reference) ?: return null
        return listServices(
            services.NAME.eq(reference.service).and(services.TENANT.eq(tenant.id)),
            permissions
        ).firstOrNull()
    }

    fun getService(id: UUID, permissions: ResourcePermissions? = null): ServiceEntity? {
        return listServices(
            services.ID.eq(id), permissions
        ).firstOrNull()
    }

    fun allServices(): List<ServiceEntity> = listServices()

    fun hasService(reference: ServiceReference, permissions: ResourcePermissions? = null) =
        getService(reference, permissions) != null
}
