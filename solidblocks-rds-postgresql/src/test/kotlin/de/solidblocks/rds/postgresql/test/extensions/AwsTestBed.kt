package de.solidblocks.rds.postgresql.test.extensions

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import java.util.UUID

class AwsTestBed {

    val bucket = "test-${UUID.randomUUID()}"

    private val s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build()

    fun clean() {
        logger.info { "[test] deleting bucket '${bucket}'" }

        if (s3.doesBucketExistV2(bucket)) {
            emptyBucket()

            try {
                s3.deleteBucket(bucket)
            } catch (e: AmazonS3Exception) {
                if (e.errorCode == "BucketNotEmpty") {
                    emptyBucket()
                }
                s3.deleteBucket(bucket)
            }
        }
    }

    private fun emptyBucket() {
        var objectListing = s3.listObjects(bucket)

        while (true) {
            for (objectSummary in objectListing.getObjectSummaries()) {
                s3.deleteObject(bucket, objectSummary.getKey())
            }
            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing)
            } else {
                break
            }
        }
    }

    fun initTestbed() {
        if (!s3.doesBucketExistV2(bucket)) {
            logger.info { "[test] creating bucket '${bucket}'" }
            s3.createBucket(bucket)
        }

        while (!s3.doesBucketExistV2(bucket)) {
            logger.info { "[test] waiting for '${bucket}'" }
            Thread.sleep(1000)
        }
    }
}