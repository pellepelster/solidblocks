package de.solidblocks.cloud

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.model.generateRsaKeyPair
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class CloudApiHttpServerTest {

    private val keyPair = generateRsaKeyPair()

    private val httpServer = CloudApiHttpServer(privateKey = keyPair.first, publicKey = keyPair.second, port = -1)

    @Test
    fun testRegisterProtectedRoute() {
        httpServer.createSubRouter("/protected").route().handler {
            it.response().end("protected stuff")
        }

        given().port(httpServer.port).get("/protected").then().assertThat()
            .statusCode(401)
            .contentType("application/json")
            .body("messages[0].code", equalTo("unauthorized"))
    }

    @Test
    fun testRegisterUnprotectedRoute() {
        httpServer.addUnprotectedRouter("/unprotected").route().handler {
            it.response().end("unprotected stuff")
        }

        given().port(httpServer.port).get("/unprotected").then().assertThat()
            .statusCode(200)
            .body(equalTo("unprotected stuff"))
    }

    @Test
    fun testCrossOriginHeader() {
        httpServer.addUnprotectedRouter("/unprotected").route().handler {
            it.response().end("unprotected stuff")
        }

        httpServer.createSubRouter("/protected").route().handler {
            it.response().end("protected stuff")
        }

        given().port(httpServer.port).get("/protected").then().assertThat()
            .header("Access-Control-Allow-Origin", "*")

        given().port(httpServer.port).get("/unprotected").then().assertThat()
            .header("Access-Control-Allow-Origin", "*")
    }
}
