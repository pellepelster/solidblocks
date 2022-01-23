package de.solidblocks.cloud.clouds

import de.solidblocks.base.resources.CloudResource
import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.cloud.CloudUtils
import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.UsersRepository
import de.solidblocks.cloud.model.entities.CloudEntity
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import mu.KotlinLogging

class CloudsManager(
    val cloudRepository: CloudRepository,
    val environmentRepository: EnvironmentRepository,
    val usersRepository: UsersRepository,
    val isDevelopment: Boolean
) {

    private val logger = KotlinLogging.logger {}

    fun createCloud(name: String, domain: String): CloudResource? {

        if (cloudRepository.hasCloud(name)) {
            logger.info { "cloud '$name' already exists" }
            return null
        }

        logger.info { "creating cloud '$name'" }

        cloudRepository.createCloud(name, domain, development = isDevelopment)

        return CloudResource(name)
    }

    fun rotateEnvironmentSecrets(reference: EnvironmentResource): Boolean {

        if (!environmentRepository.hasEnvironment(reference)) {
            logger.info { "environment '$reference.environment' and/or cloud '${reference.cloud}' does no exist" }
            return false
        }

        environmentRepository.rotateEnvironmentSecrets(reference)
        return true
    }

    fun listEnvironments(reference: CloudResource): List<EnvironmentEntity> {
        val cloud = cloudRepository.getCloud(reference) ?: throw RuntimeException("cloud '${reference}' not found")
        return environmentRepository.listEnvironments()
    }

    fun getByHostHeader(hostHeader: String?): CloudEntity? {
        val rootDomain = CloudUtils.extractRootDomain(hostHeader) ?: return null
        return cloudRepository.getCloudByRootDomain(rootDomain)
    }

    fun listCloudsForUser(email: String): List<CloudEntity> {
        val user = usersRepository.getUser(email) ?: return emptyList()
        return cloudRepository.listClouds(permissions = user.permissions())
    }
}
