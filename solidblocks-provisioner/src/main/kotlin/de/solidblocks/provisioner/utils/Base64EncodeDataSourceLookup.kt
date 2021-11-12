package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IDataSourceLookup
import de.solidblocks.api.resources.infrastructure.utils.Base64Encode
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import org.springframework.stereotype.Component
import java.util.*

@Component
class Base64EncodeDataSourceLookup(val provisioner: Provisioner) : IDataSourceLookup<Base64Encode, String> {

    override fun lookup(datasource: Base64Encode): Result<String> {
        return provisioner.lookup(datasource.datasource).mapNonNullResult {
            Base64.getEncoder().encodeToString(it.encodeToByteArray())
        }
    }

    override fun getDatasourceType(): Class<Base64Encode> {
        return Base64Encode::class.java
    }
}
