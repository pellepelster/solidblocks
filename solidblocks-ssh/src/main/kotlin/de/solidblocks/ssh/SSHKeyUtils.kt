package de.solidblocks.ssh

import java.security.PrivateKey
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

data class KeyPairRaw(val privateKey: String, val publicKey: String)

object SSHKeyUtils {

  val RSA = RSAKeyFactory

  val ED25519 = ED25519KeyFactory

  init {
    Security.addProvider(BouncyCastleProvider())
  }

  fun tryLoadKey(key: String) =
      listOf(
              { ED25519.loadFromOpenSSH(key) },
              { ED25519.loadFromPem(key) },
              { RSA.loadFromOpenSSH(key) },
              { RSA.loadFromPem(key) },
          )
          .firstNotNullOfOrNull { it.invoke() } ?: throw RuntimeException("could load private key")

  fun privateKeyToOpenSsh(key: PrivateKey): String =
      listOf(
              { ED25519.privateKeyToOpenSsh(key) },
              { RSA.privateKeyToOpenSsh(key) },
          )
          .firstNotNullOfOrNull { it.invoke() }
          ?: throw RuntimeException("could not convert private key")
}
