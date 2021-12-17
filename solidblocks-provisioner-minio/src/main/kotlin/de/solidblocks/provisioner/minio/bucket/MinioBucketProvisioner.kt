package de.solidblocks.provisioner.minio.bucket

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import mu.KotlinLogging
import java.util.*

class MinioBucketProvisioner(val minioClient: MinioClient) :
    IResourceLookupProvider<IMinioBucketLookup, MinioBucketRuntime>,
    IInfrastructureResourceProvisioner<MinioBucket, MinioBucketRuntime> {

    private val logger = KotlinLogging.logger {}

    override fun getResourceType(): Class<MinioBucket> {
        return MinioBucket::class.java
    }

    override fun diff(resource: MinioBucket): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
            {
                ResourceDiff(resource, missing = false)
            },
            {
                ResourceDiff(resource, missing = true)
            }
        )
    }

    override fun apply(resource: MinioBucket): Result<*> {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(resource.name).build())
        return Result<Any>(resource)
    }

    override fun lookup(resource: IMinioBucketLookup): Result<MinioBucketRuntime> {
        return try {
            val bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(resource.name()).build())

            if (bucketExists) {
                Result.resultOf(MinioBucketRuntime(resource.name()))
            } else {
                Result.emptyResult()
            }
        } catch (e: Exception) {
            Result(failed = true, message = e.message)
        }
    }

    override fun getLookupType(): Class<*> {
        return IMinioBucketLookup::class.java
    }
}
