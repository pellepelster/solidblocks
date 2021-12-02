package de.solidblocks.base

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.SecureRandom
import java.util.Base64

class Utils {
    companion object {

        fun generateSshKey(name: String): Pair<String, String> {

            val generator = Ed25519KeyPairGenerator()
            generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
            val pair = generator.generateKeyPair()
            val publicKey =
                "ssh-ed25519 " + Base64.getEncoder().encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(pair.public))

            val content = OpenSSHPrivateKeyUtil.encodePrivateKey(pair.private)
            val o = PemObject("OPENSSH PRIVATE KEY", content)

            val sw = StringWriter()
            val w = PemWriter(sw)
            w.writeObject(o)
            w.close()

            return sw.toString() to publicKey
        }
    }
}
