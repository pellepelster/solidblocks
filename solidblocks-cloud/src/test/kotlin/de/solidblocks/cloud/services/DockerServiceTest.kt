package de.solidblocks.cloud.services

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.configuration.ConfigurationParser
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.services.docker.DockerServiceManager
import de.solidblocks.cloud.services.docker.DockerServiceRegistration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfiguration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfigurationRuntime
import de.solidblocks.cloud.services.docker.model.DockerServiceEndpointConfiguration
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class DockerServiceTest {

    @Test
    fun testParse() {
        val rawYml =
            """
        name: foo-bar
        root_domain: foo.bar
        services:
            - name: service1
              type: docker
              image: image1
            - name: service2
              type: docker
              image: image2
              links:
                - service1
        """
                .trimIndent()

        val cloud =
            ConfigurationParser(
                CloudConfigurationFactory(emptyList(), listOf(DockerServiceRegistration())),
            )
                .parse(rawYml)
                .shouldBeTypeOf<Success<CloudConfiguration>>()
        cloud.data.services shouldHaveSize 2

        val service1 = cloud.data.services[0].shouldBeTypeOf<DockerServiceConfiguration>()
        service1.name shouldBe "service1"
        service1.links shouldHaveSize 0

        val service2 = cloud.data.services[1].shouldBeTypeOf<DockerServiceConfiguration>()
        service2.name shouldBe "service2"
        service2.links shouldHaveSize 1
        service2.links[0] shouldBe "service1"
    }

    @Test
    fun testValidate() {
        val cloud =
            CloudConfiguration(
                "cloud1",
                "cloud1.test-blcks.de",
                emptyList(),
                listOf(
                    DockerServiceConfiguration(
                        "docker2",
                        "image2",
                        InstanceConfig(16, HetznerLocation.fsn1, HetznerServerType.cx23),
                        BackupConfig(16, 7),
                        listOf(DockerServiceEndpointConfiguration(8080)),
                        listOf("docker1"),
                    ),
                ),
            )
        val configuration =
            DockerServiceConfiguration(
                "docker1",
                "image1",
                InstanceConfig(16, HetznerLocation.fsn1, HetznerServerType.cx23),
                BackupConfig(16, 7),
                listOf(DockerServiceEndpointConfiguration(8080)),
                listOf("docker2"),
            )

        val result =
            DockerServiceManager()
                .validateConfiguration(
                    2,
                    cloud,
                    configuration,
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                )
                .shouldBeInstanceOf<Success<DockerServiceConfigurationRuntime>>()
                .data
        result.name shouldBe "docker1"
        result.index shouldBe 2
        result.links shouldHaveSize 1
        result.links[0] shouldBe "docker2"
    }

    @Test
    fun testInvalidLink() {
        val cloud = CloudConfiguration("cloud1", "cloud1.test-blcks.de", emptyList(), emptyList())
        val configuration =
            DockerServiceConfiguration(
                "docker1",
                "image11",
                InstanceConfig(16, HetznerLocation.fsn1, HetznerServerType.cx23),
                BackupConfig(16, 7),
                listOf(DockerServiceEndpointConfiguration(8080)),
                listOf("docker3"),
            )

        val result =
            DockerServiceManager()
                .validateConfiguration(
                    2,
                    cloud,
                    configuration,
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                )
                .shouldBeInstanceOf<Error<DockerServiceConfigurationRuntime>>()
        result.error shouldBe "linked service 'docker3' not found for service 'docker1'"
    }

    @Test
    fun testSelfLink() {
        val configuration =
            DockerServiceConfiguration(
                "docker1",
                "image1",
                InstanceConfig(16, HetznerLocation.fsn1, HetznerServerType.cx23),
                BackupConfig(16, 7),
                listOf(DockerServiceEndpointConfiguration(8080)),
                listOf("docker1"),
            )
        val cloud =
            CloudConfiguration("cloud1", "cloud1.test-blcks.de", emptyList(), listOf(configuration))

        val result =
            DockerServiceManager()
                .validateConfiguration(
                    2,
                    cloud,
                    configuration,
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                )
                .shouldBeInstanceOf<Error<DockerServiceConfigurationRuntime>>()
        result.error shouldBe "service can not be linked with itself 'docker1' -> 'docker1'"
    }

    @Test
    fun testDuplicatedPortConfig() {
        val cloud = CloudConfiguration("cloud1", "cloud1.test-blcks.de", emptyList(), emptyList())
        val configuration =
            DockerServiceConfiguration(
                "docker1",
                "image1",
                InstanceConfig(16, HetznerLocation.fsn1, HetznerServerType.cx23),
                BackupConfig(16, 7),
                listOf(
                    DockerServiceEndpointConfiguration(8080),
                    DockerServiceEndpointConfiguration(8080),
                ),
                emptyList(),
            )

        val result =
            DockerServiceManager()
                .validateConfiguration(
                    2,
                    cloud,
                    configuration,
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                )
                .shouldBeInstanceOf<Error<DockerServiceConfigurationRuntime>>()
        result.error shouldBe "more than one endpoint is currently not supported for service 'docker1'"
    }

    @Test
    fun testNoEndpointConfig() {
        val cloud = CloudConfiguration("cloud1", "cloud1.test-blcks.de", emptyList(), emptyList())
        val configuration =
            DockerServiceConfiguration(
                "docker1",
                "image1",
                InstanceConfig(16, HetznerLocation.fsn1, HetznerServerType.cx23),
                BackupConfig(16, 7),
                emptyList(),
                emptyList(),
            )

        val result =
            DockerServiceManager()
                .validateConfiguration(
                    2,
                    cloud,
                    configuration,
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                )
                .shouldBeInstanceOf<Error<DockerServiceConfigurationRuntime>>()
        result.error shouldBe "no endpoint configured for service 'docker1'"
    }
}
