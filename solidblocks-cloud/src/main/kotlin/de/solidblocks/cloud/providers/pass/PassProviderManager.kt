package de.solidblocks.cloud.providers.pass

import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.provisioner.pass.PassSecretProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.commandExists
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logDebug
import de.solidblocks.utils.logError

class PassProviderManager :
    ProviderConfigurationManager<PassProviderConfiguration, PassProviderRuntime> {

  override fun validate(
      configuration: PassProviderConfiguration,
      context: LogContext,
  ): Result<PassProviderRuntime> {
    if (commandExists("pass")) {
      logDebug("found 'pass' executable", context = context)
    } else {
      "'pass' executable not found"
          .also {
            logError(it, context = context)
            return Error<PassProviderRuntime>(it)
          }
    }

    return Success(PassProviderRuntime())
  }

  // TODO
  override fun createProvisioners(runtime: PassProviderRuntime) =
      listOf(PassSecretProvisioner(null))

  override val supportedConfiguration = PassProviderConfiguration::class
}
