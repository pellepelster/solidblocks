package de.solidblocks.ssh

import de.solidblocks.ssh.SSHClient.SSHCommandResult
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.security.KeyPair
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.client.session.ClientSession

class SSHClient(
    val host: String,
    val keyPair: KeyPair,
    val username: String = "root",
    val port: Int = 22,
) : Closeable {

  data class SSHCommandResult(val stdOut: String, val stdErr: String, val exitCode: Int)

  val client = SshClient.setUpDefaultClient().also { it.start() }

  val timeout = Duration.ofSeconds(10)

  var session1: ClientSession? = null

  private fun getSession(): ClientSession {
    if (session1 == null || !session1!!.isOpen) {
      session1 = client.connect(username, host, port).verify(timeout).session
      session1!!.addPublicKeyIdentity(keyPair)
      session1!!.auth().verify(timeout)
    }

    return session1!!
  }

  @OptIn(ExperimentalTime::class)
  fun sshCommand(
      command: String,
  ): SSHCommandResult {
    ByteArrayOutputStream().use { stdErr ->
      ByteArrayOutputStream().use { stdOut ->
        getSession().createExecChannel(command).use { channel ->
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

          return SSHCommandResult(
              stdOut.toString("UTF-8"),
              stdErr.toString("UTF-8"),
              channel.exitStatus,
          )
        }
      }
    }
  }

  override fun close() {
    if (session1 != null && session1!!.isOpen) {
      session1!!.close()
    }
    client.close()
  }
}
