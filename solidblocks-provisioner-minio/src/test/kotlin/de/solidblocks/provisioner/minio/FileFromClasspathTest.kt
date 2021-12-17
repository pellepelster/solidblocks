package de.solidblocks.provisioner.minio

import de.solidblocks.base.CommandExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class FileFromClasspathTest {

    @Test
    fun testEnsureFile() {

        val file = File("/tmp/mc_test_${UUID.randomUUID()}")

        assertThat(file).doesNotExist()
        FileFromClasspath.ensureFile("/mc", file)
        assertThat(file).exists()

        assertThat(CommandExecutor().run(command = listOf(file.toString())).stdout).contains("RELEASE.2021-12-16T23-38-39Z")
    }
}
