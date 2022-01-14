package de.solidblocks.cloud

import de.solidblocks.base.*
import de.solidblocks.base.lookups.Lookups
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

    val cloudRepository: CloudRepository
    val serviceRepository: ServiceRepository
    val environmentRepository: EnvironmentRepository
    val tenantRepository: TenantRepository
    val usersRepository: UsersRepository

    val cloudManager: CloudManager
    val environmentsManager: EnvironmentsManager
    val tenantsManager: TenantsManager
    val usersManager: UsersManager

    init {
        val database = SolidblocksDatabase(jdbcUrl)
        database.ensureDBSchema()

        cloudRepository = CloudRepository(database.dsl)
        environmentRepository = EnvironmentRepository(database.dsl, cloudRepository)
        tenantRepository = TenantRepository(database.dsl, environmentRepository)
        usersRepository = UsersRepository(database.dsl)

        serviceRepository = ServiceRepository(database.dsl, environmentRepository)

        cloudManager = CloudManager(cloudRepository, environmentRepository, tenantRepository, serviceRepository, development)
        environmentsManager = EnvironmentsManager(database.dsl, cloudRepository, environmentRepository, development)
        usersManager = UsersManager(usersRepository)
        tenantsManager = TenantsManager(database.dsl, cloudRepository, environmentsManager, tenantRepository, usersManager, development)
    }

    fun vaultRootClientProvider(reference: EnvironmentReference): VaultRootClientProvider {
        if (vaultRootClientProvider == null) {
            vaultRootClientProvider = VaultRootClientProvider(reference, environmentRepository, vaultAddressOverride)
        }

        return vaultRootClientProvider!!
    }

    fun createEnvironmentProvisioner(reference: EnvironmentReference) = EnvironmentProvisioner(
        environmentRepository.getEnvironment(reference),
        vaultRootClientProvider(reference),
        createProvisioner(reference),
    )

    fun createTenantProvisioner(reference: TenantReference) = TenantProvisioner(reference, createProvisioner(reference), environmentRepository, tenantRepository)

    fun createServiceProvisioner(reference: ServiceReference) = ServiceProvisioner(createProvisioner(reference), reference, environmentRepository, EnvironmentVaultManager(vaultRootClientProvider(reference).createClient(), reference))

    fun createProvisioner(reference: EnvironmentReference): Provisioner {

        val provisionerRegistry = ProvisionerRegistry()
        val provisioner = Provisioner(provisionerRegistry)

        val environment = environmentRepository.getEnvironment(reference)

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

        if (!tenantRepository.hasTenant(reference)) {
            logger.error { "tenant '${reference.tenant}' not found" }
            return false
        }

        return true
    }

    fun verifyEnvironmentReference(reference: EnvironmentReference): Boolean {
        if (!verifyCloudReference(reference)) {
            return false
        }

        if (!environmentRepository.hasEnvironment(reference)) {
            logger.error { "environment '${reference.environment}' not found" }
            return false
        }

        return true
    }

    fun verifyCloudReference(reference: CloudReference): Boolean {
        if (!cloudRepository.hasCloud(reference)) {
            logger.error { "cloud '${reference.cloud}' not found" }
            return false
        }

        return true
    }

    fun createEnvironmentContext(reference: EnvironmentReference) = EnvironmentApplicationContext(reference, environmentRepository)
}
