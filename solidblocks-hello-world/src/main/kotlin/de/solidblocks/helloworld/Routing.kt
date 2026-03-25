package de.solidblocks.helloworld

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.longLiteral
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsertReturning

@Serializable
data class CounterResponse(val counter: Long)

fun Application.configureRouting() {
    routing {
        get("/hello") {
            val newValue = transaction {
                CounterTable.upsertReturning(
                    onUpdate = listOf(
                        CounterTable.value to (CounterTable.value + longLiteral(1))
                    )
                ) {
                    it[id] = "hello"
                    it[value] = 1
                }.single()[CounterTable.value]
            }
            call.respond(HttpStatusCode.OK, CounterResponse(newValue))
        }
    }
}
