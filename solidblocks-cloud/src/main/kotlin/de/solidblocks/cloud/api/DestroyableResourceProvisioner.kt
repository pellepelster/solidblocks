package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.provisioner.BaseDestroyableResourceProvisioner
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.context.ProvisionerDestroyContext

interface DestroyableResourceProvisioner<LookupType : InfrastructureResourceLookup<*>> : BaseDestroyableResourceProvisioner<LookupType, ProvisionerDestroyContext>
