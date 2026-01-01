package de.solidblocks.infra.test.ssh

import de.solidblocks.infra.test.command.CommandResult
import de.solidblocks.infra.test.output.OutputLine
import de.solidblocks.infra.test.output.OutputType
import de.solidblocks.ssh.SSHClient
import java.nio.file.Path
import java.security.KeyPair
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SshCommandManager(
    val host: String,
    val keyPair: KeyPair,
    val username: String = "root",
    val port: Int = 22,
) {

  val sshClient = SSHClient(host, keyPair, username, port)

  @OptIn(ExperimentalTime::class)
  fun sshCommand(
      command: String,
  ): CommandResult<OutputLine> {
    val start = Clock.System.now()
    val result = sshClient.command(command)
    val end = Clock.System.now()

    val output =
        result.stdOut.lines().map { line -> OutputLine(line, OutputType.STDOUT) } +
            result.stdErr.lines().map { line -> OutputLine(line, OutputType.STDERR) }

    return CommandResult(
        result.exitCode,
        end.minus(start),
        output,
    )
  }

  fun download(file: String) = sshClient.download(file)

  fun upload(localFile: Path, remoteFile: String) = sshClient.upload(localFile, remoteFile)
}
