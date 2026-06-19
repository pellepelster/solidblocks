package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.provisioner.BaseResourceProvisioner
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext

interface ResourceProvisioner<ResourceType, RuntimeType, LookupType> : BaseResourceProvisioner<ResourceType, RuntimeType, ProvisionerDiffContext, ProvisionerApplyContext>
