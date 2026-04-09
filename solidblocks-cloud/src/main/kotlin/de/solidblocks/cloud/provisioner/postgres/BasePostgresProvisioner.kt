package de.solidblocks.cloud.provisioner.postgres

import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.provisioner.withPortForward
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.Waiter
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.DriverManager

open class BasePostgresProvisioner {

    private val logger = KotlinLogging.logger {}

    suspend fun CloudProvisionerContext.waitForAdminConnection(server: HetznerServerLookup, superUserPassword: PassSecretLookup, log: LogContext): Result<Connection> = Waiter.longWaitForResult {
        logInfo("waiting for Postgres admin connection", context = log)
        createAdminConnection(server, superUserPassword)
    }

    suspend fun CloudProvisionerContext.createAdminConnection(server: HetznerServerLookup, superUserPassword: PassSecretLookup): Result<Connection> {
        val password = this.lookup(superUserPassword)

        return this.withPortForward(server, 5432) {
            if (it == null || password == null) {
                Error("could not establish Postgres admin connection for ${server.logText()}")
            } else {
                try {
                    Success(
                        DriverManager.getConnection(
                            "jdbc:postgresql://localhost:$it/postgres",
                            "rds",
                            password.secret,
                        ),
                    )
                } catch (e: Exception) {
                    Error<Connection>(e.message ?: "<unknown")
                }
            }
        }
    }
}
