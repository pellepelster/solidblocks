package de.solidblocks.provisioner.utils

import org.springframework.stereotype.Component

@Component
class PassSecretDirectoryProvider {
    private val directories = ArrayList<String>()

    fun addDirectory(directory: String) {
        this.directories.add(directory)
    }

    fun defaultDirectory(): String {
        return directories.first()
    }
}
