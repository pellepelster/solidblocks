package de.solidblocks.cloud.model

import de.solidblocks.base.CloudReference
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.CloudEntity
import de.solidblocks.config.db.tables.references.CLOUDS
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import org.jooq.DSLContext
import java.util.*

class CloudRepository(dsl: DSLContext) : BaseRepository(dsl) {

    fun createCloud(
        reference: CloudReference,
        rootDomain: String,
        configValues: List<CloudConfigValue> = emptyList(),
        development: Boolean = false,
    ): CloudEntity {
        logger.info { "creating cloud '${reference.cloud}'" }

        val id = UUID.randomUUID()
        dsl.insertInto(CLOUDS)
            .columns(
                CLOUDS.ID,
                CLOUDS.NAME,
                CLOUDS.DELETED,
            )
            .values(id, reference.cloud, false).execute()

        setConfiguration(CloudId(id), CloudEntity.ROOT_DOMAIN_KEY, rootDomain)
        setConfiguration(CloudId(id), CloudEntity.DEVELOPMENT_KEY, development.toString())

        configValues.forEach {
            setConfiguration(CloudId(id), it.name, it.value)
        }

        return getCloud(reference)
    }

    fun hasCloud(reference: CloudReference): Boolean {
        return dsl.fetchCount(CLOUDS, CLOUDS.NAME.eq(reference.cloud).and(CLOUDS.DELETED.isFalse)) == 1
    }

    fun listClouds(name: String? = null): List<CloudEntity> {
        val latest = latestConfigurationValues(CONFIGURATION_VALUES.CLOUD)

        val clouds = CLOUDS.`as`("clouds")

        var condition = clouds.DELETED.isFalse

        if (name != null) {
            condition = condition.and(clouds.NAME.eq(name))
        }

        return dsl.selectFrom(clouds.leftJoin(latest).on(clouds.ID.eq(latest.field(CONFIGURATION_VALUES.CLOUD))))
            .where(condition)
            .fetchGroups(
                { it.into(clouds) }, { it.into(latest) }
            ).map {
                CloudEntity(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    rootDomain = it.value.configValue(CloudEntity.ROOT_DOMAIN_KEY).value,
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

    fun getCloud(reference: CloudReference): CloudEntity {
        return listClouds(reference.cloud).first()
    }
}
