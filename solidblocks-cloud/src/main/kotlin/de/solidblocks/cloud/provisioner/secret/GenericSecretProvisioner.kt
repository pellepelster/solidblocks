package de.solidblocks.cloud.provisioner.secret

import de.solidblocks.cloud.api.ResourceProvisioner

interface GenericSecretProvisioner<ResourceType : GenericSecret, RuntimeType : GenericSecretRuntime, LookupType : GenericSecretLookup> :
    ResourceProvisioner<ResourceType, RuntimeType, LookupType>
