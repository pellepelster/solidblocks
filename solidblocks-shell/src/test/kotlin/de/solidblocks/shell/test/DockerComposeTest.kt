package de.solidblocks.shell.test

import de.solidblocks.shell.docker.ComposeFile
import de.solidblocks.shell.docker.Mount
import de.solidblocks.shell.docker.MountType
import de.solidblocks.shell.docker.PortMapping
import de.solidblocks.shell.docker.Service
import de.solidblocks.shell.docker.toYaml
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
                            volumes = listOf(Mount(MountType.bind, "/data", "/foo-bar")),
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
            volumes:
            - type: "bind"
              source: "/data"
              target: "/foo-bar"
        """
                .trimIndent()
    }
}
