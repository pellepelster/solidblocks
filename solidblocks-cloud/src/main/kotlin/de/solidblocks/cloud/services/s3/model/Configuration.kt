package de.solidblocks.cloud.services.s3.model

import de.solidblocks.cloud.services.ServiceConfiguration

data class S3ServiceConfiguration(
    override val name: String,
    val buckets: List<S3ServiceBucketConfiguration>,
) : ServiceConfiguration {
    override val type = "s3"
}

data class S3ServiceBucketAccessKeyConfiguration(val name: String)

data class S3ServiceBucketConfiguration(val name: String, val publicAccess: Boolean, val accessKeys: List<S3ServiceBucketAccessKeyConfiguration>)
