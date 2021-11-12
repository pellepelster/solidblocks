package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.api.resources.infrastructure.ssh.SshKeyRuntime
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.SSHKeyRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerSshResourceProvisioner(credentialsProvider: HetznerCloudCredentialsProvider) :
    BaseHetznerProvisioner<SshKey, SshKeyRuntime, HetznerCloudAPI>(
        { HetznerCloudAPI(credentialsProvider.defaultApiToken()) },
        SshKey::class.java
    ) {

    private val logger = KotlinLogging.logger {}

    override fun diff(resource: SshKey): Result<ResourceDiff<SshKeyRuntime>> {
        return lookup(resource).mapResourceResultOrElse(
            {
                ResourceDiff(resource)
            },
            {
                ResourceDiff(resource, true)
            }
        )
    }

    override fun lookup(resource: SshKey): Result<SshKeyRuntime> {
        return checkedApiCall(resource, HetznerCloudAPI::getSSHKeys) {
            it.sshKeys.sshKeys.filter { it.name == resource.name }.firstOrNull()
        }.mapNonNullResult {
            SshKeyRuntime(it.id.toString())
        }
    }

    override fun apply(resource: SshKey): Result<*> {
        val request = SSHKeyRequest.builder().name(resource.name).publicKey(resource.publicKey)

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
}
