package de.solidblocks.cloud

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.auth.api.AuthApi
import de.solidblocks.cloud.environments.api.EnvironmentsApi
import de.solidblocks.cloud.model.generateRsaKeyPair
import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestEnvironmentExtension::class)
class EnvironmentsApiTest {

    private val keyPair = generateRsaKeyPair()
    private val httpServer = CloudApiHttpServer(privateKey = keyPair.first, publicKey = keyPair.second, port = -1)

    @Test
    fun testEnvironmentsApi(testEnvironment: TestEnvironment) {
        testEnvironment.testContext.createCloud("cloud1")

        AuthApi(
            httpServer,
            testEnvironment.repositories.clouds,
            testEnvironment.repositories.environments,
            testEnvironment.managers.users
        )
        EnvironmentsApi(httpServer, testEnvironment.managers.environments)

        val token = given().port(httpServer.port).login()

        // access without token is denied
        given().port(httpServer.port).get("/api/v1/environments").then().assertThat()
            .statusCode(401)

        // get list of environments
        given().port(httpServer.port).withAuthToken(token).get("/api/v1/environments").then().assertThat()
            .statusCode(200).assertThat().body("environments.size()", `is`(0))

        // missing environment name
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local"
                }
            """.trimIndent()
        ).post("/api/v1/environments").then().statusCode(422).body("messages.size()", `is`(1))
            .body("messages[0].attribute", `is`("environment")).body("messages[0].code", `is`("mandatory"))

        // missing email
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "environment": "environment1"
                }
            """.trimIndent()
        ).post("/api/v1/environments").then().statusCode(422).body("messages.size()", `is`(1))
            .body("messages[0].attribute", `is`("email")).body("messages[0].code", `is`("mandatory"))

        // invalid tenant name
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local",
                    "environment": "environment!@"
                }
            """.trimIndent()
        ).post("/api/v1/environments").then().statusCode(422).body("messages.size()", `is`(1))
            .body("messages[0].attribute", `is`("environment")).body("messages[0].code", `is`("invalid"))

        // create environment
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local",
                    "environment": "environment1",
                    "githubReadOnlyToken": "token1",
                    "hetznerCloudApiTokenReadOnly": "token2",
                    "hetznerCloudApiTokenReadWrite": "token3",
                    "hetznerDnsApiToken": "token4"
                }
            """.trimIndent()
        ).post("/api/v1/environments").then().statusCode(201).body("messages.size()", `is`(0))

        // duplicate tenant name
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local1",
                    "environment": "environment1"
                }
            """.trimIndent()
        ).post("/api/v1/environments").then().statusCode(422).body("messages.size()", `is`(1))
            .body("messages[0].attribute", `is`("environment")).body("messages[0].code", `is`("duplicate"))

        // duplicate email
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local",
                    "environment": "environment12"
                }
            """.trimIndent()
        ).post("/api/v1/environments").then().statusCode(422).body("messages.size()", `is`(1))
            .body("messages[0].attribute", `is`("email")).body("messages[0].code", `is`("duplicate"))

        val environmentsId = given().port(httpServer.port).withAuthToken(token).get("/api/v1/environments").then().assertThat()
            .statusCode(200).assertThat()
            .body("environments.size()", `is`(1))
            .assertThat().body("environments[0].name", `is`("environment1"))
            .assertThat().body("environments[0].id", CoreMatchers.notNullValue())
            .extract().body().jsonPath().get<String>("environments[0].id")

        // get existing environment
        given().port(httpServer.port).with().withAuthToken(token).get("/api/v1/environments/{id}", environmentsId).then()
            .assertThat()
            .statusCode(200).assertThat()
            .assertThat().body("environment.name", `is`("environment1"))
            .assertThat().body("environment.id", `is`(environmentsId))

        // get non existing environments
        given().port(httpServer.port).with().withAuthToken(token).get("/api/v1/environments/f4b21644-ba00-46ff-a445-e7ac2e8762f3").then()
            .assertThat()
            .statusCode(404).assertThat()

        // get invalid environment id
        given().port(httpServer.port).with().withAuthToken(token).get("/api/v1/environments/xxx").then()
            .assertThat()
            .statusCode(400).assertThat()
    }
}
