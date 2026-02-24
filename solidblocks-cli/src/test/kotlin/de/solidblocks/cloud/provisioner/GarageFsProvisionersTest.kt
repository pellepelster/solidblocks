package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.healthcheck.SSHClientTest
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKey
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyProvisioner
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucket
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermission
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerRuntime
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretProvisioner
import de.solidblocks.cloud.provisioner.pass.PassSecretRuntime
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.hetzner.cloud.resources.ServerStatus
import de.solidblocks.ssh.SSHKeyUtils
import fr.deuxfleurs.garagehq.api.BucketApi
import fr.deuxfleurs.garagehq.api.ClusterApi
import fr.deuxfleurs.garagehq.model.CreateBucketRequest
import io.kotest.assertions.assertSoftly
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.coEvery
import io.mockk.mockk
import java.util.*
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.openapitools.client.infrastructure.ApiClient
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GarageFsProvisionersTest {

  private val logger = LoggerFactory.getLogger(GarageFsProvisionersTest::class.java)

  val garagefs =
      GenericContainer(
              ImageFromDockerfile()
                  .withFileFromClasspath("Dockerfile", "garagefs/Dockerfile")
                  .withFileFromClasspath("supervisord.conf", "garagefs/supervisord.conf")
                  .withFileFromClasspath("garage.toml", "garagefs/garage.toml")
                  .withFileFromClasspath("authorized_keys", "test_ed25519.key.pub"),
          )
          .also {
            it.addExposedPort(22)
            it.addExposedPort(3903)
            it.start()
          }

  val key =
      SSHKeyUtils.tryLoadKey(SSHClientTest::class.java.getResource("/test_ed25519.key").readText())

  @BeforeAll
  fun setup() {
    await().until { garagefs.execInContainer("/garage", "status").exitCode == 0 }

    val stdout = garagefs.execInContainer("/garage", "status").stdout
    val nodeId = stdout.lines()[2].split(" ").first()

    garagefs.execInContainer("/garage", "layout", "assign", "-z", "dc1", "-c", "1G", nodeId).also {
      if (it.exitCode != 0) {
        throw RuntimeException(it.stderr)
      }
    }

    garagefs.execInContainer("/garage", "layout", "show").also {
      if (it.exitCode != 0) {
        throw RuntimeException(it.stderr)
      }
    }

    garagefs.execInContainer("/garage", "layout", "apply", "--version", "1").also {
      if (it.exitCode != 0) {
        throw RuntimeException(it.stderr)
      }
    }

    runBlocking {
      System.getProperties()
          .setProperty(ApiClient.baseUrlKey, "http://localhost:${garagefs.getMappedPort(3903)}")
      ApiClient.accessToken = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      println(ClusterApi().getClusterHealth().status)

      BucketApi().createBucket(CreateBucketRequest("bucket1"))
      BucketApi().listBuckets().forEach {
        println(it.id)
        println(it.localAliases)
        println(it.globalAliases)
      }
    }
  }

  @Test
  fun testFlow() {
    val serverProvisioner = mockk<HetznerServerProvisioner>()
    coEvery { serverProvisioner.lookup(any(), any()) } returns
        HetznerServerRuntime(
            1,
            "server1",
            ServerStatus.running,
            "debian12",
            "cx23",
            emptyMap(),
            emptyList(),
            null,
            "127.0.0.1",
            emptyList(),
            sshPort = garagefs.getMappedPort(22),
        )
    coEvery { serverProvisioner.supportedLookupType } returns HetznerServerLookup::class

    val secretProvisioner = mockk<PassSecretProvisioner>()
    coEvery { secretProvisioner.lookup(any(), any()) } returns
        PassSecretRuntime(
            "admin_token",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        )
    coEvery { secretProvisioner.supportedLookupType } returns PassSecretLookup::class

    val server =
        HetznerServer(
            "server1",
            UserData(emptySet(), { "" }),
        )

    val buckerProvisioner = GarageFsBucketProvisioner()
    val accessKeyProvisioner = GarageFsAccessKeyProvisioner()
    val permissionProvisioner = GarageFsPermissionProvisioner()

    val context =
        TEST_PROVISIONER_CONTEXT.copy(
            registry =
                ProvisionersRegistry(
                    listOf(
                        serverProvisioner,
                        secretProvisioner,
                        permissionProvisioner,
                        accessKeyProvisioner,
                        buckerProvisioner,
                    ),
                    listOf(
                        serverProvisioner,
                        secretProvisioner,
                        permissionProvisioner,
                        accessKeyProvisioner,
                        buckerProvisioner,
                    ),
                ),
        )

    val adminToken = PassSecret("admin_token")
    val bucket = GarageFsBucket(UUID.randomUUID().toString(), server, adminToken)
    val accessKey = GarageFsAccessKey(UUID.randomUUID().toString(), server, adminToken)

    runBlocking {
      // check non-existing bucket
      buckerProvisioner.lookup(bucket.asLookup(), context) shouldBe null
      assertSoftly(buckerProvisioner.diff(bucket, context)) { it.status shouldBe missing }

      // check non-existing access key
      accessKeyProvisioner.lookup(accessKey.asLookup(), context) shouldBe null
      assertSoftly(accessKeyProvisioner.diff(accessKey, context)) { it.status shouldBe missing }

      buckerProvisioner.apply(bucket, context, TEST_LOG_CONTEXT).runtime!!.name shouldBe bucket.name
      buckerProvisioner.apply(bucket, context, TEST_LOG_CONTEXT).runtime!!.name shouldBe bucket.name

      accessKeyProvisioner.apply(accessKey, context, TEST_LOG_CONTEXT).runtime!!.name shouldBe
          accessKey.name
      accessKeyProvisioner.apply(accessKey, context, TEST_LOG_CONTEXT).runtime!!.name shouldBe
          accessKey.name

      // check created bucket
      assertSoftly(buckerProvisioner.lookup(bucket.asLookup(), context)!!) {
        it.name shouldBe bucket.name
      }
      assertSoftly(buckerProvisioner.diff(bucket, context)) { it.status shouldBe up_to_date }

      assertSoftly(buckerProvisioner.diff(bucket.copy(websiteAccess = true), context)) {
        it.status shouldBe has_changes
        it.changes shouldHaveSize 1
        it.changes[0].expectedValue shouldBe true
        it.changes[0].actualValue shouldBe false
      }

      buckerProvisioner
          .apply(
              bucket.copy(websiteAccess = true),
              context,
              TEST_LOG_CONTEXT,
          )
          .runtime!!
          .name shouldBe bucket.name
      assertSoftly(buckerProvisioner.diff(bucket.copy(websiteAccess = true), context)) {
        it.status shouldBe up_to_date
      }

      // check created access key
      assertSoftly(accessKeyProvisioner.lookup(accessKey.asLookup(), context)!!) {
        it.name shouldBe accessKey.name
        it.secretAccessKey shouldHaveLength 64
      }

      assertSoftly(accessKeyProvisioner.diff(accessKey, context)) { it.status shouldBe up_to_date }

      val permission = GarageFsPermission(bucket, accessKey, server, adminToken, true, true, true)

      permissionProvisioner.lookup(permission.asLookup(), context) shouldBe null
      permissionProvisioner.apply(permission, context, TEST_LOG_CONTEXT).runtime shouldNotBe null

      assertSoftly(permissionProvisioner.lookup(permission.asLookup(), context)!!) {
        it.name shouldBe "${bucket.name}.${accessKey.name}"
        it.owner shouldBe true
        it.read shouldBe true
        it.write shouldBe true
      }
      assertSoftly(permissionProvisioner.diff(permission, context)) {
        it.status shouldBe up_to_date
      }

      assertSoftly(permissionProvisioner.diff(permission.copy(owner = false), context)) {
        it.status shouldBe has_changes
        it.changes shouldHaveSize 1
        it.changes[0].name shouldBe "owner"
        it.changes[0].expectedValue shouldBe false
        it.changes[0].actualValue shouldBe true
      }
      permissionProvisioner
          .apply(
              permission.copy(owner = false),
              context,
              TEST_LOG_CONTEXT,
          )
          .runtime shouldNotBe null
      assertSoftly(permissionProvisioner.lookup(permission.asLookup(), context)!!) {
        it.name shouldBe "${bucket.name}.${accessKey.name}"
        it.owner shouldBe false
        it.read shouldBe true
        it.write shouldBe true
      }

      permissionProvisioner.apply(
          permission.copy(owner = false, read = false),
          context,
          TEST_LOG_CONTEXT,
      ) shouldNotBe null
      assertSoftly(permissionProvisioner.lookup(permission.asLookup(), context)!!) {
        it.name shouldBe "${bucket.name}.${accessKey.name}"
        it.owner shouldBe false
        it.read shouldBe false
        it.write shouldBe true
      }

      permissionProvisioner
          .apply(
              permission.copy(owner = false, read = false, write = false),
              context,
              TEST_LOG_CONTEXT,
          )
          .runtime shouldBe null
      permissionProvisioner.lookup(permission.asLookup(), context) shouldBe null

      permissionProvisioner.apply(permission, context, TEST_LOG_CONTEXT).runtime shouldNotBe null
      assertSoftly(permissionProvisioner.lookup(permission.asLookup(), context)!!) {
        it.name shouldBe "${bucket.name}.${accessKey.name}"
        it.owner shouldBe true
        it.read shouldBe true
        it.write shouldBe true
      }
    }
  }
}
