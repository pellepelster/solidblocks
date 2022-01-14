package de.solidblocks.cloud

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.users.api.UsersApi
import de.solidblocks.cloud.model.generateRsaKeyPair
import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestEnvironmentExtension::class)
class UsersApiTest {

    val keyPair = generateRsaKeyPair()
    val httpServer = CloudApiHttpServer(privateKey = keyPair.first, publicKey = keyPair.second, port = -1)

    @Test
    fun testRegisterProtectedRoute(serviceTestEnvironment: TestEnvironment) {

        val usersApi = UsersApi(
            httpServer,
            serviceTestEnvironment.cloudRepository,
            serviceTestEnvironment.environmentRepository,
            serviceTestEnvironment.usersRepository
        )

        val token = given().port(httpServer.port)
            .with().body(
                """{
                    "email": "juergen",
                    "password": "schmidt"
                  }
                """.trimIndent()
            ).post("/api/v1/users/login")
            .then().extract().jsonPath().get<String>("token")

        given().port(httpServer.port)
            .with().header("Authorization", "Bearer $token")
            .get("/api/v1/users/whoami")
            .then().body("email", equalTo("yyyy"))
    }
}
