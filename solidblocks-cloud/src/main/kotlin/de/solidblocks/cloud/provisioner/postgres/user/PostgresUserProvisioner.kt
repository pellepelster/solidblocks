package de.solidblocks.cloud.provisioner.postgres.user

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.unknown
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.ensureLookup
import de.solidblocks.cloud.provisioner.postgres.BasePostgresProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.postgresql.util.PSQLException
import java.sql.Connection
import java.sql.DriverManager

class PostgresUserProvisioner :
    BasePostgresProvisioner(),
    ResourceLookupProvider<PostgresUserLookup, PostgresUserRuntime>,
    InfrastructureResourceProvisioner<PostgresUser, PostgresUserRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun diff(resource: PostgresUser, context: ProvisionerDiffContext) = when (val result = lookupInternal(resource.asLookup(), context)) {
        is Error<PostgresUserRuntime?> -> ResourceDiff(resource, unknown)
        is Success<PostgresUserRuntime?> -> {
            if (result.data == null) {
                ResourceDiff(resource, missing)
            } else {
                val changes = mutableListOf<ResourceDiffItem>()

                context.withPortForward(resource.server, 5432) {
                    if (it == null) {
                        return@withPortForward ResourceDiff(resource, unknown)
                    }

                    val passwordValid = try {
                        DriverManager.getConnection(
                            "jdbc:postgresql://localhost:$it/postgres",
                            resource.name,
                            context.ensureLookup(resource.password).secret,
                        ).use { true }
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

                if (changes.isEmpty()) {
                    ResourceDiff(resource, up_to_date)
                } else {
                    ResourceDiff(resource, has_changes, changes = changes)
                }
            }
        }
    }

    private suspend fun lookupInternal(lookup: PostgresUserLookup, context: ProvisionerContext): Result<PostgresUserRuntime?> =
        when (val result = context.createConnection(lookup.server, lookup.superUserPassword)) {
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

    override suspend fun apply(resource: PostgresUser, context: ProvisionerApplyContext, log: LogContext): Result<PostgresUserRuntime> {
        val password = context.ensureLookup(resource.password)

        when (
            val result =
                context.waitForAdminConnection(resource.server, resource.superUserPassword, log)
        ) {
            is Error<Connection> -> Error<PostgresUserRuntime?>(result.error)
            is Success<Connection> -> {
                if (lookup(resource.asLookup(), context) == null) {
                    result.data.createStatement().use {
                        it.execute(
                            "CREATE USER ${it.enquoteIdentifier(resource.name, false)} WITH ENCRYPTED PASSWORD ${it.enquoteLiteral(password.secret)}",
                        )
                    }
                }

                result.data.createStatement().use {
                    it.execute(
                        "ALTER USER ${it.enquoteIdentifier(resource.name, false)} WITH ENCRYPTED PASSWORD ${it.enquoteLiteral(password.secret)}",
                    )
                }
            }
        }

        return Success(lookup(resource.asLookup(), context)!!)
    }

    override suspend fun lookup(lookup: PostgresUserLookup, context: ProvisionerContext): PostgresUserRuntime? = when (val result = lookupInternal(lookup, context)) {
        is Error<PostgresUserRuntime?> -> null
        is Success<PostgresUserRuntime?> -> result.data
    }

    override val supportedLookupType = PostgresUserLookup::class

    override val supportedResourceType = PostgresUser::class
}
