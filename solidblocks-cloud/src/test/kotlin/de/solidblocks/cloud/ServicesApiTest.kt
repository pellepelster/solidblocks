package de.solidblocks.cloud

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.auth.api.AuthApi
import de.solidblocks.cloud.model.generateRsaKeyPair
import de.solidblocks.cloud.services.api.ServicesApi
import de.solidblocks.test.TestConstants.ADMIN_PASSWORD
import de.solidblocks.test.TestConstants.ADMIN_USER
import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestEnvironmentExtension::class)
class ServicesApiTest {

    val keyPair = generateRsaKeyPair()
    val httpServer = CloudApiHttpServer(privateKey = keyPair.first, publicKey = keyPair.second, port = -1)

    companion object {
        @BeforeAll
        fun configureLogging() {
            RestAssured.filters(RequestLoggingFilter(), ResponseLoggingFilter())
        }
    }

    @Test
    fun testCatalog(testEnvironment: TestEnvironment) {

        testEnvironment.testContext.createCloudEnvironmentTenant("cloud1", "env1", "tenant1")

        AuthApi(httpServer, testEnvironment.repositories.clouds, testEnvironment.repositories.environments, testEnvironment.managers.users)
        ServicesApi(httpServer, testEnvironment.managers.services)

        val token = given().port(httpServer.port).login(ADMIN_USER, ADMIN_PASSWORD)

        given().port(httpServer.port).withAuthToken(token).get("/api/v1/services/catalog").then()
            .assertThat()
            .statusCode(200)
            .assertThat().body("items.size()", `is`(2))
            .assertThat().body("items[0].type", `is`("helloworld"))
    }

    @Test
    fun testCreateService(testEnvironment: TestEnvironment) {

        testEnvironment.testContext.createCloudEnvironmentTenant("cloud2", "env2", "tenant2")

        AuthApi(httpServer, testEnvironment.repositories.clouds, testEnvironment.repositories.environments, testEnvironment.managers.users)
        ServicesApi(httpServer, testEnvironment.managers.services)

        val token = given().port(httpServer.port).login("juergen@tenant2.env2.cloud2.local.test", "tenantpassword")

        given().port(httpServer.port).with().withAuthToken(token).get("/api/v1/services").then().assertThat().statusCode(200).assertThat().body("services.size()", `is`(0))

        given().port(httpServer.port).withAuthToken(token).with().body(
            """{
                    "name": "service1",
                    "type": "invalid-service"
                  }
            """.trimIndent()
        )
            .post("/api/v1/services").then()
            .assertThat()
            .statusCode(422)
            .body("messages.size()", `is`(1))
            .body("messages[0].code", `is`("services_invalid_type"))

        given().port(httpServer.port).withAuthToken(token).with().body(
            """{
                    "name": "service1",
                    "type": "helloworld"
                  }
            """.trimIndent()
        )
            .post("/api/v1/services").then()
            .assertThat()
            .statusCode(201)

        given().port(httpServer.port).with().withAuthToken(token).get("/api/v1/services").then().assertThat().statusCode(200).assertThat().body("services.size()", `is`(1))
    }
}
