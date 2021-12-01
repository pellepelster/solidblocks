package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.ssh.ISshKeyLookup
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.api.resources.infrastructure.ssh.SshKeyRuntime
import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.Constants
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.SSHKeyRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerSshResourceProvisioner(cloudContext: CloudConfigurationContext) :
        IResourceLookupProvider<ISshKeyLookup, SshKeyRuntime>,
        IInfrastructureResourceProvisioner<SshKey, SshKeyRuntime>,
        BaseHetznerProvisioner<SshKey, SshKeyRuntime, HetznerCloudAPI>(
                { HetznerCloudAPI(cloudContext.configurationValue(Constants.ConfigKeys.HETZNER_CLOUD_API_TOKEN_RW_KEY)) }) {

    private val logger = KotlinLogging.logger {}

    override fun diff(resource: SshKey): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
                {
                    ResourceDiff(resource)
                },
                {
                    ResourceDiff(resource, true)
                }
        )
    }

    override fun apply(resource: SshKey): Result<*> {
        val request = SSHKeyRequest.builder().name(resource.id).publicKey(resource.publicKey)

        return checkedApiCall(resource, HetznerCloudAPI::createSSHKey) {
            it.createSSHKey(request.build())
        }
    }

    private fun destroyById(id: Long): Result<*> {
        return checkedApiCall(NullResource, HetznerCloudAPI::deleteSSHKey) {
            it.deleteSSHKey(id)
        }
    }

    override fun destroyAll(): Result<*> {
        return checkedApiCall(NullResource, HetznerCloudAPI::getSSHKeys) {
            it.sshKeys.sshKeys
        }.mapNonNullResult {
            it.map { sshKey ->
                logger.info { "destroying ssh keys '${sshKey.name}'" }
                destroyById(sshKey.id)
            }.reduceResults()
        }
    }

    override fun destroy(resource: SshKey): Result<*> {
        return lookup(resource).mapNonNullResult {
            destroyById(it.id.toLong())
        }
    }

    override fun getResourceType(): Class<*> {
        return SshKey::class.java
    }

    override fun lookup(lookup: ISshKeyLookup): Result<SshKeyRuntime> {
        return checkedApiCall(lookup, HetznerCloudAPI::getSSHKeys) {
            it.sshKeys.sshKeys.filter { it.name == lookup.id() }.firstOrNull()
        }.mapNonNullResult {
            SshKeyRuntime(it.id.toString())
        }
    }

    override fun getLookupType(): Class<*> {
        return ISshKeyLookup::class.java
    }
}
