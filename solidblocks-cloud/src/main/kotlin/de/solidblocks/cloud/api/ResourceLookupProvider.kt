package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.lookup.BaseResourceLookupProvider
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.context.ProvisionerLookupContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext

interface ResourceLookupProvider<ResourceLookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime> :
    BaseResourceLookupProvider<ResourceLookupType, RuntimeType, ProvisionerLookupContext>
