package de.solidblocks.rds.postgresql.test.extensions

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.*

class GcsTestBedExtension : ParameterResolver, AfterEachCallback {

  val testBeds = mutableMapOf<String, GcsTestBed>()

  val key = System.getenv("GCP_SERVICE_ACCOUNT_KEY")
  var credential = ServiceAccountCredentials.fromStream(key.byteInputStream())

  val storageClient =
      StorageOptions.newBuilder()
          .setCredentials(credential)
          .setProjectId("solidblocks-test")
          .build()
          .service

  override fun supportsParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ): Boolean = parameterContext.parameter.type == GcsTestBed::class.java

  @Throws(ParameterResolutionException::class)
  override fun resolveParameter(
      parameterContext: ParameterContext,
      context: ExtensionContext,
  ): Any = createTestBed(context)

  override fun afterEach(context: ExtensionContext) {
    // testBeds[context.uniqueId]?.destroyTestBed()
  }

  private fun createTestBed(context: ExtensionContext): GcsTestBed =
      testBeds.getOrPut(context.uniqueId) { GcsTestBed(storageClient) }.also { it.initTestbed() }

  private fun removeAllTestBuckets() {
    storageClient.list(Storage.BucketListOption.prefix("test-")).streamAll().forEach { bucket ->
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

      logger.info { "[test] deleting bucket '${bucket.name}'" }
      storageClient.get(bucket.name).delete()
    }
  }
}
