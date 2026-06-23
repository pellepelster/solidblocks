package de.solidblocks.cloud.api.resources

import de.solidblocks.cloud.api.provisioner.BaseDestroyableResourceProvisioner

interface DestroyableResourceProvisioner<LookupType : InfrastructureResourceLookup<*>, DestroyContext> :
    BaseDestroyableResourceProvisioner<LookupType, DestroyContext>
