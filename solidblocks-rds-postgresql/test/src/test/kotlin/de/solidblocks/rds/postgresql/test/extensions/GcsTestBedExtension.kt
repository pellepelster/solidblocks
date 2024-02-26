package de.solidblocks.rds.postgresql.test.extensions

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.extension.*


class GcsTestBedExtension : ParameterResolver, AfterEachCallback, BeforeEachCallback, AfterAllCallback,
        BeforeAllCallback {

    private val logger = KotlinLogging.logger {}

    val testBeds = mutableMapOf<String, GcsTestBed>()

    val storageClient = StorageOptions.newBuilder().setProjectId("solidblocks-test").build().service

    override fun supportsParameter(
            parameterContext: ParameterContext,
            extensionContext: ExtensionContext
    ): Boolean {
        return parameterContext.parameter.type == GcsTestBed::class.java
    }

    @Throws(ParameterResolutionException::class)
    override fun resolveParameter(
            parameterContext: ParameterContext,
            context: ExtensionContext
    ): Any {
        return createTestBed(context)
    }

    override fun afterEach(context: ExtensionContext) {
        //testBeds[context.uniqueId]?.destroyTestBed()
    }

    override fun beforeEach(context: ExtensionContext) {
        createTestBed(context)
    }

    private fun createTestBed(context: ExtensionContext): GcsTestBed {
        return testBeds.getOrPut(context.uniqueId) {
            GcsTestBed(storageClient)
        }.also { it.initTestbed() }
    }

    override fun afterAll(context: ExtensionContext?) {
        removeAllTestBuckets()
    }

    override fun beforeAll(p0: ExtensionContext?) {
        removeAllTestBuckets()
    }

    private fun removeAllTestBuckets() {
        storageClient.list(Storage.BucketListOption.prefix("test-")).streamAll().forEach { bucket ->
            logger.info { "deleting all objects for bucket '${bucket.name}'" }
            runBlocking {

                while (true) {
                    val blobs = storageClient.get(bucket.name).list().values.toList()

                    if (blobs.isEmpty()) {
                        break
                    }

                    blobs.map { blob ->
                        async {
                            logger.info { "deleting blob '${blob.name}' for bucket '${bucket.name}'" }
                            blob.delete()
                        }
                    }.awaitAll()
                }
            }

            logger.info { "deleting bucket '${bucket.name}'" }
            storageClient.get(bucket.name).delete()
        }
    }
}