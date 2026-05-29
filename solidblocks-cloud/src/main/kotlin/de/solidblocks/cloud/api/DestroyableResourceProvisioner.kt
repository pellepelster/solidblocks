package de.solidblocks.cloud.api

import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.utils.LogContext

/**
 * Optional capability for provisioners that can destroy a resource. Provisioners that do not
 * implement this cannot be destroyed (e.g. as part of a recreate); the registry reports that
 * clearly instead of failing with a [NotImplementedError].
 */
interface DestroyableResourceProvisioner<LookupType> {
    suspend fun destroy(lookup: LookupType, context: SSHProvisionerContext, log: LogContext): Boolean
}
