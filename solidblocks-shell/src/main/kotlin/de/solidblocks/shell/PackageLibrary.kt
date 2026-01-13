package de.solidblocks.shell

import java.io.BufferedReader

object PackageLibrary {
    fun source() =
        PackageLibrary::class.java.classLoader.getResourceAsStream("package.sh").bufferedReader(Charsets.UTF_8).use(
            BufferedReader::readText
        )

    class UpdateSystem :
        LibraryCommand {
        override fun toShell() = listOf("package_update_system")
    }

    class UpdateRepositories :
        LibraryCommand {
        override fun toShell() = listOf("package_update_repositories")
    }

    class InstallPackage(val pkg: String) :
        LibraryCommand {
        override fun toShell() = listOf("package_ensure_package ${pkg}")
    }
}