package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.configuration.model.EnvironmentReference
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import java.nio.file.Path

@Suppress("ktlint:standard:property-naming")
object Constants {

    const val namespace: String = "blcks.de"

    const val managedByLabel: String = "$namespace/managed-by"

    const val serviceLabel: String = "$namespace/service"

    const val versionLabel: String = "$namespace/version"

    const val cloudLabel: String = "$namespace/cloud"

    const val sshKeysLabel: String = "$namespace/ssh-keys"

    const val userDataLabel: String = "$namespace/user-data"

    const val defaultNetwork = "10.0.0.0/8"

    const val defaultServiceSubnet = "10.0.1.0/24"

    fun sshConfigFilePath(configFileDirectory: Path, cloudName: String) = configFileDirectory.resolve("${cloudName}_ssh_config")

    fun sshKeyName(configuration: CloudConfigurationRuntime) = "${configuration.name}-${configuration.getDefaultEnvironment()}"

    fun networkName(configuration: CloudConfigurationRuntime) = "${configuration.name}-${configuration.getDefaultEnvironment()}"

    fun firewallName(configuration: CloudConfigurationRuntime, name: String) = "${configuration.name}-${configuration.getDefaultEnvironment()}-$name"

    fun serverName(configuration: CloudConfigurationRuntime, name: String, index: Int = 0) = "${configuration.name}-${configuration.getDefaultEnvironment()}-$name-$index"

    fun serviceDnsName(service: ServiceConfigurationRuntime) = service.name

    fun secretPath(configuration: CloudConfigurationRuntime, runtime: ServiceConfigurationRuntime, segments: List<String>) =
        "${configuration.name}/${configuration.getDefaultEnvironment()}/${runtime.name}/${segments.joinToString("/")}"

    @Deprecated("use EnvironmentReference if possible")
    fun secretPath(configuration: CloudConfigurationRuntime, segments: List<String>) = "${configuration.name}/${configuration.getDefaultEnvironment()}/${segments.joinToString("/")}"

    fun secretPath(environment: EnvironmentReference, segments: List<String>) = "${environment.cloud}/${environment.environment}/${segments.joinToString("/")}"

    fun serviceLabels(runtime: ServiceConfigurationRuntime) = mapOf(serviceLabel to runtime.name)

    fun volumeLabels(runtime: ServiceConfigurationRuntime) = mapOf(serviceLabel to runtime.name)

    fun dnsRecordLabels(runtime: ServiceConfigurationRuntime) = mapOf(serviceLabel to runtime.name)

    fun cloudLabels(cloud: CloudConfigurationRuntime) = cloudLabels(cloud.name)

    fun cloudLabels(cloudName: String) = mapOf(
        managedByLabel to "blcks",
        cloudLabel to cloudName,
    )

    fun solidblocksVersion() = "0.0.0"

    fun serverPrivateIp(index: Int) = "10.0.1.${index + 1}"
}
