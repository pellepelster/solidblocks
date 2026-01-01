package de.solidblocks.infra.test.cloudinit

import de.solidblocks.infra.test.TestContext
import de.solidblocks.infra.test.ssh.sshTestContext
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogSource
import de.solidblocks.utils.logError
import de.solidblocks.utils.logInfo
import de.solidblocks.utils.logWarning
import kotlinx.serialization.json.Json
import java.security.KeyPair

fun cloudInitTestContext(
    host: String,
    privateKey: String,
    username: String = "root",
    port: Int = 22,
) = CloudInitTestContext(host, SSHKeyUtils.tryLoadKey(privateKey), username, port)

fun cloudInitTestContext(
    host: String,
    keyPair: KeyPair,
    username: String = "root",
    port: Int = 22,
) = CloudInitTestContext(host, keyPair, username, port)

class CloudInitTestContext(
    val host: String,
    val keyPair: KeyPair,
    val username: String = "root",
    val port: Int = 22,
) : TestContext(-99) {

    companion object {
        val json = Json { this.ignoreUnknownKeys = true }
    }

    val ssh = sshTestContext(host, keyPair, username, port).also { testContexts.add(it) }

    fun isFinished() =
        try {
            logInfo("checking for existence of '/var/lib/cloud/instance/boot-finished' on host '$host'")
            ssh.command("test -f /var/lib/cloud/instance/boot-finished").exitCode == 0
        } catch (e: Exception) {
            logWarning("failed to retrieve cloud init finished marker")
            false
        }

    fun result(): CloudInitResultWrapper? =
        ssh.command("cat /var/lib/cloud/data/status.json").stdout.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                logError("failed to deserialize cloud-init status")
                null
            }
        }

    fun outputLog() = ssh.command("cat /var/log/cloud-init-output.log").stdout

    fun printOutputLog() {
        outputLog().lines().forEach { logInfo(it, LogSource.CLOUDINIT) }
    }

    fun fileExists(file: String) = ssh.command("test -f $file").exitCode == 0

    var printOutputLogOnTestFailure = false

    fun printOutputLogOnTestFailure() {
        this.printOutputLogOnTestFailure = true
    }

    override fun afterAll() {
        if (printOutputLogOnTestFailure) {
            try {
                printOutputLog()
            }catch (e: Exception) {
                logError("failed fetch cloud-init output log")
            }
        }
    }
}
