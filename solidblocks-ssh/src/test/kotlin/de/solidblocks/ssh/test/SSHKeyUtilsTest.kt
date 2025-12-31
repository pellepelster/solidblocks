package de.solidblocks.ssh.test

import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.ssh.SSHKeyUtils.tryLoadKey
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import java.nio.file.Files
import kotlin.io.path.writeText
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

class SSHKeyUtilsTest {

  @Test
  fun testGenerateRsaKeyPair() {
    assertSoftly(SSHKeyUtils.RSA.generate()) {
      it.privateKey shouldStartWith "-----BEGIN RSA PRIVATE KEY-----"
      it.privateKey shouldEndWith "-----END RSA PRIVATE KEY-----\n"
      it.publicKey shouldStartWith "-----BEGIN PUBLIC KEY-----"
      it.publicKey shouldEndWith "-----END PUBLIC KEY-----\n"

      SSHKeyUtils.RSA.publicKeyToOpenSsh(it.publicKey) shouldStartWith "ssh-rsa AAA"
      SSHKeyUtils.RSA.privateKeyToOpenSsh(it.privateKey) shouldStartWith
          "-----BEGIN OPENSSH PRIVATE KEY-----"

      val key = tryLoadKey(it.privateKey)
      key shouldNotBe null
      SSHKeyUtils.RSA.privateKeyToOpenSsh(key.private) shouldStartWith
          "-----BEGIN OPENSSH PRIVATE KEY-----"
    }
  }

  @Test
  fun testGenerateED25519KeyPair() {
    assertSoftly(SSHKeyUtils.ED25519.generate()) {
      it.privateKey shouldStartWith "-----BEGIN PRIVATE KEY-----"
      it.privateKey shouldEndWith "-----END PRIVATE KEY-----\n"
      it.publicKey shouldStartWith "-----BEGIN PUBLIC KEY-----"
      it.publicKey shouldEndWith "-----END PUBLIC KEY-----\n"

      SSHKeyUtils.ED25519.publicKeyToOpenSsh(it.publicKey) shouldStartWith "ssh-ed25519 AAA"
      SSHKeyUtils.ED25519.privateKeyToOpenSsh(it.privateKey) shouldStartWith
          "-----BEGIN OPENSSH PRIVATE KEY-----"

      val key = tryLoadKey(it.privateKey)
      key shouldNotBe null
      SSHKeyUtils.ED25519.privateKeyToOpenSsh(key.private) shouldStartWith
          "-----BEGIN OPENSSH PRIVATE KEY-----"
    }
  }

  val privateKeyOpensshEd25519 =
      """
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtz
c2gtZWQyNTUxOQAAACBXBwhdauwPrBhr/qOmL3BO4+0QH+ix7qImiUw9vjzjbwAA
AIh6c26senNurAAAAAtzc2gtZWQyNTUxOQAAACBXBwhdauwPrBhr/qOmL3BO4+0Q
H+ix7qImiUw9vjzjbwAAAEAZ9Z5VRUkYFSWWL6l8ECH9QEMSjltl5JSoaQ4zaccl
gFcHCF1q7A+sGGv+o6YvcE7j7RAf6LHuoiaJTD2+PONvAAAAAAECAwQF
-----END OPENSSH PRIVATE KEY-----
    """
          .trimIndent()

  val privateKeyPemEd25519 =
      """
-----BEGIN PRIVATE KEY-----
MC4CAQAwBQYDK2VwBCIEIBn1nlVFSRgVJZYvqXwQIf1AQxKOW2XklKhpDjNpxyWA
-----END PRIVATE KEY-----
    """
          .trimIndent()

  val privateKeyOpensshRsa =
      """
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAABFwAAAAdz
c2gtcnNhAAAAAwEAAQAAAQEA1aqOBDfzcJ+hquvudhIiEwEO4U0g2fEhJdLTSoTD
eqmLk+ti4HKoJ4SFhsEw+IdkCVJGPI+c5nTIa7hy05lRW9kmJ9+/aQAjDqZdLGmR
tHjTF2SSL7Qzfzo5tKiTjGzCvAQMP3oNZ2aL041hZu/xxVLDGF36Ur9ISnvY4GbP
VYZbJCQXrB7H6oyPUCAgnRPOWnURUj35IcVV4KtibyZd5M4VN9TOzJz4wX1jf5Lm
petdvOcrBxSix5oCQ6pm0BZ1vfSGERuAl3RVjrwBqj+yrbDGZd5uX5Cg06QZeLuU
doE5B/zFwfRPGtSBZXM9tDJtByEYPt0Pu4bDNPtUIbHY/QAAA7jOqAzJzqgMyQAA
AAdzc2gtcnNhAAABAQDVqo4EN/Nwn6Gq6+52EiITAQ7hTSDZ8SEl0tNKhMN6qYuT
62LgcqgnhIWGwTD4h2QJUkY8j5zmdMhruHLTmVFb2SYn379pACMOpl0saZG0eNMX
ZJIvtDN/Ojm0qJOMbMK8BAw/eg1nZovTjWFm7/HFUsMYXfpSv0hKe9jgZs9Vhlsk
JBesHsfqjI9QICCdE85adRFSPfkhxVXgq2JvJl3kzhU31M7MnPjBfWN/kual6128
5ysHFKLHmgJDqmbQFnW99IYRG4CXdFWOvAGqP7KtsMZl3m5fkKDTpBl4u5R2gTkH
/MXB9E8a1IFlcz20Mm0HIRg+3Q+7hsM0+1Qhsdj9AAAAAwEAAQAAAQAORE0nSvUe
WApbd1V83MkZq8BqmtPOuaMU+3bQSv0ie5+uSNFZW06PFPI1hUDX13J+jNfTw2Me
oD2hs7c3Gc2s7Fr33qDRSgkNOV6PUJ1CB69QqI56UPX/UMv5nCf+AGUgWMTYmDW8
6cP8ZDxUu0DRhC0yu1OzosIY9xwMH9FITIkSP8ZUCVAw+BhvPWLDLE8m+R2lcd81
5bp5TdU8c8zrF/II1zSUeBUHlOCcT81GY+/+yQM+Tj5r0NGswcaXMSyYwI1guPWd
KQOzIvSrtKTaSq2RPJu5mqJ5CJ9qQFVhiJIcYGKnANTVu5RfdG5rqC8GcN4CzI7t
5wfgEtFoQmqhAAAAgQC+vsie+ZrH5hveuFIz7OllIs+sH6CUu9XXivA2yj1g5OWZ
l0kvSsJ8gMOJwVma2JLApAOxtGwcyAr0EINnUtKBq8kWOBAFs7HQy69JCHsR11K5
EXuGCqsMf9S03Kvu7LqFVokuYMy0Y4ItdpRGZsSUXRcEKOE8TBSsfhLrdlUlCAAA
AIEA3ayt5pS/dqkKHbVK99m/uZgdKcQwiczIgKOOBNwW7Sa6Lg502Sto3UiN/xvf
ECWGHDzn0VE94Tt7ucmi+k4TeT2Wh0RGZrC9IOOihXsqpdhjSjzHZZ0t3YNTrk1s
koDPCWTVVO26Li12HEmeBs8YiLE0UH4KxcaZNBEKqfgtuicAAACBAPbAa9yghPqK
gH1YCP2sc00yLnMa82wRxg3smXkNnho50K2M/OSRBMA7C+3p/xgg2eJV0V32Vj7a
UeklHikO5Ue86Cq33o8DWCZioxcsMoB+R+CNlZLtEgb1zsEdSHog316d3CX7zctd
qo6Vd7y9ZvTL3nrJGGONniC+OZZTab47AAAAAAEC
-----END OPENSSH PRIVATE KEY-----
    """
          .trimIndent()

  val privateKeyPemRsa =
      """
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA1aqOBDfzcJ+hquvudhIiEwEO4U0g2fEhJdLTSoTDeqmLk+ti
4HKoJ4SFhsEw+IdkCVJGPI+c5nTIa7hy05lRW9kmJ9+/aQAjDqZdLGmRtHjTF2SS
L7Qzfzo5tKiTjGzCvAQMP3oNZ2aL041hZu/xxVLDGF36Ur9ISnvY4GbPVYZbJCQX
rB7H6oyPUCAgnRPOWnURUj35IcVV4KtibyZd5M4VN9TOzJz4wX1jf5LmpetdvOcr
BxSix5oCQ6pm0BZ1vfSGERuAl3RVjrwBqj+yrbDGZd5uX5Cg06QZeLuUdoE5B/zF
wfRPGtSBZXM9tDJtByEYPt0Pu4bDNPtUIbHY/QIDAQABAoIBAA5ETSdK9R5YClt3
VXzcyRmrwGqa0865oxT7dtBK/SJ7n65I0VlbTo8U8jWFQNfXcn6M19PDYx6gPaGz
tzcZzazsWvfeoNFKCQ05Xo9QnUIHr1CojnpQ9f9Qy/mcJ/4AZSBYxNiYNbzpw/xk
PFS7QNGELTK7U7Oiwhj3HAwf0UhMiRI/xlQJUDD4GG89YsMsTyb5HaVx3zXlunlN
1TxzzOsX8gjXNJR4FQeU4JxPzUZj7/7JAz5OPmvQ0azBxpcxLJjAjWC49Z0pA7Mi
9Ku0pNpKrZE8m7maonkIn2pAVWGIkhxgYqcA1NW7lF90bmuoLwZw3gLMju3nB+AS
0WhCaqECgYEA3ayt5pS/dqkKHbVK99m/uZgdKcQwiczIgKOOBNwW7Sa6Lg502Sto
3UiN/xvfECWGHDzn0VE94Tt7ucmi+k4TeT2Wh0RGZrC9IOOihXsqpdhjSjzHZZ0t
3YNTrk1skoDPCWTVVO26Li12HEmeBs8YiLE0UH4KxcaZNBEKqfgtuicCgYEA9sBr
3KCE+oqAfVgI/axzTTIucxrzbBHGDeyZeQ2eGjnQrYz85JEEwDsL7en/GCDZ4lXR
XfZWPtpR6SUeKQ7lR7zoKrfejwNYJmKjFywygH5H4I2Vku0SBvXOwR1IeiDfXp3c
JfvNy12qjpV3vL1m9MveeskYY42eIL45llNpvjsCgYEAko+SwnriQ8/rckzk7g23
pzudPHoMJW+RuQtp4Gird8w9GCpSsyryQCuyRlLlHkXQ72aNVmVCZmHvoZxg9uEc
GvLPTUukyExeHxqh32LZhaEVtIWOx+4t3uDvOLTT7eDgAbP7IBW1HMbN6lH5+0J9
VBLlJbrP4Ic3z6bcyBfgE80CgYBS5zWWK+xhzRT8iA6FRGJ85kZK8BwnDBWx6fNq
g5PCFfixxrPVC5BAEdahOcQ2VBtAezrbyf8SIQHyRkFK5DFOl/6dE6fX/vSn+O34
xCW3nDYEES3W7oXnBsFPisomFlNWE826iU6MbEz4mOlg5XXo+3IlaNkj4ZnmQGNS
yXW2rwKBgQC+vsie+ZrH5hveuFIz7OllIs+sH6CUu9XXivA2yj1g5OWZl0kvSsJ8
gMOJwVma2JLApAOxtGwcyAr0EINnUtKBq8kWOBAFs7HQy69JCHsR11K5EXuGCqsM
f9S03Kvu7LqFVokuYMy0Y4ItdpRGZsSUXRcEKOE8TBSsfhLrdlUlCA==
-----END RSA PRIVATE KEY-----
    """
          .trimIndent()

  @Test
  fun testLoadED25519KeyPairFromPem() {
    SSHKeyUtils.ED25519.loadFromPem("invalid") shouldBe null
    assertSoftly(SSHKeyUtils.ED25519.loadFromPem(privateKeyPemEd25519)) {
      it?.private?.algorithm shouldBe "Ed25519"
    }
  }

  @Test
  fun testLoadED25519KeyPairFromOpenSSH() {
    SSHKeyUtils.ED25519.loadFromOpenSSH("invalid") shouldBe null
    assertSoftly(SSHKeyUtils.ED25519.loadFromOpenSSH(privateKeyOpensshEd25519)!!) {
      it.private?.algorithm shouldBe "Ed25519"
    }
  }

  @Test
  fun testLoadRSAKeyPairFromPem() {
    SSHKeyUtils.RSA.loadFromPem("invalid") shouldBe null
    assertSoftly(SSHKeyUtils.RSA.loadFromPem(privateKeyPemRsa)) {
      it?.private?.algorithm shouldBe "RSA"
    }
  }

  @Test
  fun testLoadRSAKeyPairFromOpenSSH() {
    SSHKeyUtils.RSA.loadFromOpenSSH("invalid") shouldBe null
    assertSoftly(SSHKeyUtils.RSA.loadFromOpenSSH(privateKeyOpensshRsa)!!) {
      it.private?.algorithm shouldBe "RSA"
    }
  }

  @Test
  fun testTryLoadKeyED25519() {
    assertSoftly(tryLoadKey(privateKeyOpensshEd25519)) { it.private?.algorithm shouldBe "Ed25519" }
  }

  @Test
  fun testTryLoadKeyRSA() {
    assertSoftly(tryLoadKey(privateKeyOpensshRsa)) { it.private?.algorithm shouldBe "RSA" }
  }

  @Test
  fun testGeneratedKeysWithOpenSSH() {
    val factories = listOf(SSHKeyUtils.ED25519, SSHKeyUtils.RSA)
    factories.forEach { factory ->
      val sshKey = factory.generate()
      val publicKeyFile =
          Files.createTempFile("rsa_public", ".key").also {
            it.writeText(factory.publicKeyToOpenSsh(sshKey.publicKey))
          }

      val sshServer =
          GenericContainer(
                  ImageFromDockerfile()
                      .withFileFromClasspath("Dockerfile", "Dockerfile")
                      .withFileFromFile("authorized_keys", publicKeyFile.toFile()),
              )
              .also {
                it.addExposedPort(22)
                it.start()
              }

      val key = tryLoadKey(sshKey.privateKey)
      val client = SSHClient(sshServer.host, key, port = sshServer.getMappedPort(22))

      assertSoftly(client.sshCommand("whoami")) { it.exitCode shouldBe 0 }

      sshServer.stop()
    }
  }
}
