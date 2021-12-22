package de.solidblocks.cloud

import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.model.EnvironmentModel
import de.solidblocks.cloud.model.model.createConfigValue
import mu.KotlinLogging

class CloudManager(val cloudRepository: CloudRepository, val environmentRepository: EnvironmentRepository) {

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
                createConfigValue(ModelConstants.HETZNER_CLOUD_API_TOKEN_RO_KEY, hetznerCloudApiTokenReadOnly),
                createConfigValue(ModelConstants.HETZNER_CLOUD_API_TOKEN_RW_KEY, hetznerCloudApiTokenReadWrite),
                createConfigValue(ModelConstants.HETZNER_DNS_API_TOKEN_RW_KEY, hetznerDnsApiToken),
            )
        )

        return true
    }

    fun rotateEnvironmentSecrets(cloud: String, environment: String): Boolean {

        if (!environmentRepository.hasEnvironment(cloud, environment)) {
            logger.info { "environment '$environment' and/or cloud '$cloud' does no exist" }
            return false
        }

        environmentRepository.rotateEnvironmentSecrets(cloud, environment)
        return true
    }

    fun listEnvironments(cloud: String): List<EnvironmentModel> {
        val cloud = cloudRepository.getCloud(cloud) ?: throw RuntimeException("cloud '$cloud' not found")
        return environmentRepository.listEnvironments(cloud)
    }
}
