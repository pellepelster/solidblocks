package de.solidblocks.ssh

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.net.ServerSocket
import java.nio.file.Path
import java.security.KeyPair
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.common.keyprovider.KeyIdentityProvider
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.apache.sshd.scp.client.ScpClientCreator

class SSHClient(
    val host: String,
    val keyPair: KeyPair,
    val username: String = "root",
    val port: Int = 22,
) : Closeable {

  data class SSHCommandResult(val stdOut: String, val stdErr: String, val exitCode: Int)

  val client =
      SshClient.setUpDefaultClient().also {
        it.serverKeyVerifier = TrustAllKeyVerifier()
        it.keyIdentityProvider = KeyIdentityProvider.wrapKeyPairs(keyPair)
        it.start()
      }

  val timeout = Duration.ofSeconds(10)

  var session =
      client.connect(username, host, port).verify(timeout).session.also {
        it.addPublicKeyIdentity(keyPair)
        it.auth().verify(timeout)
      }

  fun download(file: String): ByteArray? {
    val creator = ScpClientCreator.instance()
    val scpClient = creator.createScpClient(session)
    return scpClient.downloadBytes(file)
  }

  fun upload(localFile: Path, remoteFile: String) {
    val creator = ScpClientCreator.instance()
    val scpClient = creator.createScpClient(session)
    return scpClient.upload(localFile, remoteFile)
  }

  @OptIn(ExperimentalTime::class)
  fun command(command: String): SSHCommandResult {
    ByteArrayOutputStream().use { stdErr ->
      ByteArrayOutputStream().use { stdOut ->
        session.createExecChannel(command).use { channel ->
          val start = Clock.System.now()
          channel.out = stdOut
          channel.err = stdErr
          channel.open().verify(timeout)

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

  fun findAvailablePort(): Int {
    ServerSocket(0).use { socket ->
      return socket.localPort
    }
  }

  suspend fun <T> portForward(
      remotePort: Int,
      localPort: Int? = null,
      block: suspend (Int) -> T,
  ): T {
    val port = localPort ?: findAvailablePort()
    session.createLocalPortForwardingTracker(port, SshdSocketAddress("localhost", remotePort)).use {
      return block.invoke(port)
    }
  }

  override fun close() {
    session.close()
    client.close()
  }
}
