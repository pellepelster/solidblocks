package de.solidblocks.infra.test.ssh

import de.solidblocks.infra.test.command.CommandResult
import de.solidblocks.infra.test.output.OutputLine
import de.solidblocks.infra.test.output.OutputType
import java.io.ByteArrayOutputStream
import java.security.KeyPair
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.common.keyprovider.KeyIdentityProvider

class SshCommandManager(
    val host: String,
    val keyPair: KeyPair,
    val username: String = "root",
    val port: Int = 22,
) {

  @OptIn(ExperimentalTime::class)
  fun sshCommand(
      command: String,
  ): CommandResult<OutputLine> {
    val timeout = Duration.ofSeconds(10)

    try {
      // TODO use verified host keys
      SshClient.setUpDefaultClient().use {
        it.serverKeyVerifier = TrustAllKeyVerifier()
        it.keyIdentityProvider = KeyIdentityProvider.wrapKeyPairs(keyPair)
        it.start()

        it.connect(username, host, port).verify(timeout).session.use { session ->
          session.addPublicKeyIdentity(keyPair)
          session.auth().verify(timeout)

          ByteArrayOutputStream().use { stdErr ->
            ByteArrayOutputStream().use { stdOut ->
              session.createExecChannel(command).use { channel ->
                val start = Clock.System.now()
                channel.out = stdOut
                channel.err = stdErr
                channel.open().verify(timeout)

                /*
                responseStream.reset()
                channel.invertedIn.use { pipedIn ->
                    pipedIn.write(command.toByteArray())
                    pipedIn.flush()
                }*/

                channel.waitFor(
                    EnumSet.of(ClientChannelEvent.CLOSED),
                    TimeUnit.SECONDS.toMillis(10),
                )

                val end = Clock.System.now()

                val output =
                    stdOut.toString("UTF-8").lines().map { line ->
                      OutputLine(line, OutputType.STDOUT)
                    } +
                        stdErr.toString("UTF-8").lines().map { line ->
                          OutputLine(line, OutputType.STDERR)
                        }

                return CommandResult<OutputLine>(
                    channel.exitStatus,
                    end.minus(start),
                    output,
                )
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      throw e
    }
  }
}
