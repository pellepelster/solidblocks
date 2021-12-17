package de.solidblocks.provisioner.minio

import de.solidblocks.provisioner.minio.bucket.MinioBucket
import de.solidblocks.provisioner.minio.bucket.MinioBucketProvisioner
import io.minio.MinioClient
import org.assertj.core.api.Assertions.assertThat
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

        private var minioClient: MinioClient? = null

        @Container
        val environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                .apply {
                    withPull(true)
                    withExposedService("minio", 9000)
                    start()
                }

        private fun minioAddress() = "http://localhost:${environment.getServicePort("minio", 9000)}"

        fun minioClientProvider(): () -> MinioClient {
            if (minioClient == null) {
                minioClient = MinioClient.builder()
                    .endpoint(minioAddress())
                    .credentials("admin", "c3344fa0-5eb2-11ec-95ba-77ffa3cedca9")
                    .build()
            }

            return { minioClient!! }
        }
    }

    @Test
    fun testDiffAndApply() {
        val provisioner = MinioBucketProvisioner(minioClientProvider().invoke())
        val bucket = MinioBucket(UUID.randomUUID().toString())

        assertThat(provisioner.diff(bucket).result!!.missing).isTrue

        provisioner.apply(bucket)

        assertThat(provisioner.diff(bucket).result!!.missing).isFalse
    }
}
