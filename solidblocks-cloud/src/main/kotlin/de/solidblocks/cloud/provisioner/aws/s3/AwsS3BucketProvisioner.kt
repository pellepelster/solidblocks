package de.solidblocks.cloud.provisioner.aws.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.BucketVersioningStatus
import aws.sdk.kotlin.services.s3.model.CreateBucketConfiguration
import aws.sdk.kotlin.services.s3.model.CreateBucketRequest
import aws.sdk.kotlin.services.s3.model.Delete
import aws.sdk.kotlin.services.s3.model.DeleteBucketRequest
import aws.sdk.kotlin.services.s3.model.DeleteObjectsRequest
import aws.sdk.kotlin.services.s3.model.GetBucketVersioningRequest
import aws.sdk.kotlin.services.s3.model.GetPublicAccessBlockRequest
import aws.sdk.kotlin.services.s3.model.HeadBucketRequest
import aws.sdk.kotlin.services.s3.model.ListObjectVersionsRequest
import aws.sdk.kotlin.services.s3.model.NoSuchBucket
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.ObjectIdentifier
import aws.sdk.kotlin.services.s3.model.PublicAccessBlockConfiguration
import aws.sdk.kotlin.services.s3.model.PutBucketVersioningRequest
import aws.sdk.kotlin.services.s3.model.PutPublicAccessBlockRequest
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.model.VersioningConfiguration
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.SHORT_WAIT
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.WaitConfig
import de.solidblocks.cloud.utils.waitForCondition
import de.solidblocks.cloud.utils.waitForConsecutive
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

class AwsS3BucketProvisioner(
    private val accessKeyId: String,
    private val secretAccessKey: String,
    private val region: String,
) : ResourceLookupProvider<AwsS3BucketLookup, AwsS3BucketRuntime>,
    InfrastructureResourceProvisioner<AwsS3Bucket, AwsS3BucketRuntime> {

    val waitConfig = WaitConfig(15, 2.seconds)

    override suspend fun lookup(lookup: AwsS3BucketLookup, context: CloudProvisionerContext): AwsS3BucketRuntime? = s3Client().use { client ->
        try {
            client.headBucket(HeadBucketRequest { bucket = lookup.name })
        } catch (e: NotFound) {
            return null
        } catch (e: NoSuchBucket) {
            return null
        }

        AwsS3BucketRuntime(
            lookup.name,
            region,
        )
    }

    override suspend fun diff(resource: AwsS3Bucket, context: CloudProvisionerContext): ResourceDiff {
        val runtime = lookup(resource.asLookup(), context) ?: return ResourceDiff(resource, missing)

        val changes = mutableListOf<ResourceDiffItem>()

        return if (changes.isEmpty()) {
            ResourceDiff(resource, up_to_date)
        } else {
            ResourceDiff(resource, has_changes, changes = changes)
        }
    }

    override suspend fun apply(resource: AwsS3Bucket, context: CloudProvisionerContext, log: LogContext): Result<AwsS3BucketRuntime> = s3Client().use { client ->
        val exists = try {
            client.headBucket(HeadBucketRequest { bucket = resource.name })
            true
        } catch (e: NotFound) {
            false
        } catch (e: NoSuchBucket) {
            false
        }

        if (!exists) {
            log.info("creating S3 bucket '${resource.name}' in region '$region'")
            client.createBucket(
                CreateBucketRequest {
                    bucket = resource.name
                    if (region != "us-east-1") {
                        createBucketConfiguration = CreateBucketConfiguration {
                            locationConstraint = BucketLocationConstraint.fromValue(region)
                        }
                    }
                },
            )

            waitConfig.waitForConsecutive(3) {
                try {
                    log.info("waiting for creation of bucket '${resource.name}'...")
                    lookup(resource.asLookup(), context)
                } catch (e: Exception) {
                    null
                }
            }
        }

        client.putPublicAccessBlock(
            PutPublicAccessBlockRequest {
                bucket = resource.name
                publicAccessBlockConfiguration = PublicAccessBlockConfiguration {
                    blockPublicAcls = true
                    blockPublicPolicy = true
                    restrictPublicBuckets = true
                    ignorePublicAcls = true
                }
            },
        )

        lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error("error applying ${resource.logText()}")
    }

    override suspend fun destroy(resource: AwsS3Bucket, context: CloudProvisionerContext, log: LogContext): Boolean {
        s3Client().use { client ->
            try {
                var truncated = true
                while (truncated) {
                    val response = client.listObjectVersions(ListObjectVersionsRequest { bucket = resource.name })
                    val toDelete =
                        (response.versions ?: emptyList()).map {
                            ObjectIdentifier {
                                key = it.key
                                versionId = it.versionId
                            }
                        } + (response.deleteMarkers ?: emptyList()).map {
                            ObjectIdentifier {
                                key = it.key
                                versionId = it.versionId
                            }
                        }

                    if (toDelete.isNotEmpty()) {
                        client.deleteObjects(
                            DeleteObjectsRequest {
                                bucket = resource.name
                                delete = Delete { objects = toDelete }
                            },
                        )
                    }
                    truncated = response.isTruncated == true
                }

                client.deleteBucket(DeleteBucketRequest { bucket = resource.name })

                WaitConfig(60, 2.seconds).waitForConsecutive(3) {
                    try {
                        log.info("waiting for deletion of bucket '${resource.name}'...")
                        lookup(resource.asLookup(), context)
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: NoSuchBucket) {
                return false
            }
        }
        return true
    }

    private fun s3Client() = S3Client {
        region = this@AwsS3BucketProvisioner.region
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = this@AwsS3BucketProvisioner.accessKeyId
            secretAccessKey = this@AwsS3BucketProvisioner.secretAccessKey
        }
    }

    override val supportedLookupType: KClass<*> = AwsS3BucketLookup::class

    override val supportedResourceType: KClass<*> = AwsS3Bucket::class
}
