package de.solidblocks.docker

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DockerComposeTest {

  @Test
  fun testRender() {
    val dockerCompose =
        ComposeFile(
            services =
                mapOf(
                    "service1" to
                        Service(
                            "image1:latest",
                            environment = mapOf("foo" to "bar"),
                            ports = listOf(PortMapping(80, 8080)),
                        ),
                ),
        )

    dockerCompose.toYaml() shouldBe
        """
        services:
          "service1":
            image: "image1:latest"
            ports:
            - target: 80
              published: 8080
            environment:
              "foo": "bar"
        """
            .trimIndent()
  }
}
