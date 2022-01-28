package de.solidblocks.cloud

import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.base.lookups.Lookups
import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.ServiceReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.cloud.clouds.CloudsManager
import de.solidblocks.cloud.environments.EnvironmentApplicationContext
import de.solidblocks.cloud.environments.EnvironmentProvisioner
import de.solidblocks.cloud.environments.EnvironmentsManager
import de.solidblocks.cloud.model.*
import de.solidblocks.cloud.services.ServiceProvisioner
import de.solidblocks.cloud.tenants.TenantProvisioner
import de.solidblocks.cloud.tenants.TenantsManager
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.consul.Consul
import de.solidblocks.provisioner.hetzner.Hetzner
import de.solidblocks.provisioner.minio.Minio
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.provisioner.vault.Vault
import de.solidblocks.provisioner.vault.VaultRootClientProvider
import de.solidblocks.vault.EnvironmentVaultManager
import mu.KotlinLogging

class ApplicationContext(jdbcUrl: String, private val vaultAddressOverride: String? = null, private val minioCredentialsProvider: (() -> MinioCredentials)? = null, development: Boolean = false) {

    private val logger = KotlinLogging.logger {}

    private var vaultRootClientProvider: VaultRootClientProvider? = null

    val repositories: RepositoriesContext

    val cloudsManager: CloudsManager
    val environmentsManager: EnvironmentsManager
    val tenantsManager: TenantsManager
    val usersManager: UsersManager

    init {
        val database = SolidblocksDatabase(jdbcUrl)
        database.ensureDBSchema()

        repositories = RepositoriesContext(database.dsl)

        cloudsManager = CloudsManager(repositories.clouds, repositories.environments, repositories.users, true)
        usersManager = UsersManager(database.dsl, repositories.users)
        environmentsManager = EnvironmentsManager(database.dsl, repositories.clouds, repositories.environments, usersManager, development)
        tenantsManager = TenantsManager(database.dsl, repositories.clouds, environmentsManager, repositories.tenants, usersManager, development)
    }

    fun vaultRootClientProvider(reference: EnvironmentReference): VaultRootClientProvider {
        if (vaultRootClientProvider == null) {
            vaultRootClientProvider = VaultRootClientProvider(reference, repositories.environments, vaultAddressOverride)
        }

        return vaultRootClientProvider!!
    }

    fun createEnvironmentProvisioner(reference: EnvironmentReference) = EnvironmentProvisioner(
        repositories.environments.getEnvironment(reference)
            ?: throw RuntimeException("environment '$reference' not found"),
        vaultRootClientProvider(reference),
        createProvisioner(reference),
    )

    fun createTenantProvisioner(reference: TenantReference) = TenantProvisioner(reference, createProvisioner(reference), repositories.environments, repositories.tenants)

    fun createServiceProvisioner(reference: ServiceReference) = ServiceProvisioner(createProvisioner(reference), reference, repositories.environments, EnvironmentVaultManager(vaultRootClientProvider(reference).createClient(), reference))

    fun createProvisioner(reference: EnvironmentReference): Provisioner {

        val provisionerRegistry = ProvisionerRegistry()
        val provisioner = Provisioner(provisionerRegistry)

        val environment = repositories.environments.getEnvironment(reference)
            ?: throw RuntimeException("environment '$reference' not found")

        Hetzner.registerProvisioners(provisionerRegistry, environment, provisioner)
        Hetzner.registerLookups(provisionerRegistry, provisioner)
        Lookups.registerLookups(provisionerRegistry, provisioner)
        Consul.registerProvisioners(provisionerRegistry, Consul.consulClient(environment))

        Vault.registerProvisioners(provisionerRegistry) {
            vaultRootClientProvider(reference).createClient()
        }

        Minio.registerProvisioners(
            provisionerRegistry,
            minioCredentialsProvider ?: {
                MinioCredentials(Minio.minioAddress(environment), "xx", "ss")
            }
        )

        return provisioner
    }

    fun verifyTenantReference(reference: TenantReference): Boolean {
        if (!verifyEnvironmentReference(reference)) {
            return false
        }

        if (!repositories.tenants.hasTenant(reference)) {
            logger.error { "tenant '${reference.tenant}' not found" }
            return false
        }

        return true
    }

    fun verifyEnvironmentReference(reference: EnvironmentReference): Boolean {
        if (!verifyCloudReference(reference)) {
            return false
        }

        if (!repositories.environments.hasEnvironment(reference)) {
            logger.error { "environment '${reference.environment}' not found" }
            return false
        }

        return true
    }

    fun verifyCloudReference(reference: CloudReference): Boolean {
        if (!repositories.clouds.hasCloud(reference)) {
            logger.error { "cloud '${reference.cloud}' not found" }
            return false
        }

        return true
    }

    fun createEnvironmentContext(reference: EnvironmentReference) = EnvironmentApplicationContext(reference, repositories.environments)
}
