package de.solidblocks.cloud.clouds

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cloud.model.entities.CloudEntity
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.repositories.CloudsRepository
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.cloud.utils.CloudUtils
import mu.KotlinLogging

class CloudsManager(
    val cloudsRepository: CloudsRepository,
    val environmentsRepository: EnvironmentsRepository,
    val usersManager: UsersManager,
    val isDevelopment: Boolean
) {

    private val logger = KotlinLogging.logger {}

    public fun newEnvironmentsDefaultCloud(email: String) = listCloudsForUser(email).singleOrNull()

    fun createCloud(name: String, domain: String): CloudEntity? {
        if (cloudsRepository.hasCloud(name)) {
            logger.info { "cloud '$name' already exists" }
            return null
        }

        logger.info { "creating cloud '$name'" }

        return cloudsRepository.createCloud(name, domain, development = isDevelopment)
    }

    fun rotateEnvironmentSecrets(reference: EnvironmentReference): Boolean {

        if (!environmentsRepository.hasEnvironment(reference)) {
            logger.info { "environment '$reference.environment' and/or cloud '${reference.cloud}' does no exist" }
            return false
        }

        environmentsRepository.rotateEnvironmentSecrets(reference)
        return true
    }

    fun listEnvironments(reference: CloudReference): List<EnvironmentEntity> {
        val cloud = cloudsRepository.getCloud(reference) ?: throw RuntimeException("cloud '$reference' not found")
        return environmentsRepository.listEnvironments()
    }

    fun getByHostHeader(hostHeader: String?): CloudEntity? {
        val rootDomain = CloudUtils.extractRootDomain(hostHeader) ?: return null
        return cloudsRepository.getCloudByRootDomain(rootDomain)
    }

    fun listCloudsForUser(email: String): List<CloudEntity> {
        val user = usersManager.getUser(email) ?: return emptyList()
        return cloudsRepository.listClouds(permissions = user.permissions())
    }

    fun verifyReference(reference: CloudReference): Boolean {
        if (!cloudsRepository.hasCloud(reference)) {
            logger.error { "cloud '${reference.cloud}' not found" }
            return false
        }

        return true
    }
}
