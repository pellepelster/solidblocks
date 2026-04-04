package de.solidblocks.cloud.provisioner.postgres

import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.DriverManager

open class BasePostgresProvisioner {

  private val logger = KotlinLogging.logger {}

  suspend fun CloudProvisionerContext.createAdminConnection(
      server: HetznerServerLookup,
      superUserPassword: PassSecretLookup,
  ): Result<Connection> {
    val password = this.lookup(superUserPassword)

    return this.withPortForward(server, 5432) {
      if (it == null || password == null) {
        Error("could not establish Postgres admin connection for ${server.logText()}")
      } else {
        Success(
            DriverManager.getConnection(
                "jdbc:postgresql://localhost:$it/postgres",
                "rds",
                password.secret,
            ),
        )
      }
    }
  }
}
