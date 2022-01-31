package de.solidblocks.cloud.tenants

import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.base.validateId
import de.solidblocks.cloud.NetworkUtils
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.CreationResult
import de.solidblocks.cloud.model.ErrorCodes
import de.solidblocks.cloud.model.ValidationResult
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.cloud.model.repositories.TenantsRepository
import de.solidblocks.cloud.tenants.api.TenantCreateRequest
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.provisioner.hetzner.Hetzner
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.TransactionalCallable

class TenantsManager(
    val dsl: DSLContext,
    private val environmentsManager: EnvironmentsManager,
    private val tenantsRepository: TenantsRepository,
    private val usersManager: UsersManager,
    private val isDevelopment: Boolean,
) {

    private val logger = KotlinLogging.logger {}

    fun create(environmentReference: EnvironmentReference, name: String, email: String, password: String): CreationResult<TenantEntity> {
        val environment = environmentsManager.getOptional(environmentReference)
            ?: return CreationResult.error(ErrorCodes.ENVIRONMENT.NOT_FOUND)

        val reference = environment.reference.toTenant(name)

        return dsl.transactionResult(
            TransactionalCallable {
                logger.info { "creating tenant '$name' for environment '$environment'" }
                tenantsRepository.createTenant(environment.reference, name, nextNetworkCidr(environment))
                // TODO(pelle) create random password
                usersManager.createTenantUser(environment.reference.toTenant(name), email, password)
                CreationResult(tenantsRepository.getTenant(reference))
            }
        )
    }

    fun createTenantForDefaultEnvironment(name: String, email: String, password: String): CreationResult<TenantEntity> {
        val environment = environmentsManager.newTenantsDefaultEnvironment(email)
            ?: return CreationResult.error(ErrorCodes.ENVIRONMENT.NOT_FOUND)

        return create(environment.reference, name, email, password)
    }

    fun validate(request: TenantCreateRequest): ValidationResult {

        if (request.tenant == null || request.tenant.isBlank()) {
            return ValidationResult.error(TenantCreateRequest::tenant, ErrorCodes.MANDATORY)
        }

        if (request.email == null || request.email.isBlank()) {
            return ValidationResult.error(TenantCreateRequest::email, ErrorCodes.MANDATORY)
        }

        val environment = environmentsManager.newTenantsDefaultEnvironment(request.email)
            ?: return ValidationResult.error(ErrorCodes.TENANT.ENVIRONMENT_NOT_FOUND)

        if (!validateId(request.tenant)) {
            return ValidationResult.error(TenantCreateRequest::tenant, ErrorCodes.TENANT.INVALID)
        }

        if (tenantsRepository.hasTenant(environment.reference.toTenant(request.tenant))) {
            return ValidationResult.error(TenantCreateRequest::tenant, ErrorCodes.TENANT.DUPLICATE)
        }

        if (usersManager.hasUser(request.email)) {
            return ValidationResult.error(TenantCreateRequest::email, ErrorCodes.TENANT.DUPLICATE)
        }

        return ValidationResult.ok()
    }

    fun listTenantsForUser(email: String): List<TenantEntity> {
        val user = usersManager.getUser(email) ?: return emptyList()
        return tenantsRepository.listTenants(permissions = user.permissions())
    }

    private fun nextNetworkCidr(environment: EnvironmentEntity) = if (isDevelopment) {
        "<none>"
    } else {
        val hetznerCloudApi = Hetzner.createCloudApi(environment)
        val currentNetworks = hetznerCloudApi.allNetworks.networks.map { it.ipRange }

        NetworkUtils.nextNetwork(currentNetworks.toSet())
            ?: throw RuntimeException("could not determine next network CIDR")
    }

    fun verifyReference(reference: TenantReference): Boolean {
        if (!environmentsManager.verifyReference(reference)) {
            return false
        }

        if (!tenantsRepository.hasTenant(reference)) {
            logger.error { "tenant '${reference.tenant}' not found" }
            return false
        }

        return true
    }
}
