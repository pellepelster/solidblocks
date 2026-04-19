package de.solidblocks.ssh

import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.bouncycastle.crypto.util.PublicKeyFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.interfaces.EdECKey
import java.security.interfaces.RSAKey
import kotlin.io.encoding.Base64

object SSHKeyUtils {
    val RSA = RSAKeyFactory

    val ED25519 = ED25519KeyFactory

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun isEncrypted(key: String) = listOf(
        { ED25519.isEncryptedOpenSSH(key) },
        { RSA.isEncryptedOpenSSH(key) },
    )
        .mapNotNull { it.invoke() }
        .any { it }

    fun tryLoadKey(key: String) = listOf(
        { ED25519.loadFromOpenSSH(key) },
        { ED25519.loadFromPem(key) },
        { RSA.loadFromOpenSSH(key) },
        { RSA.loadFromPem(key) },
    )
        .firstNotNullOfOrNull { it.invoke() }

    fun loadKey(key: String) = tryLoadKey(key) ?: throw RuntimeException("could load private key")

    fun publicKeyToOpenSSH(publicKey: PublicKey): String {
        val publicKeyParam = PublicKeyFactory.createKey(publicKey.encoded)
        val openSSHBytes = OpenSSHPublicKeyUtil.encodePublicKey(publicKeyParam)

        return when (val algorithm = publicKey.algorithm.lowercase()) {
            "ed25519" -> "ssh-ed25519 ${Base64.encode(openSSHBytes)}"
            "rsa" -> "ssh-rsa ${Base64.encode(openSSHBytes)}"
            else -> throw RuntimeException("unsupported algorithm '$algorithm'")
        }
    }

    fun privateKeyToOpenSsh(key: PrivateKey): String = listOf(
        { ED25519.privateKeyToOpenSsh(key) },
        { RSA.privateKeyToOpenSsh(key) },
    )
        .firstNotNullOfOrNull { it.invoke() }
        ?: throw RuntimeException("could not convert private key")
}

enum class KeyType {
    rsa,
    ed25519,
}

fun KeyPair.keyType() = when (this.private) {
    is RSAKey -> KeyType.rsa
    is EdECKey -> KeyType.ed25519
    else -> throw RuntimeException("unsupported key type '${this.javaClass.name}'")
}
