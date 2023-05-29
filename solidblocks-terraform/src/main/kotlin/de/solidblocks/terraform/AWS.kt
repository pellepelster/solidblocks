package de.solidblocks.terraform

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.*
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import io.github.oshai.KotlinLogging


class AWS(private val accessKeyId: String, private val secretAccessKey: String, private val region: String) {

    private val logger = KotlinLogging.logger {}

    suspend fun ensureBucket(name: String) {
        s3Client().use {

            if (it.hasBucket(name)) {
                logger.info { "bucket '${name}' already exists" }
            } else {
                logger.info { "creating bucket '${name}'" }
                it.createNewBucket(name)
            }

            val currentPublicAccessBlock = it.getPublicAccessBlock(GetPublicAccessBlockRequest {
                bucket = name
            }).publicAccessBlockConfiguration

            logger.info { "setting public access block config for bucket '${name}'" }

            if (currentPublicAccessBlock == null || !currentPublicAccessBlock.blockPublicAcls || !currentPublicAccessBlock.blockPublicPolicy || !currentPublicAccessBlock.restrictPublicBuckets || !currentPublicAccessBlock.ignorePublicAcls) {
                it.putPublicAccessBlock(PutPublicAccessBlockRequest {
                    bucket = name
                    publicAccessBlockConfiguration = PublicAccessBlockConfiguration {
                        blockPublicAcls = true
                        blockPublicPolicy = true
                        restrictPublicBuckets = true
                        ignorePublicAcls = true
                    }
                })
            }

            val versioning = it.getBucketVersioning(GetBucketVersioningRequest {
                this.bucket = name
            })

            if (versioning.status != BucketVersioningStatus.Enabled) {
                logger.info("enabling versioning for bucket '${name}'")

                it.putBucketVersioning(PutBucketVersioningRequest {
                    bucket = name
                    versioningConfiguration = VersioningConfiguration {
                        status = BucketVersioningStatus.Enabled
                    }
                })
            } else {
                logger.info("bucket '${name}' versioning is already enabled")
            }
        }

        dynamodDbClient().use {
            if (it.hasTable(name)) {
                logger.info { "table '${name}' already exists" }
            } else {
                logger.info { "creating table '${name}'" }
                it.createTable(name)
            }
        }

        logger.info("example configuration for created backend")
        println()
        println(backendConfig(name, region))
    }

    private fun s3Client() = S3Client {
        region = this@AWS.region
        this.credentialsProvider = StaticCredentialsProvider {
            accessKeyId = this@AWS.accessKeyId
            secretAccessKey = this@AWS.secretAccessKey
            region = this@AWS.region
        }
    }

    private fun dynamodDbClient() = DynamoDbClient {
        region = this@AWS.region
        this.credentialsProvider = StaticCredentialsProvider {
            accessKeyId = this@AWS.accessKeyId
            secretAccessKey = this@AWS.secretAccessKey
            region = this@AWS.region
        }
    }


    private suspend fun DynamoDbClient.hasTable(name: String): Boolean {
        try {
            this.describeTable(DescribeTableRequest {
                tableName = name
            })
        } catch (e: ResourceNotFoundException) {
            return false
        }

        return true
    }


    private suspend fun DynamoDbClient.createTable(name: String) {
        this.createTable(CreateTableRequest {
            tableName = name
            billingMode = BillingMode.PayPerRequest

            keySchema = listOf(
                KeySchemaElement {
                    attributeName = "LockID"
                    keyType = KeyType.Hash
                }
            )
            attributeDefinitions = listOf(AttributeDefinition {
                attributeName = "LockID"
                attributeType = ScalarAttributeType.S
            })
        })
    }

    private suspend fun S3Client.createNewBucket(name: String) {
        this.createBucket(CreateBucketRequest {
            bucket = name
            createBucketConfiguration = CreateBucketConfiguration {
                locationConstraint = BucketLocationConstraint.fromValue(region)
            }
        })
    }

    private suspend fun S3Client.hasBucket(name: String): Boolean {
        try {
            this.headBucket(HeadBucketRequest {
                bucket = name
            })
        } catch (e: NotFound) {
            return false
        }

        return true
    }

    private fun backendConfig(name: String, region: String): String {
        return """
        terraform {
          backend "s3" {
            region          = "${region}"
            bucket          = "${name}"
            dynamodb_table  = "${name}"
            key             = "<some_key>"
          }
        }
    """.trimIndent()

    }

}