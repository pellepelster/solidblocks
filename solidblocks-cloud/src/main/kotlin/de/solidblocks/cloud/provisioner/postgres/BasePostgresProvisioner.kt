package de.solidblocks.cloud.provisioner.postgres

import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.LONG_WAIT
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.waitForResult
import de.solidblocks.ssh.SSHClient
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.DriverManager

open class BasePostgresProvisioner {

    private val logger = KotlinLogging.logger {}

    suspend fun SSHProvisionerContext.waitForAdminConnection(server: HetznerServerLookup, superUserPassword: PassSecretLookup, log: LogContext): Result<Connection> = LONG_WAIT.waitForResult {
        log.info("waiting for Postgres admin connection")
        createConnection(server, superUserPassword)
    }

    suspend fun SSHProvisionerContext.createConnection(server: HetznerServerLookup, userPassword: PassSecretLookup, userName: String = "rds"): Result<Connection> {
        val password = this.lookup(userPassword)

        when (val result = this.createOrGetSshClient(server.name)) {
            is Error<SSHClient> -> return Error<Connection>(result.error)
            is Success<SSHClient> -> {
                return result.data.portForward(5432) {
                    if (it == null || password == null) {
                        Error("could not establish Postgres admin connection for ${server.logText()}")
                    } else {
                        try {
                            Success(
                                DriverManager.getConnection(
                                    "jdbc:postgresql://localhost:$it/postgres",
                                    userName,
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
    }
}
