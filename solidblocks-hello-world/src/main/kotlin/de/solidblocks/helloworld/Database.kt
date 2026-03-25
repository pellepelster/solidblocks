package de.solidblocks.helloworld

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object CounterTable : Table("counter") {
    val id = varchar("id", 64)
    val value = long("value").default(0)
    override val primaryKey = PrimaryKey(id)
}

fun Application.configureDatabases() {
    val host = System.getenv("DB_HOST") ?: error("DB_HOST env variable is required")
    val port = System.getenv("DB_PORT") ?: "5432"
    val database = System.getenv("DB_NAME") ?: error("DB_NAME env variable is required")
    val username = System.getenv("DB_USER") ?: error("DB_USER env variable is required")
    val password = System.getenv("DB_PASSWORD") ?: error("DB_PASSWORD env variable is required")

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$host:$port/$database"
        driverClassName = "org.postgresql.Driver"
        this.username = username
        this.password = password
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val dataSource = HikariDataSource(hikariConfig)

    Database.connect(dataSource)

    transaction {
        SchemaUtils.createMissingTablesAndColumns(CounterTable)
        /*
         val exists = CounterTable.selectAll().where { CounterTable.id eq "hello" }.count() > 0
         if (!exists) {
             CounterTable.insert {
                 it[id] = "hello"
                 it[value] = 0
             }
         }*/
    }

    log.info("Database connected and schema initialised.")
}
