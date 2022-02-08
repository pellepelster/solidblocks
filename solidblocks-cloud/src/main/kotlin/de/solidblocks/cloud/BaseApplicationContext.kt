package de.solidblocks.cloud

import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cloud.environments.EnvironmentApplicationContext
import de.solidblocks.cloud.model.SolidblocksDatabase
import de.solidblocks.cloud.model.repositories.RepositoriesContext
import de.solidblocks.provisioner.minio.MinioCredentials
import mu.KotlinLogging

open class BaseApplicationContext(jdbcUrl: String, private val vaultAddressOverride: String? = null, private val minioCredentialsProvider: (() -> MinioCredentials)? = null, development: Boolean = false) {

    private val logger = KotlinLogging.logger {}

    val repositories: RepositoriesContext
    val managers: ManagersContext
    val provisionerContext: ProvisionerContext
    val database: SolidblocksDatabase
    val status: StatusContext

    init {
        database = SolidblocksDatabase(jdbcUrl)
        database.ensureDBSchema()

        repositories = RepositoriesContext(database.dsl)
        managers = ManagersContext(database.dsl, repositories, development)
        status = StatusContext(repositories)

        provisionerContext = ProvisionerContext(repositories, managers, status)
    }

    fun createEnvironmentContext(reference: EnvironmentReference) = EnvironmentApplicationContext(reference, repositories.environments)
}
