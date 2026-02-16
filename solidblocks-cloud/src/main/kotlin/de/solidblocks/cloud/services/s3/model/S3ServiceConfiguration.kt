package de.solidblocks.cloud.services.s3.model

import de.solidblocks.cloud.services.ServiceConfiguration

data class S3ServiceConfiguration(override val name: String, val buckets: List<S3ServiceBucket>) :
    ServiceConfiguration {
  override val type = "s3"
}
