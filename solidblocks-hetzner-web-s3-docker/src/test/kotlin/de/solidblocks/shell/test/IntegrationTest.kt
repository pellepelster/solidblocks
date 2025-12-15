package de.solidblocks.shell.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.minio.BucketExistsArgs
import io.minio.DownloadObjectArgs
import io.minio.MinioClient
import io.minio.UploadObjectArgs
import java.net.URL
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

  lateinit var s3Buckets: List<S3Bucket>
  lateinit var s3Host: String

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

  @BeforeAll
  fun setup(context: SolidblocksTestContext) {
    val baseTerraform = context.terraform(Path.of("./test/terraform/base"))
    baseTerraform.init()
    baseTerraform.apply()
    val baseOutput = baseTerraform.output()

    val terraform = context.terraform(Path.of("./test/terraform/web-s3-docker"))
    terraform.addVariable("test_id", baseOutput.getString("test_id"))

    terraform.init()
    terraform.apply()
    val output = terraform.output()

    s3Host = output.getString("s3_host")
    println("s3Host: $s3Host")
    s3Buckets = output.getList("s3_buckets", S3Bucket::class)
  }

  @Test
  fun testDeploy() {
    val s3Bucket1 = s3Buckets[0]
    val ownerClient =
        MinioClient.builder()
            .endpoint("https://$s3Host")
            .region("garage")
            .credentials(s3Bucket1.owner_key_id, s3Bucket1.owner_secret_key)
            .build()

    val roClient =
        MinioClient.builder()
            .endpoint("https://$s3Host")
            .region("garage")
            .credentials(s3Bucket1.ro_key_id, s3Bucket1.ro_secret_key)
            .build()

    val rwClient =
        MinioClient.builder()
            .endpoint("https://$s3Host")
            .region("garage")
            .credentials(s3Bucket1.rw_key_id, s3Bucket1.rw_secret_key)
            .build()

    ownerClient.bucketExists(BucketExistsArgs.builder().bucket("bucket1").build()) shouldBe true
    assertSoftly(ownerClient.listBuckets().map { it.name() }) {
      it shouldHaveAtLeastSize 3
      it shouldContain "bucket1"
      it shouldContain "www.blcks-test.de"
      it shouldContain "www.web-s3-docker.blcks-test.de"
    }

    val random1 = UUID.randomUUID().toString()
    val uploadFile = kotlin.io.path.createTempFile(prefix = "upload_test", suffix = ".tmp")
    uploadFile.writeText(random1)

    ownerClient.uploadObject(
        UploadObjectArgs.builder()
            .bucket("bucket1")
            .`object`(random1)
            .filename(uploadFile.toAbsolutePath().toString())
            .build(),
    )

    val ownerDownloadFile = createTempDirectory().resolve(random1)
    ownerClient.downloadObject(
        DownloadObjectArgs.builder()
            .bucket("bucket1")
            .`object`(random1)
            .filename(ownerDownloadFile.toAbsolutePath().toString())
            .build(),
    )
    ownerDownloadFile.readText() shouldBe random1

    val rwDownloadFile = createTempDirectory().resolve(random1)
    rwClient.downloadObject(
        DownloadObjectArgs.builder()
            .bucket("bucket1")
            .`object`(random1)
            .filename(rwDownloadFile.toAbsolutePath().toString())
            .build(),
    )
    rwDownloadFile.readText() shouldBe random1

    val roDownloadFile = createTempDirectory().resolve(random1)
    roClient.downloadObject(
        DownloadObjectArgs.builder()
            .bucket("bucket1")
            .`object`(random1)
            .filename(roDownloadFile.toAbsolutePath().toString())
            .build(),
    )
    roDownloadFile.readText() shouldBe random1

    s3Bucket1.web_access_addresses shouldHaveSize 4
    s3Bucket1.web_access_addresses
        .map { "$it/$random1" }
        .forEach {
          println("fetching '$it'")
          URL(it).readText() shouldBe random1
        }
  }
}
