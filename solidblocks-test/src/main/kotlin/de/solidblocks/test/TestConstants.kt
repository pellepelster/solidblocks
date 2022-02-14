package de.solidblocks.test

import java.util.*

object TestConstants {

    fun TEST_DB_JDBC_URL() = "jdbc:derby:memory:${UUID.randomUUID()};create=true"

    val ROOT_DOMAIN = "local.test"

    val ADMIN_USER = "admin@$ROOT_DOMAIN"

    val ADMIN_PASSWORD = "admin"

    val CLOUD_PASSWORD = "cloudpassword"

    val ENVIRONMENT_PASSWORD = "environmentpassword"

    val TENANT_PASSWORD = "tenantpassword"
}
