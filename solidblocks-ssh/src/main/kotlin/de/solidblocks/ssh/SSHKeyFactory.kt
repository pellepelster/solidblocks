package de.solidblocks.ssh

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.StringReader
import java.io.StringWriter
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.params.*
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter

abstract class SSHKeyFactory {

  private val logger = KotlinLogging.logger {}

  abstract fun generate(): KeyPairRaw

  abstract fun publicKeyToOpenSsh(key: String): String

  abstract fun loadFromPem(key: String): KeyPair?

  fun isEncryptedOpenSSH(key: String) =
      try {
        val key = OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(key.readPemObject()?.content)
        if (key != null) {
          false
        } else {
          null
        }
      } catch (e: IllegalStateException) {
        e.message == "encrypted keys not supported"
      } catch (e: Exception) {
        null
      }

  fun loadFromOpenSSH(key: String) =
      try {
        val privateKey = OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(key.readPemObject().content)
        privateKey.toKeyPair()
      } catch (e: Exception) {
        logger.warn { "could not load private key" }
        null
      }

  fun AsymmetricKeyParameter.toKeyPair(): KeyPair {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(BouncyCastleProvider())
    }

    return when (this) {
      is Ed25519PrivateKeyParameters -> {
        val publicKey = this.generatePublicKey()
        convertToKeyPair(this, publicKey, "Ed25519")
      }

      is Ed25519PublicKeyParameters -> {
        throw IllegalArgumentException("Cannot create KeyPair from public key only")
      }

      is RSAPrivateCrtKeyParameters -> {
        val publicKey = RSAKeyParameters(false, this.modulus, this.publicExponent)
        convertToKeyPair(this, publicKey, "RSA")
      }

      is ECPrivateKeyParameters -> {
        val publicKey = this.parameters.g.multiply(this.d)
        val ecPublicKey = ECPublicKeyParameters(publicKey, this.parameters)
        convertToKeyPair(this, ecPublicKey, "EC")
      }

      else -> throw IllegalArgumentException("Unsupported key type: ${this.javaClass}")
    }
  }

  private fun convertToKeyPair(
      privateKeyParam: AsymmetricKeyParameter,
      publicKeyParam: AsymmetricKeyParameter,
      algorithm: String,
  ): KeyPair {
    val keyFactory = KeyFactory.getInstance(algorithm, "BC")

    val privateKeyInfo: PrivateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(privateKeyParam)
    val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyInfo.encoded))

    val publicKeyInfo: SubjectPublicKeyInfo =
        SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKeyParam)
    val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyInfo.encoded))

    return KeyPair(publicKey, privateKey)
  }

  fun privateKeyToOpenSsh(key: String): String {
    val privateKeyInfo =
        when (val pemObject = key.readObject()) {
          is PrivateKeyInfo -> pemObject
          is PEMKeyPair -> pemObject.privateKeyInfo
          else ->
              throw IllegalArgumentException("Invalid PEM format '${pemObject?.javaClass!!.name}'")
        }

    val privateKey = PrivateKeyFactory.createKey(privateKeyInfo)
    val opensshPrivateKey = OpenSSHPrivateKeyUtil.encodePrivateKey(privateKey)

    return toPemString("OPENSSH PRIVATE KEY", opensshPrivateKey)
  }

  fun privateKeyToOpenSsh(key: PrivateKey) = privateKeyToOpenSsh(key.toPem())

  fun String.readObject() = PEMParser(StringReader(this)).use { it.readObject() }

  fun String.readPemObject() = PEMParser(StringReader(this)).use { it.readPemObject() }

  fun Ed25519PrivateKeyParameters.toPem(): String {
    val keyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(this)
    return toPemString("PRIVATE KEY", keyInfo.encoded)
  }

  fun Key.toPem() =
      StringWriter().use { sw ->
        JcaPEMWriter(sw).use { pw -> pw.writeObject(this) }
        sw.toString()
      }

  fun toPemString(type: String, encoded: ByteArray) =
      StringWriter().use { sw ->
        PemWriter(sw).use { pw -> pw.writeObject(PemObject(type, encoded)) }
        sw.toString()
      }
}
