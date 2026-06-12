package de.solidblocks.cloud.provisioner.postgres.grant

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabase
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUser
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup

class PostgresDatabaseGrant(
    val user: PostgresUser,
    val database: PostgresDatabase,
    val admin: Boolean,
    val read: Boolean,
    val write: Boolean,
    val server: HetznerServerLookup,
    val superUserPassword: GenericSecretLookup,
) : BaseInfrastructureResource<PostgresDatabaseGrantRuntime>(
    "${database.name}-${user.name}",
    setOf(user, database, server, superUserPassword),
) {

    override fun asLookup() = PostgresDatabaseGrantLookup(name, user.asLookup(), database.asLookup(), server, superUserPassword)

    override fun logText() = "Postgres grant for user '${user.name}' on database '${database.name}'"

    override val lookupType = PostgresDatabaseGrantLookup::class
}
