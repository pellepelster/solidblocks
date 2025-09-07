package de.solidblocks.rds.postgresql.test.extensions

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.RemoveBucketArgs
import java.util.*

class HetznerTestBed {

  val bucketName = "test-${UUID.randomUUID()}"

  var minioClient: MinioClient =
      MinioClient.builder()
          .endpoint("fsn1.your-objectstorage.com")
          .credentials(
              System.getenv("HETZNER_S3_ACCESS_KEY"),
              System.getenv("HETZNER_S3_SECRET_KEY"),
          )
          .build()

  fun clean() {
    logger.info { "[test] deleting bucket '$bucketName'" }

    if (bucketExists()) {
      emptyBucket()
      minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).region("fsn1").build())
    }
  }

  private fun emptyBucket() {
    /*
    var objectListing = minioClient.listObjects(bucket)

    while (true) {
        for (objectSummary in objectListing.getObjectSummaries()) {
            s3.deleteObject(bucket, objectSummary.getKey())
        }
        if (objectListing.isTruncated()) {
            objectListing = s3.listNextBatchOfObjects(objectListing)
        } else {
            break
        }
    }*/
  }

  fun initTestbed() {
    if (!bucketExists()) {
      logger.info { "[test] creating bucket '$bucketName'" }
      minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).region("fsn1").build())
    }

    while (!bucketExists()) {
      logger.info { "[test] waiting for '$bucketName'" }
      Thread.sleep(1000)
    }
  }

  private fun bucketExists() =
      minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).region("fsn1").build())
}
