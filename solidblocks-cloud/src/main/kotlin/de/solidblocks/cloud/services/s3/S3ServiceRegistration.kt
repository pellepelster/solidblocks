package de.solidblocks.cloud.services.s3

import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfigurationFactory
import de.solidblocks.cloud.services.s3.model.S3ServiceConfigurationRuntime

class S3ServiceRegistration :
    ServiceRegistration<S3ServiceConfiguration, S3ServiceConfigurationRuntime> {
  override val supportedConfiguration = S3ServiceConfiguration::class
  override val supportedRuntime = S3ServiceConfigurationRuntime::class

  override fun createManager(cloudConfiguration: CloudConfigurationRuntime) =
      S3ServiceConfigurationManager(cloudConfiguration)

  override fun createConfigurationFactory() = S3ServiceConfigurationFactory()

  override val type = "s3"
}
