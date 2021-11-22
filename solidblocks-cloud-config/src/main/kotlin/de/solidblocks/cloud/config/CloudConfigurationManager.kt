package de.solidblocks.cloud.config

import de.solidblocks.base.Utils.Companion.generateSshKey
import de.solidblocks.cloud.config.SeedConfig.Companion.CONFIG_RO_PASSWORD
import de.solidblocks.cloud.config.SeedConfig.Companion.CONFIG_RO_USERNAME
import de.solidblocks.cloud.config.SeedConfig.Companion.CONFIG_RW_PASSWORD
import de.solidblocks.cloud.config.SeedConfig.Companion.CONFIG_RW_USERNAME
import de.solidblocks.cloud.config.SshConfig.Companion.CONFIG_SSH_IDENTITY_PRIVATE_KEY
import de.solidblocks.cloud.config.SshConfig.Companion.CONFIG_SSH_IDENTITY_PUBLIC_KEY
import de.solidblocks.cloud.config.SshConfig.Companion.CONFIG_SSH_PRIVATE_KEY
import de.solidblocks.cloud.config.SshConfig.Companion.CONFIG_SSH_PUBLIC_KEY
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

    private val logger = KotlinLogging.logger {}

    fun createCloud(name: String, rootDomain: String, configValues: List<CloudConfigValue> = emptyList()): Boolean {
        if (hasCloud(name)) {
            logger.info { "cloud '${name}' already exists" }
            return false
        }

        val id = UUID.randomUUID()

        dsl.insertInto(CLOUDS)
                .columns(
                        CLOUDS.ID,
                        CLOUDS.NAME,
                        CLOUDS.DELETED,
                )
                .values(id, name, false).execute()

        setConfiguration(CloudId(id), CloudConfig.ROOT_DOMAIN_KEY, rootDomain)

        configValues.forEach {
            setConfiguration(CloudId(id), it.name, it.value)
        }

        return true
    }

    fun createEnvironment(cloudName: String, environmentName: String, configValues: List<CloudConfigValue> = emptyList()): Boolean {
        if (!hasCloud(cloudName)) {
            logger.info { "cloud '${cloudName}' does not exist" }
            return false
        }

        val cloud = cloudByName(cloudName)

        val id = UUID.randomUUID()

        dsl.insertInto(CLOUDS_ENVIRONMENTS)
                .columns(
                        CLOUDS_ENVIRONMENTS.ID,
                        CLOUDS_ENVIRONMENTS.CLOUD,
                        CLOUDS_ENVIRONMENTS.NAME)
                .values(id, cloud.id, environmentName).execute()

        storeSshConfig(EnvironmentId(id), createSshConfig(environmentName))

        configValues.forEach {
            setConfiguration(EnvironmentId(id), it.name, it.value)
        }

        return true
    }

    fun hasCloud(name: String): Boolean {
        return dsl.fetchCount(CLOUDS, CLOUDS.NAME.eq(name).and(CLOUDS.DELETED.isFalse)) == 1
    }

    fun listClouds(name: String? = null): List<CloudConfig> {

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
                    CloudConfig(
                            id = it.key.id!!,
                            name = it.key.name!!,
                            rootDomain = it.value.configValue(CloudConfig.ROOT_DOMAIN_KEY).value,
                            it.value.map {
                                CloudConfigValue(
                                        it.getValue(CONFIGURATION_VALUES.NAME)!!,
                                        it.getValue(CONFIGURATION_VALUES.CONFIG_VALUE)!!,
                                        it.getValue(CONFIGURATION_VALUES.VERSION)!!
                                )
                            },
                            listEnvironments(CloudId(it.key.id!!))
                    )
                }
    }

    fun listEnvironments(id: CloudId): List<CloudEnvironmentConfig> {

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

        return dsl.selectFrom(environments.leftJoin(latest).on(environments.ID.eq(latest.field(CONFIGURATION_VALUES.CLOUD_ENVIRONMENT))))
                .where(environments.CLOUD.eq(id.id))
                .fetchGroups(
                        { it.into(environments) }, { it.into(latest) }
                ).map {
                    CloudEnvironmentConfig(
                            id = it.key.id!!,
                            name = it.key.name!!,
                            sshConfig = loadSshConfig(it.value),
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


    fun updateEnvironment(cloudName: String, environmentName: String, values: Map<String, String>): Boolean {
        return values.map {
            updateEnvironment(cloudName, environmentName, it.key, it.value)
        }.all { it }
    }

    fun updateEnvironment(cloudName: String, environmentName: String, name: String, value: String): Boolean {
        if (!hasCloud(cloudName)) {
            logger.info { "cloud '${cloudName}' does not exist" }
            return false
        }

        val cloud = cloudByName(cloudName)

        val environment = cloud.environments.firstOrNull { it.name == environmentName } ?: return false
        setConfiguration(EnvironmentId(environment.id), name, value)

        return true
    }


    fun list(cloudName: String? = null): List<TenantConfig> {

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

        if (cloudName != null) {
            condition = condition.and(tenants.NAME.eq(cloudName))
        }

        return dsl.selectFrom(tenants.leftJoin(latest).on(tenants.ID.eq(latest.field(CONFIGURATION_VALUES.TENANT))))
                .where(condition)
                .fetchGroups(
                        { it.into(tenants) }, { it.into(latest) }
                ).map {
                    val seedConfig = loadSeedConfig(it.value)
                    TenantConfig(
                            id = it.key.id!!,
                            name = it.key.name!!
                    )
                }
    }

    fun cloudByName(name: String): CloudConfig {
        return listClouds(name).first()
    }

    fun environmentByName(cloudName: String, environmentName: String): CloudEnvironmentConfig {
        val cloud = cloudByName(cloudName)
        return cloud.environments.first { it.name == environmentName }
    }

    fun regenerateCloudSecrets(cloudName: String, environmentName: String): Boolean {
        if (!hasCloud(cloudName)) {
            logger.info { "cloud '${cloudName}' does not exist" }
            return false
        }

        val cloud = cloudByName(cloudName)
        val environment = cloud.environments.firstOrNull { it.name == environmentName }

        if (environment == null) {
            logger.info { "environment '${environmentName}' does not exist for cloud '${cloudName}'" }
            return false
        }

        storeSshConfig(EnvironmentId(environment.id), createSshConfig(environmentName))

        return true
    }

    fun getTenant(name: String): TenantConfig {
        return list(name).first()
    }

    fun hasTenant(name: String): Boolean {
        return dsl.fetchCount(TENANTS, TENANTS.NAME.eq(name).and(TENANTS.DELETED.isFalse)) == 1
    }


    fun create(cloudName: String, domain: String, email: String, configuration: List<CloudConfigValue>) {

        val cloudId = UUID.randomUUID()

        dsl.insertInto(TENANTS)
                .columns(
                        TENANTS.ID,
                        TENANTS.NAME,
                        TENANTS.DELETED,
                )
                .values(cloudId, cloudName, false).execute()

        configuration.forEach {
            setConfiguration(TenantId(cloudId), it.name, it.value)
        }

        storeSshConfig(TenantId(cloudId), createSshConfig(cloudName))
        storeSeedConfig(cloudId, createSeedConfig(cloudName))
        storeSolidblocksConfig(TenantId(cloudId), createSolidblocksConfig(domain, email))
    }

    fun delete(cloudName: String) {
        getTenant(cloudName).let {
            val result = dsl.update(TENANTS).set(TENANTS.DELETED, true).where(TENANTS.ID.eq(it.id)).execute()
            result.toString()
        }
    }

    sealed class IdType(private val id: UUID)
    class CloudId(val id: UUID) : IdType(id)
    class EnvironmentId(val id: UUID) : IdType(id)
    class TenantId(val id: UUID) : IdType(id)

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
            ).values(UUID.randomUUID(), cloud.firstOrNull()?.let { it.version!! + 1 }
                    ?: 0, cloudId, cloudEnvironmentId, tenantId, name, value)
                    .execute()
        }
    }

    private fun createSshConfig(name: String): SshConfig {
        val sshIdentity = generateSshKey(name)
        val sshKey = generateSshKey(name)

        return SshConfig(
                sshIdentityPrivateKey = sshIdentity.first,
                sshIdentityPublicKey = sshIdentity.second,
                sshPrivateKey = sshKey.first,
                sshPublicKey = sshKey.second,
        )
    }

    private fun createSeedConfig(cloudName: String): SeedConfig {
        return SeedConfig(
                rwUsername = "$cloudName-rw",
                rwPassword = generateRandomPassword(),
                roUsername = "$cloudName-ro",
                roPassword = generateRandomPassword(),
        )
    }

    private fun loadSeedConfig(list: List<Record5<UUID?, UUID?, String?, String?, Int?>>): SeedConfig {
        return SeedConfig(
                rwUsername = list.configValue(CONFIG_RW_USERNAME).value,
                rwPassword = list.configValue(CONFIG_RW_PASSWORD).value,
                roUsername = list.configValue(CONFIG_RO_USERNAME).value,
                roPassword = list.configValue(CONFIG_RO_PASSWORD).value,
        )
    }

    private fun loadSshConfig(list: List<Record5<UUID?, UUID?, String?, String?, Int?>>): SshConfig {
        return SshConfig(
                sshIdentityPrivateKey = list.configValue(CONFIG_SSH_IDENTITY_PRIVATE_KEY).value,
                sshIdentityPublicKey = list.configValue(CONFIG_SSH_IDENTITY_PUBLIC_KEY).value,
                sshPrivateKey = list.configValue(CONFIG_SSH_PRIVATE_KEY).value,
                sshPublicKey = list.configValue(CONFIG_SSH_PUBLIC_KEY).value,
        )
    }

    private fun loadSolidblocksConfig(
            list: List<Record5<UUID?, UUID?, String?, String?, Int?>>,
            seedConfig: SeedConfig,
            cloudName: String
    ): SolidblocksConfig {
        return SolidblocksConfig(
                consulMasterToken = list.configValue(CONFIG_CONSUL_MASTER_TOKEN_KEY).value,
                dockerRegistry = DockerRegistryConfig(
                        address = "https://seed.$cloudName.${list.configValue(CONFIG_DOMAIN_KEY).value}",
                        username = seedConfig.roUsername,
                        password = seedConfig.roPassword
                ),
                rootPassword = list.configValue(CONFIG_ROOT_PASSWORD_KEY).value,
                apiKey = list.configValue(CONFIG_API_KEY).value,
                backupPassword = list.configValue(CONFIG_BACKUP_PASSWORD_KEY).value,
                consulSecret = list.configValue(CONFIG_CONSUL_SECRET_KEY).value,
                domain = list.configValue(CONFIG_DOMAIN_KEY).value,
                adminEmail = list.configValue(CONFIG_ADMIN_EMAIL_KEY).value,
                adminPassword = list.configValue(CONFIG_ADMIN_PASSWORD_KEY).value,
        )
    }

    private fun storeSshConfig(id: IdType, config: SshConfig) {
        setConfiguration(id, CONFIG_SSH_IDENTITY_PRIVATE_KEY, config.sshIdentityPrivateKey)
        setConfiguration(id, CONFIG_SSH_IDENTITY_PUBLIC_KEY, config.sshIdentityPublicKey)
        setConfiguration(id, CONFIG_SSH_PRIVATE_KEY, config.sshPrivateKey)
        setConfiguration(id, CONFIG_SSH_PUBLIC_KEY, config.sshPublicKey)
    }

    private fun storeSolidblocksConfig(id: IdType, config: SolidblocksConfig) {
        setConfiguration(id, CONFIG_CONSUL_MASTER_TOKEN_KEY, config.consulMasterToken)
        setConfiguration(id, CONFIG_CONSUL_SECRET_KEY, config.consulSecret)
        setConfiguration(id, CONFIG_ROOT_USERNAME_KEY, config.rootUsername)
        setConfiguration(id, CONFIG_ROOT_PASSWORD_KEY, config.rootPassword)
        setConfiguration(id, CONFIG_API_KEY, config.apiKey)
        setConfiguration(id, CONFIG_BACKUP_PASSWORD_KEY, config.backupPassword)
        setConfiguration(id, CONFIG_DOMAIN_KEY, config.domain)
        setConfiguration(id, CONFIG_ADMIN_EMAIL_KEY, config.adminEmail)
        setConfiguration(id, CONFIG_ADMIN_PASSWORD_KEY, config.adminPassword)
    }

    private fun storeSeedConfig(cloudId: UUID, config: SeedConfig) {
        setConfiguration(TenantId(cloudId), CONFIG_RW_USERNAME, config.rwUsername)
        setConfiguration(TenantId(cloudId), CONFIG_RW_PASSWORD, config.rwPassword)
        setConfiguration(TenantId(cloudId), CONFIG_RO_USERNAME, config.roUsername)
        setConfiguration(TenantId(cloudId), CONFIG_RO_PASSWORD, config.roPassword)
    }

    private fun createSolidblocksConfig(domain: String, adminEmail: String): SolidblocksConfig {
        return SolidblocksConfig(
                consulMasterToken = generateRandomPassword(),
                dockerRegistry =
                DockerRegistryConfig(
                        address = "address",
                        username = "user",
                        password = "password"
                ),
                rootPassword = generateRandomPassword(),
                apiKey = generateRandomPassword(),
                backupPassword = generateRandomPassword(),
                consulSecret = generateConsulSecret(),
                domain = domain,
                adminEmail = adminEmail,
                adminPassword = generateRandomPassword()
        )
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
