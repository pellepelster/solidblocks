package de.solidblocks.cloud

import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKey
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyProvisioner
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucket
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.garagefs.layout.GarageFsLayout
import de.solidblocks.cloud.provisioner.garagefs.layout.GarageFsLayoutProvisioner
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
import de.solidblocks.garagefs.GarageFsApi
import de.solidblocks.hetzner.cloud.resources.ServerStatus
import de.solidblocks.ssh.SSHKeyUtils
import io.kotest.assertions.assertSoftly
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.coEvery
import io.mockk.mockk
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.Base58
import java.util.*
import java.util.Locale.getDefault

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GarageFsProvisionersTest {

    val garageFsContainer =
        GenericContainer(
            ImageFromDockerfile("localhost/testcontainers/" + Base58.randomString(16).lowercase(getDefault()), false)
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
        SSHKeyUtils.tryLoadKey(GarageFsProvisionersTest::class.java.getResource("/test_ed25519.key").readText())

    @BeforeAll
    fun setup() {
        await().until { garageFsContainer.execInContainer("/garage", "status").exitCode == 0 }

        runBlocking {
            val api = GarageFsApi("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "http://localhost:${garageFsContainer.getMappedPort(3903)}")
            val status = api.clusterApi.getClusterStatus()
            println(status)
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
                    "nbg1",
                    emptyMap(),
                    emptyList(),
                    null,
                    "127.0.0.1",
                    emptyList(),
                    sshPort = garageFsContainer.getMappedPort(22),
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
                "nbg1",
                "cx23",
                UserData(emptySet(), { "" }),
            )

        val bucketProvisioner = GarageFsBucketProvisioner()
        val accessKeyProvisioner = GarageFsAccessKeyProvisioner()
        val permissionProvisioner = GarageFsPermissionProvisioner()
        val layoutProvisioner = GarageFsLayoutProvisioner()

        val context =
            TEST_PROVISIONER_CONTEXT.copy(
                registry =
                    ProvisionersRegistry(
                        listOf(
                            serverProvisioner,
                            secretProvisioner,
                            permissionProvisioner,
                            accessKeyProvisioner,
                            bucketProvisioner,
                        ),
                        listOf(
                            serverProvisioner,
                            secretProvisioner,
                            permissionProvisioner,
                            accessKeyProvisioner,
                            bucketProvisioner,
                        ),
                    ),
            )

        val adminToken = PassSecret("admin_token")
        val bucketName = UUID.randomUUID().toString()
        val bucket = GarageFsBucket(bucketName, server, adminToken)
        val accessKey = GarageFsAccessKey(UUID.randomUUID().toString(), server, adminToken)

        runBlocking {
            val layout = GarageFsLayout(UUID.randomUUID().toString(), 1 * 1000 * 1000, server, adminToken)
            layoutProvisioner.lookup(layout.asLookup(), context) shouldBe null
            layoutProvisioner.apply(layout, context, TEST_LOG_CONTEXT) shouldNotBe null
            layoutProvisioner.apply(layout, context, TEST_LOG_CONTEXT) shouldNotBe null

            // check non-existing bucket
            bucketProvisioner.lookup(bucket.asLookup(), context) shouldBe null
            assertSoftly(bucketProvisioner.diff(bucket, context)) { it.status shouldBe ResourceDiffStatus.missing }

            // check non-existing access key
            accessKeyProvisioner.lookup(accessKey.asLookup(), context) shouldBe null
            assertSoftly(accessKeyProvisioner.diff(accessKey, context)) { it.status shouldBe ResourceDiffStatus.missing }

            bucketProvisioner.apply(bucket, context, TEST_LOG_CONTEXT).runtime!!.name shouldBe bucket.name
            bucketProvisioner.apply(bucket, context, TEST_LOG_CONTEXT).runtime!!.name shouldBe bucket.name

            accessKeyProvisioner.apply(accessKey, context, TEST_LOG_CONTEXT).runtime!!.name shouldBe
                    accessKey.name
            accessKeyProvisioner.apply(accessKey, context, TEST_LOG_CONTEXT).runtime!!.name shouldBe
                    accessKey.name

            // check created bucket
            assertSoftly(bucketProvisioner.lookup(bucket.asLookup(), context)!!) {
                it.name shouldBe bucket.name
            }
            assertSoftly(bucketProvisioner.diff(bucket, context)) { it.status shouldBe ResourceDiffStatus.up_to_date }

            val bucketWithWebsiteAccess =
                GarageFsBucket(bucketName, server, adminToken, websiteAccess = true)
            assertSoftly(bucketProvisioner.diff(bucketWithWebsiteAccess, context)) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].expectedValue shouldBe true
                it.changes[0].actualValue shouldBe false
            }

            bucketProvisioner
                .apply(
                    bucketWithWebsiteAccess,
                    context,
                    TEST_LOG_CONTEXT,
                )
                .runtime!!
                .name shouldBe bucket.name
            assertSoftly(bucketProvisioner.diff(bucketWithWebsiteAccess, context)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            val bucketWithWebsiteAccessDomains =
                GarageFsBucket(
                    bucketName,
                    server,
                    adminToken,
                    websiteAccess = true,
                    websiteAccessDomains = listOf("yolo.de"),
                )

            assertSoftly(
                bucketProvisioner.diff(
                    bucketWithWebsiteAccessDomains,
                    context,
                ),
            ) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].expectedValue shouldBe listOf("yolo.de")
                it.changes[0].actualValue shouldBe emptyList<String>()
            }

            bucketProvisioner
                .apply(
                    bucketWithWebsiteAccessDomains,
                    context,
                    TEST_LOG_CONTEXT,
                )
                .runtime!!
                .name shouldBe bucket.name

            assertSoftly(
                bucketProvisioner.diff(
                    bucketWithWebsiteAccessDomains,
                    context,
                ),
            ) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes shouldHaveSize 0
            }

            // check created access key
            assertSoftly(accessKeyProvisioner.lookup(accessKey.asLookup(), context)!!) {
                it.name shouldBe accessKey.name
                it.secretAccessKey shouldHaveLength 64
            }

            assertSoftly(
                accessKeyProvisioner.diff(
                    accessKey,
                    context
                )
            ) { it.status shouldBe ResourceDiffStatus.up_to_date }

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
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            assertSoftly(permissionProvisioner.diff(permission.copy(owner = false), context)) {
                it.status shouldBe ResourceDiffStatus.has_changes
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