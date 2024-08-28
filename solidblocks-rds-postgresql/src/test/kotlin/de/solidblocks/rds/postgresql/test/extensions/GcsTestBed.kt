package de.solidblocks.rds.postgresql.test.extensions

import com.google.cloud.storage.BucketInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageClass
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class GcsTestBed(val storageClient: Storage) {

  val bucket = "test-${UUID.randomUUID()}"

  fun initTestbed() {
    if (storageClient.get(bucket) == null) {
      val bucket =
          storageClient.create(
              BucketInfo.newBuilder(bucket)
                  .setStorageClass(StorageClass.STANDARD)
                  .setLocation("EU")
                  .build(),
          )

      logger.info { "[test] created bucket '${bucket.asBucketInfo().name}'" }
    } else {
      logger.info { "[test] bucket '$bucket' already exists" }
    }
  }

  fun destroyTestBed() {
    storageClient.get(bucket)?.also { bucket ->
      logger.info { "[test] deleting all objects for bucket '${bucket.name}'" }
      runBlocking {
        while (true) {
          val blobs = storageClient.get(bucket.name).list().values.toList()

          if (blobs.isEmpty()) {
            break
          }

          blobs
              .map { blob ->
                async {
                  logger.info { "[test] deleting blob '${blob.name}' for bucket '${bucket.name}'" }
                  blob.delete()
                }
              }
              .awaitAll()
        }
      }

      logger.info { "deleting bucket '${bucket.name}'" }
      storageClient.get(bucket.name).delete()
    }
  }
}
