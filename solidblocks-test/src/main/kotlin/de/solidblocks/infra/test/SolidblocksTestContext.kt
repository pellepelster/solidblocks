package de.solidblocks.infra.test

import de.solidblocks.infra.test.cloudinit.CloudInitTestContext
import de.solidblocks.infra.test.cloudinit.cloudInitTestContext
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.hetzner.hetznerTestContext
import de.solidblocks.infra.test.host.hostTestContext
import de.solidblocks.infra.test.ssh.sshTestContext
import de.solidblocks.infra.test.terraform.TerraformTestContext
import de.solidblocks.infra.test.terraform.terraformTestContext
import de.solidblocks.ssh.SSHKeyUtils
import java.io.Closeable
import java.nio.file.Path
import java.security.KeyPair
import localTestContext

class SolidblocksTestContext(val testId: String) {

  private var cleanupAfterTest: Boolean = true

  private var failed: Boolean = false

  private val tempDirs = mutableListOf<Closeable>()

  private val terraformContexts = mutableListOf<TerraformTestContext>()

  private val cloudInitContexts = mutableListOf<CloudInitTestContext>()

  fun createTempDir() = tempDir().apply { tempDirs.add(this) }

  fun local() = localTestContext().apply { tempDirs.add(this) }

  fun docker(image: DockerTestImage) = dockerTestContext(image).apply { tempDirs.add(this) }

  fun terraform(dir: Path, version: String? = null) =
      terraformTestContext(dir, version).also { terraformContexts.add(it) }

  fun terraform(dir: String, version: String? = null) =
      terraformTestContext(Path.of(dir), version).also { terraformContexts.add(it) }

  fun ssh(host: String, privateKey: String, username: String = "root", port: Int = 22) =
      sshTestContext(host, SSHKeyUtils.tryLoadKey(privateKey), username, port)

  fun ssh(host: String, keyPair: KeyPair, username: String = "root", port: Int = 22) =
      sshTestContext(host, keyPair, username, port)

  fun host(host: String) = hostTestContext(host)

  fun hetzner(hcloudToken: String) = hetznerTestContext(hcloudToken, testId)

  fun cloudInit(host: String, privateKey: String, username: String = "root", port: Int = 22) =
      cloudInitTestContext(host, privateKey, username, port).also { cloudInitContexts.add(it) }

  fun afterAll() {
    cloudInitContexts.forEach { it.afterAll() }
  }

  fun cleanup() {
    if (!cleanupAfterTest) {
      log("skipping cleanup")
      return
    }

    tempDirs.forEach { it.close() }
    terraformContexts.forEach { it.destroy() }
  }

  fun cleanupAfterTestFailure(cleanup: Boolean) {
    this.cleanupAfterTest = cleanup
  }

  fun markFailed() {
    failed = true
  }
}
