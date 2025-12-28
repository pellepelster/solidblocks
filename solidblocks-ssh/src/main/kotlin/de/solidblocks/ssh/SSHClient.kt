package de.solidblocks.ssh

import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.common.keyprovider.KeyIdentityProvider
import java.io.ByteArrayOutputStream
import java.security.KeyPair
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SSHClient(
    val host: String,
    val keyPair: KeyPair,
    val username: String = "root",
    val port: Int = 22,
) {

    data class SSHCommandResult(val stdOut: String, val stdErr: String, val exitCode: Int)

    @OptIn(ExperimentalTime::class)
    fun sshCommand(
        command: String,
    ): SSHCommandResult {
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

                                return SSHCommandResult(
                                    stdOut.toString("UTF-8"),
                                    stdErr.toString("UTF-8"),
                                    channel.exitStatus
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
