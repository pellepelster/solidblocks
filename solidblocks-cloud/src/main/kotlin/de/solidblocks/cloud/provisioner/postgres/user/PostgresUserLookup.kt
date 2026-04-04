package de.solidblocks.cloud.provisioner.postgres.user

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup

class PostgresUserLookup(
    name: String,
    val server: HetznerServerLookup,
    val superUserPassword: PassSecretLookup,
) : InfrastructureResourceLookup<PostgresUserRuntime>(name, emptySet()) {
  override fun logText() = "Postgres user '$name.'"
}
