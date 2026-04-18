package de.solidblocks.ssh

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier
import org.apache.sshd.client.keyverifier.RequiredServerKeyVerifier
import org.apache.sshd.common.keyprovider.KeyIdentityProvider
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.apache.sshd.scp.client.ScpClientCreator
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.net.ServerSocket
import java.nio.file.Path
import java.security.KeyPair
import java.security.PublicKey
import java.time.Duration
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class SSHClient(val host: String, val keyPair: KeyPair, val hostKey: PublicKey?, val username: String = "root", val port: Int = 22) : Closeable {
    private val logger = KotlinLogging.logger {}

    data class SSHCommandResult(val stdOut: String, val stdErr: String, val exitCode: Int)

    init {
        logger.info { "creating SSH client for $host:$port" }
    }

    val client =
        SshClient.setUpDefaultClient().also {
            it.serverKeyVerifier = if (hostKey == null) {
                AcceptAllServerKeyVerifier.INSTANCE
            } else {
                RequiredServerKeyVerifier(hostKey)
            }
            it.keyIdentityProvider = KeyIdentityProvider.wrapKeyPairs(keyPair)
            it.start()
        }

    val verifyTimeout = Duration.ofSeconds(10)

    var session =
        client.connect(username, host, port).verify(verifyTimeout).session.also {
            it.addPublicKeyIdentity(keyPair)
            it.auth().verify(verifyTimeout)
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
    fun command(command: String, timeout: kotlin.time.Duration = 1.minutes): SSHCommandResult {
        ByteArrayOutputStream().use { stdErr ->
            ByteArrayOutputStream().use { stdOut ->
                session.createExecChannel(command).use { channel ->
                    val start = Clock.System.now()
                    channel.out = stdOut
                    channel.err = stdErr
                    channel.open().verify(verifyTimeout)

                    channel.waitFor(
                        EnumSet.of(ClientChannelEvent.CLOSED),
                        timeout.inWholeMilliseconds,
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

    suspend fun <T> portForward(remotePort: Int, localPort: Int? = null, block: suspend (Int) -> T): T {
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
