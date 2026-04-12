package de.solidblocks.helloworld

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.header
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.longLiteral
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsertReturning

@Serializable
data class CounterResponse(val visitor: Long)

fun Application.configureRouting() {
    routing {
        get("/") {
            val accept = call.request.header(HttpHeaders.Accept)

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

            when (accept) {
                "application/json" -> call.respond(HttpStatusCode.OK, CounterResponse(newValue))
                else -> call.respondText(
                    """
                    Welcome Visitor ${newValue}
                    
                    """.trimIndent()
                )
            }
        }
    }
}
