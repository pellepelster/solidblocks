package de.solidblocks.cloud.api.endpoint

import de.solidblocks.cloud.utils.WaitConfig
import de.solidblocks.cloud.utils.waitForCondition
import de.solidblocks.ssh.SSHClient
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.KeyPair

private val logger = KotlinLogging.logger {}

suspend fun WaitConfig.waitForSSH(endpoint: Endpoint, keyPair: KeyPair, log: LogContext) = this.waitForCondition {
    try {
        log.info("waiting for SSH on endpoint on ${endpoint.serverName} (${endpoint.address}:${endpoint.port})")
        val client = SSHClient(endpoint.address, keyPair, null, port = endpoint.port)
        client.command("whoami").exitCode == 0
    } catch (e: Exception) {
        logger.error(e) {
            "error waiting for '${endpoint.protocol}' endpoint ${endpoint.address}:${endpoint.port}"
        }
        false
    }
}

suspend fun WaitConfig.waitForNoSSH(endpoint: Endpoint, keyPair: KeyPair, log: () -> Unit) = this.waitForCondition {
    try {
        SSHClient(endpoint.address, keyPair, null, port = endpoint.port)
        log.invoke()
        false
    } catch (e: Exception) {
        true
    }
}
