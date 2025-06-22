package de.solidblocks.cli

import com.saveourtool.okio.toRealPath
import de.solidblocks.cli.docs.ansible.AnsibleCollectionHugoGenerator
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnsibleCollectionHugoGeneratorTest {

    val fs = FileSystem.SYSTEM

    @Test
    fun invalidPath() {
        assertFalse(AnsibleCollectionHugoGenerator("invalid_path".toPath(), "invalid_path".toPath()).run())
    }

    @Test
    fun generateDocumentation() {
        val testbedDir = ".".toPath().toRealPath().resolve("test").resolve("ansible").resolve("test_collection1")
        val targetDir = ".".toPath().toRealPath().resolve("build").resolve("hugo").resolve("collection1")

        fs.deleteRecursively(targetDir)

        assertTrue(AnsibleCollectionHugoGenerator(testbedDir, targetDir).run())

        assertTrue(fs.exists(targetDir.resolve("_index.md")))
        assertTrue(fs.exists(targetDir.resolve("role1.md")))
    }
}