package de.solidblocks.ssh

import de.solidblocks.ssh.ED25519KeyFactory.toJCAPem
import de.solidblocks.ssh.ED25519KeyFactory.toPemString
import io.github.oshai.kotlinlogging.KotlinLogging
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
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.math.BigInteger
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

data class KeyPairPem(val privateKey: String, val publicKey: String)

abstract class SSHKeyFactory {
    private val logger = KotlinLogging.logger {}

    abstract fun generate(): KeyPair

    abstract fun publicKeyToOpenSsh(key: String): String

    abstract fun loadFromPem(key: String): KeyPair?

    fun isEncryptedOpenSSH(key: String) = try {
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

    fun loadFromOpenSSH(key: String) = try {
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

    private fun convertToKeyPair(privateKeyParam: AsymmetricKeyParameter, publicKeyParam: AsymmetricKeyParameter, algorithm: String): KeyPair {
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
        val opensshPrivateKey =
            when (privateKey) {
                // BouncyCastle's OpenSSHPrivateKeyUtil only emits the openssh-key-v1 container for
                // Ed25519 keys; for RSA it falls back to a raw PKCS#1 body that ssh tooling cannot
                // read under an "OPENSSH PRIVATE KEY" header, so we encode the container ourselves.
                is RSAPrivateCrtKeyParameters -> encodeRsaOpenSshV1(privateKey)
                else -> OpenSSHPrivateKeyUtil.encodePrivateKey(privateKey)
            }

        return toPemString("OPENSSH PRIVATE KEY", opensshPrivateKey)
    }

    fun privateKeyToOpenSsh(key: PrivateKey) = privateKeyToOpenSsh(key.toJCAPem())

    fun String.readObject() = PEMParser(StringReader(this)).use { it.readObject() }

    fun String.readPemObject() = PEMParser(StringReader(this)).use { it.readPemObject() }

    fun Ed25519PrivateKeyParameters.toPem(): String {
        val keyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(this)
        return toPemString("PRIVATE KEY", keyInfo.encoded)
    }

    fun Key.toJCAPem() = StringWriter().use { sw ->
        JcaPEMWriter(sw).use { pw -> pw.writeObject(this) }
        sw.toString()
    }

    fun toPemString(type: String, encoded: ByteArray) = StringWriter().use { sw ->
        PemWriter(sw).use { pw -> pw.writeObject(PemObject(type, encoded)) }
        sw.toString()
    }
}

private fun ByteArrayOutputStream.writeSshUInt32(value: Int) {
    write((value ushr 24) and 0xff)
    write((value ushr 16) and 0xff)
    write((value ushr 8) and 0xff)
    write(value and 0xff)
}

private fun ByteArrayOutputStream.writeSshString(bytes: ByteArray) {
    writeSshUInt32(bytes.size)
    write(bytes)
}

private fun ByteArrayOutputStream.writeSshString(value: String) = writeSshString(value.toByteArray(Charsets.UTF_8))

// ssh mpint matches BigInteger's two's-complement big-endian encoding: a leading 0x00 is prepended
// whenever the high bit of the top byte is set, which is exactly what BigInteger.toByteArray() produces.
private fun ByteArrayOutputStream.writeSshMpint(value: BigInteger) = writeSshString(value.toByteArray())

// Encodes an RSA private key into the openssh-key-v1 container (unencrypted, "none" cipher) as
// specified by PROTOCOL.key, so the result is readable by OpenSSH tooling such as ssh-keygen.
private fun encodeRsaOpenSshV1(key: RSAPrivateCrtKeyParameters): ByteArray {
    val publicBlob = ByteArrayOutputStream().apply {
        writeSshString("ssh-rsa")
        writeSshMpint(key.publicExponent)
        writeSshMpint(key.modulus)
    }
        .toByteArray()

    val checkInt = 0
    val privateSection = ByteArrayOutputStream().apply {
        writeSshUInt32(checkInt)
        writeSshUInt32(checkInt)
        writeSshString("ssh-rsa")
        writeSshMpint(key.modulus)
        writeSshMpint(key.publicExponent)
        writeSshMpint(key.exponent)
        writeSshMpint(key.qInv)
        writeSshMpint(key.p)
        writeSshMpint(key.q)
        writeSshString("") // comment
        var pad = 1
        while (size() % 8 != 0) {
            write(pad++)
        }
    }
        .toByteArray()

    return ByteArrayOutputStream().apply {
        write("openssh-key-v1".toByteArray(Charsets.US_ASCII))
        write(0)
        writeSshString("none") // ciphername
        writeSshString("none") // kdfname
        writeSshString("") // kdfoptions
        writeSshUInt32(1) // number of keys
        writeSshString(publicBlob)
        writeSshString(privateSection)
    }
        .toByteArray()
}

fun KeyPair.toPem() = when (this.private) {
    is RSAPrivateKey -> {
        KeyPairPem(
            this.private.toJCAPem(),
            this.public.toJCAPem(),
        )
    }

    else -> {
        KeyPairPem(
            toPemString("PRIVATE KEY", this.private.encoded),
            toPemString("PUBLIC KEY", this.public.encoded),
        )
    }
}
