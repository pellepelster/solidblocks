package de.solidblocks.provisioner.minio

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import java.io.File
import java.util.*

class MinioMcWrapperTest {

    val minioAddress: String
        get() = "http://localhost:${environment.getServicePort("minio", 9000)}"

    @Container
    val environment: DockerComposeContainer<*> =
        KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
            .apply {
                withPull(true)
                withExposedService("minio", 9000)
                start()
            }

    private val wrapper =
        MinioMcWrapper { MinioCredentials(minioAddress, "admin", "c3344fa0-5eb2-11ec-95ba-77ffa3cedca9") }

    @Test
    fun testGetNonExistingUser() {
        assertThat(wrapper.getUser(UUID.randomUUID().toString())).isNull()
    }

    @Test
    fun testPolicies() {
        assertThat(wrapper.listPolicies()).contains(
            MinioMcWrapper.MinioPolicy(
                status = "success",
                policy = "readwrite",
                isGroup = false
            )
        )

        val newPolicyName = UUID.randomUUID().toString()
        assertThat(
            wrapper.addPolicy(
                newPolicyName,
                MinioMcWrapper.Policy(
                    statement = listOf(
                        MinioMcWrapper.Statement(
                            action = listOf("s3:GetObject"),
                            resource = listOf("arn:aws:s3:::my-bucketname/*")
                        )
                    )
                )
            )
        ).isTrue

        assertThat(wrapper.listPolicies()).contains(
            MinioMcWrapper.MinioPolicy(
                status = "success",
                policy = newPolicyName,
                isGroup = false
            )
        )

        val newUserName = UUID.randomUUID().toString()
        wrapper.addUser(newUserName, UUID.randomUUID().toString())

        assertThat(wrapper.listUsers().first { it.accessKey == newUserName }.policies).isEmpty()

        wrapper.assignPolicy(newPolicyName, newUserName)
        assertThat(wrapper.listUsers().first { it.accessKey == newUserName }.policies).contains(newPolicyName)
    }

    @Test
    fun testUserList() {
        val newAccessKey = UUID.randomUUID().toString()
        assertThat(wrapper.listUsers()).noneMatch { it.accessKey == newAccessKey }

        wrapper.addUser(newAccessKey, UUID.randomUUID().toString())
        assertThat(wrapper.listUsers()).anyMatch { it.accessKey == newAccessKey }

        val user = wrapper.getUser(newAccessKey)
    }
}
