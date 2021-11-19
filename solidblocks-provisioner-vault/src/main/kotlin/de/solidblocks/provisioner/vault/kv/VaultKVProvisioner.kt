package de.solidblocks.provisioner.vault.kv

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.mount.VaultMount
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.vault.core.VaultKeyValueOperations
import org.springframework.vault.core.VaultKeyValueOperationsSupport
import org.springframework.vault.core.VaultTemplate

@Component
class VaultKVProvisioner(val provisioner: Provisioner) :
    IInfrastructureResourceProvisioner<VaultKV, VaultKVRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun getResourceType(): Class<VaultKV> {
        return VaultKV::class.java
    }

    override fun lookup(resource: VaultKV): Result<VaultKVRuntime> {
        return try {
            val kvOperations = kvOperations(resource.mount)
            val result = kvOperations.get(resource.path)

            if (null == result) {
                Result(resource)
            } else {
                Result(resource, VaultKVRuntime(result.data))
            }
        } catch (e: Exception) {
            Result(resource, failed = true, message = e.message)
        }
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
        val kvOperations = kvOperations(resource.mount)
        kvOperations.put(resource.path, resource.data)

        return Result<Any>(resource)
    }

    private fun kvOperations(
        mount: VaultMount
    ): VaultKeyValueOperations {
        val vaultClient = provisioner.provider(VaultTemplate::class.java).createClient()
        return vaultClient.opsForKeyValue(mount.name, VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
    }
}
