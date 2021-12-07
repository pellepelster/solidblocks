package de.solidblocks.provisioner.vault.mount

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class VaultMountProvisioner(val vaultRootClientProvider: VaultRootClientProvider, val provisioner: Provisioner) :
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
        val vaultClient = vaultRootClientProvider.createClient()

        val mount = org.springframework.vault.support.VaultMount.create(resource.type)
        vaultClient.opsForSys().mount(resource.id, mount)

        return Result<Any>(resource)
    }

    override fun getResourceType(): Class<VaultMount> {
        return VaultMount::class.java
    }

    override fun lookup(lookup: IVaultMountLookup): Result<VaultMountRuntime> {
        val vaultClient = vaultRootClientProvider.createClient()

        return if (vaultClient.opsForSys().mounts.keys.any { it == "${lookup.id()}/" }) {
            Result(VaultMountRuntime())
        } else {
            Result()
        }
    }

    override fun getLookupType(): Class<*> {
        return IVaultMountLookup::class.java
    }
}
