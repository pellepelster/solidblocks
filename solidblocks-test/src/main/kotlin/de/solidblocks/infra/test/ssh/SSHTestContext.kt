package de.solidblocks.infra.test.ssh

import de.solidblocks.infra.test.TestContext
import de.solidblocks.infra.test.cloudinit.cloudInitTestContext
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.logInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPair

fun sshTestContext(
    host: String,
    privateKey: String,
    username: String = "root",
    port: Int = 22,
    testId: String? = null,
) = SSHTestContext(host, SSHKeyUtils.loadKey(privateKey), username, port, testId)

fun sshTestContext(
    host: String,
    keyPair: KeyPair,
    username: String = "root",
    port: Int = 22,
    testId: String? = null,
): SSHTestContext = SSHTestContext(host, keyPair, username, port, testId)

class SSHTestContext(val host: String, val keyPair: KeyPair, val username: String = "root", val port: Int = 22, testId: String? = null) : TestContext(testId) {

    private val commandManager: SshCommandManager = SshCommandManager(host, keyPair, null, username, port)

    fun cloudInit() = cloudInitTestContext(host, keyPair, username, port).also { testContexts.add(it) }

    fun command(command: String) = commandManager.sshCommand(command)

    fun fileExists(file: String) = commandManager.sshCommand("test -f $file").exitCode == 0

    fun filePermissions(file: String) = commandManager.sshCommand("ls -ld $file | awk '{ print \$1; }'").stdout.trim()

    fun download(file: String) = commandManager.download(file)

    fun upload(localFile: Path, remoteFile: String) {
        logInfo("uploading '$localFile' to '$host:$remoteFile'")
        commandManager.upload(localFile, remoteFile)
    }

    override fun cleanUp() {
        commandManager.close()
    }
}
