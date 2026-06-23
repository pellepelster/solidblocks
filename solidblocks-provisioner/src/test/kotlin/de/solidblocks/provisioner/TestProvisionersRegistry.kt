package de.solidblocks.provisioner

import de.solidblocks.cloud.api.lookup.BaseResourceLookupProvider
import de.solidblocks.cloud.api.provisioner.BaseResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.BaseProvisionersRegistry

class TestProvisionersRegistry(
    resourceLookupProviders: List<TestResourceLookupProvider<*, *>> = emptyList(),
    resourceProvisioners: List<TestResourceProvisioner<*, *, *>> = emptyList(),
) : BaseProvisionersRegistry<TestDiffContext, TestApplyContext, TestDestroyContext, TestLookupContext>(
    (resourceLookupProviders as List<BaseResourceLookupProvider<InfrastructureResourceLookup<BaseInfrastructureResourceRuntime>, BaseInfrastructureResourceRuntime, TestLookupContext>>),
    @Suppress("UNCHECKED_CAST")
    (resourceProvisioners as List<BaseResourceProvisioner<BaseInfrastructureResource<BaseInfrastructureResourceRuntime>, BaseInfrastructureResourceRuntime, TestDiffContext, TestApplyContext>>),
)
