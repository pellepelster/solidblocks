package de.solidblocks.cloud.config

import de.solidblocks.base.Utils.Companion.generateSshKey
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.CONSUL_SECRET_KEY
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.SSH_IDENTITY_PRIVATE_KEY
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.SSH_IDENTITY_PUBLIC_KEY
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.SSH_PRIVATE_KEY
import de.solidblocks.cloud.config.Constants.ConfigKeys.Companion.SSH_PUBLIC_KEY
import de.solidblocks.cloud.config.model.CloudConfigValue
import de.solidblocks.cloud.config.model.CloudConfiguration
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.cloud.config.model.ConsulSecrets
import de.solidblocks.cloud.config.model.SshSecrets
import de.solidblocks.cloud.config.model.TenantConfiguration
import de.solidblocks.config.db.tables.references.CLOUDS
import de.solidblocks.config.db.tables.references.CLOUDS_ENVIRONMENTS
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.TENANTS
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.Record5
import org.jooq.impl.DSL.max
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.*

@Component
class CloudConfigurationManager(private val dsl: DSLContext) {

    sealed class IdType(private val id: UUID)

    class CloudId(val id: UUID) : IdType(id)

    class EnvironmentId(val id: UUID) : IdType(id)

    class TenantId(val id: UUID) : IdType(id)

    private val logger = KotlinLogging.logger {}

    fun createCloud(name: String, rootDomain: String, configValues: List<CloudConfigValue> = emptyList()): Boolean {
        if (hasCloud(name)) {
            logger.error { "cloud '$name' already exists" }
            return false
        }

        logger.info { "creating cloud '$name'" }

        val id = UUID.randomUUID()

        dsl.insertInto(CLOUDS)
            .columns(
                CLOUDS.ID,
                CLOUDS.NAME,
                CLOUDS.DELETED,
            )
            .values(id, name, false).execute()

        setConfiguration(CloudId(id), CloudConfiguration.ROOT_DOMAIN_KEY, rootDomain)

        configValues.forEach {
            setConfiguration(CloudId(id), it.name, it.value)
        }

        return true
    }

    fun createEnvironment(cloudName: String, environmentName: String, configValues: List<CloudConfigValue> = emptyList()): Boolean {
        val cloud = getCloud(cloudName) ?: return false

        logger.info { "creating environment '$environmentName' for cloud '$cloudName'" }
        val id = UUID.randomUUID()

        dsl.insertInto(CLOUDS_ENVIRONMENTS)
            .columns(
                CLOUDS_ENVIRONMENTS.ID,
                CLOUDS_ENVIRONMENTS.CLOUD,
                CLOUDS_ENVIRONMENTS.NAME
            )
            .values(id, cloud.id, environmentName).execute()

        generateAndStoreSecrets(id, environmentName)

        configValues.forEach {
            setConfiguration(EnvironmentId(id), it.name, it.value)
        }

        return true
    }

    fun hasCloud(name: String): Boolean {
        return dsl.fetchCount(CLOUDS, CLOUDS.NAME.eq(name).and(CLOUDS.DELETED.isFalse)) == 1
    }

    fun listClouds(name: String? = null): List<CloudConfiguration> {
        val latestVersions =
            dsl.select(
                CONFIGURATION_VALUES.CLOUD,
                CONFIGURATION_VALUES.NAME,
                max(CONFIGURATION_VALUES.VERSION).`as`(CONFIGURATION_VALUES.VERSION)
            )
                .from(CONFIGURATION_VALUES).groupBy(CONFIGURATION_VALUES.CLOUD, CONFIGURATION_VALUES.NAME)
                .asTable("latest_versions")

        val latest = dsl.select(
            CONFIGURATION_VALUES.CLOUD,
            CONFIGURATION_VALUES.ID,
            CONFIGURATION_VALUES.NAME,
            CONFIGURATION_VALUES.CONFIG_VALUE,
            CONFIGURATION_VALUES.VERSION
        ).from(
            CONFIGURATION_VALUES.rightJoin(latestVersions).on(
                CONFIGURATION_VALUES.NAME.eq(latestVersions.field(CONFIGURATION_VALUES.NAME))
                    .and(CONFIGURATION_VALUES.VERSION.eq(latestVersions.field(CONFIGURATION_VALUES.VERSION)))
                    .and(CONFIGURATION_VALUES.CLOUD.eq(latestVersions.field(CONFIGURATION_VALUES.CLOUD)))
            )
        ).asTable("latest_configurations")
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
                CloudConfiguration(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    rootDomain = it.value.configValue(CloudConfiguration.ROOT_DOMAIN_KEY).value,
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

    fun listEnvironments(cloud: CloudConfiguration): List<CloudEnvironmentConfiguration> {

        val latestVersions =
            dsl.select(
                CONFIGURATION_VALUES.CLOUD_ENVIRONMENT,
                CONFIGURATION_VALUES.NAME,
                max(CONFIGURATION_VALUES.VERSION).`as`(CONFIGURATION_VALUES.VERSION)
            )
                .from(CONFIGURATION_VALUES).groupBy(CONFIGURATION_VALUES.CLOUD_ENVIRONMENT, CONFIGURATION_VALUES.NAME)
                .asTable("latest_versions")

        val latest = dsl.select(
            CONFIGURATION_VALUES.CLOUD_ENVIRONMENT,
            CONFIGURATION_VALUES.ID,
            CONFIGURATION_VALUES.NAME,
            CONFIGURATION_VALUES.CONFIG_VALUE,
            CONFIGURATION_VALUES.VERSION
        ).from(
            CONFIGURATION_VALUES.rightJoin(latestVersions).on(
                CONFIGURATION_VALUES.NAME.eq(latestVersions.field(CONFIGURATION_VALUES.NAME))
                    .and(CONFIGURATION_VALUES.VERSION.eq(latestVersions.field(CONFIGURATION_VALUES.VERSION)))
                    .and(CONFIGURATION_VALUES.CLOUD_ENVIRONMENT.eq(latestVersions.field(CONFIGURATION_VALUES.CLOUD_ENVIRONMENT)))
            )
        ).asTable("latest_configurations")
        val environments = CLOUDS_ENVIRONMENTS.`as`("clouds")

        return dsl.selectFrom(
            environments.leftJoin(latest).on(environments.ID.eq(latest.field(CONFIGURATION_VALUES.CLOUD_ENVIRONMENT)))
        )
            .where(environments.CLOUD.eq(cloud.id))
            .fetchGroups(
                { it.into(environments) }, { it.into(latest) }
            ).map {
                CloudEnvironmentConfiguration(
                    id = it.key.id!!,
                    name = it.key.name!!,
                    sshSecrets = loadSshCredentials(it.value),
                    configValues = it.value.map {
                        CloudConfigValue(
                            it.getValue(CONFIGURATION_VALUES.NAME)!!,
                            it.getValue(CONFIGURATION_VALUES.CONFIG_VALUE)!!,
                            it.getValue(CONFIGURATION_VALUES.VERSION)!!
                        )
                    },
                    cloud = cloud
                )
            }
    }

    fun updateEnvironment(cloudName: String, environmentName: String, name: String, value: String): Boolean {
        return updateEnvironment(cloudName, environmentName, mapOf(name to value))
    }

    fun updateEnvironment(cloudName: String, environmentName: String, values: Map<String, String>): Boolean {
        val environment = environmentByName(cloudName, environmentName) ?: return false

        return values.map {
            updateEnvironment(environment, it.key, it.value)
        }.all { it }
    }

    private fun updateEnvironment(
        environment: CloudEnvironmentConfiguration,
        name: String,
        value: String
    ): Boolean {
        setConfiguration(EnvironmentId(environment.id), name, value)
        return true
    }

    fun getCloud(name: String): CloudConfiguration? {

        val cloud = listClouds(name).firstOrNull()

        if (cloud == null) {
            logger.info { "cloud '$name' does not exist" }
        }

        return cloud
    }

    fun listTenants(cloudName: String? = null, environment: CloudEnvironmentConfiguration): List<TenantConfiguration> {

        val latestVersions =
                dsl.select(
                        CONFIGURATION_VALUES.TENANT,
                        CONFIGURATION_VALUES.NAME,
                        max(CONFIGURATION_VALUES.VERSION).`as`(CONFIGURATION_VALUES.VERSION)
                )
                        .from(CONFIGURATION_VALUES).groupBy(CONFIGURATION_VALUES.TENANT, CONFIGURATION_VALUES.NAME)
                        .asTable("latest_versions")

        val latest = dsl.select(
            CONFIGURATION_VALUES.TENANT,
            CONFIGURATION_VALUES.ID,
            CONFIGURATION_VALUES.NAME,
            CONFIGURATION_VALUES.CONFIG_VALUE,
            CONFIGURATION_VALUES.VERSION
        ).from(
            CONFIGURATION_VALUES.rightJoin(latestVersions).on(
                    CONFIGURATION_VALUES.NAME.eq(latestVersions.field(CONFIGURATION_VALUES.NAME))
                            .and(
                                    CONFIGURATION_VALUES.VERSION.eq(latestVersions.field(CONFIGURATION_VALUES.VERSION))
                                            .and(CONFIGURATION_VALUES.TENANT.eq(latestVersions.field(CONFIGURATION_VALUES.TENANT)))
                            )
            )
        ).asTable("latest_configurations")
        val tenants = TENANTS.`as`("tenants")

        var condition = tenants.DELETED.isFalse
        condition = condition.and(tenants.ENVRIONMENT.eq(environment.id))

        if (cloudName != null) {
            condition = condition.and(tenants.NAME.eq(cloudName))
        }

        return dsl.selectFrom(tenants.leftJoin(latest).on(tenants.ID.eq(latest.field(CONFIGURATION_VALUES.TENANT))))
                .where(condition)
                .fetchGroups(
                        { it.into(tenants) }, { it.into(latest) }
                ).map {
                    TenantConfiguration(
                            id = it.key.id!!,
                            name = it.key.name!!,
                            environment = environment
                    )
            }
    }

    fun environmentByName(cloudName: String, environmentName: String): CloudEnvironmentConfiguration? {
        val cloud = getCloud(cloudName) ?: return null

        return listEnvironments(cloud).first { it.name == environmentName }
    }

    fun rotateEnvironmentSecrets(cloudName: String, environmentName: String): Boolean {
        val environment = environmentByName(cloudName, environmentName) ?: return false
        generateAndStoreSecrets(environment)

        return true
    }

    private fun generateAndStoreSecrets(id: UUID, name: String) {
        storeSshCredentials(EnvironmentId(id), generateSshCredentials(name))
        storeConsulSecrets(EnvironmentId(id), generateConsulSecrets())
    }

    private fun generateAndStoreSecrets(
        environment: CloudEnvironmentConfiguration
    ) {
        generateAndStoreSecrets(environment.id, environment.name)
    }

    fun getTenant(name: String, cloudName: String, environmentName: String): TenantConfiguration? {
        val environment = environmentByName(cloudName, environmentName) ?: return null
        return listTenants(name, environment).firstOrNull()
    }

    fun createTenant(name: String, cloudName: String, environmentName: String): Boolean {

        val id = UUID.randomUUID()
        val environment = environmentByName(cloudName, environmentName) ?: return false

        dsl.insertInto(TENANTS)
                .columns(
                        TENANTS.ID,
                TENANTS.NAME,
                TENANTS.DELETED,
                TENANTS.ENVRIONMENT,
            )
            .values(id, name, false, environment.id).execute()

        return true
    }

    /*
    fun deleteTenant(cloudName: String) {
        getTenant(cloudName).let {
            val result = dsl.update(TENANTS).set(TENANTS.DELETED, true).where(TENANTS.ID.eq(it.id)).execute()
            result.toString()
        }
    }*/

    private fun setConfiguration(id: IdType, name: String, value: String?) {

        var tenantId: UUID? = null
        var cloudId: UUID? = null
        var cloudEnvironmentId: UUID? = null

        when (id) {
            is CloudId -> cloudId = id.id
            is EnvironmentId -> cloudEnvironmentId = id.id
            is TenantId -> tenantId = id.id
        }

        val condition = when (id) {
            is CloudId -> CONFIGURATION_VALUES.CLOUD.eq(id.id)
            is TenantId -> CONFIGURATION_VALUES.TENANT.eq(id.id)
            is EnvironmentId -> CONFIGURATION_VALUES.CLOUD_ENVIRONMENT.eq(id.id)
        }

        if (value != null) {
            // unfortunately derby does not support limits .limit(1).offset(0)
            val cloud = dsl.selectFrom(CONFIGURATION_VALUES)
                .where(CONFIGURATION_VALUES.NAME.eq(name).and(condition))
                .orderBy(CONFIGURATION_VALUES.VERSION.desc()).fetch()

            dsl.insertInto(CONFIGURATION_VALUES).columns(
                CONFIGURATION_VALUES.ID,
                CONFIGURATION_VALUES.VERSION,
                CONFIGURATION_VALUES.CLOUD,
                CONFIGURATION_VALUES.CLOUD_ENVIRONMENT,
                CONFIGURATION_VALUES.TENANT,
                CONFIGURATION_VALUES.NAME,
                CONFIGURATION_VALUES.CONFIG_VALUE
            ).values(
                UUID.randomUUID(),
                cloud.firstOrNull()?.let { it.version!! + 1 }
                    ?: 0,
                cloudId, cloudEnvironmentId, tenantId, name, value
            )
                .execute()
        }
    }

    private fun generateSshCredentials(name: String): SshSecrets {
        val sshIdentity = generateSshKey(name)
        val sshKey = generateSshKey(name)

        return SshSecrets(
            sshIdentityPrivateKey = sshIdentity.first,
            sshIdentityPublicKey = sshIdentity.second,
            sshPrivateKey = sshKey.first,
            sshPublicKey = sshKey.second,
        )
    }

    private fun loadSshCredentials(list: List<Record5<UUID?, UUID?, String?, String?, Int?>>): SshSecrets {
        return SshSecrets(
            sshIdentityPrivateKey = list.configValue(SSH_IDENTITY_PRIVATE_KEY).value,
            sshIdentityPublicKey = list.configValue(SSH_IDENTITY_PUBLIC_KEY).value,
            sshPrivateKey = list.configValue(SSH_PRIVATE_KEY).value,
            sshPublicKey = list.configValue(SSH_PUBLIC_KEY).value,
        )
    }

    private fun storeSshCredentials(id: IdType, secrets: SshSecrets) {
        setConfiguration(id, SSH_IDENTITY_PRIVATE_KEY, secrets.sshIdentityPrivateKey)
        setConfiguration(id, SSH_IDENTITY_PUBLIC_KEY, secrets.sshIdentityPublicKey)
        setConfiguration(id, SSH_PRIVATE_KEY, secrets.sshPrivateKey)
        setConfiguration(id, SSH_PUBLIC_KEY, secrets.sshPublicKey)
    }

    private fun generateConsulSecrets(): ConsulSecrets {
        return ConsulSecrets(generateConsulSecret(), generateRandomPassword())
    }

    private fun loadConsulSecrets(list: List<Record5<UUID?, UUID?, String?, String?, Int?>>): ConsulSecrets {
        return ConsulSecrets(
            list.configValue(CONSUL_SECRET_KEY).value,
            list.configValue(CONSUL_MASTER_TOKEN_KEY).value
        )
    }

    private fun storeConsulSecrets(id: IdType, secrets: ConsulSecrets) {
        setConfiguration(id, CONSUL_SECRET_KEY, secrets.consul_secret)
        setConfiguration(id, CONSUL_MASTER_TOKEN_KEY, secrets.consul_master_token)
    }

    private fun List<Record5<UUID?, UUID?, String?, String?, Int?>>.configValue(name: String): CloudConfigValue {
        return this.firstOrNull { it.getValue(CONFIGURATION_VALUES.NAME) == name }?.map {
            CloudConfigValue(
                it.getValue(CONFIGURATION_VALUES.NAME)!!,
                it.getValue(CONFIGURATION_VALUES.CONFIG_VALUE)!!,
                it.getValue(CONFIGURATION_VALUES.VERSION)!!
            )
        }!!
    }

    private fun generateConsulSecret(): String {
        val bytes = ByteArray(16)
        try {
            SecureRandom.getInstanceStrong().nextBytes(bytes)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun generateRandomPassword(): String {
        return UUID.randomUUID().toString()
    }
}
