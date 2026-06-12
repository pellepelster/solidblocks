package de.solidblocks.cloud.provisioner.postgres.grant

import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.unknown
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.postgres.BasePostgresProvisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection

class PostgresDatabaseGrantProvisioner :
    BasePostgresProvisioner(),
    InfrastructureResourceLookupProvider<PostgresDatabaseGrantLookup, PostgresDatabaseGrantRuntime>,
    InfrastructureResourceProvisioner<PostgresDatabaseGrant, PostgresDatabaseGrantRuntime, PostgresDatabaseGrantLookup> {

    private val logger = KotlinLogging.logger {}

    override suspend fun diff(resource: PostgresDatabaseGrant, context: ProvisionerDiffContext): Result<ResourceDiff> = Success(
        when (val result = lookupInternal(resource.asLookup(), context)) {
            is Error<PostgresDatabaseGrantRuntime?> -> ResourceDiff(resource, unknown)
            is Success<PostgresDatabaseGrantRuntime?> -> {
                if (result.data == null) {
                    ResourceDiff(resource, missing)
                } else {
                    val changes = mutableListOf<ResourceDiffItem>()

                    if (result.data.admin != resource.admin) {
                        changes.add(
                            ResourceDiffItem(
                                "admin",
                                true,
                                false,
                                false,
                                resource.admin,
                                result.data.admin,
                            ),
                        )
                    }

                    // admin implies full read/write access on all tables
                    if (result.data.read != (resource.read || resource.admin)) {
                        changes.add(
                            ResourceDiffItem(
                                "read",
                                true,
                                false,
                                false,
                                resource.read || resource.admin,
                                result.data.read,
                            ),
                        )
                    }

                    if (result.data.write != (resource.write || resource.admin)) {
                        changes.add(
                            ResourceDiffItem(
                                "write",
                                true,
                                false,
                                false,
                                resource.write || resource.admin,
                                result.data.write,
                            ),
                        )
                    }

                    if (changes.isNotEmpty()) {
                        ResourceDiff(resource, has_changes, changes = changes)
                    } else {
                        ResourceDiff(resource, up_to_date)
                    }
                }
            }
        },
    )

    private suspend fun lookupInternal(lookup: PostgresDatabaseGrantLookup, context: SSHProvisionerContext): Result<PostgresDatabaseGrantRuntime?> =
        when (val result = context.createConnection(lookup.server, lookup.superUserPassword, database = lookup.database.name)) {
            is Error<Connection> -> Error<PostgresDatabaseGrantRuntime?>(result.error)
            is Success<Connection> -> {
                val userExists = result.data
                    .prepareStatement("SELECT 1 FROM pg_roles WHERE rolname = ?")
                    .use { stmt ->
                        stmt.setString(1, lookup.user.name)
                        stmt.executeQuery().use { rs -> rs.next() }
                    }

                if (!userExists) {
                    Success<PostgresDatabaseGrantRuntime?>(null)
                } else {
                    // only explicit grants to the user are checked, `has_*_privilege` functions would also
                    // report privileges inherited via PUBLIC and never converge with the configured flags
                    result.data
                        .prepareStatement(
                            """
                            SELECT
                                EXISTS (SELECT 1 FROM pg_database d, aclexplode(d.datacl) a WHERE d.datname = current_database() AND a.grantee = ?::regrole AND a.privilege_type = 'CONNECT') AS "applied",
                                has_database_privilege(?, current_database(), 'CREATE') AS "admin",
                                EXISTS (SELECT 1 FROM pg_default_acl d, aclexplode(d.defaclacl) a WHERE d.defaclnamespace = 'public'::regnamespace AND d.defaclobjtype = 'r' AND a.grantee = ?::regrole AND a.privilege_type = 'SELECT') AS "read",
                                EXISTS (SELECT 1 FROM pg_default_acl d, aclexplode(d.defaclacl) a WHERE d.defaclnamespace = 'public'::regnamespace AND d.defaclobjtype = 'r' AND a.grantee = ?::regrole AND a.privilege_type = 'INSERT') AS "write"
                            """.trimIndent(),
                        )
                        .use { stmt ->
                            (1..4).forEach { stmt.setString(it, lookup.user.name) }
                            stmt.executeQuery().use { rs ->
                                rs.next()
                                if (!rs.getBoolean("applied")) {
                                    Success<PostgresDatabaseGrantRuntime?>(null)
                                } else {
                                    Success<PostgresDatabaseGrantRuntime?>(
                                        PostgresDatabaseGrantRuntime(
                                            lookup.user.name,
                                            lookup.database.name,
                                            rs.getBoolean("admin"),
                                            rs.getBoolean("read"),
                                            rs.getBoolean("write"),
                                        ),
                                    )
                                }
                            }
                        }
                }
            }
        }

    override suspend fun apply(resource: PostgresDatabaseGrant, context: ProvisionerApplyContext, log: LogContext): Result<PostgresDatabaseGrantRuntime> {
        when (
            val result =
                context.waitForAdminConnection(resource.server, resource.superUserPassword, log, database = resource.database.name)
        ) {
            is Error<Connection> -> return Error(result.error)
            is Success<Connection> -> {
                result.data.createStatement().use {
                    val user = it.enquoteIdentifier(resource.user.name, false)
                    val database = it.enquoteIdentifier(resource.database.name, false)
                    val owner = it.enquoteIdentifier(resource.database.user.name, false)

                    // reset all privileges so removed grants are revoked, then re-grant the configured ones
                    it.execute("ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public REVOKE ALL PRIVILEGES ON TABLES FROM $user")
                    it.execute("ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM $user")
                    it.execute("REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM $user")
                    it.execute("REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM $user")
                    it.execute("REVOKE ALL PRIVILEGES ON SCHEMA public FROM $user")
                    it.execute("REVOKE ALL PRIVILEGES ON DATABASE $database FROM $user")

                    it.execute("GRANT CONNECT ON DATABASE $database TO $user")

                    if (resource.admin) {
                        it.execute("GRANT ALL PRIVILEGES ON DATABASE $database TO $user")
                        it.execute("GRANT ALL PRIVILEGES ON SCHEMA public TO $user")
                        it.execute("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $user")
                        it.execute("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $user")
                        it.execute("ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO $user")
                        it.execute("ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO $user")
                    }

                    if (resource.read) {
                        it.execute("GRANT USAGE ON SCHEMA public TO $user")
                        it.execute("GRANT SELECT ON ALL TABLES IN SCHEMA public TO $user")
                        it.execute("GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO $user")
                        it.execute("ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public GRANT SELECT ON TABLES TO $user")
                        it.execute("ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public GRANT SELECT ON SEQUENCES TO $user")
                    }

                    if (resource.write) {
                        it.execute("GRANT USAGE ON SCHEMA public TO $user")
                        it.execute("GRANT INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO $user")
                        it.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO $user")
                        it.execute("ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public GRANT INSERT, UPDATE, DELETE ON TABLES TO $user")
                        it.execute("ALTER DEFAULT PRIVILEGES FOR ROLE $owner IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO $user")
                    }
                }
            }
        }

        return Success(lookup(resource.asLookup(), context)!!)
    }

    override suspend fun lookup(lookup: PostgresDatabaseGrantLookup, context: SSHProvisionerContext): PostgresDatabaseGrantRuntime? = when (val result = lookupInternal(lookup, context)) {
        is Error<PostgresDatabaseGrantRuntime?> -> null
        is Success<PostgresDatabaseGrantRuntime?> -> result.data
    }

    override val supportedLookupType = PostgresDatabaseGrantLookup::class

    override val supportedResourceType = PostgresDatabaseGrant::class
}
