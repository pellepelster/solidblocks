package de.solidblocks.debug.container

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DebugContainerIntegrationTests {

  private val debugContainer =
      GenericContainer(
              "ghcr.io/pellepelster/solidblocks-debug-container:${System.getenv("VERSION") ?: "0.0.0"}-snapshot",
          )
          .withExposedPorts(8080)

  @Test
  fun indexLoads() {
    RestAssured.port = debugContainer.firstMappedPort
    // .body(hasXPath("/greeting/firstName[text()='John']"))
    given().`when`().get("/").then().statusCode(200).body(containsString("h2"))
  }
}
