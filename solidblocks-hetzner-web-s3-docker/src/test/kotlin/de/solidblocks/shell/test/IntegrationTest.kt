package de.solidblocks.shell.test

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PushImageResultCallback
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import io.minio.BucketExistsArgs
import io.minio.DownloadObjectArgs
import io.minio.MinioClient
import io.minio.UploadObjectArgs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.time.Duration
import java.util.*
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

    lateinit var docker: DockerClient
    lateinit var s3Buckets: List<S3Bucket>
    lateinit var s3Host: String
    lateinit var dockerHost: String
    lateinit var dockerRwUsers: List<DockerUser>

    @Serializable
    @JsonIgnoreUnknownKeys
    data class S3Bucket(
        val name: String,
        val owner_key_id: String,
        val owner_secret_key: String,
        val ro_key_id: String,
        val ro_secret_key: String,
        val rw_key_id: String,
        val rw_secret_key: String,
        val web_access_addresses: List<String>,
        val web_access_public_enable: Boolean,
    )

    @Serializable
    @JsonIgnoreUnknownKeys
    data class DockerUser(
        val username: String,
        val password: String,
    )

    @BeforeAll
    fun setup(context: SolidblocksTestContext) {
        val baseTerraform = context.terraform(Path.of("./src/test/resources/terraform/base"))
        baseTerraform.init()
        baseTerraform.apply()
        val baseOutput = baseTerraform.output()

        val terraform = context.terraform(Path.of("./src/test/resources/terraform/web-s3-docker"))
        terraform.addVariable("test_id", baseOutput.getString("test_id"))

        terraform.init()
        terraform.apply()
        val output = terraform.output()

        s3Host = output.getString("s3_host")
        println("s3Host: $s3Host")
        s3Buckets = output.getList("s3_buckets", S3Bucket::class)

        dockerHost = output.getString("docker_host")
        println("s3Host: $dockerHost")

        dockerRwUsers = output.getList("docker_rw_users", DockerUser::class)

        docker = createDockerClient()

        docker.pullImageCmd("alpine")
            .withTag("latest")
            .exec(PullImageResultCallback())
            .awaitCompletion()

        docker.tagImageCmd("alpine:latest", "${dockerHost}/alpine", "latest").exec()

        await.atMost(Duration.ofMinutes(3)).pollInterval(Duration.ofSeconds(5)).until {
            try {
                val url = "https://${dockerHost}"
                println("waiting for '${url}'")
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                connection.disconnect()

                if (connection.responseCode == 401) {
                    true
                } else {
                    false
                }

            } catch (e: Exception) {
                false
            }
        }


    }

    fun createDockerClient(): DockerClient {
        val config: DefaultDockerClientConfig.Builder =
            DefaultDockerClientConfig.createDefaultConfigBuilder()

        val httpClient = ZerodepDockerHttpClient.Builder()
        httpClient.dockerHost(URI.create("unix:///var/run/docker.sock"))

        val dockerClient: DockerClient =
            DockerClientBuilder.getInstance(config.build())
                .withDockerHttpClient(httpClient.build())
                .build()
        return dockerClient
    }

    @Test
    fun testDockerRwUserCanPush() {
        val dockerRwUser = dockerRwUsers.first()
        val rwAuth = AuthConfig().withUsername(dockerRwUser.username).withPassword(dockerRwUser.password)

        docker.pushImageCmd("${dockerHost}/alpine")
            .withTag("latest")
            .withAuthConfig(rwAuth)
            .exec(PushImageResultCallback())
            .awaitCompletion()
    }

    @Test
    fun testDockerRoUserCanNotPush() {
        val exception = shouldThrow<Exception> {
            docker.pushImageCmd("${dockerHost}/alpine")
                .withTag("latest")
                .exec(PushImageResultCallback())
                .awaitCompletion()
        }
        exception.message shouldBe ("Could not push image: no basic auth credentials")
    }

    @Test
    fun testS3OwnerCanList() {
        val s3Bucket1 = s3Buckets[0]

        val s3Clients =
            listOf(s3Bucket1.ownerS3Client(s3Host))

        s3Clients.forEach { s3Client ->
            s3Client.bucketExists(BucketExistsArgs.builder().bucket("bucket1").build()) shouldBe true

            assertSoftly(s3Client.listBuckets().map { it.name() }) {
                it shouldHaveAtLeastSize 3
                it shouldContain "bucket1"
                it shouldContain "www.blcks-test.de"
                it shouldContain "www.web-s3-docker.blcks-test.de"
            }
        }
    }

    @Test
    fun testOwnerCanUploadAndDownloadFile() {
        val s3Bucket1 = s3Buckets[0]
        val s3Client = s3Bucket1.ownerS3Client(s3Host)

        val random1 = UUID.randomUUID().toString()
        val uploadFile = createTempFile(prefix = "upload_test", suffix = ".tmp")
        uploadFile.writeText(random1)

        s3Client.uploadObject(
            UploadObjectArgs.builder()
                .bucket("bucket1")
                .`object`(random1)
                .filename(uploadFile.toAbsolutePath().toString())
                .build(),
        )

        val downloadFile = createTempDirectory().resolve(random1)
        s3Client.downloadObject(
            DownloadObjectArgs.builder()
                .bucket("bucket1")
                .`object`(random1)
                .filename(downloadFile.toAbsolutePath().toString())
                .build(),
        )
        downloadFile.readText() shouldBe random1
    }

    @Test
    fun roCannotUploadFile() {
        val s3Bucket1 = s3Buckets[0]
        val s3Client = s3Bucket1.roS3Client(s3Host)

        val roUploadFile = createTempFile(prefix = "upload_test", suffix = ".tmp")
        roUploadFile.writeText(UUID.randomUUID().toString())

        val exception = shouldThrow<Exception> {
            s3Client.uploadObject(
                UploadObjectArgs.builder()
                    .bucket("bucket1")
                    .`object`("ro")
                    .filename(roUploadFile.toAbsolutePath().toString())
                    .build(),
            )
        }
        exception.message shouldBe ("Forbidden: Operation is not allowed for this key.")
    }

    @Test
    fun roCanDownloadFile() {
        val s3Bucket1 = s3Buckets[0]
        val roS3Client = s3Bucket1.roS3Client(s3Host)
        val rwS3Client = s3Bucket1.rwS3Client(s3Host)

        val random1 = UUID.randomUUID().toString()
        val uploadFile = createTempFile(prefix = "upload_test", suffix = ".tmp")
        uploadFile.writeText(random1)

        rwS3Client.uploadObject(
            UploadObjectArgs.builder()
                .bucket("bucket1")
                .`object`(random1)
                .filename(uploadFile.toAbsolutePath().toString())
                .build(),
        )

        val downloadFile = createTempDirectory().resolve(random1)
        roS3Client.downloadObject(
            DownloadObjectArgs.builder()
                .bucket("bucket1")
                .`object`(random1)
                .filename(downloadFile.toAbsolutePath().toString())
                .build(),
        )
        downloadFile.readText() shouldBe random1
    }

    @Test
    fun rwCanUploadAndDownloadFile() {
        val s3Bucket1 = s3Buckets[0]
        val s3Client = s3Bucket1.rwS3Client(s3Host)

        val random1 = UUID.randomUUID().toString()
        val uploadFile = createTempFile(prefix = "upload_test", suffix = ".tmp")
        uploadFile.writeText(random1)

        s3Client.uploadObject(
            UploadObjectArgs.builder()
                .bucket("bucket1")
                .`object`(random1)
                .filename(uploadFile.toAbsolutePath().toString())
                .build(),
        )

        val downloadFile = createTempDirectory().resolve(random1)
        s3Client.downloadObject(
            DownloadObjectArgs.builder()
                .bucket("bucket1")
                .`object`(random1)
                .filename(downloadFile.toAbsolutePath().toString())
                .build(),
        )
        downloadFile.readText() shouldBe random1
    }
}


fun IntegrationTest.S3Bucket.ownerS3Client(s3Host: String) = MinioClient.builder()
    .endpoint("https://$s3Host")
    .region("garage")
    .credentials(this.owner_key_id, this.owner_secret_key)
    .build()

fun IntegrationTest.S3Bucket.rwS3Client(s3Host: String) = MinioClient.builder()
    .endpoint("https://$s3Host")
    .region("garage")
    .credentials(this.rw_key_id, this.rw_secret_key)
    .build()

fun IntegrationTest.S3Bucket.roS3Client(s3Host: String) = MinioClient.builder()
    .endpoint("https://$s3Host")
    .region("garage")
    .credentials(this.ro_key_id, this.ro_secret_key)
    .build()