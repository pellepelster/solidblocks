package de.solidblocks.infra.test

import de.solidblocks.infra.test.aws.awsTestContext
import de.solidblocks.infra.test.cloudinit.cloudInitTestContext
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.hetzner.hetznerTestContext
import de.solidblocks.infra.test.host.hostTestContext
import de.solidblocks.infra.test.ssh.sshTestContext
import de.solidblocks.infra.test.terraform.terraformTestContext
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.logInfo
import java.io.Closeable
import java.nio.file.Path
import java.security.KeyPair
import localTestContext

class SolidblocksTestContext(testId: String) : TestContext(testId) {

  private var cleanupAfterTest: Boolean = true

  private var failed: Boolean = false

  private val tempDirs = mutableListOf<Closeable>()

  fun createTempDir() = tempDir().also { tempDirs.add(it) }

  fun local() =
      localTestContext(testId).also {
        testContexts.add(it)
        tempDirs.add(it)
      }

  fun aws() = awsTestContext(testId).also { testContexts.add(it) }

  fun docker(image: DockerTestImage) =
      dockerTestContext(image, testId).also {
        testContexts.add(it)
        tempDirs.add(it)
      }

  fun terraform(dir: Path, version: String? = null) =
      terraformTestContext(dir, version, testId).also { testContexts.add(it) }

  fun terraform(dir: String, version: String? = null) =
      terraformTestContext(Path.of(dir), version, testId).also { testContexts.add(it) }

  fun ssh(host: String, privateKey: String, username: String = "root", port: Int = 22) =
      sshTestContext(host, SSHKeyUtils.loadKey(privateKey), username, port, testId).also {
        testContexts.add(it)
      }

  fun ssh(host: String, keyPair: KeyPair, username: String = "root", port: Int = 22) =
      sshTestContext(host, keyPair, username, port, testId).also { testContexts.add(it) }

  fun host(host: String) = hostTestContext(host).also { testContexts.add(it) }

  fun hetzner(hcloudToken: String) =
      hetznerTestContext(hcloudToken, testId).also { testContexts.add(it) }

  fun cloudInit(host: String, privateKey: String, username: String = "root", port: Int = 22) =
      cloudInitTestContext(host, privateKey, username, port, testId).also { testContexts.add(it) }

  override fun cleanUp() {
    if (!cleanupAfterTest) {
      logInfo("skipping cleanup")
      return
    }

    tempDirs.forEach { it.close() }
  }

  fun cleanupAfterTestFailure(cleanup: Boolean) {
    this.cleanupAfterTest = cleanup
  }

  fun markFailed() {
    failed = true
  }
}
