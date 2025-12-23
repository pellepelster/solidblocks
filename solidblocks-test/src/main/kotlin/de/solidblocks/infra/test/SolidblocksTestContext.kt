package de.solidblocks.infra.test

import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.testDocker
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.host.hostTestContext
import de.solidblocks.infra.test.ssh.SshUtils
import de.solidblocks.infra.test.ssh.sshTestContext
import de.solidblocks.infra.test.terraform.terraformTestContext
import testLocal
import java.io.Closeable
import java.nio.file.Path
import java.security.KeyPair

class SolidblocksTestContext {

    private val tempDirs = mutableListOf<Closeable>()

    fun createTempDir() = tempDir().apply { tempDirs.add(this) }

    fun local() = testLocal().apply { tempDirs.add(this) }

    fun docker(image: DockerTestImage) = testDocker(image).apply { tempDirs.add(this) }

    fun terraform(dir: Path, version: String? = null) = terraformTestContext(dir, version)

    fun terraform(dir: String, version: String? = null) = terraformTestContext(Path.of(dir), version)

    fun ssh(host: String, privateKey: String, username: String = "root", port: Int = 22) =
        sshTestContext(host, SshUtils.tryLoadKey(privateKey), username, port)

    fun ssh(host: String, keyPair: KeyPair, username: String = "root", port: Int = 22) =
        sshTestContext(host, keyPair, username, port)

    fun host(host: String) = hostTestContext(host)

    fun close() {
        tempDirs.forEach { it.close() }
    }
}
