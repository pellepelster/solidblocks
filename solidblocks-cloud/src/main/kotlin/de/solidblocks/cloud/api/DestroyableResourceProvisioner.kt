package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.provisioner.BaseDestroyableResourceProvisioner
import de.solidblocks.cloud.provisioner.context.ProvisionerDestroyContext

interface DestroyableResourceProvisioner<LookupType> : BaseDestroyableResourceProvisioner<LookupType, ProvisionerDestroyContext>
