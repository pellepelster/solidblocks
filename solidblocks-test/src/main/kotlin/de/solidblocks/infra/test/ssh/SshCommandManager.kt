package de.solidblocks.infra.test.ssh

import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.common.keyprovider.KeyIdentityProvider
import java.io.ByteArrayOutputStream
import java.security.KeyPair
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit


class SshCommandManager(
    val host: String,
    val keyPair: KeyPair,
    val username: String = "root",
    val port: Int = 22
) {

    fun sshCommand(
        command: String
    ): SshCommandResult {
        val timeout = Duration.ofSeconds(10)

        try {
            // TODO use verified host keys
            SshClient.setUpDefaultClient().use {
                it.serverKeyVerifier = BlcksKeyVerifier()
                it.keyIdentityProvider = KeyIdentityProvider.wrapKeyPairs(keyPair)
                it.start()

                it.connect(username, host, port).verify(timeout).session.use { session ->
                    session.addPublicKeyIdentity(keyPair)
                    session.auth().verify(timeout)

                    ByteArrayOutputStream().use { stderr ->
                        ByteArrayOutputStream().use { stdOut ->

                            session.createExecChannel(command).use { channel ->
                                channel.out = stdOut
                                channel.err = stderr
                                channel.open().verify(timeout)

                                /*
                                responseStream.reset()
                                channel.invertedIn.use { pipedIn ->
                                    pipedIn.write(command.toByteArray())
                                    pipedIn.flush()
                                }*/

                                channel.waitFor(
                                    EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.SECONDS.toMillis(10)
                                )

                                return SshCommandResult(
                                    channel.exitStatus == 0,
                                    String(stdOut.toByteArray()).trim(),
                                    String(stderr.toByteArray()).trim(),
                                    null
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return SshCommandResult(false, error = e)
        }
    }
}

