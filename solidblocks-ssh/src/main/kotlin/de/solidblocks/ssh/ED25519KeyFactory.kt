package de.solidblocks.ssh

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.bouncycastle.crypto.util.PublicKeyFactory
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

object ED25519KeyFactory : SSHKeyFactory() {

    private val logger = KotlinLogging.logger {}

    override fun generate(): KeyPairRaw {
        val generator = KeyPairGenerator.getInstance("Ed25519")
        val keyPair = generator.generateKeyPair()

        val privateKey = keyPair.private
        val publicKey = keyPair.public

        return KeyPairRaw(
            toPemString("PRIVATE KEY", privateKey.encoded),
            toPemString("PUBLIC KEY", publicKey.encoded),
        )
    }

    override fun publicKeyToOpenSsh(key: String): String {
        val pemReader = PemReader(StringReader(key))
        val pemObject = pemReader.readPemObject()

        val factory = KeyFactorySpi.Ed25519()

        val subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(pemObject.content)
        val publicKeyInfo = factory.generatePublic(subjectPublicKeyInfo)

        val publicKey = PublicKeyFactory.createKey(publicKeyInfo.encoded)

        return "ssh-ed25519 " +
                Base64.getEncoder().encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(publicKey))
    }

    override fun loadFromPem(key: String) =
        try {
            val pemObject = key.readObject()

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
            logger.error(e) { "could not load ed25519 key" }
            null
        }

    fun Ed25519PublicKeyParameters.toPem(): String {
        val keyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(this)
        return toPemString("PUBLIC KEY", keyInfo.encoded)
    }
}
