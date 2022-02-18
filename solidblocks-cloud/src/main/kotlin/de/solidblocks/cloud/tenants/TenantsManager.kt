package de.solidblocks.cloud.tenants

import de.solidblocks.base.CreationResult
import de.solidblocks.base.api.messageResponses
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.base.validateId
import de.solidblocks.cloud.SchedulerContext
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.ErrorCodes
import de.solidblocks.cloud.model.ValidationResult
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.ServiceEntity
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.cloud.model.repositories.ServicesRepository
import de.solidblocks.cloud.model.repositories.TenantsRepository
import de.solidblocks.cloud.model.toCreationResult
import de.solidblocks.cloud.tenants.api.TenantCreateRequest
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.cloud.utils.NetworkUtils
import de.solidblocks.provisioner.hetzner.Hetzner
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.TransactionalCallable
import java.util.*

class TenantsManager(
    val dsl: DSLContext,
    val environmentsManager: EnvironmentsManager,
    val tenantsRepository: TenantsRepository,
    val servicesRepository: ServicesRepository,
    val scheduler: SchedulerContext,
    val usersManager: UsersManager,
    val development: Boolean,
) {

    private val logger = KotlinLogging.logger {}

    fun createTenantForDefaultEnvironment(email: String, request: TenantCreateRequest): CreationResult<TenantEntity> {
        val environment = environmentsManager.newTenantsDefaultEnvironment(email)
            ?: return CreationResult.error(ErrorCodes.ENVIRONMENT.DEFAULT_NOT_FOUND)

        return create(environment.reference, email, request)
    }

    fun create(reference: EnvironmentReference, email: String, request: TenantCreateRequest) = dsl.transactionResult(
        TransactionalCallable tc@{

            val environment = environmentsManager.getEnvironment(reference)
                ?: return@tc CreationResult.error(ErrorCodes.ENVIRONMENT.NOT_FOUND)

            val result = validate(email, request, reference)
            if (result.hasErrors()) {
                return@tc result.toCreationResult()
            }

            logger.info { "creating tenant '${request.tenant}' for environment '${environment.reference}'" }

            val tenant = tenantsRepository.createTenant(environment.reference, request.tenant!!, nextNetworkCidr(environment))
                ?: return@tc CreationResult<TenantEntity>(messages = ErrorCodes.TENANT.CREATE_FAILED.messageResponses(TenantCreateRequest::tenant))
            usersManager.createTenantUser(
                tenant.reference, request.email!!,
                request.password
                    ?: UUID.randomUUID().toString()
            )

            val action = scheduler.scheduleTenantApplyTask(tenant.reference)
            CreationResult(tenant, actions = listOf(action))
        }
    )

    fun validate(email: String, request: TenantCreateRequest, reference: EnvironmentReference? = null): ValidationResult {

        if (request.tenant == null || request.tenant.isBlank()) {
            return ValidationResult.error(TenantCreateRequest::tenant, ErrorCodes.MANDATORY)
        }

        if (request.email == null || request.email.isBlank()) {
            return ValidationResult.error(TenantCreateRequest::email, ErrorCodes.MANDATORY)
        }

        var defaultEnvironment: EnvironmentEntity? = null

        if (reference == null) {
            defaultEnvironment = environmentsManager.newTenantsDefaultEnvironment(email)
                ?: return ValidationResult.error(ErrorCodes.TENANT.DEFAULT_ENVIRONMENT_NOT_FOUND)
        }

        if (reference != null || defaultEnvironment != null) {

            if (tenantsRepository.hasTenant(listOfNotNull(defaultEnvironment?.reference, reference).first().toTenant(request.tenant))) {
                return ValidationResult.error(TenantCreateRequest::tenant, ErrorCodes.DUPLICATE)
            }
        }

        if (!validateId(request.tenant)) {
            return ValidationResult.error(TenantCreateRequest::tenant, ErrorCodes.TENANT.INVALID)
        }

        if (usersManager.hasUser(request.email)) {
            return ValidationResult.error(TenantCreateRequest::email, ErrorCodes.DUPLICATE)
        }

        return ValidationResult.ok()
    }

    private fun nextNetworkCidr(environment: EnvironmentEntity) = if (development) {
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

    fun getTenant(email: String, id: UUID): TenantEntity? {
        val user = usersManager.getUser(email) ?: return null
        return tenantsRepository.getTenant(id, user.permissions())
    }

    fun listTenants(email: String): List<TenantEntity> {
        val user = usersManager.getUser(email) ?: return emptyList()
        return tenantsRepository.listTenants(permissions = user.permissions())
    }

    fun tenantServices(email: String, id: UUID): List<ServiceEntity> {
        val user = usersManager.getUser(email) ?: return emptyList()
        return servicesRepository.listServices(servicesRepository.services.TENANT.eq(id), user.permissions())
    }
}
