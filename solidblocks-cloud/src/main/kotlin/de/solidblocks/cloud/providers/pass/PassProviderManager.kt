package de.solidblocks.cloud.providers.pass

import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.provisioner.pass.PassSecretProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.commandExists
import de.solidblocks.utils.LogContext

class PassProviderManager : ProviderConfigurationManager<PassProviderConfiguration, PassProviderRuntime> {

    override fun validate(configuration: PassProviderConfiguration, context: CloudConfigurationContext, log: LogContext): Result<PassProviderRuntime> {
        if (commandExists("pass")) {
            log.debug("found 'pass' executable")
        } else {
            "'pass' executable not found"
                .also {
                    log.error(it)
                    return Error<PassProviderRuntime>(it)
                }
        }

        return Success(PassProviderRuntime())
    }

    override fun createProvisioners(runtime: PassProviderRuntime) = listOf(PassSecretProvisioner(null))

    override val supportedConfiguration = PassProviderConfiguration::class
}
