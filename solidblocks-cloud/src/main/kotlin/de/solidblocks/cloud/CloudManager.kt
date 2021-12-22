package de.solidblocks.cloud

import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.*
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.createConfigValue
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
    val serviceRepository: ServiceRepository
) {

    private val logger = KotlinLogging.logger {}

    fun createCloud(name: String, domain: String): Boolean {
        if (cloudRepository.hasCloud(name)) {
            logger.info { "cloud '$name' already exists" }
            return false
        }

        cloudRepository.createCloud(name, domain)

        return true
    }

    fun createEnvironment(
        cloud: String,
        environment: String,
        githubReadOnlyToken: String,
        hetznerCloudApiTokenReadOnly: String,
        hetznerCloudApiTokenReadWrite: String,
        hetznerDnsApiToken: String
    ): Boolean {

        if (!cloudRepository.hasCloud(cloud)) {
            logger.info { "cloud '$cloud' does not exist" }
            return false
        }

        if (environmentRepository.hasEnvironment(cloud, environment)) {
            logger.info { "environment '$environment' already exist in cloud '$cloud'" }
            return false
        }

        environmentRepository.createEnvironment(
            cloud, environment,
            listOf(
                createConfigValue(ModelConstants.GITHUB_TOKEN_RO_KEY, githubReadOnlyToken),
                createConfigValue(HETZNER_CLOUD_API_TOKEN_RO_KEY, hetznerCloudApiTokenReadOnly),
                createConfigValue(HETZNER_CLOUD_API_TOKEN_RW_KEY, hetznerCloudApiTokenReadWrite),
                createConfigValue(HETZNER_DNS_API_TOKEN_RW_KEY, hetznerDnsApiToken),
            )
        )

        return true
    }

    fun bootstrapService(reference: ServiceReference): String {

        val service = serviceRepository.createService(
            reference.cloud, reference.environment, reference.service,
            mapOf(
                MINIO_SERVICE_SECRET_KEY_KEY to UUID.randomUUID().toString()
            )
        )
        val provisioner = serviceRepository.createService(reference.cloud, reference.environment, reference.service)

        val group = ResourceGroup("${ModelConstants.serviceId(reference)}-backup")

        val bucket = MinioBucket(ModelConstants.serviceId(reference))
        group.addResource(bucket)

        // provisioner.apply()

        // return provisioner.lookup(bucket).result!!.name

        return ""
    }

    fun rotateEnvironmentSecrets(cloud: String, environment: String): Boolean {

        if (!environmentRepository.hasEnvironment(cloud, environment)) {
            logger.info { "environment '$environment' and/or cloud '$cloud' does no exist" }
            return false
        }

        environmentRepository.rotateEnvironmentSecrets(cloud, environment)
        return true
    }

    fun listEnvironments(cloud: String): List<EnvironmentEntity> {
        val cloud = cloudRepository.getCloud(cloud)
        return environmentRepository.listEnvironments(cloud)
    }
}
