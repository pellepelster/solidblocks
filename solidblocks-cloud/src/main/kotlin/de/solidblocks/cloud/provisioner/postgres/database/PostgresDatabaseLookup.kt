package de.solidblocks.cloud.provisioner.postgres.database

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup

class PostgresDatabaseLookup(
    name: String,
    val server: HetznerServerLookup,
    val superUserPassword: PassSecretLookup,
) : InfrastructureResourceLookup<PostgresDatabaseRuntime>(name, emptySet()) {
  override fun logText() = "Postgres database '$name.'"
}
