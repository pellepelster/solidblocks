package de.solidblocks.cli.terraform

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import de.solidblocks.utils.logErrorBlcks
import de.solidblocks.utils.logInfoBlcks

class AWS(
    private val accessKeyId: String,
    private val secretAccessKey: String,
    private val region: String,
) {
  suspend fun ensureBucket(name: String) {
    s3Client().use {
      if (it.hasBucket(name)) {
        logInfoBlcks("S3 bucket '$name' already exists")
      } else {
        logInfoBlcks("creating S3 bucket '$name'")
        it.createNewBucket(name)
      }

      val currentPublicAccessBlock =
          it.getPublicAccessBlock(
                  GetPublicAccessBlockRequest { bucket = name },
              )
              .publicAccessBlockConfiguration

      if (currentPublicAccessBlock == null ||
          !currentPublicAccessBlock.blockPublicAcls!! ||
          !currentPublicAccessBlock.blockPublicPolicy!! ||
          !currentPublicAccessBlock.restrictPublicBuckets!! ||
          !currentPublicAccessBlock.ignorePublicAcls!!) {
        logInfoBlcks("disabling public access for S3 bucket '$name'")

        it.putPublicAccessBlock(
            PutPublicAccessBlockRequest {
              bucket = name
              publicAccessBlockConfiguration = PublicAccessBlockConfiguration {
                blockPublicAcls = true
                blockPublicPolicy = true
                restrictPublicBuckets = true
                ignorePublicAcls = true
              }
            },
        )
      } else {
        logInfoBlcks("public access for S3 bucket '$name' already disabled")
      }

      val versioning =
          it.getBucketVersioning(
              GetBucketVersioningRequest { this.bucket = name },
          )

      if (versioning.status != BucketVersioningStatus.Enabled) {
        logInfoBlcks("enabling versioning for bucket '$name'")

        it.putBucketVersioning(
            PutBucketVersioningRequest {
              bucket = name
              versioningConfiguration = VersioningConfiguration {
                status = BucketVersioningStatus.Enabled
              }
            },
        )
      } else {
        logInfoBlcks("bucket '$name' versioning is already enabled")
      }
    }
  }

  private fun s3Client() = S3Client {
    region = this@AWS.region
    this.credentialsProvider = StaticCredentialsProvider {
      accessKeyId = this@AWS.accessKeyId
      secretAccessKey = this@AWS.secretAccessKey
    }
  }

  private suspend fun S3Client.createNewBucket(name: String) {
    this.createBucket(
        CreateBucketRequest {
          bucket = name
          createBucketConfiguration = CreateBucketConfiguration {
            locationConstraint = BucketLocationConstraint.fromValue(region)
          }
        },
    )
  }

  private suspend fun S3Client.hasBucket(name: String): Boolean {
    try {
      this.headBucket(
          HeadBucketRequest { bucket = name },
      )
    } catch (e: NotFound) {
      return false
    } catch (e: Exception) {
      logErrorBlcks(e.message.orEmpty())
      return false
    }

    return true
  }
}
