package de.solidblocks.cloud.model

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.util.*

fun generateSshKey(name: String): Pair<String, String> {

    val generator = Ed25519KeyPairGenerator()
    generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
    val keyPair = generator.generateKeyPair()

    val publicKey =
        "ssh-ed25519 " + Base64.getEncoder().encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(keyPair.public))

    val content = OpenSSHPrivateKeyUtil.encodePrivateKey(keyPair.private)
    val privatePemObject = PemObject("OPENSSH PRIVATE KEY", content)

    return pemToString(privatePemObject) to publicKey
}

fun generateRsaKeyPair(): Pair<String, String> {
    Security.addProvider(BouncyCastleProvider())
    val generator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")

    generator.initialize(4096, SecureRandom())

    val keyPair = generator.generateKeyPair()

    val privateKey = keyPair.private as BCRSAPrivateCrtKey
    val publicKey = keyPair.public as BCRSAPublicKey

    val publicPemObject = PemObject("RSA PUBLIC KEY", publicKey.encoded)
    val privatePemObject = PemObject("RSA PRIVATE KEY", privateKey.encoded)

    return pemToString(privatePemObject) to pemToString(publicPemObject)
}

fun pemToString(pemObject: PemObject): String {
    val str = StringWriter()

    val writer = PemWriter(str)
    writer.writeObject(pemObject)
    writer.flush()

    return str.toString()
}
