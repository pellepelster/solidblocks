package de.solidblocks.infra.test

import de.solidblocks.infra.test.files.DirectoryBuilder
import de.solidblocks.infra.test.files.tempDir

class SolidblocksTestContext {

    private val tempDirs = mutableListOf<DirectoryBuilder>()

    fun createTempDir() = tempDir().apply {
        tempDirs.add(this)
    }

    fun close() {
        tempDirs.forEach { it.remove() }
    }
}