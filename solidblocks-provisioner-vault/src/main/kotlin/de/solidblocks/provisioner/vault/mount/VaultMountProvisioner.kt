package de.solidblocks.provisioner.vault.mount

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import mu.KotlinLogging
import org.springframework.vault.core.VaultTemplate

class VaultMountProvisioner(val vaultTemplateProvider: () -> VaultTemplate) :
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
        val mount = org.springframework.vault.support.VaultMount.create(resource.type)
        val vaultTemplate = vaultTemplateProvider.invoke()

        vaultTemplate.opsForSys().mount(resource.id, mount)

        return Result<Any>(resource)
    }

    override fun lookup(lookup: IVaultMountLookup): Result<VaultMountRuntime> {
        val vaultTemplate = vaultTemplateProvider.invoke()

        return if (vaultTemplate.opsForSys().mounts.keys.any { it == "${lookup.id()}/" }) {
            Result(VaultMountRuntime())
        } else {
            Result()
        }
    }

    override fun getLookupType(): Class<*> {
        return IVaultMountLookup::class.java
    }

    override fun getResourceType(): Class<VaultMount> {
        return VaultMount::class.java
    }
}
