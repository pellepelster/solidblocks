package de.solidblocks.cloud.provisioner.postgres.database

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup

class PostgresDatabaseLookup(name: String, val server: HetznerServerLookup, val superUserPassword: GenericSecretLookup) : InfrastructureResourceLookup<PostgresDatabaseRuntime>(name, emptySet()) {
    override fun logText() = "Postgres database '$name.'"
}
