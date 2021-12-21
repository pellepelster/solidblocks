package de.solidblocks.provisioner.minio

import de.solidblocks.provisioner.minio.bucket.MinioBucket
import de.solidblocks.provisioner.minio.bucket.MinioBucketProvisioner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.util.*

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

@Testcontainers
class MinioProvisionerTest {

    companion object {
        @Container
        val environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                .apply {
                    withPull(true)
                    withExposedService("minio", 9000)
                    start()
                }

        private val minioAddress: String
            get() = "http://localhost:${environment.getServicePort("minio", 9000)}"

        fun minioCredentialsProvider(): () -> MinioCredentials {
            return { MinioCredentials(minioAddress, "admin", "c3344fa0-5eb2-11ec-95ba-77ffa3cedca9") }
        }
    }

    @Test
    fun testDiffAndApply() {
        val provisioner = MinioBucketProvisioner(minioCredentialsProvider())
        val bucket = MinioBucket(UUID.randomUUID().toString())

        assertThat(provisioner.diff(bucket).result!!.missing).isTrue

        provisioner.apply(bucket)

        assertThat(provisioner.diff(bucket).result!!.missing).isFalse
    }

    @Test
    @Disabled
    fun testInvalidCredentials() {
        val provisioner = MinioBucketProvisioner { MinioCredentials(minioAddress, "admin", "xxx") }

        val bucket = MinioBucket(UUID.randomUUID().toString())

        assertThat(provisioner.diff(bucket).result).isNull()
        assertThat(provisioner.lookup(bucket).result).isNull()
        assertThat(provisioner.apply(bucket).result).isNull()
    }
}
