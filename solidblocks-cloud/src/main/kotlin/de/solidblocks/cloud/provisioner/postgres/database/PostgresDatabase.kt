package de.solidblocks.cloud.provisioner.postgres.database

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUserLookup

class PostgresDatabase(name: String, val user: PostgresUserLookup, val server: HetznerServerLookup, val superUserPassword: PassSecretLookup) :
    BaseInfrastructureResource<PostgresDatabaseRuntime>(name, emptySet()) {

    fun asLookup() = PostgresDatabaseLookup(name, server, superUserPassword)

    override fun logText() = "Postgres database '$name'"

    override val lookupType = PostgresDatabaseLookup::class
}
