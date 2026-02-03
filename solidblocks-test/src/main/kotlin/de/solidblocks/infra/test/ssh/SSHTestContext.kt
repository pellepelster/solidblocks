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

fun sshTestContext(host: String, privateKey: String, username: String = "root", port: Int = 22) =
    SSHTestContext(host, SSHKeyUtils.loadKey(privateKey), username, port)

fun sshTestContext(host: String, keyPair: KeyPair, username: String = "root", port: Int = 22) =
    SSHTestContext(host, keyPair, username, port)

class SSHTestContext(
    val host: String,
    val keyPair: KeyPair,
    val username: String = "root",
    val port: Int = 22,
) : TestContext() {

  init {
    val openSSHKey = SSHKeyUtils.privateKeyToOpenSsh(keyPair.private)

    val openSSHKeyFile = File.createTempFile("identity", ".key")
    logInfo("writing open ssh test key for host '$host' to '${openSSHKeyFile.absolutePath}'")
    openSSHKeyFile.writeText(openSSHKey)
    Files.setPosixFilePermissions(
        openSSHKeyFile.toPath(),
        PosixFilePermissions.fromString("rw-------"),
    )

    val openSSHConfigFile = File.createTempFile("ssh", ".config")
    logInfo("writing open ssh config for host '$host' to '${openSSHConfigFile.absolutePath}'")
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

    logInfo("run 'ssh -F ${openSSHConfigFile.absolutePath} $username@$host' to access host")
  }

  private val commandManager: SshCommandManager = SshCommandManager(host, keyPair, username, port)

  fun cloudInit() =
      cloudInitTestContext(host, keyPair, username, port).also { testContexts.add(it) }

  fun command(command: String) = commandManager.sshCommand(command)

  fun fileExists(file: String) = commandManager.sshCommand("test -f $file").exitCode == 0

  fun filePermissions(file: String) =
      commandManager.sshCommand("ls -ld $file | awk '{ print \$1; }'").stdout.trim()

  fun download(file: String) = commandManager.download(file)

  fun upload(localFile: Path, remoteFile: String) {
    logInfo("uploading '$localFile' to '$host:$remoteFile'")
    commandManager.upload(localFile, remoteFile)
  }

  override fun cleanUp() {
    commandManager.close()
  }
}
