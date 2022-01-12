package de.solidblocks.cloud

import de.solidblocks.base.*
import de.solidblocks.base.lookups.Lookups
import de.solidblocks.cloud.model.*
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.consul.Consul
import de.solidblocks.provisioner.hetzner.Hetzner
import de.solidblocks.provisioner.minio.Minio
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.provisioner.vault.Vault
import de.solidblocks.vault.EnvironmentVaultManager
import de.solidblocks.vault.VaultRootClientProvider
import mu.KotlinLogging

class SolidblocksAppplicationContext(
    jdbcUrl: String,
    private val vaultAddressOverride: String? = null,
    private val minioCredentialsProvider: (() -> MinioCredentials)? = null,
    isDevelopmenmt: Boolean = false
) {

    private val logger = KotlinLogging.logger {}

    private var vaultRootClientProvider: VaultRootClientProvider? = null

    val cloudRepository: CloudRepository
    val serviceRepository: ServiceRepository
    val environmentRepository: EnvironmentRepository
    val tenantRepository: TenantRepository
    val cloudManager: CloudManager

    init {
        val database = SolidblocksDatabase(jdbcUrl)
        database.ensureDBSchema()

        cloudRepository = CloudRepository(database.dsl)
        environmentRepository = EnvironmentRepository(database.dsl, cloudRepository)
        tenantRepository = TenantRepository(database.dsl, environmentRepository)
        serviceRepository = ServiceRepository(database.dsl, environmentRepository)

        cloudManager = CloudManager(
            cloudRepository,
            environmentRepository,
            tenantRepository,
            serviceRepository,
            isDevelopmenmt
        )
    }

    fun vaultRootClientProvider(reference: EnvironmentReference): VaultRootClientProvider {
        if (vaultRootClientProvider == null) {
            vaultRootClientProvider =
                VaultRootClientProvider(reference, environmentRepository, vaultAddressOverride)
        }

        return vaultRootClientProvider!!
    }

    fun createEnvironmentProvisioner(reference: EnvironmentReference) = EnvironmentProvisioner(
        environmentRepository.getEnvironment(reference),
        vaultRootClientProvider(reference),
        createProvisioner(reference),
    )

    fun createTenantProvisioner(reference: TenantReference) = TenantProvisioner(
        reference,
        createProvisioner(reference),
        environmentRepository,
        tenantRepository
    )

    fun createServiceProvisioner(reference: ServiceReference) =
        ServiceProvisioner(
            createProvisioner(reference),
            reference,
            environmentRepository,
            EnvironmentVaultManager(vaultRootClientProvider(reference).createClient(), reference)
        )

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
}
