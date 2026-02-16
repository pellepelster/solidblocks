package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfiguration

@Suppress("ktlint:standard:property-naming")
object Constants {
  fun sshKeyName(configuration: CloudConfiguration) =
      "${configuration.name}-${configuration.getDefaultEnvironment()}"

  fun serverName(configuration: CloudConfiguration, name: String) =
      "${configuration.name}-${configuration.getDefaultEnvironment()}-$name"

  const val namespace: String = "blcks.de"

  const val managedByLabel: String = "$namespace/managed-by"

  const val versionLabel: String = "$namespace/version"

  const val cloudLabel: String = "$namespace/cloud"

  const val sshKeysLabel: String = "$namespace/ssh-keys"

  const val userDataLabel: String = "$namespace/user-data"

  fun defaultLabels(cloud: String) =
      mapOf(
          managedByLabel to "blcks",
          cloudLabel to cloud,
          versionLabel to solidblocksVersion(),
      )

  fun solidblocksVersion() = "0.0.0"
}
