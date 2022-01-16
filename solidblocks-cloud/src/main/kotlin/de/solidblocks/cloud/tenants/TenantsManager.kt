package de.solidblocks.cloud.tenants

import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.cloud.NetworkUtils
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.CreationResult
import de.solidblocks.cloud.model.ErrorCodes
import de.solidblocks.cloud.model.TenantRepository
import de.solidblocks.cloud.model.ValidationResult
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.TenantEntity
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

    fun create(environmentReference: EnvironmentResource, name: String, email: String, password: String): CreationResult<TenantEntity> {
        val environment = environmentsManager.getOptional(environmentReference)
            ?: return CreationResult.error(ErrorCodes.ENVIRONMENT.NOT_FOUND)

        val reference = environment.reference.toTenant(name)

        return dsl.transactionResult(
            TransactionalCallable {
                logger.info { "creating tenant '$name'" }
                tenantRepository.createTenant(environment.reference, name, nextNetworkCidr(environment))
                CreationResult(tenantRepository.getTenant(reference))
            }
        )
    }

    fun create(name: String, email: String, password: String): CreationResult<TenantEntity> {
        val environment = environmentsManager.newTenantsDefaultEnvironment()
            ?: return CreationResult.error(ErrorCodes.ENVIRONMENT.NOT_FOUND)

        return create(environment.reference, name, email, password)
    }

    fun validate(name: String, email: String, password: String): ValidationResult {

        val environment = environmentsManager.newTenantsDefaultEnvironment()
            ?: return ValidationResult.error(ErrorCodes.ENVIRONMENT.NOT_FOUND)

        if (tenantRepository.hasTenant(environment.reference.toTenant(name))) {
            return ValidationResult.error(ErrorCodes.TENANT.DUPLICATE)
        }

        return ValidationResult.ok()
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
