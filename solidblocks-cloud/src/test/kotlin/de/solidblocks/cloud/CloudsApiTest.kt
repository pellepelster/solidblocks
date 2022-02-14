package de.solidblocks.cloud

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.auth.api.AuthApi
import de.solidblocks.cloud.clouds.api.CloudsApi
import de.solidblocks.cloud.model.generateRsaKeyPair
import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestEnvironmentExtension::class)
class CloudsApiTest {

    val keyPair = generateRsaKeyPair()
    val httpServer = CloudApiHttpServer(privateKey = keyPair.first, publicKey = keyPair.second, port = -1)

    /*
    @Test
    fun testLoadsCloudFromHostHeader(testEnvironment: TestEnvironment) {
        testEnvironment.ensureCloud("localhost")

        CloudsApi(httpServer, testEnvironment.cloudsManager)

        given().port(httpServer.port).with().header("Host", "").get(" ").then()
            .assertThat()
            .statusCode(404)
            .body("messages[0].code", equalTo("cloud_unknown_domain"))

        given().port(httpServer.port).with().header("Host", "localhost").get("/api/v1/clouds").then()
            .assertThat()
            .statusCode(200)
            .body("cloud.name", equalTo("cloud1"))
    }
    */

    @Test
    fun testListClouds(testEnvironment: TestEnvironment) {
        testEnvironment.testContext.createCloud("cloud1")
        testEnvironment.testContext.createCloud("cloud2")

        AuthApi(
            httpServer,
            testEnvironment.repositories.clouds,
            testEnvironment.repositories.environments,
            testEnvironment.managers.users
        )
        CloudsApi(httpServer, testEnvironment.managers.clouds, testEnvironment.managers.environments)

        val token = given().port(httpServer.port).login()

        given().port(httpServer.port).with().withAuthToken(token).get("/api/v1/clouds").then()
            .assertThat()
            .statusCode(200).assertThat().body("clouds.size()", `is`(2))
    }
}
