package de.solidblocks.provisioner

import de.solidblocks.cloud.api.provisioner.BaseResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

interface TestResourceProvisioner<ResourceType : BaseInfrastructureResource<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime, LookupType : InfrastructureResourceLookup<RuntimeType>> :
    BaseResourceProvisioner<ResourceType, RuntimeType, TestDiffContext, TestApplyContext>
