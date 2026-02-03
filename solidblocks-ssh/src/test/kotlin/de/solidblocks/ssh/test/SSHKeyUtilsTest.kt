package de.solidblocks.ssh.test

import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.ssh.SSHKeyUtils.loadKey
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

      val key = loadKey(it.privateKey)
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

      val key = loadKey(it.privateKey)
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
  fun testIsEncryptedEd25519() {
    val encryptedKey =
        """
        -----BEGIN OPENSSH PRIVATE KEY-----
        b3BlbnNzaC1rZXktdjEAAAAACmFlczI1Ni1jdHIAAAAGYmNyeXB0AAAAGAAAABBv3SJoam
        DKPDyCSKxmoNtTAAAAEAAAAAEAAAAzAAAAC3NzaC1lZDI1NTE5AAAAILNszSuEBpMyM6LE
        qWnG2kfxtGE/ItKR9BU0WnQeHtqeAAAAkCLhl8PzMZzm/qRFf5Rh1HTAYNnftmRRk81ZVk
        2tx93AVZHplBVq1/IAkfYZSwdvt9hDarMJNqmwXjtgXQwLTGE2qndmotawcdduJyLnaZs2
        2ju2NU2ZCiNy1sdtnxkEEaCcRrm9imn34uv5+/eCzLU/JpogvUlLH81iOq1FjuHOxQz8Kt
        eoND2XJopCE5dphg==
        -----END OPENSSH PRIVATE KEY-----
        """
            .trimIndent()
    SSHKeyUtils.isEncrypted(encryptedKey) shouldBe true

    val unencryptedKey =
        """
        -----BEGIN OPENSSH PRIVATE KEY-----
        b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
        QyNTUxOQAAACDiwYWAns9fmH5iFbkh4Ty1QQDLDv1DRWK3lGRhV71hdQAAAJDs9NrY7PTa
        2AAAAAtzc2gtZWQyNTUxOQAAACDiwYWAns9fmH5iFbkh4Ty1QQDLDv1DRWK3lGRhV71hdQ
        AAAEDtcfO7yIUJhvPHNlzK6PaGJ3U6zdnVFrvgq1w4BW9RCOLBhYCez1+YfmIVuSHhPLVB
        AMsO/UNFYreUZGFXvWF1AAAACXBlbGxlQGZyeQECAwQ=
        -----END OPENSSH PRIVATE KEY-----
        """
            .trimIndent()
    SSHKeyUtils.isEncrypted(unencryptedKey) shouldBe false

    SSHKeyUtils.isEncrypted("invalid") shouldBe false
  }

  @Test
  fun testIsEncryptedRSA() {
    val encryptedKey =
        """
        -----BEGIN OPENSSH PRIVATE KEY-----
        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
        9uErRg==
        -----END OPENSSH PRIVATE KEY-----
        """
            .trimIndent()
    SSHKeyUtils.isEncrypted(encryptedKey) shouldBe true

    val unencryptedKey =
        """
        -----BEGIN OPENSSH PRIVATE KEY-----
        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
        ORDUEDLlc12dEAAAAJcGVsbGVAZnJ5AQI=
        -----END OPENSSH PRIVATE KEY-----
        """
            .trimIndent()
    SSHKeyUtils.isEncrypted(unencryptedKey) shouldBe false

    SSHKeyUtils.isEncrypted("invalid") shouldBe false
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
    assertSoftly(loadKey(privateKeyOpensshEd25519)) { it.private?.algorithm shouldBe "Ed25519" }
  }

  @Test
  fun testPublicKeyToOpenSSHEd25519() {
    val key = loadKey(privateKeyOpensshEd25519)
    SSHKeyUtils.publicKeyToOpenSSH(key.public) shouldBe
        "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIFcHCF1q7A+sGGv+o6YvcE7j7RAf6LHuoiaJTD2+PONv"
  }

  @Test
  fun testPublicKeyToOpenSSHRSA() {
    val key = loadKey(privateKeyOpensshRsa)
    SSHKeyUtils.publicKeyToOpenSSH(key.public) shouldBe
        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDVqo4EN/Nwn6Gq6+52EiITAQ7hTSDZ8SEl0tNKhMN6qYuT62LgcqgnhIWGwTD4h2QJUkY8j5zmdMhruHLTmVFb2SYn379pACMOpl0saZG0eNMXZJIvtDN/Ojm0qJOMbMK8BAw/eg1nZovTjWFm7/HFUsMYXfpSv0hKe9jgZs9VhlskJBesHsfqjI9QICCdE85adRFSPfkhxVXgq2JvJl3kzhU31M7MnPjBfWN/kual61285ysHFKLHmgJDqmbQFnW99IYRG4CXdFWOvAGqP7KtsMZl3m5fkKDTpBl4u5R2gTkH/MXB9E8a1IFlcz20Mm0HIRg+3Q+7hsM0+1Qhsdj9"
  }

  @Test
  fun testTryLoadKeyRSA() {
    assertSoftly(loadKey(privateKeyOpensshRsa)) { it.private?.algorithm shouldBe "RSA" }
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

      val key = loadKey(sshKey.privateKey)
      val client = SSHClient(sshServer.host, key, port = sshServer.getMappedPort(22))

      assertSoftly(client.command("whoami")) { it.exitCode shouldBe 0 }

      sshServer.stop()
    }
  }
}
