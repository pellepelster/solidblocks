package de.solidblocks.cloud.provisioner.postgres.user

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup

class PostgresUser(name: String, val password: GenericSecretLookup, val server: HetznerServerLookup, val superUserPassword: GenericSecretLookup) :
    BaseInfrastructureResource<PostgresUserRuntime>(name, emptySet()) {

    override fun asLookup() = PostgresUserLookup(name, server, superUserPassword)

    override fun logText() = "Postgres user '$name'"

    override val lookupType = PostgresUserLookup::class
}
