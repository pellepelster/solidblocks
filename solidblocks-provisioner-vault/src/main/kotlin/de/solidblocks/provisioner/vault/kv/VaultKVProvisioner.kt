package de.solidblocks.provisioner.vault.kv

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.vault.mount.IVaultMountLookup
import mu.KotlinLogging
import org.springframework.vault.core.VaultKeyValueOperations
import org.springframework.vault.core.VaultKeyValueOperationsSupport
import org.springframework.vault.core.VaultTemplate

class VaultKVProvisioner(val vaultTemplateProvider: () -> VaultTemplate) :
    IResourceLookupProvider<IVaultKVLookup, VaultKVRuntime>,
    IInfrastructureResourceProvisioner<VaultKV, VaultKVRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun getResourceType(): Class<VaultKV> {
        return VaultKV::class.java
    }

    override fun diff(resource: VaultKV): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
            {
                val changes = mutableListOf<ResourceDiffItem>()

                if (it.data != resource.data) {
                    changes.add(ResourceDiffItem("data", changed = true))
                }

                ResourceDiff(resource, changes = changes)
            },
            {
                ResourceDiff(resource, missing = true)
            }
        )
    }

    override fun apply(resource: VaultKV): Result<*> {
        val kvOperations = kvOperations(resource.mount())
        kvOperations.put(resource.path, resource.data)

        return Result<Any>(resource)
    }

    private fun kvOperations(
        mount: IVaultMountLookup
    ): VaultKeyValueOperations {
        val vaultTemplate = vaultTemplateProvider.invoke()

        return vaultTemplate.opsForKeyValue(mount.id(), VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
    }

    override fun lookup(lookup: IVaultKVLookup): Result<VaultKVRuntime> {
        return try {
            val kvOperations = kvOperations(lookup.mount())
            val result = kvOperations.get(lookup.path())

            if (null == result) {
                Result()
            } else {
                Result(VaultKVRuntime(result.data as Map<String, Any>))
            }
        } catch (e: Exception) {
            Result(failed = true, message = e.message)
        }
    }

    override fun getLookupType(): Class<*> {
        return IVaultKVLookup::class.java
    }
}
