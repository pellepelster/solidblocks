package de.solidblocks.cloud.provisioner.postgres.user

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup

class PostgresUser(
    name: String,
    val password: PassSecretLookup,
    val server: HetznerServerLookup,
    val superUserPassword: PassSecretLookup,
) : BaseInfrastructureResource<PostgresUserRuntime>(name, emptySet()) {

  fun asLookup() = PostgresUserLookup(name, server, superUserPassword)

  override fun logText() = "Postgres user '$name'"

  override val lookupType = PostgresUserLookup::class
}
