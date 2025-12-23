package de.solidblocks.infra.test.ssh

import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.client.session.ClientSession
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil.generateKeyFingerprint
import java.net.SocketAddress
import java.security.PublicKey

class BlcksKeyVerifier : ServerKeyVerifier {
    override fun verifyServerKey(
        clientSession: ClientSession,
        socketAddress: SocketAddress,
        publicKey: PublicKey
    ): Boolean {

        if (publicKey is BCECPublicKey) {
            val fingerprint = generateKeyFingerprint(publicKey.q, publicKey.parameters)
            // TODO use ssh host identity
        }

        return true
    }
}