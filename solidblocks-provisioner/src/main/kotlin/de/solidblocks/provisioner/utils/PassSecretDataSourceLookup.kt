package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IDataSourceLookup
import de.solidblocks.api.resources.infrastructure.utils.PassSecretLookup
import de.solidblocks.core.Result
import de.solidblocks.core.utils.PassWrapper
import org.springframework.stereotype.Component

@Component
class PassSecretDataSourceLookup(val directoryProvider: PassSecretDirectoryProvider) :
    IDataSourceLookup<PassSecretLookup, String> {

    private fun passWrapperInstance(): PassWrapper {
        return PassWrapper(directoryProvider.defaultDirectory())
    }

    override fun lookup(datasource: PassSecretLookup): Result<String> {
        return try {
            Result(datasource, passWrapperInstance().getSecret(datasource.key))
        } catch (e: Exception) {
            Result(datasource, failed = true, message = e.message)
        }
    }

    override fun getDatasourceType(): Class<PassSecretLookup> {
        return PassSecretLookup::class.java
    }
}
