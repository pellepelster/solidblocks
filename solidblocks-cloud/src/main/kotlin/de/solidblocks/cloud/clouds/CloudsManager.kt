package de.solidblocks.cloud.clouds

import de.solidblocks.base.CreationResult
import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cloud.clouds.api.CloudCreateRequest
import de.solidblocks.cloud.model.ErrorCodes
import de.solidblocks.cloud.model.ValidationResult
import de.solidblocks.cloud.model.entities.CloudEntity
import de.solidblocks.cloud.model.repositories.CloudsRepository
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.cloud.model.toCreationResult
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.cloud.utils.CloudUtils
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.TransactionalCallable
import java.util.*

class CloudsManager(
    val dsl: DSLContext,
    val cloudsRepository: CloudsRepository,
    val environmentsRepository: EnvironmentsRepository,
    val usersManager: UsersManager,
    val isDevelopment: Boolean
) {

    private val logger = KotlinLogging.logger {}

    public fun newEnvironmentsDefaultCloud(email: String): CloudEntity? {

        val clouds = listClouds(email)

        if (clouds.size > 1) {
            logger.error { "could not find default cloud to use, expected one, found ${clouds.size}" }
        }

        return clouds.singleOrNull()
    }

    fun createCloud(email: String, request: CloudCreateRequest) = dsl.transactionResult(
        TransactionalCallable tc@{

            val result = validate(email, request)
            if (result.hasErrors()) {
                return@tc result.toCreationResult()
            }

            logger.info { "creating cloud '${request.cloud}'" }

            val cloud = cloudsRepository.createCloud(request.cloud!!, request.domain!!, development = isDevelopment)
            usersManager.createCloudUser(
                cloud.reference, request.email!!,
                request.password
                    ?: UUID.randomUUID().toString()
            )

            CreationResult(cloud)
        }
    )

    fun validate(email: String, request: CloudCreateRequest): ValidationResult {

        if (request.cloud == null || request.cloud.isBlank()) {
            return ValidationResult.error(CloudCreateRequest::cloud, ErrorCodes.MANDATORY)
        }

        if (request.domain == null || request.domain.isBlank()) {
            return ValidationResult.error(CloudCreateRequest::domain, ErrorCodes.MANDATORY)
        }

        if (request.email == null || request.email.isBlank()) {
            return ValidationResult.error(CloudCreateRequest::email, ErrorCodes.MANDATORY)
        }

        if (cloudsRepository.hasCloud(request.cloud)) {
            return ValidationResult.error(CloudCreateRequest::cloud, ErrorCodes.DUPLICATE)
        }

        return ValidationResult.ok()
    }

    fun rotateEnvironmentSecrets(reference: EnvironmentReference): Boolean {

        if (!environmentsRepository.hasEnvironment(reference)) {
            logger.info { "environment '$reference.environment' and/or cloud '${reference.cloud}' does no exist" }
            return false
        }

        environmentsRepository.rotateEnvironmentSecrets(reference)
        return true
    }

    fun getByHostHeader(hostHeader: String?): CloudEntity? {
        val rootDomain = CloudUtils.extractRootDomain(hostHeader) ?: return null
        return cloudsRepository.getCloudByRootDomain(rootDomain)
    }

    fun listClouds(email: String): List<CloudEntity> {
        val user = usersManager.getUser(email) ?: return emptyList()
        return cloudsRepository.listClouds(permissions = user.permissions())
    }

    fun hasCloud(reference: CloudReference) = cloudsRepository.hasCloud(reference)

    fun verifyReference(reference: CloudReference): Boolean {
        if (!cloudsRepository.hasCloud(reference)) {
            logger.error { "cloud '${reference.cloud}' not found" }
            return false
        }

        return true
    }
}
