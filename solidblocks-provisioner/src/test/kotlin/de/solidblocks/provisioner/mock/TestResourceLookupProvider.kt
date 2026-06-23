package de.solidblocks.provisioner.mock

import de.solidblocks.cloud.api.lookup.BaseResourceLookupProvider
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

interface TestResourceLookupProvider<ResourceLookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime> :
    BaseResourceLookupProvider<ResourceLookupType, RuntimeType, TestLookupContext>