package de.solidblocks.test

import java.util.*

object TestConstants {
    fun TEST_DB_JDBC_URL() = "jdbc:derby:memory:${UUID.randomUUID()};create=true"
}
