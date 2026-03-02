package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.ApplyResult
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.unknown
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.garagefs.bucket.BaseGarageFsProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.garagefs.CreateKeyRequest
import de.solidblocks.garagefs.GarageFsApi
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass

class GarageFsAccessKeyProvisioner :
    BaseGarageFsProvisioner(),
    ResourceLookupProvider<GarageFsAccessKeyLookup, GarageFsAccessKeyRuntime>,
    InfrastructureResourceProvisioner<GarageFsAccessKey, GarageFsAccessKeyRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun diff(resource: GarageFsAccessKey, context: ProvisionerContext) = when (val result = lookupInternal(resource.asLookup(), context)) {
        is Error<GarageFsAccessKeyRuntime?> -> ResourceDiff(resource, unknown)
        is Success<GarageFsAccessKeyRuntime?> -> {
            if (result.data == null) {
                ResourceDiff(resource, missing)
            } else {
                ResourceDiff(resource, up_to_date)
            }
        }
    }

    override suspend fun lookup(lookup: GarageFsAccessKeyLookup, context: ProvisionerContext) =
        when (val result = lookupInternal(lookup, context)) {
            is Error<GarageFsAccessKeyRuntime?> -> null
            is Success<GarageFsAccessKeyRuntime?> -> result.data
        }

    suspend fun lookupInternal(lookup: GarageFsAccessKeyLookup, context: ProvisionerContext): Result<GarageFsAccessKeyRuntime?> =
        context.withApiClients(lookup.server, lookup.adminToken.asLookup()) { apis ->
            when (apis) {
                is Error<GarageFsApi> -> Error(apis.error)
                is Success<GarageFsApi> -> {
                    val accessKey = apis.data.accessKeyApi.listKeys().firstOrNull { it.name == lookup.name }

                    if (accessKey == null) {
                        logger.warn { "access key '${lookup.name}' not found" }
                        return@withApiClients Success(null)
                    }

                    val keyInfo = apis.data.accessKeyApi.getKeyInfo(accessKey.id, showSecretKey = true)

                    if (keyInfo.secretAccessKey == null) {
                        logger.error { "secret access key not set" }
                        Success(null)
                    } else {
                        Success(GarageFsAccessKeyRuntime(lookup.name, accessKey.id, keyInfo.secretAccessKey!!))
                    }
                }
            }

        }

    override suspend fun apply(
        resource: GarageFsAccessKey,
        context: ProvisionerContext,
        log: LogContext,
    ): ApplyResult<GarageFsAccessKeyRuntime> {
        val runtime = when (val result = lookupInternal(resource.asLookup(), context)) {
            is Error<GarageFsAccessKeyRuntime?> -> return ApplyResult(null)
            is Success<GarageFsAccessKeyRuntime?> -> result.data
        }

        if (runtime != null) {
            return ApplyResult(runtime)
        }

        context.withApiClients(resource.server.asLookup(), resource.adminToken.asLookup()) {
            val apis = when (it) {
                is Error<GarageFsApi> -> throw RuntimeException(it.error)
                is Success<GarageFsApi> -> it.data
            }

            apis.accessKeyApi.createKey(CreateKeyRequest(name = resource.name))
        }

        return ApplyResult(lookup(resource.asLookup(), context))
    }

    override val supportedLookupType: KClass<*> = GarageFsAccessKeyLookup::class

    override val supportedResourceType: KClass<*> = GarageFsAccessKey::class
}
