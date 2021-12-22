package de.solidblocks.cloud.model

import de.solidblocks.base.Utils.Companion.generateSshKey
import de.solidblocks.cloud.model.ModelConstants.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.model.ModelConstants.CONSUL_SECRET_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_IDENTITY_PRIVATE_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_IDENTITY_PUBLIC_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_PRIVATE_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_PUBLIC_KEY
import de.solidblocks.cloud.model.model.*
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.ENVIRONMENTS
import org.jooq.DSLContext
import org.jooq.Record5
import java.security.SecureRandom
import java.util.*

class EnvironmentRepository(dsl: DSLContext, val cloudRepository: CloudRepository) : BaseRepository(dsl) {

    fun createEnvironment(
        cloud: String,
        environment: String,
        configValues: List<CloudConfigValue> = emptyList()
    ): EnvironmentModel? {
        val cloud = cloudRepository.getCloud(cloud) ?: return null

        logger.info { "creating environment '$environment' for cloud '$cloud'" }
        val id = UUID.randomUUID()

        dsl.insertInto(ENVIRONMENTS)
            .columns(
                ENVIRONMENTS.ID,
                ENVIRONMENTS.CLOUD,
                ENVIRONMENTS.NAME
            )
            .values(id, cloud.id, environment).execute()

        generateAndStoreSecrets(id, environment)

        configValues.forEach {
            setConfiguration(EnvironmentId(id), it.name, it.value)
        }

        return getEnvironment(cloud.name, environment)
    }

    fun listEnvironments(cloud: CloudModel): List<EnvironmentModel> {

        val latest = latestConfigurationValues(CONFIGURATION_VALUES.ENVIRONMENT)

        val environments = ENVIRONMENTS.`as`("environments")

        return dsl.selectFrom(
            environments.leftJoin(latest).on(environments.ID.eq(latest.field(CONFIGURATION_VALUES.ENVIRONMENT)))
        )
            .where(environments.CLOUD.eq(cloud.id))
            .fetchGroups(
                { it.into(environments) }, { it.into(latest) }
            ).map {
                EnvironmentModel(
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
        val environment = getEnvironment(cloudName, environmentName) ?: return false

        return values.map {
            updateEnvironment(environment, it.key, it.value)
        }.all { it }
    }

    private fun updateEnvironment(
        environment: EnvironmentModel,
        name: String,
        value: String
    ): Boolean {
        setConfiguration(EnvironmentId(environment.id), name, value)
        return true
    }

    fun getEnvironment(cloudName: String, environment: String): EnvironmentModel {
        val cloud = cloudRepository.getCloud(cloudName)

        return listEnvironments(cloud).first { it.name == environment }
    }

    fun hasEnvironment(cloud: String, environmentName: String): Boolean {
        val cloud = cloudRepository.getCloud(cloud) ?: return false
        return listEnvironments(cloud).any { it.name == environmentName }
    }

    fun rotateEnvironmentSecrets(cloud: String, environment: String): Boolean {
        val environment = getEnvironment(cloud, environment) ?: return false
        generateAndStoreSecrets(environment)
        return true
    }

    private fun storeConsulSecrets(id: IdType, secrets: ConsulSecrets) {
        setConfiguration(id, CONSUL_SECRET_KEY, secrets.consul_secret)
        setConfiguration(id, CONSUL_MASTER_TOKEN_KEY, secrets.consul_master_token)
    }

    private fun generateRandomPassword(): String {
        return UUID.randomUUID().toString()
    }

    private fun generateConsulSecrets(): ConsulSecrets {
        return ConsulSecrets(generateConsulSecret(), generateRandomPassword())
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

    private fun generateAndStoreSecrets(id: UUID, name: String) {
        storeSshCredentials(EnvironmentId(id), generateSshCredentials(name))
        storeConsulSecrets(EnvironmentId(id), generateConsulSecrets())
    }

    private fun storeSshCredentials(id: IdType, secrets: SshSecrets) {
        setConfiguration(id, SSH_IDENTITY_PRIVATE_KEY, secrets.sshIdentityPrivateKey)
        setConfiguration(id, SSH_IDENTITY_PUBLIC_KEY, secrets.sshIdentityPublicKey)
        setConfiguration(id, SSH_PRIVATE_KEY, secrets.sshPrivateKey)
        setConfiguration(id, SSH_PUBLIC_KEY, secrets.sshPublicKey)
    }

    private fun generateAndStoreSecrets(environment: EnvironmentModel) {
        generateAndStoreSecrets(environment.id, environment.name)
    }

    private fun loadSshCredentials(list: List<Record5<UUID?, UUID?, String?, String?, Int?>>): SshSecrets {
        return SshSecrets(
            sshIdentityPrivateKey = list.configValue(SSH_IDENTITY_PRIVATE_KEY).value,
            sshIdentityPublicKey = list.configValue(SSH_IDENTITY_PUBLIC_KEY).value,
            sshPrivateKey = list.configValue(SSH_PRIVATE_KEY).value,
            sshPublicKey = list.configValue(SSH_PUBLIC_KEY).value,
        )
    }
}
