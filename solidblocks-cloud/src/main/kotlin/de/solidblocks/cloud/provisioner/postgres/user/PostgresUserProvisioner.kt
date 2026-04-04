package de.solidblocks.cloud.provisioner.postgres.user

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.postgres.BasePostgresProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.DriverManager
import kotlin.let
import kotlin.use
import org.postgresql.util.PSQLException

class PostgresUserProvisioner :
    BasePostgresProvisioner(),
    ResourceLookupProvider<PostgresUserLookup, PostgresUserRuntime>,
    InfrastructureResourceProvisioner<PostgresUser, PostgresUserRuntime> {

  private val logger = KotlinLogging.logger {}

  override suspend fun diff(resource: PostgresUser, context: CloudProvisionerContext) =
      when (val result = lookupInternal(resource.asLookup(), context)) {
        is Error<PostgresUserRuntime?> -> ResourceDiff(resource, unknown)
        is Success<PostgresUserRuntime?> -> {
          if (result.data == null) {
            ResourceDiff(resource, missing)
          } else {
            val changes = mutableListOf<ResourceDiffItem>()

            when (
                val result =
                    context.createAdminConnection(resource.server, resource.superUserPassword)
            ) {
              is Error<Connection> -> Error<PostgresUserRuntime?>(result.error)
              is Success<Connection> -> {
                val url = result.data.metaData.url

                val passwordValid =
                    try {
                      DriverManager.getConnection(
                              url,
                              resource.name,
                              context.ensureLookup(resource.password).secret,
                          )
                          .use { true }
                    } catch (e: PSQLException) {
                      if (e.sqlState == "28P01") {
                        false
                      } else {
                        true
                      }
                    }

                if (!passwordValid) {
                  changes.add(
                      ResourceDiffItem(
                          "password",
                          changed = true,
                      ),
                  )
                }
              }
            }

            if (changes.isEmpty()) {
              ResourceDiff(resource, up_to_date)
            } else {
              ResourceDiff(resource, has_changes, changes = changes)
            }
          }
        }
      }

  private suspend fun lookupInternal(
      lookup: PostgresUserLookup,
      context: CloudProvisionerContext,
  ): Result<PostgresUserRuntime?> =
      when (val result = context.createAdminConnection(lookup.server, lookup.superUserPassword)) {
        is Error<Connection> -> Error<PostgresUserRuntime?>(result.error)
        is Success<Connection> ->
            result.data
                .prepareStatement(
                    "SELECT 1 FROM pg_roles WHERE rolname = ?",
                )
                .use { stmt ->
                  stmt.setString(1, lookup.name)
                  stmt.executeQuery().use { rs -> rs.next() }
                }
                .let { hasUserName ->
                  if (hasUserName) {
                    Success<PostgresUserRuntime?>(PostgresUserRuntime(lookup.name))
                  } else {
                    Success<PostgresUserRuntime?>(null)
                  }
                }
      }

  override suspend fun apply(
      resource: PostgresUser,
      context: CloudProvisionerContext,
      log: LogContext,
  ): Result<PostgresUserRuntime> {
    val password = context.ensureLookup(resource.password)

    when (val result = context.createAdminConnection(resource.server, resource.superUserPassword)) {
      is Error<Connection> -> Error<PostgresUserRuntime?>(result.error)
      is Success<Connection> -> {
        if (lookup(resource.asLookup(), context) == null) {
          result.data.createStatement().use {
            it.execute(
                "CREATE USER ${it.enquoteIdentifier(resource.name, false)} WITH PASSWORD ${it.enquoteLiteral(password.secret)}",
            )
          }
        }

        result.data.createStatement().use {
          it.execute(
              "ALTER USER ${it.enquoteIdentifier(resource.name, false)} WITH PASSWORD ${it.enquoteLiteral(password.secret)}",
          )
        }
      }
    }

    return Success(lookup(resource.asLookup(), context)!!)
  }

  override suspend fun lookup(
      lookup: PostgresUserLookup,
      context: CloudProvisionerContext,
  ): PostgresUserRuntime? =
      when (val result = lookupInternal(lookup, context)) {
        is Error<PostgresUserRuntime?> -> null
        is Success<PostgresUserRuntime?> -> result.data
      }

  override val supportedLookupType = PostgresUserLookup::class

  override val supportedResourceType = PostgresUser::class
}
