package de.solidblocks.cloud

import io.restassured.specification.RequestSpecification
import org.hamcrest.core.IsNull

fun RequestSpecification.login(username: String = "admin", password: String = "admin") = this.with().body(
    """{
                    "email": "$username",
                    "password": "$password"
                  }
    """.trimIndent()
).post("/api/v1/auth/login").then().statusCode(200).body("token", IsNull.notNullValue()).extract().jsonPath().get<String>("token")

fun RequestSpecification.withAuthToken(token: String) = this.header("Authorization", "Bearer $token")
