package de.solidblocks.provisioner.minio

import de.solidblocks.provisioner.minio.bucket.MinioBucket
import de.solidblocks.provisioner.minio.bucket.MinioBucketProvisioner
import de.solidblocks.provisioner.minio.policy.MinioPolicy
import de.solidblocks.provisioner.minio.policy.MinioPolicyProvisioner
import de.solidblocks.provisioner.minio.policyassignment.MinioPolicyAssignment
import de.solidblocks.provisioner.minio.policyassignment.MinioPolicyAssignmentProvisioner
import de.solidblocks.provisioner.minio.user.MinioUser
import de.solidblocks.provisioner.minio.user.MinioUserProvisioner
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
            KDockerComposeContainer(File("src/test/resources/minio/docker-compose.yml"))
                .apply {
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
    fun testBucketDiffAndApply() {
        val provisioner = MinioBucketProvisioner(minioCredentialsProvider())
        val bucket = MinioBucket(UUID.randomUUID().toString())

        assertThat(provisioner.diff(bucket).result!!.missing).isTrue
        provisioner.apply(bucket)
        assertThat(provisioner.diff(bucket).result!!.missing).isFalse
    }

    @Test
    fun testUserDiffAndApply() {
        val provisioner = MinioUserProvisioner(minioCredentialsProvider())
        val user = MinioUser(UUID.randomUUID().toString(), UUID.randomUUID().toString())

        assertThat(provisioner.diff(user).result!!.missing).isTrue

        provisioner.apply(user)

        assertThat(provisioner.diff(user).result!!.missing).isFalse

        assertThat(provisioner.lookup(user).result!!.secretKey).isNotNull
    }

    @Test
    fun testPolicyAssignmentDiffAndApply() {
        val userProvisioner = MinioUserProvisioner(minioCredentialsProvider())
        val policyProvisioner = MinioPolicyProvisioner(minioCredentialsProvider())

        val user = MinioUser(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val policy = MinioPolicy(
            UUID.randomUUID().toString(),
            MinioMcWrapper.Policy(
                statement = listOf(
                    MinioMcWrapper.Statement(
                        action = listOf("s3:GetObject"),
                        resource = listOf("arn:aws:s3:::my-bucketname/*")
                    )
                )
            )
        )

        val provisioner = MinioPolicyAssignmentProvisioner(minioCredentialsProvider())

        val policyAssignment = MinioPolicyAssignment(user, policy)

        assertThat(provisioner.diff(policyAssignment).result!!.missing).isTrue
        assertThat(userProvisioner.apply(user).failed).isFalse
        assertThat(userProvisioner.lookup(user).result).isNotNull

        assertThat(provisioner.diff(policyAssignment).result!!.missing).isTrue
        assertThat(policyProvisioner.apply(policy).failed).isFalse

        assertThat(provisioner.diff(policyAssignment).result!!.missing).isTrue

        assertThat(provisioner.apply(policyAssignment).failed).isFalse
        assertThat(provisioner.diff(policyAssignment).result!!.missing).isFalse
    }

    @Test
    fun testPolicyDiffAndApply() {
        val provisioner = MinioPolicyProvisioner(minioCredentialsProvider())

        val policy = MinioPolicy(
            UUID.randomUUID().toString(),
            MinioMcWrapper.Policy(
                statement = listOf(
                    MinioMcWrapper.Statement(
                        action = listOf("s3:GetObject"),
                        resource = listOf("arn:aws:s3:::my-bucketname/*")
                    )
                )
            )
        )

        assertThat(provisioner.diff(policy).result!!.missing).isTrue

        provisioner.apply(policy)

        assertThat(provisioner.diff(policy).result!!.missing).isFalse

        assertThat(provisioner.lookup(policy).result).isNotNull
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
