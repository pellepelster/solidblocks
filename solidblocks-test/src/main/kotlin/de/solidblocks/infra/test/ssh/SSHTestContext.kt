package de.solidblocks.infra.test.ssh

import de.solidblocks.infra.test.log
import de.solidblocks.ssh.SSHKeyUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPair


fun sshTestContext(host: String, privateKey: String, username: String = "root", port: Int = 22) =
    SSHTestContext(host, SSHKeyUtils.tryLoadKey(privateKey), username, port)

fun sshTestContext(host: String, keyPair: KeyPair, username: String = "root", port: Int = 22) =
    SSHTestContext(host, keyPair, username, port)

class SSHTestContext(
    val host: String,
    val keyPair: KeyPair,
    val username: String = "root",
    val port: Int = 22,
) {

    init {
        val openSSHKey = SSHKeyUtils.privateKeyToOpenSsh(keyPair.private)

        val openSSHKeyFile = File.createTempFile("identity", ".key")
        log("writing open ssh test key for host '${host}' to '${openSSHKeyFile.absolutePath}'")
        openSSHKeyFile.writeText(openSSHKey)
        Files.setPosixFilePermissions(openSSHKeyFile.toPath(), PosixFilePermissions.fromString("rw-------"))

        val openSSHConfigFile = File.createTempFile("ssh", ".config")
        log("writing open ssh config for host '${host}' to '${openSSHConfigFile.absolutePath}'")
        val sshConfig =
            """
            Host $host
                HostName $host
                User root
                IdentityFile ${openSSHKeyFile.absolutePath}
                StrictHostKeyChecking no
                UserKnownHostsFile /dev/null
        """
                .trimIndent()
        openSSHConfigFile.writeText(sshConfig)

        log("run \"ssh -F '${openSSHConfigFile.absolutePath}' ${username}@${host}\" to access host")
    }

    private val commandManager: SshCommandManager = SshCommandManager(host, keyPair, username, port)

    fun command(command: String) = commandManager.sshCommand(command)

    fun fileExists(file: String) = commandManager.sshCommand("test -f ${file}").exitCode == 0

    fun filePermissions(file: String) = commandManager.sshCommand("ls -ld ${file} | awk '{ print \$1; }'").stdout.trim()

    fun download(file: String) = commandManager.download(file)
}
