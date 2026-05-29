package de.solidblocks.cloud.provisioner.secret

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner

interface GenericSecretProvisioner<ResourceType : GenericSecret<GenericSecretRuntime>, RuntimeType : GenericSecretRuntime, LookupType : GenericSecretLookup> :
    InfrastructureResourceProvisioner<ResourceType, RuntimeType, LookupType>
