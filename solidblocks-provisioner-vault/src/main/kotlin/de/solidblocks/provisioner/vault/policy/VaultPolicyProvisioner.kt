package de.solidblocks.provisioner.vault.policy

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.vault.support.Policy

@Component
class VaultPolicyProvisioner(val vaultClientProvider: VaultRootClientProvider, val provisioner: Provisioner) :
        IResourceLookupProvider<IVaultPolicyLookup, VaultPolicyRuntime>,
        IInfrastructureResourceProvisioner<VaultPolicy, VaultPolicyRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun getResourceType(): Class<VaultPolicy> {
        return VaultPolicy::class.java
    }

    override fun diff(resource: VaultPolicy): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
                {

                val changes = mutableListOf<ResourceDiffItem>()

                if (!it.rules.containsAll(resource.rules)) {
                    changes.add(ResourceDiffItem("rules", changed = true))
                }

                ResourceDiff(resource, changes = changes)
            },
            {
                ResourceDiff(resource, missing = true)
            }
        )
    }

    override fun apply(resource: VaultPolicy): Result<*> {
        val vaultClient = vaultClientProvider.createClient()
        vaultClient.opsForSys().createOrUpdatePolicy(resource.id, Policy.of(resource.rules))
        return Result<Any>(resource)
    }

    override fun lookup(lookup: IVaultPolicyLookup): Result<VaultPolicyRuntime> {
        val vaultClient = vaultClientProvider.createClient()

        return try {
            val policy = vaultClient.opsForSys().getPolicy(lookup.id())

            if (null == policy) {
                Result(lookup)
            } else {
                Result(lookup, VaultPolicyRuntime(policy.rules))
            }
        } catch (e: Exception) {
            Result(lookup, failed = true, message = e.message)
        }
    }

    override fun getLookupType(): Class<*> {
        return IVaultPolicyLookup::class.java
    }
}
