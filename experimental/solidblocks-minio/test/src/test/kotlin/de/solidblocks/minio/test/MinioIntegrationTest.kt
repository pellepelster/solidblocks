package de.solidblocks.minio.test

import io.minio.BucketExistsArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import mu.KotlinLogging
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.util.PemUtils
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MinioIntegrationTest {

    private val logger = KotlinLogging.logger {}

    companion object {
        const val bucket = "bucket1"
        const val accessKey = "user1"
        const val secretKey = "ccbaa67e-cf26-432f-a11f-0c9e72abccf8"

        val minioCertificatePrivate = MinioIntegrationTest::class.java.getResource("/minio.key.pem")?.readText()
        val minioCertificatePublic = MinioIntegrationTest::class.java.getResource("/minio.pem")?.readText()

        private val caPublic = MinioIntegrationTest::class.java.getResource("/ca.pem")?.openStream()

        private val caCert = PemUtils.loadCertificate(caPublic)

        private val sslFactory = SSLFactory.builder().withTrustMaterial(caCert).build()

        val httpClient =
            OkHttpClient.Builder().sslSocketFactory(sslFactory.sslSocketFactory, sslFactory.trustManager.get()).build()
    }

    @AfterAll
    fun cleanup() {
        val client = DockerClientFactory.instance().client()
        client.listContainersCmd().exec().forEach {
            client.killContainerCmd(it.id).exec()
        }
    }

    @Test
    fun doesNotStartIfNoStorageIsMounted() {
        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

        val container = GenericContainer("solidblocks-minio:snapshot").apply {
            withLogConsumer(logConsumer)
            withEnv(
                mapOf(
                    "MINIO_ADMIN_USER" to "admin12345",
                    "MINIO_ADMIN_PASSWORD" to "admin12345",
                    "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivate,
                    "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublic,
                )
            )
        }

        Assertions.assertThrows(ContainerLaunchException::class.java) {
            container.start()
        }

        logConsumer.waitForLogLine("[solidblocks-minio] storage dir '/storage/local' not mounted")
    }

    @Test
    fun startsWithStorageMount() {
        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))
        val tempDir = initWorldReadableTempDir("startsWithStorageMount")

        val container = GenericContainer("solidblocks-minio:snapshot").apply {
            withLogConsumer(logConsumer)
            withFileSystemBind(tempDir.absolutePath, "/storage/local")
            withEnv(
                mapOf(
                    "MINIO_ADMIN_USER" to "admin12345",
                    "MINIO_ADMIN_PASSWORD" to "admin12345",
                    "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivate,
                    "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublic,
                )
            )
        }
        container.start()
        logConsumer.waitForLogLine("[solidblocks-minio] provisioning completed")
    }

    @Test
    fun createsBucketFromBucketSpec() {
        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))
        val tempDir = initWorldReadableTempDir("testMinioStartWithBucketSpec")

        val container = GenericContainer("solidblocks-minio:snapshot").apply {
            withLogConsumer(logConsumer)
            withFileSystemBind(tempDir.absolutePath, "/storage/local")
            withExposedPorts(443)
            withEnv(
                mapOf(
                    "MINIO_ADMIN_USER" to "admin12345",
                    "MINIO_ADMIN_PASSWORD" to "admin12345",
                    "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivate,
                    "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublic,
                    "BUCKET_SPECS" to "${bucket}:${accessKey}:${secretKey}"
                )
            )
        }
        container.start()

        val adminMinioClient =
            MinioClient.builder().endpoint("https://localhost:${container.getMappedPort(443)}").httpClient(httpClient)
                .credentials("admin12345", "admin12345").build()

        logConsumer.waitForLogLine("[solidblocks-minio] provisioning completed")
        assertThat(adminMinioClient.bucketExists(BucketExistsArgs.builder().bucket("${bucket}-new").build())).isFalse

        container.execInContainer("/minio/bin/provision.sh", "${bucket}-new:${accessKey}:${secretKey}")
        assertThat(adminMinioClient.bucketExists(BucketExistsArgs.builder().bucket("${bucket}-new").build())).isTrue
    }

    @Test
    fun startsWithInvalidBucketSpec() {
        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))
        val tempDir = initWorldReadableTempDir("testMinioStartWithBucketSpec")

        val container = GenericContainer("solidblocks-minio:snapshot").apply {
            withLogConsumer(logConsumer)
            withFileSystemBind(tempDir.absolutePath, "/storage/local")
            withExposedPorts(443)
            withEnv(
                mapOf(
                    "MINIO_ADMIN_USER" to "admin12345",
                    "MINIO_ADMIN_PASSWORD" to "admin12345",
                    "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivate,
                    "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublic,
                    "BUCKET_SPECS" to "bucket1:aa#bucket2::#bucket3"
                )
            )
        }
        container.start()

        val minioClient =
            MinioClient.builder().endpoint("https://localhost:${container.getMappedPort(443)}").httpClient(httpClient)
                .credentials("admin12345", "admin12345").build()

        logConsumer.waitForLogLine("[solidblocks-minio] provisioning completed")
        assertThat(minioClient.bucketExists(BucketExistsArgs.builder().bucket("bucket1").build())).isFalse
        assertThat(minioClient.bucketExists(BucketExistsArgs.builder().bucket("bucket2").build())).isFalse
        assertThat(minioClient.bucketExists(BucketExistsArgs.builder().bucket("bucket3").build())).isFalse
    }

    @Test
    fun startsWithoutBucketSpec() {
        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))
        val tempDir = initWorldReadableTempDir("testMinioStartWithBucketSpec")

        val container = GenericContainer("solidblocks-minio:snapshot").apply {
            withLogConsumer(logConsumer)
            withFileSystemBind(tempDir.absolutePath, "/storage/local")
            withExposedPorts(443)
            withEnv(
                mapOf(
                    "MINIO_ADMIN_USER" to "admin12345",
                    "MINIO_ADMIN_PASSWORD" to "admin12345",
                    "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivate,
                    "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublic
                )
            )
        }
        container.start()


        val minioClient =
            MinioClient.builder().endpoint("https://localhost:${container.getMappedPort(443)}").httpClient(httpClient)
                .credentials("admin12345", "admin12345").build()

        logConsumer.waitForLogLine("[solidblocks-minio] provisioning completed")
        assertThat(minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())).isFalse
        assertThat(minioClient.bucketExists(BucketExistsArgs.builder().bucket("${bucket}-new").build())).isFalse

        container.execInContainer("/minio/bin/provision.sh", "${bucket}-new:${accessKey}:${secretKey}")

        assertThat(minioClient.bucketExists(BucketExistsArgs.builder().bucket("${bucket}-new").build())).isTrue
    }

    fun initWorldReadableTempDir(basename: String): File {
        val tempDir = "/tmp/$basename-temp-${UUID.randomUUID()}"

        File(tempDir).mkdirs()
        Files.setPosixFilePermissions(File(tempDir).toPath(), PosixFilePermissions.fromString("rwxrwxrwx"))

        return File(tempDir)
    }

}
