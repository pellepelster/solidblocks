package de.solidblocks.infra.test.aws

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.BucketTaggingConfiguration
import com.amazonaws.services.s3.model.SetBucketTaggingConfigurationRequest
import com.amazonaws.services.s3.model.TagSet
import de.solidblocks.infra.test.TestContext
import de.solidblocks.infra.test.testLabels
import de.solidblocks.utils.logInfo
import de.solidblocks.utils.logWarning

fun awsTestContext(testId: String? = null) = AwsTestContext(testId)

class AwsTestContext(testId: String? = null) : TestContext(testId) {

  private val s3 = AmazonS3ClientBuilder.defaultClient()

  val defaultLabels = testLabels(this.testId)

  val buckets = mutableListOf<String>()

  init {
    logInfo(
        "created AWS test context, all created resources will be labeled with ${
                defaultLabels.entries.joinToString(", ") {
                    "${it.key}=${it.value}"
                }
            }",
    )

    logWarning(
        "after test completion, alls resources with the labels ${
                defaultLabels.entries.joinToString(", ") {
                    "${it.key}=${it.value}"
                }
            } will be removed",
    )
  }

  private fun hasDefaultTags(bucketName: String): Boolean {
    val taggingConfig = s3.getBucketTaggingConfiguration(bucketName) ?: return false

    for (tagSet in taggingConfig.allTagSets) {
      if (defaultLabels.all { tagSet.allTags[it.key] == it.value }) {
        return true
      }
    }

    return false
  }

  override fun afterAll() {
    buckets.forEach { clean(it) }
    /*
    s3.listBuckets().stream().filter { bucket ->
        hasDefaultTags(bucket.name)
    }.forEach {
        logInfo(it.name)
    }*/
  }

  fun clean(bucket: String) {
    logInfo("deleting S3 bucket '$bucket'")

    if (s3.doesBucketExistV2(bucket)) {
      emptyBucket(bucket)

      try {
        s3.deleteBucket(bucket)
      } catch (e: AmazonS3Exception) {
        if (e.errorCode == "BucketNotEmpty") {
          emptyBucket(bucket)
        }
        s3.deleteBucket(bucket)
      }
    }
  }

  private fun emptyBucket(bucket: String) {
    var objectListing = s3.listObjects(bucket)

    while (true) {
      for (objectSummary in objectListing.objectSummaries) {
        s3.deleteObject(bucket, objectSummary.getKey())
      }

      if (objectListing.isTruncated) {
        objectListing = s3.listNextBatchOfObjects(objectListing)
      } else {
        break
      }
    }
  }

  fun createBucket(name: String? = null): String {
    val bucket = name ?: "test-${this.testId.lowercase()}"

    if (!s3.doesBucketExistV2(bucket)) {
      logInfo("creating S3 bucket '$bucket'")
      s3.createBucket(bucket)
      buckets.add(bucket)
      s3.setBucketTaggingConfiguration(
          SetBucketTaggingConfigurationRequest(
              bucket,
              BucketTaggingConfiguration()
                  .withTagSets(
                      TagSet(defaultLabels),
                  ),
          ),
      )
    }

    while (!s3.doesBucketExistV2(bucket)) {
      logInfo("waiting for S3 bucket '$bucket'")
      Thread.sleep(1000)
    }

    return bucket
  }
}
