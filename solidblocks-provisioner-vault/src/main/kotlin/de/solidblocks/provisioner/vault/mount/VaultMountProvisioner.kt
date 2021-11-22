package de.solidblocks.provisioner.vault.mount

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.vault.core.VaultTemplate

@Component
class VaultMountProvisioner(val provisioner: Provisioner) :
        IResourceLookupProvider<IVaultMountLookup, VaultMountRuntime>,
        IInfrastructureResourceProvisioner<VaultMount, VaultMountRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun diff(resource: VaultMount): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
                {
                    ResourceDiff(
                            resource
                    )
                },
                {
                    ResourceDiff(
                            resource,
                            missing = true
                    )
                }
        )
    }

    override fun apply(resource: VaultMount): Result<*> {
        val vaultClient = provisioner.provider(VaultTemplate::class.java).createClient()

        val mount = org.springframework.vault.support.VaultMount.create(resource.type)
        vaultClient.opsForSys().mount(resource.name, mount)

        return Result<Any>(resource)
    }

    override fun getResourceType(): Class<VaultMount> {
        return VaultMount::class.java
    }

    override fun lookup(lookup: IVaultMountLookup): Result<VaultMountRuntime> {
        val vaultClient = provisioner.provider(VaultTemplate::class.java).createClient()

        return if (vaultClient.opsForSys().mounts.keys.any { it == "${lookup.name()}/" }) {
            Result(lookup, VaultMountRuntime())
        } else {
            Result(lookup)
        }
    }

    override fun getLookupType(): Class<*> {
        return IVaultMountLookup::class.java
    }
}
