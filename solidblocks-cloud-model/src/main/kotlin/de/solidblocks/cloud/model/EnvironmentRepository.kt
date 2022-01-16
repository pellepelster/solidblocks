package de.solidblocks.cloud.model

import de.solidblocks.base.resources.CloudResource
import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.cloud.model.ModelConstants.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.model.ModelConstants.CONSUL_SECRET_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_IDENTITY_PRIVATE_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_IDENTITY_PUBLIC_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_PRIVATE_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_PUBLIC_KEY
import de.solidblocks.cloud.model.entities.*
import de.solidblocks.cloud.model.entities.EnvironmentEntity.Companion.ROOT_TOKEN_KEY
import de.solidblocks.config.db.tables.references.CLOUDS
import de.solidblocks.config.db.tables.references.CONFIGURATION_VALUES
import de.solidblocks.config.db.tables.references.ENVIRONMENTS
import org.jooq.DSLContext
import org.jooq.Record5
import java.security.SecureRandom
import java.util.*

class EnvironmentRepository(dsl: DSLContext, val cloudRepository: CloudRepository) : BaseRepository(dsl) {

    fun createEnvironment(
            reference: CloudResource,
            name: String,
            configValues: List<CloudConfigValue> = emptyList()
    ): EnvironmentResource {
        val cloud = cloudRepository.getCloud(reference)
        logger.info { "creating environment '$reference.environment' for cloud '$cloud'" }

        val id = UUID.randomUUID()

        dsl.insertInto(ENVIRONMENTS)
            .columns(
                ENVIRONMENTS.ID,
                ENVIRONMENTS.CLOUD,
                ENVIRONMENTS.NAME
            )
            .values(id, cloud.id, name).execute()

        generateAndStoreSecrets(id, name)

        configValues.forEach {
            setConfiguration(EnvironmentId(id), it.name, it.value)
        }

        return reference.toEnvironment(name)
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

    fun updateEnvironment(reference: EnvironmentResource, name: String, value: String): Boolean {
        return updateEnvironment(reference, mapOf(name to value))
    }

    fun updateRootToken(reference: EnvironmentResource, rootToken: String) {
        updateEnvironment(reference, ROOT_TOKEN_KEY, rootToken)
    }

    fun updateEnvironment(reference: EnvironmentResource, values: Map<String, String>): Boolean {
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

    fun getEnvironment(id: UUID): EnvironmentEntity? {
        val record = dsl.selectFrom(ENVIRONMENTS.join(CLOUDS).on(ENVIRONMENTS.CLOUD.eq(CLOUDS.ID))).where(ENVIRONMENTS.ID.eq(id)).fetchOne()
            ?: return null

        val cloud = record.getValue(CLOUDS.NAME)
        val environment = record.getValue(ENVIRONMENTS.NAME)

        return getEnvironment(EnvironmentResource(cloud!!, environment!!))
    }

    fun getEnvironment(reference: EnvironmentResource): EnvironmentEntity {
        val cloud = cloudRepository.getCloud(reference)
        return listEnvironments(cloud).first { it.name == reference.environment }
    }

    fun getOptional(reference: EnvironmentResource): EnvironmentEntity? {
        val cloud = cloudRepository.getCloud(reference)
        return listEnvironments(cloud).firstOrNull { it.name == reference.environment }
    }

    fun hasEnvironment(reference: EnvironmentResource): Boolean {
        val cloud = cloudRepository.getCloud(reference)
        return listEnvironments(cloud).any { it.name == reference.environment }
    }

    fun rotateEnvironmentSecrets(reference: EnvironmentResource): Boolean {
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
