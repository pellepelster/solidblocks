package de.solidblocks.cli.docker

import de.solidblocks.cloud.configuration.ConfigurationParser
import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.services.docker.DockerServiceConfigurationManager
import de.solidblocks.cloud.services.docker.DockerServiceRegistration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfiguration
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class DockerServiceServiceTest {

    @Test
    fun testParse() {
        val rawYml = """
        name: foo-bar
        root_domain: foo.bar
        services:
            - name: service1
              type: docker
            - name: service2
              type: docker
              links:
                - service1
        """.trimIndent()

        val cloud = ConfigurationParser(CloudConfigurationFactory(emptyList(), listOf(DockerServiceRegistration()))).parse(rawYml).shouldBeTypeOf<Success<CloudConfigurationRuntime>>()
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
    fun testSelfLinkError() {

        val cloudConfig = CloudConfiguration("test")
        DockerServiceConfigurationManager()
    }

    }