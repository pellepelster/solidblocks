package de.solidblocks.cli.docs.ansible

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cli.utils.*
import de.solidblocks.utils.logErrorBlcks
import de.solidblocks.utils.logInfoBlcks
import de.solidblocks.utils.logWarningBlcks
import java.nio.file.Path
import kotlin.io.path.*

class AnsibleCollectionHugoGenerator(val collectionDir: Path, val targetDir: Path) {

  fun run(): Boolean {
    if (!verifyInput()) {
      return false
    }

    val galaxy = loadGalaxyYml() ?: return false

    val docsReadme = collectionDir.resolve("docs").resolve("README.md")

    val descriptionLong =
        if (docsReadme.exists()) {
          logInfoBlcks("found collection readme at '$docsReadme'")
          docsReadme.readText()
        } else {
          logWarningBlcks("collection readme '$docsReadme' not found")
          galaxy.description
        }

    val rolesDir = collectionDir.resolve("roles")
    val roles =
        rolesDir.listDirectoryEntries().map {
          val roleName = it.getName(it.nameCount - 1).name

          val metaData = loadRoleMetaYml(roleName)

          val defaults =
              parseRoleVariables(rolesDir.resolve(roleName).resolve("defaults").resolve("main.yml"))
                  ?: emptyList()
          val variables =
              parseRoleVariables(rolesDir.resolve(roleName).resolve("vars").resolve("main.yml"))
                  ?: emptyList()

          Role(roleName, defaults, variables, metaData ?: RoleMetaData(null, null, emptyList()))
        }

    val collectionIndexHugo =
        """
                    +++
                    title = "Collection ${galaxy.name}"
                    description = "${galaxy.description}"
                    +++

                    ## Description
                    $descriptionLong

                    ## Roles
                    {{% children description="true" %}}
        """
            .trimIndent()

    targetDir.createDirectories()

    val collectionIndex = targetDir.resolve("_index.md")
    logInfoBlcks("writing collection index to '$collectionIndex'")
    collectionIndex.writeText(collectionIndexHugo)

    roles.forEach { role ->
      val roleIndex = targetDir.resolve("${role.name}.md")
      logInfoBlcks("writing role index to '$roleIndex'")

      val roleIndexHugo =
          """
+++
title = "Role ${role.name}"
description = "${role.metaData.shortDescription ?: "<no description>"}"
+++

${role.metaData.description ?: role.metaData.shortDescription ?: "<no description>"}

## Variables

| Name | Value | Description | Required |
| ---- | ----- | ----------- | -------- |
${role.tableRows().toMarkdownTableRow()}
            """
              .trimIndent()
      roleIndex.writeText(roleIndexHugo)
    }

    return true
  }

  private fun verifyInput(): Boolean {
    if (!collectionDir.exists()) {
      logErrorBlcks("collection directory '$collectionDir' not found")
      return false
    }

    if (!collectionDir.isDirectory()) {
      logErrorBlcks("collection directory '$collectionDir' is not a directory")
      return false
    }

    if (!targetDir.exists()) {
      targetDir.createDirectories()
    }

    if (!targetDir.isDirectory()) {
      logErrorBlcks("target directory '$collectionDir' is not a directory")
      return false
    }

    return true
  }

  private fun parseRoleVariables(mainYmlFile: Path): List<Variable>? {
    if (!mainYmlFile.exists()) {
      logInfoBlcks("file '$mainYmlFile' does not exist")
      return null
    }

    val yml = yamlParse(mainYmlFile.readText())

    return when (yml) {
      is Success -> {
        parseVariables(yml.data)
      }
      else -> {
        logErrorBlcks("failed to parse '$mainYmlFile'")
        return null
      }
    }
  }

  private fun parseGalaxy(data: YamlNode): Galaxy? {
    val version = data.getMapString("version")
    if (version == null) {
      logErrorBlcks("collection version not found")
      return null
    }
    val namespace = data.getMapString("namespace")
    if (namespace == null) {
      logErrorBlcks("collection namespace not found")
      return null
    }

    val name = data.getMapString("name")
    if (name == null) {
      logErrorBlcks("collection name not found")
      return null
    }

    return Galaxy(namespace, name, version, data.getMapString("description"))
  }

  private fun parseRoleMetadata(data: YamlNode): RoleMetaData? {
    val argumentSpecs = data.getMapMap("argument_specs")
    val main = argumentSpecs?.getMapMap("main")

    val shortDescription = main?.getMapString("short_description")
    val description = main?.getMapString("description")
    val options = main?.getMapMap("options")

    val argumentSpecOptions =
        when (val optionNames = options?.getKeys()) {
          is Success -> {
            optionNames.data.map {
              val option = options.getMapMap(it)
              val description = option?.getMapString("description")
              val required = option?.getMapBool("required") ?: false
              val type = option?.getMapString("type")
              val default = option?.getMapString("default")

              Option(it, description, required, default, type)
            }
          }
          else -> emptyList()
        }

    return RoleMetaData(shortDescription, description, argumentSpecOptions)
  }

  private fun parseVariables(data: YamlNode) =
      when (val ymlKeys = data.getKeys()) {
        is Success -> ymlKeys.data
        else -> {
          logErrorBlcks("invalid format")
          null
        }
      }?.map { key -> Variable(key, data.getMapString(key)) }

  fun loadGalaxyYml(): Galaxy? {
    val galaxyYmlFile = collectionDir.resolve("galaxy.yml")
    if (!galaxyYmlFile.exists()) {
      logWarningBlcks("expected metadata '$galaxyYmlFile' not found")
      return null
    }

    logInfoBlcks("loading metadata from '$galaxyYmlFile'")
    val galaxyYml = yamlParse(galaxyYmlFile.readText())

    return when (galaxyYml) {
      is Success -> {
        val galaxy = parseGalaxy(galaxyYml.data)
        if (galaxy == null) {
          logErrorBlcks("failed to parse metadata from '$galaxyYmlFile'")
          return null
        }
        galaxy
      }
      else -> {
        logErrorBlcks("failed to parse metadata from '$galaxyYmlFile'")
        return null
      }
    }
  }

  fun loadRoleMetaYml(roleName: String): RoleMetaData? {
    val metaYmlFile =
        collectionDir.resolve("roles").resolve(roleName).resolve("meta").resolve("main.yml")
    if (!metaYmlFile.exists()) {
      logWarningBlcks("role metadata '$metaYmlFile' not found")
      return null
    }

    logInfoBlcks("loading role metadata from '$metaYmlFile'")
    val metaYml = yamlParse(metaYmlFile.readText())

    return when (metaYml) {
      is Success -> {
        val roleMetaData = parseRoleMetadata(metaYml.data)
        if (roleMetaData == null) {
          logErrorBlcks("failed to parse role metadata from '$metaYmlFile'")
          return null
        }
        roleMetaData
      }
      else -> {
        logErrorBlcks("failed to parse role metadata from '$metaYmlFile'")
        return null
      }
    }
  }
}
