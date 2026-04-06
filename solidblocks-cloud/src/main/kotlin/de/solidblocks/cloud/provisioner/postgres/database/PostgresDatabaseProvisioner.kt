package de.solidblocks.cloud.provisioner.postgres.database

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

class PostgresDatabaseProvisioner :
    BasePostgresProvisioner(),
    ResourceLookupProvider<PostgresDatabaseLookup, PostgresDatabaseRuntime>,
    InfrastructureResourceProvisioner<PostgresDatabase, PostgresDatabaseRuntime> {

  private val logger = KotlinLogging.logger {}

  override suspend fun diff(resource: PostgresDatabase, context: CloudProvisionerContext) =
      when (val result = lookupInternal(resource.asLookup(), context)) {
        is Error<PostgresDatabaseRuntime?> -> ResourceDiff(resource, unknown)
        is Success<PostgresDatabaseRuntime?> -> {
          if (result.data == null) {
            ResourceDiff(resource, missing)
          } else {
            val changes = mutableListOf<ResourceDiffItem>()
            if (changes.isEmpty()) {
              ResourceDiff(resource, up_to_date)
            } else {
              ResourceDiff(resource, has_changes, changes = changes)
            }
          }
        }
      }

  private suspend fun lookupInternal(
      lookup: PostgresDatabaseLookup,
      context: CloudProvisionerContext,
  ): Result<PostgresDatabaseRuntime?> =
      when (val result = context.createAdminConnection(lookup.server, lookup.superUserPassword)) {
        is Error<Connection> -> Error<PostgresDatabaseRuntime?>(result.error)
        is Success<Connection> ->
            result.data
                .prepareStatement(
                    "SELECT datname FROM pg_catalog.pg_database WHERE datname = ?",
                )
                .use { stmt ->
                  stmt.setString(1, lookup.name)
                  stmt.executeQuery().use { rs -> rs.next() }
                }
                .let { hasUserName ->
                  if (hasUserName) {
                    Success<PostgresDatabaseRuntime?>(PostgresDatabaseRuntime(lookup.name))
                  } else {
                    Success<PostgresDatabaseRuntime?>(null)
                  }
                }
      }

  override suspend fun apply(
      resource: PostgresDatabase,
      context: CloudProvisionerContext,
      log: LogContext,
  ): Result<PostgresDatabaseRuntime> {
    val user = context.ensureLookup(resource.user)

    when (val result = context.createAdminConnection(resource.server, resource.superUserPassword)) {
      is Error<Connection> -> Error<PostgresDatabaseRuntime?>(result.error)
      is Success<Connection> -> {
        if (lookup(resource.asLookup(), context) == null) {
          result.data.createStatement().use {
            it.execute(
                "CREATE DATABASE ${it.enquoteIdentifier(resource.name, false)} WITH OWNER = ${it.enquoteLiteral(user.name)}",
            )
          }
        }
      }
    }

    return Success(lookup(resource.asLookup(), context)!!)
  }

  override suspend fun lookup(
      lookup: PostgresDatabaseLookup,
      context: CloudProvisionerContext,
  ): PostgresDatabaseRuntime? =
      when (val result = lookupInternal(lookup, context)) {
        is Error<PostgresDatabaseRuntime?> -> null
        is Success<PostgresDatabaseRuntime?> -> result.data
      }

  override val supportedLookupType = PostgresDatabaseLookup::class

  override val supportedResourceType = PostgresDatabase::class
}
