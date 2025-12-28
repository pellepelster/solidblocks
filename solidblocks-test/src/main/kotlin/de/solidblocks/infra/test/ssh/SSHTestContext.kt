package de.solidblocks.infra.test.ssh

import de.solidblocks.ssh.SSHKeyUtils
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

  private val commandManager: SshCommandManager = SshCommandManager(host, keyPair, username, port)

  fun command(command: String) = commandManager.sshCommand(command)
}
