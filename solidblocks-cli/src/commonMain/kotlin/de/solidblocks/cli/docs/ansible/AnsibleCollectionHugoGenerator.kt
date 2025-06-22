package de.solidblocks.cli.docs.ansible

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cli.utils.*
import okio.FileSystem
import okio.Path
import okio.SYSTEM

class AnsibleCollectionHugoGenerator(val collectionDir: Path, val targetDir: Path) {

    val fs = FileSystem.SYSTEM

    fun run(): Boolean {
        if (!verifyInput()) {
            return false
        }

        val galaxy = loadGalaxyYml() ?: return false

        val docsReadme = collectionDir.resolve("docs").resolve("README.md")

        val descriptionLong = if (fs.exists(docsReadme)) {
            logInfo("found collection readme at '${docsReadme}'")
            fs.read(docsReadme) {
                readUtf8()
            }
        } else {
            logWarning("collection readme '${docsReadme}' not found")
            galaxy.description
        }

        val rolesDir = collectionDir.resolve("roles")
        val roles = fs.list(rolesDir).map {
            val roleName = it.segments.last()

            val defaults = parseRoleVariables(rolesDir.resolve(roleName).resolve("defaults").resolve("main.yml"))
            val variables = parseRoleVariables(rolesDir.resolve(roleName).resolve("variables").resolve("main.yml"))

            Role(roleName, defaults, variables)
        }

        val collectionIndexHugo = """
                    +++
                    title = "Collection ${galaxy.name}"
                    description = "${galaxy.description}"
                    +++

                    ## Description
                    ${descriptionLong}

                    ## Roles
                    {{% children description="true" %}}
                """.trimIndent()

        fs.createDirectories(targetDir)

        val collectionIndex = targetDir.resolve("_index.md")
        logInfo("writing collection index to '${collectionIndex}'")
        fs.write(collectionIndex) {
            writeUtf8(collectionIndexHugo)
        }

        roles.forEach { role ->
            val roleIndex = targetDir.resolve("${role.name}.md")
            logInfo("writing role index to '${roleIndex}'")

            val roleIndexHugo = """
+++
title = "Role ${role.name}"
+++

## Defaults

| Name    | Value |
| ------- | ----- |
${
                role.defaults?.joinToString("\n") {
                    "|${it.name}|${it.value}|"
                } ?: "| &lt;none&gt; | &lt;none&gt; |"
            }

## Variables

| Name    | Value |
| ------- | ----- |
${
                role.variables?.joinToString("\n") {
                    "|${it.name}|${it.value}|"
                } ?: "| &lt;none&gt; | &lt;none&gt; |"
            }
""".trimIndent()

            fs.write(roleIndex) {
                writeUtf8(roleIndexHugo)
            }
        }

        return true

    }

    private fun verifyInput(): Boolean {
        if (!fs.exists(collectionDir)) {
            logError("collection directory '${collectionDir}' not found")
            return false
        }

        if (!fs.metadata(collectionDir).isDirectory) {
            logError("collection directory '${collectionDir}' is not a directory")
            return false
        }

        if (!fs.exists(targetDir)) {
            fs.createDirectories(targetDir)
        }

        if (!fs.metadata(targetDir).isDirectory) {
            logError("target directory '${collectionDir}' is not a directory")
            return false
        }

        return true
    }

    private fun parseRoleVariables(mainYmlFile: Path): List<Variable>? {

        if (!fs.exists(mainYmlFile)) {
            logInfo("file '${mainYmlFile}' does not exist")
            return null
        }

        val defaultsYml = yamlParse(fs.read(mainYmlFile) {
            readUtf8()
        })

        return when (defaultsYml) {
            is Success -> {
                parseVariables(defaultsYml.data)
            }

            else -> {
                logError("failed to parse '${mainYmlFile}'")
                return null
            }
        }
    }

    private fun parseGalaxy(data: YamlNode): Galaxy? {
        val version = data.getMapString("version")
        if (version == null) {
            logError("collection version not found")
            return null
        }
        val namespace = data.getMapString("namespace")
        if (namespace == null) {
            logError("collection namespace not found")
            return null
        }

        val name = data.getMapString("name")
        if (name == null) {
            logError("collection name not found")
            return null
        }

        return Galaxy(namespace, name, version, data.getMapString("description"))
    }

    private fun parseVariables(data: YamlNode) = when (val ymlKeys = data.getKeys()) {
        is Success ->
            ymlKeys.data

        else -> {
            logError("invalid format")
            null
        }
    }?.map { key ->
        Variable(key, data.getMapString(key))
    }

    fun loadGalaxyYml(): Galaxy? {
        val galaxyYmlFile = collectionDir.resolve("galaxy.yml")
        if (!fs.exists(galaxyYmlFile)) {
            logWarning("expected metadata '$galaxyYmlFile' not found")
            return null
        }

        logInfo("loading metadata from '$galaxyYmlFile'")
        val galaxyYml = yamlParse(fs.read(galaxyYmlFile) {
            readUtf8()
        })

        return when (galaxyYml) {
            is Success -> {
                val galaxy = parseGalaxy(galaxyYml.data)
                if (galaxy == null) {
                    logError("failed to parse metadata from '${galaxyYmlFile}'")
                    return null
                }
                galaxy
            }

            else -> {
                logError("failed to parse metadata from '${galaxyYmlFile}'")
                return null
            }
        }
    }
}

