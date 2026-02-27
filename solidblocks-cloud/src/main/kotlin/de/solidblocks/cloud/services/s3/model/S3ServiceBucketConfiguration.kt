package de.solidblocks.cloud.services.s3.model

data class S3ServiceBucketConfiguration(val name: String, val publicAccess: Boolean, val accessKeys: List<S3ServiceBucketAccessKeyConfiguration>)
