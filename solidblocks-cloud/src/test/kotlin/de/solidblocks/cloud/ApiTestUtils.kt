package de.solidblocks.cloud

import de.solidblocks.test.TestConstants.ADMIN_PASSWORD
import de.solidblocks.test.TestConstants.ADMIN_USER
import io.restassured.specification.RequestSpecification
import org.hamcrest.core.IsNull

fun RequestSpecification.login(username: String = ADMIN_USER, password: String = ADMIN_PASSWORD) = this.with().body(
    """{
                    "email": "$username",
                    "password": "$password"
                  }
    """.trimIndent()
).post("/api/v1/auth/login").then().statusCode(200).body("token", IsNull.notNullValue()).extract().jsonPath().get<String>("token")

fun RequestSpecification.withAuthToken(token: String) = this.header("Authorization", "Bearer $token")
