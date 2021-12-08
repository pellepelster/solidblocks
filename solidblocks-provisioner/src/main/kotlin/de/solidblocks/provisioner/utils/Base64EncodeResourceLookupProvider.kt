package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.utils.Base64Encode
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import java.util.*

class Base64EncodeResourceLookupProvider(val provisioner: Provisioner) : IResourceLookupProvider<Base64Encode, String> {

    override fun lookup(datasource: Base64Encode): Result<String> {
        return provisioner.lookup(datasource.datasource).mapNonNullResult {
            Base64.getEncoder().encodeToString(it.encodeToByteArray())
        }
    }

    override fun getLookupType(): Class<Base64Encode> {
        return Base64Encode::class.java
    }
}
