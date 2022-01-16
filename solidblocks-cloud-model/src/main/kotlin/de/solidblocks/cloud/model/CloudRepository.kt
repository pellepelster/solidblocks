package de.solidblocks.cloud.model

import de.solidblocks.base.resources.CloudResource
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.CloudEntity
import de.solidblocks.config.db.tables.references.CLOUDS
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import org.jooq.Condition
import org.jooq.DSLContext
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

    fun listClouds(extraCondition: Condition? = null): List<CloudEntity> {
        val latest = latestConfigurationValues(CONFIGURATION_VALUES.CLOUD)

        var condition = clouds.DELETED.isFalse
        if (extraCondition != null) {
            condition = condition.and(extraCondition)
        }

        return dsl.selectFrom(clouds.leftJoin(latest).on(clouds.ID.eq(latest.field(CONFIGURATION_VALUES.CLOUD))))
            .where(condition)
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

    fun getCloud(reference: CloudResource) = getCloud(reference.cloud)!!

    fun getCloud(name: String) = listClouds(clouds.NAME.eq(name)).firstOrNull()

    fun getCloud(id: UUID) = listClouds(clouds.ID.eq(id)).firstOrNull()

    fun getCloudByRootDomain(rootDomain: String) = listClouds(clouds.ROOT_DOMAIN.eq(rootDomain)).firstOrNull()
}
