package de.solidblocks.ssh

import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.*
import org.bouncycastle.crypto.util.*
import org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringReader
import java.io.StringWriter
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*


object SSHKeyUtils {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    data class KeyPairRaw(val privateKey: String, val publicKey: String)

    fun generateED25519KeyPair(): KeyPairRaw {
        val generator = Ed25519KeyPairGenerator().apply {
            init(Ed25519KeyGenerationParameters(SecureRandom()))
        }

        val keyPair = generator.generateKeyPair();
        val privateKey = keyPair.private as Ed25519PrivateKeyParameters
        val publicKey = keyPair.public as Ed25519PublicKeyParameters


        return KeyPairRaw(privateKey.toPem(), publicKey.toPem())
    }


    fun generateRsaKeyPair(): KeyPairRaw {
        val generator = KeyPairGenerator.getInstance("RSA", "BC").also {
            it.initialize(4096)
        }

        val keyPair = generator.generateKeyPair()
        return KeyPairRaw(keyPair.private.toPEM(), keyPair.public.toPEM())
    }

    private fun Ed25519PrivateKeyParameters.toPem(): String {
        val keyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(this)
        return toPemString("PRIVATE KEY", keyInfo.encoded)
    }

    private fun Ed25519PublicKeyParameters.toPem(): String {
        val keyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(this)
        return toPemString("PUBLIC KEY", keyInfo.encoded)
    }

    private fun toPemString(type: String, encoded: ByteArray) = StringWriter().use { sw ->
        PemWriter(sw).use { pw ->
            pw.writeObject(PemObject(type, encoded))
        }
        sw.toString()
    }

    private fun Key.toPEM() = StringWriter().use { sw ->
        JcaPEMWriter(sw).use { pw ->
            pw.writeObject(this)
        }
        sw.toString()
    }

    fun publicED25519KeyToSsh(publicKeyPem: String): String {
        val pemReader = PemReader(StringReader(publicKeyPem))
        val pemObject = pemReader.readPemObject()

        val factory = KeyFactorySpi.Ed25519()

        val subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(pemObject.content)
        val publicKeyInfo = factory.generatePublic(subjectPublicKeyInfo)

        val publicKey = PublicKeyFactory.createKey(publicKeyInfo.encoded)

        return "ssh-ed25519 " + Base64.getEncoder()
            .encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(publicKey))
    }


    fun publicRsaKeyToSsh(publicKeyPem: String): String {

        val factory = KeyFactory.getInstance("RSA", "BC")

        val pemReader = PemReader(StringReader(publicKeyPem))
        val pemObject = pemReader.readPemObject()

        val pubKeySpec = X509EncodedKeySpec(pemObject.content)
        val publicKey = factory.generatePublic(pubKeySpec)

        val bpuv = PublicKeyFactory.createKey(publicKey.encoded)

        return "ssh-rsa " + Base64.getEncoder()
            .encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(bpuv))
    }

    fun tryLoadKey(key: String) =
        listOf(
            { loadEd25519KeyPairFromOpenSSH(key) },
            { loadEd25519KeyPairFromPem(key) },
            { loadRSAKeyPairFromOpenSSH(key) },
            { loadRSAKeyPairFromPem(key) },
            { loadECDSAKeyPairFromPem(key) },
        ).firstNotNullOfOrNull { it.invoke() } ?: throw RuntimeException("could load private key")

    fun loadRSAKeyPairFromPem(pemKey: String) =
        try {
            val pemParser = PEMParser(StringReader(pemKey))
            val pemObject = pemParser.readObject()
            pemParser.close()

            val converter = JcaPEMKeyConverter()

            when (pemObject) {
                is PEMKeyPair -> converter.getKeyPair(pemObject)
                else -> null
            }
        } catch (e: Exception) {
            null
        }

    fun loadECDSAKeyPairFromPem(pemKey: String) =
        try {
            val pemParser = PEMParser(StringReader(pemKey))
            val pemObject = pemParser.readObject()
            pemParser.close()

            val converter = JcaPEMKeyConverter()

            when (pemObject) {
                is PEMKeyPair -> converter.getKeyPair(pemObject)
                else -> null
            }
        } catch (e: Exception) {
            null
        }

    fun loadEd25519KeyPairFromPem(key: String) =
        try {
            val pemParser = PEMParser(StringReader(key))
            val pemObject = pemParser.readObject()
            pemParser.close()

            val converter = JcaPEMKeyConverter().setProvider("BC")

            val privateKey: PrivateKey =
                when (pemObject) {
                    is PrivateKeyInfo -> converter.getPrivateKey(pemObject)
                    is PEMKeyPair -> converter.getPrivateKey(pemObject.privateKeyInfo)
                    else -> throw IllegalArgumentException("Invalid PEM format: ${pemObject.javaClass}")
                }

            val keyFactory = KeyFactory.getInstance("Ed25519", "BC")
            val privateKeySpec = PKCS8EncodedKeySpec(privateKey.encoded)
            val regeneratedPrivateKey = keyFactory.generatePrivate(privateKeySpec)

            val bcPrivateKey =
                Ed25519PrivateKeyParameters(
                    privateKey.encoded.takeLast(32).toByteArray(),
                    0,
                )
            val bcPublicKey = bcPrivateKey.generatePublicKey()

            val publicKeySpec =
                X509EncodedKeySpec(
                    SubjectPublicKeyInfo(
                        AlgorithmIdentifier(
                            EdECObjectIdentifiers.id_Ed25519,
                        ),
                        bcPublicKey.encoded,
                    )
                        .encoded,
                )
            val publicKey = keyFactory.generatePublic(publicKeySpec)

            KeyPair(publicKey, regeneratedPrivateKey)
        } catch (e: Exception) {
            null
        }

    fun loadEd25519KeyPairFromOpenSSH(key: String) =
        try {
            val pemObject = PemReader(StringReader(key)).readPemObject()
            val privateKey = OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(pemObject.content)
            privateKey.toKeyPair()
        } catch (e: Exception) {
            null
        }

    fun loadRSAKeyPairFromOpenSSH(key: String) =
        try {
            val pemObject = PemReader(StringReader(key)).readPemObject()
            val privateKey = OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(pemObject.content)
            privateKey.toKeyPair()
        } catch (e: Exception) {
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
}
