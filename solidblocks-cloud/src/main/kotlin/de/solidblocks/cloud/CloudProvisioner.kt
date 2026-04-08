package de.solidblocks.cloud

import de.solidblocks.cloud.Constants.DEFAULT_NETWORK
import de.solidblocks.cloud.Constants.DEFAULT_SERVICE_SUBNET
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.secretPath
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.providers.ssh.sshKeyProvider
import de.solidblocks.cloud.provisioner.Provisioner
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.ProvisionersRegistry.Companion.createLookups
import de.solidblocks.cloud.provisioner.ProvisionersRegistry.Companion.createProvisioners
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyProvisioner
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.garagefs.layout.GarageFsLayoutProvisioner
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetwork
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnet
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKey
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabaseProvisioner
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUserProvisioner
import de.solidblocks.cloud.provisioner.userdata.UserDataLookupProvider
import de.solidblocks.cloud.services.CloudInfo
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.ServiceInfo
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.services.managerForService
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.bold
import de.solidblocks.utils.logInfo
import java.io.Closeable
import java.io.File
import kotlin.io.path.absolutePathString
import kotlinx.coroutines.runBlocking

class CloudProvisioner(
    val runtime: CloudConfigurationRuntime,
    val serviceRegistrations: List<ServiceRegistration<*, *>>,
    val providerRegistrations: List<ProviderRegistration<*, *, *>>,
) : Closeable {
  val registry = createRegistry()

  val context =
      ProvisionerContext(
          runtime.providers.sshKeyProvider().keyPair,
          runtime.providers.sshKeyProvider().privateKey.absolutePathString(),
          runtime.context.configFileDirectory,
          runtime.name,
          runtime.getDefaultEnvironment(),
          registry,
          serviceRegistrations,
      )

  fun plan(log: LogContext): Result<Map<ResourceGroup, List<ResourceDiff>>> = runBlocking {
    val provisioner = createProvisioner()
    logInfo(bold("planning changes for cloud configuration '${runtime.name}'"))
    val resourceGroups = createResourceGroups()
    return@runBlocking provisioner.diff(resourceGroups, context, log)
  }

  fun info(runtime: CloudConfigurationRuntime): Result<String> = runBlocking {
    val serviceOutput =
        serviceManagers().map {
          when (val result = it.second.info(runtime, it.first, context)) {
            is Error<String> -> return@runBlocking Error<String>(result.error)
            is Success<String> -> result.data
          }
        }

    return@runBlocking Success(serviceOutput.joinToString("\n"))
  }

  fun infoJson(runtime: CloudConfigurationRuntime): Result<CloudInfo> = runBlocking {
    val services =
        serviceManagers().map {
          when (val result = it.second.infoJson(runtime, it.first, context)) {
            is Error<ServiceInfo> -> return@runBlocking Error<CloudInfo>(result.error)
            is Success<ServiceInfo> -> result.data
          }
        }

    return@runBlocking Success(CloudInfo(services))
  }

  fun apply(log: LogContext): Result<Unit> = runBlocking {
    val provisioner = createProvisioner()

    val diffs =
        when (val result = plan(log)) {
          is Error<Map<ResourceGroup, List<ResourceDiff>>> ->
              return@runBlocking Error<Unit>(result.error)

          is Success<Map<ResourceGroup, List<ResourceDiff>>> -> result.data
        }

    logInfo(bold("rolling out changes for cloud configuration '${runtime.name}'"))
    return@runBlocking provisioner.apply(diffs, context, log.indent())
  }

  private fun createResourceGroups(): List<ResourceGroup> {
    val publicKey =
        SSHKeyUtils.publicKeyToOpenSSH(runtime.providers.sshKeyProvider().keyPair.public)
    val sshKey = HetznerSSHKey(sshKeyName(runtime), publicKey, emptyMap())
    val network = HetznerNetwork(networkName(runtime), DEFAULT_NETWORK)
    val subnet = HetznerSubnet(DEFAULT_SERVICE_SUBNET, network.asLookup())

    val backupPassword =
        PassSecret(
            secretPath(runtime, listOf("backup", "password")),
            length = 32,
            allowedChars = ('a'..'f') + ('0'..'9'),
        )

    val cloudResourceGroup =
        ResourceGroup(
            "cloud '${runtime.name} base resources'",
            listOf(sshKey, network, subnet, backupPassword),
        )

    val serviceResourceGroups =
        serviceManagers().map {
          ResourceGroup(
              "service '${it.first.name}'",
              it.second.createResources(runtime, it.first, context),
              setOf(cloudResourceGroup),
          )
        }
    return listOf(cloudResourceGroup) + serviceResourceGroups
  }

  private fun serviceManagers() =
      runtime.services.map {
        val manager: ServiceManager<ServiceConfiguration, ServiceConfigurationRuntime> =
            serviceRegistrations.managerForService(it)
        it to manager
      }

  private fun createProvisioner() = Provisioner(registry)

  private fun createRegistry(): ProvisionersRegistry {
    val providerProvisioners = providerRegistrations.createProvisioners(runtime.providers)
    val providerLookups = providerRegistrations.createLookups(runtime.providers)

    val serviceProvisioners =
        runtime.services.flatMap {
          val manager: ServiceManager<ServiceConfiguration, ServiceConfigurationRuntime> =
              serviceRegistrations.managerForService(it)
          manager.createProvisioners(it)
        }

    val defaultProvisioners =
        listOf(
            GarageFsBucketProvisioner(),
            GarageFsAccessKeyProvisioner(),
            GarageFsPermissionProvisioner(),
            GarageFsLayoutProvisioner(),
            PostgresUserProvisioner(),
            PostgresDatabaseProvisioner(),
        )

    val lookups =
        providerLookups +
            listOf(UserDataLookupProvider()) +
            (providerProvisioners + serviceProvisioners + defaultProvisioners).filterIsInstance<
                ResourceLookupProvider<*, *>,
            >()

    return ProvisionersRegistry(
        lookups,
        providerProvisioners + defaultProvisioners + serviceProvisioners,
    )
  }

  fun createSSHConfig(sshConfigFile: File): Result<Unit> {
    val resourceGroups = createResourceGroups()
    val servers =
        resourceGroups.flatMap { it.hierarchicalResourceList().filterIsInstance<HetznerServer>() }

    val serversIps =
        servers.mapNotNull { context.lookup(it.asLookup()) }.map { it.name to it.publicIpv4 }

    // TODO use explicit host key checking
    val sshConfigHeader =
        """
            Host *
                UserKnownHostsFile /dev/null
                StrictHostKeyChecking no
                User root
                IdentityFile ${context.sshKeyAbsolutePath}
        """
            .trimIndent()

    val sshConfigHosts =
        serversIps.joinToString("\n") { "Host ${it.first}\n    HostName ${it.second}\n" }

    return try {
      sshConfigFile.writeText(sshConfigHeader + "\n\n" + sshConfigHosts)
      Success<Unit>(Unit)
    } catch (e: Exception) {
      Error<Unit>(e.message ?: "unknown error")
    }
  }

  override fun close() {
    context.close()
  }
}
