package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import java.nio.file.Path

@Suppress("ktlint:standard:property-naming")
object Constants {

    const val DEFAULT_ENVIRONMENT = "default"

    const val namespace: String = "blcks.de"

    const val managedByLabel: String = "$namespace/managed-by"

    const val serviceLabel: String = "$namespace/service"

    const val versionLabel: String = "$namespace/version"

    const val cloudLabel: String = "$namespace/cloud"

    const val sshKeysLabel: String = "$namespace/ssh-keys"

    const val userDataLabel: String = "$namespace/user-data"

    const val defaultNetwork = "10.0.0.0/8"

    const val defaultServiceSubnet = "10.0.1.0/24"

    fun sshConfigFilePath(configFileDirectory: Path, environment: EnvironmentContext) = configFileDirectory.resolve("${environment.cloud}_${environment.environment}_ssh_config")

    fun sshKnownHosts(configFileDirectory: Path, environment: EnvironmentContext) = configFileDirectory.resolve("${environment.cloud}_${environment.environment}_known_hosts")

    fun sshKeyName(environment: EnvironmentContext) = "${environment.cloud}-${environment.environment}"

    fun networkName(environment: EnvironmentContext) = "${environment.cloud}-${environment.environment}"

    fun firewallName(environment: EnvironmentContext, name: String) = "${environment.cloud}-${environment.environment}-$name"

    fun serverName(environment: EnvironmentContext, name: String, index: Int = 0) = "${environment.cloud}-${environment.environment}-$name-$index"

    fun serviceDnsName(service: ServiceConfigurationRuntime) = service.name

    fun secretPath(environment: EnvironmentContext, runtime: ServiceConfigurationRuntime, segments: List<String>) =
        "${environment.cloud}/${environment.environment}/${runtime.name}/${segments.joinToString("/")}"

    fun secretPath(environment: EnvironmentContext, segments: List<String>) = "${environment.cloud}/${environment.environment}/${segments.joinToString("/")}"

    fun sshHostPrivateKeySecretPath(environment: EnvironmentContext, serverName: String, type: SshHostKeyType) = secretPath(environment, listOf("hosts", serverName, "ssh_host_key_$type"))

    fun serviceLabels(runtime: ServiceConfigurationRuntime) = mapOf(serviceLabel to runtime.name)

    fun volumeLabels(runtime: ServiceConfigurationRuntime) = mapOf(serviceLabel to runtime.name)

    fun dnsRecordLabels(runtime: ServiceConfigurationRuntime) = mapOf(serviceLabel to runtime.name)

    fun cloudLabels(environment: EnvironmentContext) = cloudLabels(environment.cloud)

    fun cloudLabels(cloudName: String) = mapOf(
        managedByLabel to "blcks",
        cloudLabel to cloudName,
    )

    fun solidblocksVersion() = "0.0.0"

    fun serverPrivateIp(index: Int) = "10.0.1.${index + 1}"

    enum class SshHostKeyType {
        rsa,
        ed25519,
    }
}
