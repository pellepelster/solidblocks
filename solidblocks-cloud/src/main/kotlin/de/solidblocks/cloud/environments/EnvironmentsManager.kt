package de.solidblocks.cloud.environments

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import mu.KotlinLogging
import org.jooq.DSLContext

class EnvironmentsManager(
    val ctx: DSLContext,
    val cloudRepository: CloudRepository,
    val environmentRepository: EnvironmentRepository,
    val isDevelopment: Boolean,
) {

    private val logger = KotlinLogging.logger {}

    public fun newTenantsDefaultEnvironment(): EnvironmentEntity? {

        val clouds = cloudRepository.listClouds()
        if (clouds.isEmpty()) {
            return null
        }

        val environments = environmentRepository.listEnvironments(clouds.first())
        return environments.firstOrNull()
    }

    fun getOptional(reference: EnvironmentReference) = environmentRepository.getOptional(reference)
}
