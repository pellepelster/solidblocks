package de.solidblocks.cloud.clouds

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
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

    fun createCloud(name: String, domain: String): CloudReference? {

        if (cloudRepository.hasCloud(name)) {
            logger.info { "cloud '$name' already exists" }
            return null
        }

        logger.info { "creating cloud '$name'" }

        cloudRepository.createCloud(name, domain, development = isDevelopment)

        return CloudReference(name)
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
        val cloud = cloudRepository.getCloud(reference) ?: throw RuntimeException("cloud '$reference' not found")
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
