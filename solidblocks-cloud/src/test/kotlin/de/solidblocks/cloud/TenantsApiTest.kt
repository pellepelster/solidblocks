package de.solidblocks.cloud

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.auth.api.AuthApi
import de.solidblocks.cloud.model.generateRsaKeyPair
import de.solidblocks.cloud.tenants.api.TenantsApi
import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestEnvironmentExtension::class)
class TenantsApiTest {

    private val keyPair = generateRsaKeyPair()
    private val httpServer = CloudApiHttpServer(privateKey = keyPair.first, publicKey = keyPair.second, port = -1)

    @Test
    fun testTenantsApi(testEnvironment: TestEnvironment) {
        testEnvironment.managers.users.ensureAdminUser("admin", "admin")
        testEnvironment.testContext.createCloudEnvironment("cloud1", "env1")

        AuthApi(httpServer, testEnvironment.repositories.clouds, testEnvironment.repositories.environments, testEnvironment.managers.users)
        TenantsApi(httpServer, testEnvironment.managers.tenants)

        val token = given().port(httpServer.port).login()

        // access without token is denied
        given().port(httpServer.port).get("/api/v1/tenants").then().assertThat().statusCode(401)

        // get list of tenants
        given().port(httpServer.port).withAuthToken(token).get("/api/v1/tenants").then().assertThat().statusCode(200).assertThat().body("tenants.size()", `is`(0))

        // missing tenant name
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local"
                }
            """.trimIndent()
        ).post("/api/v1/tenants").then().statusCode(422).body("messages.size()", `is`(1)).body("messages[0].attribute", `is`("tenant")).body("messages[0].code", `is`("mandatory"))

        // missing email
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "tenant": "tenant1"
                }
            """.trimIndent()
        ).post("/api/v1/tenants").then().statusCode(422).body("messages.size()", `is`(1)).body("messages[0].attribute", `is`("email")).body("messages[0].code", `is`("mandatory"))

        // invalid tenant name
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local",
                    "tenant": "tenant!@"
                }
            """.trimIndent()
        ).post("/api/v1/tenants").then().statusCode(422).body("messages.size()", `is`(1)).body("messages[0].attribute", `is`("tenant")).body("messages[0].code", `is`("invalid"))

        // create tenant
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local",
                    "tenant": "tenant1"
                }
            """.trimIndent()
        ).post("/api/v1/tenants").then().statusCode(201).body("messages.size()", `is`(0))

        // duplicate tenant name
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local1",
                    "tenant": "tenant1"
                }
            """.trimIndent()
        ).post("/api/v1/tenants").then().statusCode(422).body("messages.size()", `is`(1)).body("messages[0].attribute", `is`("tenant")).body("messages[0].code", `is`("duplicate"))

        // duplicate email
        given().port(httpServer.port).withAuthToken(token).body(
            """{
                    "email": "juergen@admin.local",
                    "tenant": "tenant12"
                }
            """.trimIndent()
        ).post("/api/v1/tenants").then().statusCode(422).body("messages.size()", `is`(1)).body("messages[0].attribute", `is`("email")).body("messages[0].code", `is`("duplicate"))

        given().port(httpServer.port).withAuthToken(token).get("/api/v1/tenants").then().assertThat().statusCode(200).assertThat().body("tenants.size()", `is`(1))
    }
}
