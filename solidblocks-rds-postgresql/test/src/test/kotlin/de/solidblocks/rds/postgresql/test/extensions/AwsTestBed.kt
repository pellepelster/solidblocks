package de.solidblocks.rds.postgresql.test.extensions

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import mu.KotlinLogging
import java.util.*

class AwsTestBed {

    val bucket = "test-${UUID.randomUUID()}"

    private val logger = KotlinLogging.logger {}

    val s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build()

    fun destroyTestBed() {
        logger.info { "deleting bucket '${bucket}'" }

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
            logger.info { "creating bucket '${bucket}'" }
            s3.createBucket(bucket)
        }

        while (!s3.doesBucketExistV2(bucket)) {
            logger.info { "waiting for '${bucket}'" }
            Thread.sleep(1000)
        }
    }

}