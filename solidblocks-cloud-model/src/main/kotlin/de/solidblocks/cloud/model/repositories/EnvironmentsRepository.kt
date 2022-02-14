package de.solidblocks.cloud.model.repositories

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.resources.ResourcePermissions
import de.solidblocks.cloud.model.ModelConstants.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.model.ModelConstants.CONSUL_SECRET_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_IDENTITY_PRIVATE_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_IDENTITY_PUBLIC_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_PRIVATE_KEY
import de.solidblocks.cloud.model.ModelConstants.SSH_PUBLIC_KEY
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.cloud.model.entities.ConsulSecrets
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.EnvironmentEntity.Companion.ROOT_TOKEN_KEY
import de.solidblocks.cloud.model.entities.SshSecrets
import de.solidblocks.cloud.model.generateSshKey
import de.solidblocks.config.db.tables.references.*
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record5
import java.security.SecureRandom
import java.util.*

class EnvironmentsRepository(dsl: DSLContext, val cloudsRepository: CloudsRepository) : BaseRepository(dsl) {

    val environments = ENVIRONMENTS.`as`("environments")

    fun createEnvironment(
        cloudReference: CloudReference,
        environment: String,
        configValues: List<CloudConfigValue> = emptyList()
    ): EnvironmentEntity? {
        val cloud = cloudsRepository.getCloud(cloudReference) ?: return null

        val id = UUID.randomUUID()

        dsl.insertInto(ENVIRONMENTS)
            .columns(
                ENVIRONMENTS.ID,
                ENVIRONMENTS.CLOUD,
                ENVIRONMENTS.NAME,
                ENVIRONMENTS.DELETED
            )
            .values(id, cloud.id, environment, false).execute()

        generateAndStoreSecrets(id, environment)

        configValues.forEach {
            setConfiguration(EnvironmentId(id), it.name, it.value)
        }

        return getEnvironment(id)
    }

    fun getEnvironment(id: UUID, permissions: ResourcePermissions? = null): EnvironmentEntity? {
        val record =
            dsl.selectFrom(ENVIRONMENTS.join(CLOUDS).on(ENVIRONMENTS.CLOUD.eq(CLOUDS.ID))).where(ENVIRONMENTS.ID.eq(id))
                .fetchOne()
                ?: return null

        val cloud = record.getValue(CLOUDS.NAME)
        val environment = record.getValue(ENVIRONMENTS.NAME)

        return getEnvironment(EnvironmentReference(cloud!!, environment!!))
    }

    fun getEnvironment(reference: EnvironmentReference, permissions: ResourcePermissions? = null): EnvironmentEntity? {
        return listEnvironments(
            CLOUDS.NAME.eq(reference.cloud).and(environments.NAME.eq(reference.environment)),
            permissions = permissions
        ).firstOrNull()
    }

    fun hasEnvironment(reference: EnvironmentReference, permissions: ResourcePermissions? = null) = getEnvironment(reference, permissions) != null

    fun listEnvironments(filter: Condition? = null, permissions: ResourcePermissions? = null): List<EnvironmentEntity> {

        var filterConditions = environments.DELETED.isFalse
        if (filter != null) {
            filterConditions = filterConditions.and(filter)
        }

        val permissionConditions = createPermissionConditions(permissions?.permissions.orEmpty(), CLOUDS, environments)

        val latest = latestConfigurationValuesQuery(CONFIGURATION_VALUES.ENVIRONMENT)

        return dsl.selectFrom(
            environments.leftJoin(CLOUDS).on(environments.CLOUD.eq(CLOUDS.ID)).leftJoin(latest)
                .on(environments.ID.eq(latest.field(CONFIGURATION_VALUES.ENVIRONMENT)))
        ).where(filterConditions).and(permissionConditions)
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
                    cloud = cloudsRepository.getCloud(it.key.cloud!!)!!
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
        val environment = getEnvironment(reference) ?: return false

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

    fun rotateEnvironmentSecrets(reference: EnvironmentReference): Boolean {
        val environment = getEnvironment(reference) ?: return false
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
