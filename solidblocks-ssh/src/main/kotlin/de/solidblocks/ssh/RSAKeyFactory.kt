package de.solidblocks.ssh

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.StringReader
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.X509EncodedKeySpec
import java.util.*
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.bouncycastle.crypto.util.PublicKeyFactory
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.util.io.pem.PemReader

object RSAKeyFactory : SSHKeyFactory() {

  private val logger = KotlinLogging.logger {}

  override fun generate(): KeyPairRaw {
    val generator = KeyPairGenerator.getInstance("RSA", "BC").also { it.initialize(4096) }

    val keyPair = generator.generateKeyPair()
    return KeyPairRaw(keyPair.private.toPem(), keyPair.public.toPem())
  }

  override fun publicKeyToOpenSsh(key: String): String {
    val factory = KeyFactory.getInstance("RSA", "BC")

    val pemReader = PemReader(StringReader(key))
    val pemObject = pemReader.readPemObject()

    val pubKeySpec = X509EncodedKeySpec(pemObject.content)
    val publicKey = factory.generatePublic(pubKeySpec)

    val bpuv = PublicKeyFactory.createKey(publicKey.encoded)

    return "ssh-rsa " +
        Base64.getEncoder().encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(bpuv))
  }

  override fun loadFromPem(key: String) =
      try {
        val converter = JcaPEMKeyConverter()

        when (val pemObject = key.readObject()) {
          is PEMKeyPair -> converter.getKeyPair(pemObject)
          else -> null
        }
      } catch (e: Exception) {
        logger.error(e) { "could not load rsa key" }
        null
      }
}
