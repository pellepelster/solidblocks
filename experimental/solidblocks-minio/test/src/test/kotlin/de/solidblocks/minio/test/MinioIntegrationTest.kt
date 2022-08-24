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
import org.testcontainers.shaded.org.bouncycastle.util.encoders.Base64Encoder
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

        val minioCertificatePrivateBase64 = Base64.getEncoder().encodeToString(MinioIntegrationTest::class.java.getResource("/minio.key.pem")?.readBytes())
        val minioCertificatePublicBase64 = Base64.getEncoder().encodeToString(MinioIntegrationTest::class.java.getResource("/minio.pem")?.readBytes())

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

        val container = GenericContainer(imageVersion("solidblocks-minio")).apply {
            withLogConsumer(logConsumer)
            withEnv(
                    mapOf(
                            "MINIO_ADMIN_USER" to "admin12345",
                            "MINIO_ADMIN_PASSWORD" to "admin12345",
                            "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivateBase64,
                            "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublicBase64,
                    )
            )
        }

        Assertions.assertThrows(ContainerLaunchException::class.java) {
            container.start()
        }

        logConsumer.waitForLogLine("[solidblocks-minio] storage dir '/storage/data' not mounted")
    }

    fun imageVersion(image: String): String {
        if (System.getenv("VERSION") != null) {
            return "${image}:${System.getenv("VERSION")}"
        }

        return image
    }

    @Test
    fun startsWithStorageMount() {
        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))
        val tempDir = initWorldReadableTempDir("startsWithStorageMount")

        val container = GenericContainer(imageVersion("solidblocks-minio")).apply {
            withLogConsumer(logConsumer)
            withFileSystemBind(tempDir.absolutePath, "/storage/data")
            withEnv(
                    mapOf(
                            "MINIO_ADMIN_USER" to "admin12345",
                            "MINIO_ADMIN_PASSWORD" to "admin12345",
                            "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivateBase64,
                            "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublicBase64,
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

        val container = GenericContainer(imageVersion("solidblocks-minio")).apply {
            withLogConsumer(logConsumer)
            withFileSystemBind(tempDir.absolutePath, "/storage/data")
            withExposedPorts(443)
            withEnv(
                    mapOf(
                            "MINIO_ADMIN_USER" to "admin12345",
                            "MINIO_ADMIN_PASSWORD" to "admin12345",
                            "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivateBase64,
                            "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublicBase64,
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
    fun startsWithDifferentPort() {
        val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))
        val tempDir = initWorldReadableTempDir("testMinioStartWithBucketSpec")

        val container = GenericContainer(imageVersion("solidblocks-minio")).apply {
            withLogConsumer(logConsumer)
            withFileSystemBind(tempDir.absolutePath, "/storage/data")
            withExposedPorts(8443)
            withEnv(
                    mapOf(
                            "MINIO_HTTPS_PORT" to "8443",
                            "MINIO_ADMIN_USER" to "admin12345",
                            "MINIO_ADMIN_PASSWORD" to "admin12345",
                            "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivateBase64,
                            "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublicBase64,
                            "BUCKET_SPECS" to "${bucket}:${accessKey}:${secretKey}"
                    )
            )
        }
        container.start()

        val adminMinioClient =
                MinioClient.builder().endpoint("https://localhost:${container.getMappedPort(8443)}").httpClient(httpClient)
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

        val container = GenericContainer(imageVersion("solidblocks-minio")).apply {
            withLogConsumer(logConsumer)
            withFileSystemBind(tempDir.absolutePath, "/storage/data")
            withExposedPorts(443)
            withEnv(
                    mapOf(
                            "MINIO_ADMIN_USER" to "admin12345",
                            "MINIO_ADMIN_PASSWORD" to "admin12345",
                            "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivateBase64,
                            "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublicBase64,
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

        val container = GenericContainer(imageVersion("solidblocks-minio")).apply {
            withLogConsumer(logConsumer)
            withFileSystemBind(tempDir.absolutePath, "/storage/data")
            withExposedPorts(443)
            withEnv(
                    mapOf(
                            "MINIO_ADMIN_USER" to "admin12345",
                            "MINIO_ADMIN_PASSWORD" to "admin12345",
                            "MINIO_TLS_PRIVATE_KEY" to minioCertificatePrivateBase64,
                            "MINIO_TLS_PUBLIC_KEY" to minioCertificatePublicBase64
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
