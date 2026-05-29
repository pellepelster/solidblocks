package de.solidblocks.cloud.provisioner.secret

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

interface SecretProvisioner<ResourceType : BaseResource, RuntimeType : GenericSecretRuntime, LookupType : InfrastructureResourceLookup<*>> :
    InfrastructureResourceProvisioner<ResourceType, RuntimeType, LookupType>