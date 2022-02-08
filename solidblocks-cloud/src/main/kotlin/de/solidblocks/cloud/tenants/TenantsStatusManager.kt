package de.solidblocks.cloud.tenants

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.cloud.status.Status
import de.solidblocks.cloud.status.StatusManager
import de.solidblocks.provisioner.hetzner.Hetzner.HETZNER_CLOUD_API_TOKEN_RW_KEY
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging
import java.time.Duration

class TenantsStatusManager(
    val statusManager: StatusManager,
    val environmentsRepository: EnvironmentsRepository
) {

    private val logger = KotlinLogging.logger {}

    var tenantNetworks: LoadingCache<TenantReference, Boolean> =
        Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(5)).build { reference ->
            val environment = environmentsRepository.getEnvironment(reference)
                ?: throw RuntimeException("failed to resolve '${reference}'")
            val api = HetznerCloudAPI(environment.getConfigValue(HETZNER_CLOUD_API_TOKEN_RW_KEY))
            api.getNetworksByName(ModelConstants.networkName(reference)).networks.isEmpty()
        }


    fun needsApply(reference: TenantReference): Boolean {
        return !tenantNetworks.get(reference).also {
            if (it) {
                logger.info { "tenant '${reference}' needs apply" }
            }
        }
    }

    fun updateStatus(tenantEntity: TenantEntity, status: Status) = statusManager.updateStatus(tenantEntity.id, status)

}
