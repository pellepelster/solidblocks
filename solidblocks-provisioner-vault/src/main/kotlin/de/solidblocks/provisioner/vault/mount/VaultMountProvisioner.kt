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

        vaultTemplate.opsForSys().mount(resource.name, mount)

        return Result<Any>(resource)
    }

    override fun lookup(lookup: IVaultMountLookup): Result<VaultMountRuntime> {
        val vaultTemplate = vaultTemplateProvider.invoke()

        return if (vaultTemplate.opsForSys().mounts.keys.any { it == "${lookup.name}/" }) {
            Result(VaultMountRuntime())
        } else {
            Result()
        }
    }

    override val resourceType = IVaultMountLookup::class.java

    override val lookupType = VaultMount::class.java
}
