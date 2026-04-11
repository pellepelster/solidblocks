package de.solidblocks.cli.terraform

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.iam.IamClient
import aws.sdk.kotlin.services.iam.model.*
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import de.solidblocks.utils.logError
import de.solidblocks.utils.logInfo

class AWS(private val accessKeyId: String, private val secretAccessKey: String, private val region: String) {
    suspend fun ensureBucket(name: String) {
        s3Client().use {
            if (it.hasBucket(name)) {
                logInfo("S3 bucket '$name' already exists")
            } else {
                logInfo("creating S3 bucket '$name'")
                it.createNewBucket(name)
            }

            val currentPublicAccessBlock =
                it.getPublicAccessBlock(
                    GetPublicAccessBlockRequest { bucket = name },
                )
                    .publicAccessBlockConfiguration

            if (
                currentPublicAccessBlock == null ||
                !currentPublicAccessBlock.blockPublicAcls!! ||
                !currentPublicAccessBlock.blockPublicPolicy!! ||
                !currentPublicAccessBlock.restrictPublicBuckets!! ||
                !currentPublicAccessBlock.ignorePublicAcls!!
            ) {
                logInfo("disabling public access for S3 bucket '$name'")

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
                logInfo("public access for S3 bucket '$name' already disabled")
            }

            val versioning =
                it.getBucketVersioning(
                    GetBucketVersioningRequest { this.bucket = name },
                )

            if (versioning.status != BucketVersioningStatus.Enabled) {
                logInfo("enabling versioning for bucket '$name'")

                it.putBucketVersioning(
                    PutBucketVersioningRequest {
                        bucket = name
                        versioningConfiguration = VersioningConfiguration {
                            status = BucketVersioningStatus.Enabled
                        }
                    },
                )
            } else {
                logInfo("bucket '$name' versioning is already enabled")
            }
        }
    }

    data class BucketUserCredentials(val accessKeyId: String, val secretAccessKey: String)

    suspend fun ensureBucketUser(bucketName: String, userName: String): BucketUserCredentials {
        iamClient().use { iam ->
            val userExists = try {
                iam.getUser(GetUserRequest { this.userName = userName })
                true
            } catch (e: NoSuchEntityException) {
                false
            }

            if (userExists) {
                logInfo("IAM user '$userName' already exists")
            } else {
                logInfo("creating IAM user '$userName'")
                iam.createUser(CreateUserRequest { this.userName = userName })
            }

            val policyName = "$userName-$bucketName-policy"
            val policyDocument = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": [
                        "s3:GetObject",
                        "s3:PutObject",
                        "s3:DeleteObject",
                        "s3:ListBucket"
                      ],
                      "Resource": [
                        "arn:aws:s3:::$bucketName",
                        "arn:aws:s3:::$bucketName/*"
                      ]
                    }
                  ]
                }
            """.trimIndent()

            logInfo("attaching inline policy '$policyName' to user '$userName'")
            iam.putUserPolicy(
                PutUserPolicyRequest {
                    this.userName = userName
                    this.policyName = policyName
                    this.policyDocument = policyDocument
                },
            )

            val existingKeys = iam.listAccessKeys(ListAccessKeysRequest { this.userName = userName }).accessKeyMetadata

            if (existingKeys.isNotEmpty()) {
                logInfo("IAM user '$userName' already has access keys, deleting and recreating")
                existingKeys.forEach { key ->
                    iam.deleteAccessKey(
                        DeleteAccessKeyRequest {
                            this.userName = userName
                            this.accessKeyId = key.accessKeyId
                        },
                    )
                }
            }

            logInfo("creating access key for IAM user '$userName'")
            val key = iam.createAccessKey(CreateAccessKeyRequest { this.userName = userName }).accessKey!!

            return BucketUserCredentials(
                accessKeyId = key.accessKeyId,
                secretAccessKey = key.secretAccessKey,
            )
        }
    }

    private fun iamClient() = IamClient {
        region = "us-east-1"
        this.credentialsProvider = StaticCredentialsProvider {
            accessKeyId = this@AWS.accessKeyId
            secretAccessKey = this@AWS.secretAccessKey
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
            logError(e.message.orEmpty())
            return false
        }

        return true
    }
}
