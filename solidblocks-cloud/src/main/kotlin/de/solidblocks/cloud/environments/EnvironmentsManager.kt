package de.solidblocks.cloud.environments

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.createConfigValue
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.provisioner.hetzner.Hetzner
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.TransactionalCallable

class EnvironmentsManager(
    val dsl: DSLContext,
    val cloudRepository: CloudRepository,
    val environmentRepository: EnvironmentRepository,
    val usersManager: UsersManager,
    val isDevelopment: Boolean,
) {

    private val logger = KotlinLogging.logger {}

    public fun newTenantsDefaultEnvironment(email: String): EnvironmentEntity? {
        val environments = environmentRepository.listEnvironments()
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
    ) = dsl.transactionResult(
        TransactionalCallable create1@{

            if (!cloudRepository.hasCloud(reference)) {
                logger.info { "cloud '${reference.cloud}' does not exist" }
                return@create1 false
            }

            if (environmentRepository.hasEnvironment(reference.toEnvironment(name))) {
                logger.info { "environment '$name' already exist in cloud '${reference.cloud}'" }
                return@create1 false
            }

            val envReference = environmentRepository.createEnvironment(
                reference,
                name,
                listOf(
                    createConfigValue(ModelConstants.GITHUB_TOKEN_RO_KEY, githubReadOnlyToken),
                    createConfigValue(Hetzner.HETZNER_CLOUD_API_TOKEN_RO_KEY, hetznerCloudApiTokenReadOnly),
                    createConfigValue(Hetzner.HETZNER_CLOUD_API_TOKEN_RW_KEY, hetznerCloudApiTokenReadWrite),
                    createConfigValue(Hetzner.HETZNER_DNS_API_TOKEN_RW_KEY, hetznerDnsApiToken),
                )
            ) ?: throw RuntimeException("failed to create environment '$name' for '$reference' not found")

            usersManager.createEnvironmentUser(envReference, email, password)

            true
        }
    )

    fun getOptional(reference: EnvironmentReference) = environmentRepository.getEnvironment(reference)
}
