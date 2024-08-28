package de.solidblocks.debug.container

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DebugContainerIntegrationTests {

  @Container
  private val debugContainer =
      KGenericContainer(
              "ghcr.io/pellepelster/solidblocks-debug-container:${System.getenv("VERSION") ?: "snapshot"}-rc")
          .withExposedPorts(8080)

  @Test
  fun indexLoads() {
    RestAssured.port = debugContainer.firstMappedPort

    // .body(hasXPath("/greeting/firstName[text()='John']"))

    given().`when`().get("/").then().statusCode(200).body(containsString("h2"))
  }
}
