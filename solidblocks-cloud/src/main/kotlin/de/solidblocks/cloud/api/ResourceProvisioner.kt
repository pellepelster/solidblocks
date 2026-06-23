package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.provisioner.BaseResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext

interface ResourceProvisioner<ResourceType : BaseInfrastructureResource<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime, LookupType : InfrastructureResourceLookup<RuntimeType>> :
    BaseResourceProvisioner<ResourceType, RuntimeType, ProvisionerDiffContext, ProvisionerApplyContext>
