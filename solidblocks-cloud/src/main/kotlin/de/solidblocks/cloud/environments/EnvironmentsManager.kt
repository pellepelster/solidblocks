package de.solidblocks.cloud.environments

import de.solidblocks.base.CreationResult
import de.solidblocks.base.api.messageResponses
import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.validateId
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.api.EnvironmentCreateRequest
import de.solidblocks.cloud.model.ErrorCodes
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.ValidationResult
import de.solidblocks.cloud.model.entities.CloudEntity
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.createConfigValue
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.cloud.model.toCreationResult
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.provisioner.hetzner.Hetzner
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.TransactionalCallable
import java.util.*

class EnvironmentsManager(
    val dsl: DSLContext,
    val cloudsManager: CloudsManager,
    val environmentsRepository: EnvironmentsRepository,
    val environmentScheduler: EnvironmentScheduler,
    val usersManager: UsersManager,
) {

    private val logger = KotlinLogging.logger {}

    fun newTenantsDefaultEnvironment(email: String): EnvironmentEntity? {

        val environments = listEnvironments(email)

        if (environments.size > 1) {
            logger.error { "could not find default environment to use, expected one, found ${environments.size}" }
        }

        return environments.singleOrNull()
    }

    fun createEnvironmentForDefaultCloud(email: String, request: EnvironmentCreateRequest): CreationResult<EnvironmentEntity> {
        val cloud = cloudsManager.newEnvironmentsDefaultCloud(email)
            ?: return CreationResult.error(ErrorCodes.ENVIRONMENT.DEFAULT_NOT_FOUND)

        return create(CloudReference(cloud.name), email, request)
    }

    fun create(reference: CloudReference, email: String, request: EnvironmentCreateRequest) = dsl.transactionResult(
        TransactionalCallable tc@{

            val result = validate(email, request, reference)
            if (result.hasErrors()) {
                return@tc result.toCreationResult()
            }

            logger.info { "creating environment '${request.environment}' for cloud '$reference'" }

            val environment = environmentsRepository.createEnvironment(
                reference, request.environment!!,
                listOf(
                    createConfigValue(ModelConstants.GITHUB_TOKEN_RO_KEY, request.githubReadOnlyToken!!),
                    createConfigValue(Hetzner.HETZNER_CLOUD_API_TOKEN_RO_KEY, request.hetznerCloudApiTokenReadOnly!!),
                    createConfigValue(Hetzner.HETZNER_CLOUD_API_TOKEN_RW_KEY, request.hetznerCloudApiTokenReadWrite!!),
                    createConfigValue(Hetzner.HETZNER_DNS_API_TOKEN_RW_KEY, request.hetznerDnsApiToken!!),
                )
            )
                ?: return@tc CreationResult<EnvironmentEntity>(messages = ErrorCodes.ENVIRONMENT.CREATE_FAILED.messageResponses(EnvironmentCreateRequest::environment))

            usersManager.createEnvironmentUser(
                environment.reference, request.email!!,
                request.password
                    ?: UUID.randomUUID().toString()
            )

            val action = environmentScheduler.scheduleApplyTask(environment.reference)
            CreationResult(environment, actions = listOf(action))
        }
    )

    fun validate(email: String, request: EnvironmentCreateRequest, reference: CloudReference? = null): ValidationResult {

        if (request.environment == null || request.environment.isBlank()) {
            return ValidationResult.error(EnvironmentCreateRequest::environment, ErrorCodes.MANDATORY)
        }

        if (request.email == null || request.email.isBlank()) {
            return ValidationResult.error(EnvironmentCreateRequest::email, ErrorCodes.MANDATORY)
        }

        if (!validateId(request.environment)) {
            return ValidationResult.error(EnvironmentCreateRequest::environment, ErrorCodes.ENVIRONMENT.INVALID)
        }

        var defaultCloud: CloudEntity? = null

        if (reference == null) {
            defaultCloud = cloudsManager.newEnvironmentsDefaultCloud(email)
                ?: return ValidationResult.error(ErrorCodes.ENVIRONMENT.DEFAULT_CLOUD_NOT_FOUND)
        }

        if (reference != null || defaultCloud != null) {
            if (environmentsRepository.hasEnvironment(listOfNotNull(reference, defaultCloud?.reference).first().toEnvironment(request.environment))) {
                return ValidationResult.error(EnvironmentCreateRequest::environment, ErrorCodes.DUPLICATE)
            }
        }

        if (usersManager.hasUser(request.email)) {
            return ValidationResult.error(EnvironmentCreateRequest::email, ErrorCodes.DUPLICATE)
        }

        return ValidationResult.ok()
    }

    fun verifyReference(reference: EnvironmentReference): Boolean {
        if (!cloudsManager.hasCloud(reference)) {
            logger.error { "cloud '${reference.cloud}' not found" }
            return false
        }

        if (!environmentsRepository.hasEnvironment(reference)) {
            logger.error { "environment '${reference.environment}' not found" }
            return false
        }

        return true
    }

    fun listEnvironments(email: String): List<EnvironmentEntity> {
        val user = usersManager.getUser(email) ?: return emptyList()
        return environmentsRepository.listEnvironments(permissions = user.permissions())
    }

    fun getEnvironment(reference: EnvironmentReference) = environmentsRepository.getEnvironment(reference)
}
