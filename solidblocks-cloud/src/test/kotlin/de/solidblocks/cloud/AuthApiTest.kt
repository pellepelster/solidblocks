package de.solidblocks.cloud

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.auth.api.AuthApi
import de.solidblocks.cloud.model.generateRsaKeyPair
import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import io.restassured.RestAssured.given
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.impl.jose.JWT
import io.vertx.kotlin.core.json.get
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.core.IsNull.notNullValue
import org.hamcrest.core.IsNull.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestEnvironmentExtension::class)
class AuthApiTest {

    val keyPair = generateRsaKeyPair()
    val httpServer = CloudApiHttpServer(privateKey = keyPair.first, publicKey = keyPair.second, port = -1)

    @Test
    fun testAuthForAdminUser(testEnvironment: TestEnvironment) {
        testEnvironment.managers.users.ensureAdminUser("juergen@admin.local", "admin-password")
        val api = AuthApi(httpServer, testEnvironment.repositories.clouds, testEnvironment.repositories.environments, testEnvironment.managers.users)

        given().port(httpServer.port).with().body(
            """{
                    "email": "juergen@admin.local",
                    "password": "invalid-admin-password"
                  }
            """.trimIndent()
        ).post("/api/v1/auth/login").then().assertThat().statusCode(401).body("token", nullValue())

        val token = given().port(httpServer.port).with().body(
            """{
                    "email": "juergen@admin.local",
                    "password": "admin-password"
                  }
            """.trimIndent()
        ).post("/api/v1/auth/login").then().statusCode(200).body("token", notNullValue()).extract().jsonPath().get<String>("token")

        val jwt = JWT.parse(token)
        val payload = jwt.get<JsonObject>("payload")
        assertThat(payload.getString("email")).isEqualTo("juergen@admin.local")
        assertThat(payload.getString("scope")).isEqualTo("root")

        given().port(httpServer.port).with().header("Authorization", "Bearer $token").get("/api/v1/auth/whoami").then().statusCode(200).body("user.email", equalTo("juergen@admin.local")).body("user.scope", equalTo("root"))
    }

    @Test
    fun testAuthForEnvironmentUser(testEnvironment: TestEnvironment) {
        testEnvironment.createEnvironment("cloud1", "environment1")

        val api = AuthApi(httpServer, testEnvironment.repositories.clouds, testEnvironment.repositories.environments, testEnvironment.managers.users)

        given().port(httpServer.port).with().body(
            """{
                    "email": "juergen@environment1.cloud1",
                    "password": "invalid-password1"
                  }
            """.trimIndent()
        ).post("/api/v1/auth/login").then().assertThat().statusCode(401).body("token", nullValue())

        val token = given().port(httpServer.port).with().body(
            """{
                    "email": "juergen@environment1.cloud1",
                    "password": "password1"
                  }
            """.trimIndent()
        ).post("/api/v1/auth/login").then().statusCode(200).body("token", notNullValue()).extract().jsonPath().get<String>("token")

        val jwt = JWT.parse(token)
        val payload = jwt.get<JsonObject>("payload")
        assertThat(payload.getString("email")).isEqualTo("juergen@environment1.cloud1")
        assertThat(payload.getString("scope")).isEqualTo("environment")

        given().port(httpServer.port).with()
            .header("Authorization", "Bearer $token").get("/api/v1/auth/whoami").then()
            .statusCode(200).body("user.email", equalTo("juergen@environment1.cloud1")).body("user.scope", equalTo("environment"))

        given().port(httpServer.port).with().get("/api/v1/auth/whoami").then().statusCode(401).body("email", nullValue())
    }
}
