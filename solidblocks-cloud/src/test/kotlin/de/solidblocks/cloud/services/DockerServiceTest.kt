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
            - name: service2
              type: docker
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
                DockerServiceConfiguration("docker2", "image2", 16, emptyList(), listOf("docker1")),
            ),
        )
    val configuration =
        DockerServiceConfiguration("docker1", "image1", 16, emptyList(), listOf("docker2"))

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
        DockerServiceConfiguration("docker1", "image11", 16, emptyList(), listOf("docker3"))

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
        DockerServiceConfiguration("docker1", "image1", 16, emptyList(), listOf("docker1"))
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
            16,
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
    result.error shouldBe "duplicated port config for port '8080' for service 'docker1'"
  }
}
