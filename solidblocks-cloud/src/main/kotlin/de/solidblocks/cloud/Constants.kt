package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.s3.model.S3ServiceConfigurationRuntime

@Suppress("ktlint:standard:property-naming")
object Constants {

    fun sshKeyName(configuration: CloudConfigurationRuntime) =
        "${configuration.name}-${configuration.getDefaultEnvironment()}"

    fun networkName(configuration: CloudConfigurationRuntime) =
        "${configuration.name}-${configuration.getDefaultEnvironment()}"

    fun serverName(configuration: CloudConfigurationRuntime, name: String, index: Int = 0) =
        "${configuration.name}-${configuration.getDefaultEnvironment()}-$name-${index}"

    fun serviceDnsName(service: ServiceConfigurationRuntime) =
        service.name

    fun secretPath(configuration: CloudConfigurationRuntime, runtime: S3ServiceConfigurationRuntime, segments: List<String>) =
        "${configuration.name}/${configuration.getDefaultEnvironment()}/${runtime.name}/${segments.joinToString("/")}"

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

    const val DEFAULT_NETWORK = "10.0.0.0/8"

    const val DEFAULT_SERVICE_SUBNET = "10.0.1.0/24"

    fun serverIp(index: Int) = "10.0.1.${index + 1}"
}
