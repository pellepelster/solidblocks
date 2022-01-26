package de.solidblocks.cloud.tenants

import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.base.validateId
import de.solidblocks.cloud.NetworkUtils
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.*
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.cloud.tenants.api.TenantCreateRequest
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.provisioner.hetzner.Hetzner
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.TransactionalCallable

class TenantsManager(
    val dsl: DSLContext,
    val cloudRepository: CloudRepository,
    val environmentsManager: EnvironmentsManager,
    val tenantRepository: TenantRepository,
    val usersManager: UsersManager,
    val isDevelopment: Boolean,
) {

    private val logger = KotlinLogging.logger {}

    fun create(environmentReference: EnvironmentResource, name: String, email: String): CreationResult<TenantEntity> {
        val environment = environmentsManager.getOptional(environmentReference)
            ?: return CreationResult.error(ErrorCodes.ENVIRONMENT.NOT_FOUND)

        val reference = environment.reference.toTenant(name)

        return dsl.transactionResult(
            TransactionalCallable {
                logger.info { "creating tenant '$name'" }
                tenantRepository.createTenant(environment.reference, name, nextNetworkCidr(environment))
                // TODO(pelle) create random password
                usersManager.createTenantUser(environment.reference.toTenant(name), email, "admin")
                CreationResult(tenantRepository.getTenant(reference))
            }
        )
    }

    fun create(name: String, email: String): CreationResult<TenantEntity> {
        val environment = environmentsManager.newTenantsDefaultEnvironment(email)
            ?: return CreationResult.error(ErrorCodes.ENVIRONMENT.NOT_FOUND)

        return create(environment.reference, name, email)
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

        if (tenantRepository.hasTenant(environment.reference.toTenant(request.tenant))) {
            return ValidationResult.error(TenantCreateRequest::tenant, ErrorCodes.TENANT.DUPLICATE)
        }

        if (usersManager.hasUser(request.email)) {
            return ValidationResult.error(TenantCreateRequest::email, ErrorCodes.TENANT.DUPLICATE)
        }

        return ValidationResult.ok()
    }

    fun listTenantsForUser(email: String): List<TenantEntity> {
        val user = usersManager.getUser(email) ?: return emptyList()
        return tenantRepository.listTenants(permissions = user.permissions())
    }

    private fun nextNetworkCidr(environment: EnvironmentEntity) = if (isDevelopment) {
        "<none>"
    } else {
        val hetznerCloudApi = Hetzner.createCloudApi(environment)
        val currentNetworks = hetznerCloudApi.allNetworks.networks.map { it.ipRange }

        NetworkUtils.nextNetwork(currentNetworks.toSet())
            ?: throw RuntimeException("could not determine next network CIDR")
    }
}
