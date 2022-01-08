package de.solidblocks.base.lookups

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.InfrastructureProvisioner
import de.solidblocks.core.Result
import java.util.*

class Base64EncodeResourceLookupProvider(val provisioner: InfrastructureProvisioner) :
    IResourceLookupProvider<Base64Encode, String> {

    override fun lookup(datasource: Base64Encode): Result<String> {
        return provisioner.lookup(datasource.datasource).mapNonNullResult {
            Base64.getEncoder().encodeToString(it.encodeToByteArray())
        }
    }

    override val lookupType = Base64Encode::class.java
}
