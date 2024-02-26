package de.solidblocks.rds.postgresql.test.extensions

import com.google.cloud.storage.BucketInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageClass
import mu.KotlinLogging
import java.util.UUID


class GcsTestBed(val storageClient: Storage) {

    val bucket = "test-${UUID.randomUUID()}"

    private val logger = KotlinLogging.logger {}

    fun initTestbed() {
        if (storageClient.get(bucket) == null) {
            val bucket = storageClient.create(
                BucketInfo.newBuilder(bucket)
                    .setStorageClass(StorageClass.STANDARD)
                    .setLocation("EU")
                    .build()
            )

            logger.info { "created bucket '${bucket.asBucketInfo().name}'" }
        } else {
            logger.info { "bucket '${bucket}' already exists" }
        }
    }

}