package de.solidblocks.cloud.provisioner.postgres.grant

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabaseLookup
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUserLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup

class PostgresDatabaseGrantLookup(name: String, val user: PostgresUserLookup, val database: PostgresDatabaseLookup, val server: HetznerServerLookup, val superUserPassword: GenericSecretLookup) :
    InfrastructureResourceLookup<PostgresDatabaseGrantRuntime>(name, emptySet()) {
    override fun logText() = "Postgres grant for user '${user.name}' on database '${database.name}'"
}
