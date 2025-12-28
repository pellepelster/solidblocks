package de.solidblocks.ssh

import java.net.SocketAddress
import java.security.PublicKey
import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.client.session.ClientSession

class TrustAllKeyVerifier : ServerKeyVerifier {
  override fun verifyServerKey(
      clientSession: ClientSession,
      socketAddress: SocketAddress,
      publicKey: PublicKey,
  ): Boolean = true
}
