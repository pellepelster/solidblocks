package de.solidblocks.cloud.model

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.generateSshKey
import de.solidblocks.cloud.model.ModelConstants.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.model.ModelConstants.CONSUL_SECRET_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_IDENTITY_PRIVATE_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_IDENTITY_PUBLIC_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_PRIVATE_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_PUBLIC_KEY
import de.solidblocks.cloud.model.entities.*
import de.solidblocks.cloud.model.entities.EnvironmentEntity.Companion.ROOT_TOKEN_KEY
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.ENVIRONMENTS
import org.jooq.DSLContext
import org.jooq.Record5
import java.security.SecureRandom
import java.util.*

class EnvironmentRepository(dsl: DSLContext, val cloudRepository: CloudRepository) : BaseRepository(dsl) {

    fun createEnvironment(
        reference: EnvironmentReference,
        configValues: List<CloudConfigValue> = emptyList()
    ): EnvironmentEntity {
        val cloud = cloudRepository.getCloud(reference)

        logger.info { "creating environment '$reference.environment' for cloud '$cloud'" }
        val id = UUID.randomUUID()

        dsl.insertInto(ENVIRONMENTS)
            .columns(
                ENVIRONMENTS.ID,
                ENVIRONMENTS.CLOUD,
                ENVIRONMENTS.NAME
            )
            .values(id, cloud.id, reference.environment).execute()

        generateAndStoreSecrets(id, reference.environment)

        configValues.forEach {
            setConfiguration(EnvironmentId(id), it.name, it.value)
        }

        return getEnvironment(reference)
    }

    fun listEnvironments(cloud: CloudEntity): List<EnvironmentEntity> {

        val latest = latestConfigurationValues(CONFIGURATION_VALUES.ENVIRONMENT)

        val environments = ENVIRONMENTS.`as`("environments")

        return dsl.selectFrom(
            environments.leftJoin(latest).on(environments.ID.eq(latest.field(CONFIGURATION_VALUES.ENVIRONMENT)))
        )
            .where(environments.CLOUD.eq(cloud.id))
            .fetchGroups(
                { it.into(environments) }, { it.into(latest) }
            ).map {
                EnvironmentEntity(
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

    fun updateEnvironment(reference: EnvironmentReference, name: String, value: String): Boolean {
        return updateEnvironment(reference, mapOf(name to value))
    }

    fun updateRootToken(reference: EnvironmentReference, rootToken: String) {
        updateEnvironment(reference, ROOT_TOKEN_KEY, rootToken)
    }

    fun updateEnvironment(reference: EnvironmentReference, values: Map<String, String>): Boolean {
        val environment = getEnvironment(reference)

        return values.map {
            updateEnvironment(environment, it.key, it.value)
        }.all { it }
    }

    private fun updateEnvironment(
        environment: EnvironmentEntity,
        name: String,
        value: String
    ): Boolean {
        setConfiguration(EnvironmentId(environment.id), name, value)
        return true
    }

    fun getEnvironment(reference: EnvironmentReference): EnvironmentEntity {
        val cloud = cloudRepository.getCloud(reference)
        return listEnvironments(cloud).first { it.name == reference.environment }
    }

    fun hasEnvironment(reference: EnvironmentReference): Boolean {
        val cloud = cloudRepository.getCloud(reference)
        return listEnvironments(cloud).any { it.name == reference.environment }
    }

    fun rotateEnvironmentSecrets(reference: EnvironmentReference): Boolean {
        val environment = getEnvironment(reference)
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

    private fun generateAndStoreSecrets(environment: EnvironmentEntity) {
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
