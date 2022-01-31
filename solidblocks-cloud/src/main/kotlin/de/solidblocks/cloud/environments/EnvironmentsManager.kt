package de.solidblocks.cloud.environments

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.createConfigValue
import de.solidblocks.cloud.model.entities.toReference
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.provisioner.hetzner.Hetzner
import mu.KotlinLogging
import org.jooq.DSLContext

class EnvironmentsManager(
    val dsl: DSLContext,
    val cloudsManager: CloudsManager,
    val environmentsRepository: EnvironmentsRepository,
    val usersManager: UsersManager,
    val isDevelopment: Boolean,
) {

    private val logger = KotlinLogging.logger {}

    public fun newTenantsDefaultEnvironment(email: String): EnvironmentEntity? {
        val environments = environmentsRepository.listEnvironments()
        return environments.firstOrNull()
    }

    fun create(
        reference: CloudReference,
        name: String,
        email: String,
        password: String,
        githubReadOnlyToken: String,
        hetznerCloudApiTokenReadOnly: String,
        hetznerCloudApiTokenReadWrite: String,
        hetznerDnsApiToken: String
    ): EnvironmentEntity? {

        if (!cloudsManager.verifyReference(reference)) {
            return null
        }

        if (environmentsRepository.hasEnvironment(reference.toEnvironment(name))) {
            logger.info { "environment '$name' already exist in cloud '${reference.cloud}'" }
            return null
        }

        val environment = environmentsRepository.createEnvironment(
            reference,
            name,
            listOf(
                createConfigValue(ModelConstants.GITHUB_TOKEN_RO_KEY, githubReadOnlyToken),
                createConfigValue(Hetzner.HETZNER_CLOUD_API_TOKEN_RO_KEY, hetznerCloudApiTokenReadOnly),
                createConfigValue(Hetzner.HETZNER_CLOUD_API_TOKEN_RW_KEY, hetznerCloudApiTokenReadWrite),
                createConfigValue(Hetzner.HETZNER_DNS_API_TOKEN_RW_KEY, hetznerDnsApiToken),
            )
        ) ?: throw RuntimeException("failed to create environment '$name' for '$reference' not found")

        usersManager.createEnvironmentUser(environment.toReference(), email, password)
        return environment
    }

    fun verifyReference(reference: EnvironmentReference): Boolean {
        if (!cloudsManager.verifyReference(reference)) {
            return false
        }

        if (!environmentsRepository.hasEnvironment(reference)) {
            logger.error { "environment '${reference.environment}' not found" }
            return false
        }

        return true
    }

    fun getOptional(reference: EnvironmentReference) = environmentsRepository.getEnvironment(reference)
}
