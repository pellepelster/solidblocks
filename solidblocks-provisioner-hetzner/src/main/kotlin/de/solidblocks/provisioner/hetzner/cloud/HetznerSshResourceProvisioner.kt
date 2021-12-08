package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.ssh.ISshKeyLookup
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.api.resources.infrastructure.ssh.SshKeyRuntime
import de.solidblocks.core.Result
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.SSHKeyRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerSshResourceProvisioner(hetznerCloudAPI: HetznerCloudAPI) :
        IResourceLookupProvider<ISshKeyLookup, SshKeyRuntime>,
        IInfrastructureResourceProvisioner<SshKey, SshKeyRuntime>,
        BaseHetznerProvisioner<SshKey, SshKeyRuntime, HetznerCloudAPI>(hetznerCloudAPI) {

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

        return checkedApiCall {
            it.createSSHKey(request.build())
        }
    }

    private fun destroyById(id: Long): Boolean {
        return checkedApiCall {
            it.deleteSSHKey(id)
        }.mapSuccessBoolean()
    }

    override fun destroyAll(): Boolean {
        return checkedApiCall {
            it.sshKeys.sshKeys
        }.mapSuccessNonNullBoolean {
            it.map { sshKey ->
                logger.info { "destroying ssh keys '${sshKey.name}'" }
                destroyById(sshKey.id)
            }.any { it }
        }
    }

    override fun destroy(resource: SshKey): Boolean {
        return lookup(resource).mapNonNullResult {
            destroyById(it.id.toLong())
        }.mapSuccessNonNullBoolean { true }
    }

    override fun getResourceType(): Class<*> {
        return SshKey::class.java
    }

    override fun lookup(lookup: ISshKeyLookup): Result<SshKeyRuntime> {
        return checkedApiCall {
            it.sshKeys.sshKeys.filter { it.name == lookup.id() }.firstOrNull()
        }.mapNonNullResult {
            SshKeyRuntime(it.id.toString())
        }
    }

    override fun getLookupType(): Class<*> {
        return ISshKeyLookup::class.java
    }
}
