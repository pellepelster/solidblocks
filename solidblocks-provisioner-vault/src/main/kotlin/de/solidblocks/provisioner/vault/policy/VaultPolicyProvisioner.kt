package de.solidblocks.provisioner.vault.policy

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import mu.KotlinLogging
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.Policy

class VaultPolicyProvisioner(val vaultTemplateProvider: () -> VaultTemplate) :
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
        val vaultTemplate = vaultTemplateProvider.invoke()

        vaultTemplate.opsForSys().createOrUpdatePolicy(resource.id, Policy.of(resource.rules))
        return Result<Any>(resource)
    }

    override fun lookup(lookup: IVaultPolicyLookup): Result<VaultPolicyRuntime> {
        return try {
            val vaultTemplate = vaultTemplateProvider.invoke()
            val policy = vaultTemplate.opsForSys().getPolicy(lookup.id())

            if (null == policy) {
                Result()
            } else {
                Result(VaultPolicyRuntime(policy.rules))
            }
        } catch (e: Exception) {
            Result(failed = true, message = e.message)
        }
    }

    override fun getLookupType(): Class<*> {
        return IVaultPolicyLookup::class.java
    }
}
