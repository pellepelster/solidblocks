package de.solidblocks.cloud

import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.base.CloudReference
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.base.TenantReference
import de.solidblocks.cloud.model.*
import de.solidblocks.cloud.model.ModelConstants.GITHUB_TOKEN_RO_KEY
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.createConfigValue
import de.solidblocks.provisioner.hetzner.Hetzner
import de.solidblocks.provisioner.hetzner.Hetzner.HETZNER_CLOUD_API_TOKEN_RO_KEY
import de.solidblocks.provisioner.hetzner.Hetzner.HETZNER_CLOUD_API_TOKEN_RW_KEY
import de.solidblocks.provisioner.hetzner.Hetzner.HETZNER_DNS_API_TOKEN_RW_KEY
import de.solidblocks.provisioner.minio.Minio.MINIO_SERVICE_SECRET_KEY_KEY
import de.solidblocks.provisioner.minio.bucket.MinioBucket
import mu.KotlinLogging
import java.util.*

class CloudManager(
    val cloudRepository: CloudRepository,
    val environmentRepository: EnvironmentRepository,
    val tenantRepository: TenantRepository,
    val serviceRepository: ServiceRepository,
    val isDevelopment: Boolean
) {

    private val logger = KotlinLogging.logger {}

    fun createCloud(reference: CloudReference, domain: String): Boolean {
        if (cloudRepository.hasCloud(reference)) {
            logger.info { "cloud '$reference.cloud' already exists" }
            return false
        }

        cloudRepository.createCloud(reference, domain, development = isDevelopment)

        return true
    }

    fun createEnvironment(
        reference: EnvironmentReference,
        githubReadOnlyToken: String,
        hetznerCloudApiTokenReadOnly: String,
        hetznerCloudApiTokenReadWrite: String,
        hetznerDnsApiToken: String
    ): Boolean {

        if (!cloudRepository.hasCloud(reference)) {
            logger.info { "cloud '${reference.cloud}' does not exist" }
            return false
        }

        if (environmentRepository.hasEnvironment(reference)) {
            logger.info { "environment '$reference.environment' already exist in cloud '$reference.cloud'" }
            return false
        }

        environmentRepository.createEnvironment(
            reference,
            listOf(
                createConfigValue(GITHUB_TOKEN_RO_KEY, githubReadOnlyToken),
                createConfigValue(HETZNER_CLOUD_API_TOKEN_RO_KEY, hetznerCloudApiTokenReadOnly),
                createConfigValue(HETZNER_CLOUD_API_TOKEN_RW_KEY, hetznerCloudApiTokenReadWrite),
                createConfigValue(HETZNER_DNS_API_TOKEN_RW_KEY, hetznerDnsApiToken),
            )
        )

        return true
    }

    fun nextNetworkCidr(reference: TenantReference) = if (isDevelopment) {
        "<none>"
    } else {
        val hetznerCloudApi = Hetzner.createCloudApi(environmentRepository.getEnvironment(reference))
        val currentNetworks = hetznerCloudApi.allNetworks.networks.map { it.ipRange }

        NetworkUtils.nextNetwork(currentNetworks.toSet())
            ?: throw RuntimeException("could not determine next network CIDR")
    }

    fun createTenant(reference: TenantReference): Boolean {
        if (tenantRepository.hasTenant(reference)) {
            logger.info { "tenant '${reference.tenant}' already exists" }
            return false
        }

        tenantRepository.createTenant(reference, nextNetworkCidr(reference))

        return true
    }

    fun bootstrapService(reference: ServiceReference): String {

        val service = serviceRepository.createService(
            reference,
            mapOf(
                MINIO_SERVICE_SECRET_KEY_KEY to UUID.randomUUID().toString()
            )
        )
        val provisioner = serviceRepository.createService(reference)

        val group = ResourceGroup("${ModelConstants.serviceId(reference)}-backup")

        val bucket = MinioBucket(ModelConstants.serviceId(reference))
        group.addResource(bucket)

        // provisioner.apply()
        // return provisioner.lookup(bucket).result!!.name

        return ""
    }

    fun rotateEnvironmentSecrets(reference: EnvironmentReference): Boolean {

        if (!environmentRepository.hasEnvironment(reference)) {
            logger.info { "environment '$reference.environment' and/or cloud '${reference.cloud}' does no exist" }
            return false
        }

        environmentRepository.rotateEnvironmentSecrets(reference)
        return true
    }

    fun listEnvironments(reference: CloudReference): List<EnvironmentEntity> {
        val cloud = cloudRepository.getCloud(reference)
        return environmentRepository.listEnvironments(cloud)
    }
}
