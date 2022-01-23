package de.solidblocks.cloud.model

import de.solidblocks.base.resources.CloudResource
import de.solidblocks.base.resources.ResourcePermissions
import de.solidblocks.base.resources.ResourcePermissions.Companion.adminPermissions
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.CloudEntity
import de.solidblocks.config.db.tables.references.CLOUDS
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.util.*

class CloudRepository(dsl: DSLContext) : BaseRepository(dsl) {

    val clouds = CLOUDS.`as`("clouds")

    fun createCloud(
        name: String,
        rootDomain: String,
        configValues: List<CloudConfigValue> = emptyList(),
        development: Boolean = false,
    ): CloudEntity {

        val id = UUID.randomUUID()
        dsl.insertInto(CLOUDS)
            .columns(
                CLOUDS.ID,
                CLOUDS.NAME,
                CLOUDS.DELETED,
                CLOUDS.ROOT_DOMAIN,
            )
            .values(id, name, false, rootDomain).execute()

        setConfiguration(CloudId(id), CloudEntity.DEVELOPMENT_KEY, development.toString())

        configValues.forEach {
            setConfiguration(CloudId(id), it.name, it.value)
        }

        return getCloud(name)!!
    }

    fun hasCloud(name: String): Boolean {
        return dsl.fetchCount(CLOUDS, CLOUDS.NAME.eq(name).and(CLOUDS.DELETED.isFalse)) == 1
    }

    fun hasCloud(reference: CloudResource) = hasCloud(reference.cloud)

    fun listClouds(filter: Condition? = null, permissions: ResourcePermissions? = null): List<CloudEntity> {
        val latest = latestConfigurationValuesQuery(CONFIGURATION_VALUES.CLOUD)

        var filterConditions = clouds.DELETED.isFalse

        if (filter != null) {
            filterConditions = filterConditions.and(filter)
        }

        var permissionConditions: Condition = DSL.noCondition()
        if (permissions != null && !permissions.isCloudWildcard && permissions.clouds.isNotEmpty()) {
            for (cloud in permissions.clouds) {
                permissionConditions = permissionConditions.and(clouds.NAME.eq(cloud))
            }
        }

        return dsl.selectFrom(clouds.leftJoin(latest).on(clouds.ID.eq(latest.field(CONFIGURATION_VALUES.CLOUD))))
            .where(filterConditions).and(permissionConditions).orderBy(clouds.NAME)
            .fetchGroups(
                { it.into(clouds) }, { it.into(latest) }
            ).map {
                CloudEntity(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    rootDomain = it.key.rootDomain!!,
                    isDevelopment = it.value.configValue(CloudEntity.DEVELOPMENT_KEY).value.toBoolean(),
                    it.value.map {
                        CloudConfigValue(
                            it.getValue(CONFIGURATION_VALUES.NAME)!!,
                            it.getValue(CONFIGURATION_VALUES.CONFIG_VALUE)!!,
                            it.getValue(CONFIGURATION_VALUES.VERSION)!!
                        )
                    }
                )
            }
    }

    fun getCloud(reference: CloudResource, permissions: ResourcePermissions? = null) =
        getCloud(reference.cloud, permissions)

    fun getCloud(name: String, permissions: ResourcePermissions? = null) =
        listClouds(clouds.NAME.eq(name), permissions ?: adminPermissions()).firstOrNull()

    fun getCloud(id: UUID, permissions: ResourcePermissions? = null) =
        listClouds(clouds.ID.eq(id), permissions ?: adminPermissions()).firstOrNull()

    fun getCloudByRootDomain(rootDomain: String, permissions: ResourcePermissions? = null) =
        listClouds(clouds.ROOT_DOMAIN.eq(rootDomain), permissions ?: adminPermissions()).firstOrNull()
}
