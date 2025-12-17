package de.solidblocks.shell.test

import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.exception.InternalServerErrorException
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.command.PushImageResultCallback
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.minio.BucketExistsArgs
import io.minio.DownloadObjectArgs
import io.minio.UploadObjectArgs
import java.util.*
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationDefaultTest : BaseIntegrationTest() {

  @BeforeAll
  fun setup(context: SolidblocksTestContext) {
    init(context, false)
  }

  @Test
  fun testDockerRwUserCanPush() {
    val dockerRwUser = dockerRwUsers.first()
    val rwAuth =
        AuthConfig().withUsername(dockerRwUser.username).withPassword(dockerRwUser.password)
    println("pushing with user '${rwAuth.username}' and password '${rwAuth.password}'")

    val randomTag = UUID.randomUUID().toString()

    docker.tagImageCmd("alpine:latest", "$dockerHostPrivate/alpine", randomTag).exec()

    docker
        .pushImageCmd("$dockerHostPrivate/alpine")
        .withTag(randomTag)
        .withAuthConfig(rwAuth)
        .exec(PushImageResultCallback())
        .awaitCompletion()
  }

  @Test
  fun testDockerRoUserCanNotPush() {
    val exception =
        shouldThrow<Exception> {
          docker
              .pushImageCmd("$dockerHostPrivate/alpine")
              .withTag("latest")
              .exec(PushImageResultCallback())
              .awaitCompletion()
        }
    exception.message shouldBe ("Could not push image: no basic auth credentials")
  }

  @Test
  fun testDockerAnonymousUserCanNotPullFromPublic() {
    val exception =
        shouldThrow<InternalServerErrorException> {
          docker
              .pullImageCmd("$dockerHostPublic/alpine")
              .withTag("latest")
              .exec(PullImageResultCallback())
              .awaitCompletion()
        }
    exception.message shouldContain ("remote error: tls: internal error")
  }

  @Test
  fun testDockerAnonymousUserCanNotPullFromPrivate() {
    val exception =
        shouldThrow<InternalServerErrorException> {
          docker
              .pullImageCmd("$dockerHostPrivate/alpine")
              .withTag("latest")
              .exec(PullImageResultCallback())
              .awaitCompletion()
        }
    exception.message shouldContain ("no basic auth credentials")
  }

  @Test
  fun testDockerAnonymousUserCanNotPushToPrivate() {
    val exception =
        shouldThrow<Exception> {
          docker
              .pushImageCmd("$dockerHostPrivate/alpine")
              .withTag("latest")
              .exec(PushImageResultCallback())
              .awaitCompletion()
        }
    exception.message shouldContain ("Could not push image: no basic auth credentials")
  }

  @Test
  fun testDockerAnonymousUserCanNotPushToPublic() {
    val exception =
        shouldThrow<Exception> {
          docker
              .pushImageCmd("$dockerHostPrivate/alpine")
              .withTag("latest")
              .exec(PushImageResultCallback())
              .awaitCompletion()
        }
    exception.message shouldContain ("Could not push image: no basic auth credentials")
  }

  @Test
  fun testS3OwnerCanList() {
    val s3Bucket1 = s3Buckets[0]

    val s3Clients = listOf(s3Bucket1.ownerS3Client(s3Host))

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
  fun testS3OwnerCanUploadAndDownloadFile() {
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
  fun testS3RoCannotUploadFile() {
    val s3Bucket1 = s3Buckets[0]
    val s3Client = s3Bucket1.roS3Client(s3Host)

    val roUploadFile = createTempFile(prefix = "upload_test", suffix = ".tmp")
    roUploadFile.writeText(UUID.randomUUID().toString())

    val exception =
        shouldThrow<Exception> {
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
  fun testS3RoCanDownloadFile() {
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
  fun testS3RwCanUploadAndDownloadFile() {
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
