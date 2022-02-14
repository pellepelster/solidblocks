package de.solidblocks.cloud.environments

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.validateId
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.api.EnvironmentCreateRequest
import de.solidblocks.cloud.model.CreationResult
import de.solidblocks.cloud.model.ErrorCodes
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.ValidationResult
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.cloud.model.entities.createConfigValue
import de.solidblocks.cloud.model.entities.toReference
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.cloud.tenants.api.TenantCreateRequest
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

    public fun newTenantsDefaultEnvironment(email: String) = listEnvironmentsForUser(email).singleOrNull()

    fun createEnvironmentForDefaultCloud(name: String, email: String, password: String): CreationResult<TenantEntity> {
        val cloud = cloudsManager.newEnvironmentsDefaultCloud(email)
            ?: return CreationResult.error(ErrorCodes.CLOUD.NOT_FOUND)
        return create(CloudReference(cloud.name), name, email, password)
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

    fun validate(request: EnvironmentCreateRequest): ValidationResult {

        if (request.environment == null || request.environment.isBlank()) {
            return ValidationResult.error(EnvironmentCreateRequest::environment, ErrorCodes.MANDATORY)
        }

        if (request.email == null || request.email.isBlank()) {
            return ValidationResult.error(EnvironmentCreateRequest::email, ErrorCodes.MANDATORY)
        }

        if (!validateId(request.environment)) {
            return ValidationResult.error(EnvironmentCreateRequest::environment, ErrorCodes.ENVIRONMENT.INVALID)
        }

        val cloud = cloudsManager.newEnvironmentsDefaultCloud(request.email)?: return ValidationResult.error(ErrorCodes.ENVIRONMENT.CLOUD_NOT_FOUND)

        if (environmentsRepository.hasEnvironment(cloud.toReference().toEnvironment(request.environment))) {
            return ValidationResult.error(EnvironmentCreateRequest::environment, ErrorCodes.ENVIRONMENT.DUPLICATE)
        }

        if (usersManager.hasUser(request.email)) {
            return ValidationResult.error(EnvironmentCreateRequest::email, ErrorCodes.EMAIL.DUPLICATE)
        }

        return ValidationResult.ok()
    }


    fun listEnvironmentsForUser(email: String): List<EnvironmentEntity> {
        val user = usersManager.getUser(email) ?: return emptyList()
        return environmentsRepository.listEnvironments(permissions = user.permissions())
    }


    fun getOptional(reference: EnvironmentReference) = environmentsRepository.getEnvironment(reference)
}
