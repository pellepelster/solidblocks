package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.utils.PassSecretLookup
import de.solidblocks.core.Result
import de.solidblocks.core.utils.PassWrapper
import org.springframework.stereotype.Component

@Component
class PassSecretResourceLookupProvider(val directoryProvider: PassSecretDirectoryProvider) :
    IResourceLookupProvider<PassSecretLookup, String> {

    private fun passWrapperInstance(): PassWrapper {
        return PassWrapper(directoryProvider.defaultDirectory())
    }

    override fun lookup(datasource: PassSecretLookup): Result<String> {
        return try {
            Result(passWrapperInstance().getSecret(datasource.key))
        } catch (e: Exception) {
            Result(failed = true, message = e.message)
        }
    }

    override fun getLookupType(): Class<PassSecretLookup> {
        return PassSecretLookup::class.java
    }
}
