package de.solidblocks.test

import de.solidblocks.base.CreationResult
import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cloud.BaseApplicationContext
import de.solidblocks.cloud.clouds.api.CloudCreateRequest
import de.solidblocks.cloud.environments.api.EnvironmentCreateRequest
import de.solidblocks.cloud.model.entities.CloudEntity
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.cloud.tenants.api.TenantCreateRequest
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.test.TestConstants.ADMIN_PASSWORD
import de.solidblocks.test.TestConstants.ADMIN_USER
import de.solidblocks.test.TestConstants.CLOUD_PASSWORD
import de.solidblocks.test.TestConstants.ENVIRONMENT_PASSWORD
import de.solidblocks.test.TestConstants.ROOT_DOMAIN
import de.solidblocks.test.TestConstants.TENANT_PASSWORD

class TestApplicationContext(
    jdbcUrl: String,
    val vaultAddressOverride: String? = null,
    val minioCredentialsProvider: (() -> MinioCredentials)? = null,
    development: Boolean = false

) : BaseApplicationContext(jdbcUrl, development = development) {

    fun ensureAdminUser() {
        managers.users.ensureAdminUser(ADMIN_USER, ADMIN_PASSWORD)
    }

    init {
        ensureAdminUser()
    }

    fun createCloud(cloud: String): CreationResult<CloudEntity> {

        val result = managers.clouds.createCloud(ADMIN_USER, CloudCreateRequest(cloud, ROOT_DOMAIN, "juergen@$cloud.$ROOT_DOMAIN", CLOUD_PASSWORD))

        if (result.hasErrors()) {
            throw RuntimeException("failed to create cloud '$cloud': ${result.messages.joinToString { it.code }}")
        }

        return result
    }

    fun createEnvironment(
        reference: CloudReference,
        environment: String,
    ): CreationResult<EnvironmentEntity> {
        val result = managers.environments.create(reference, ADMIN_USER, EnvironmentCreateRequest(environment, "juergen@$environment.${reference.cloud}.$ROOT_DOMAIN", ENVIRONMENT_PASSWORD, "<none>", "<none>", "<none>", "<none>"))

        if (result.hasErrors()) {
            throw RuntimeException("failed to create environment '$environment': ${result.messages.joinToString { it.code }}")
        }

        return result
    }

    fun createCloudEnvironment(
        cloud: String,
        environment: String,
    ): CreationResult<EnvironmentEntity> {
        val reference = createCloud(cloud).data!!.reference
        return createEnvironment(reference, environment)
    }

    fun createTenant(reference: EnvironmentReference, tenant: String): CreationResult<TenantEntity> {

        val result = managers.tenants.create(reference, ADMIN_USER, TenantCreateRequest(tenant, "juergen@$tenant.${reference.environment}.${reference.cloud}.$ROOT_DOMAIN", TENANT_PASSWORD))

        if (result.hasErrors()) {
            throw RuntimeException("failed to create tenant '$tenant': ${result.messages.joinToString { it.code }}")
        }

        return result
    }

    fun createCloudEnvironmentTenant(
        cloud: String,
        environment: String,
        tenant: String,
    ): CreationResult<TenantEntity> {
        val cloud = createCloud(cloud).data!!.reference
        val environment = createEnvironment(cloud, environment).data!!.reference
        return createTenant(environment, tenant)
    }
}
