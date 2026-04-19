package de.solidblocks.cloud

import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.defaultNetwork
import de.solidblocks.cloud.Constants.defaultServiceSubnet
import de.solidblocks.cloud.Constants.firewallName
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.sshConfigFilePath
import de.solidblocks.cloud.Constants.sshHostPrivateKeySecretPath
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.Constants.sshKnownHosts
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.providers.types.backup.backupSecretResource
import de.solidblocks.cloud.providers.types.ssh.sshKeyProvider
import de.solidblocks.cloud.provisioner.Provisioner
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.ProvisionersRegistry.Companion.createLookups
import de.solidblocks.cloud.provisioner.ProvisionersRegistry.Companion.createProvisioners
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyProvisioner
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.garagefs.layout.GarageFsLayoutProvisioner
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.firewall.HetznerFirewall
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetwork
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnet
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKey
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabaseProvisioner
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUserProvisioner
import de.solidblocks.cloud.provisioner.userdata.UserDataLookupProvider
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.resources.FirewallRuleDirection
import de.solidblocks.hetzner.cloud.resources.FirewallRuleProtocol
import de.solidblocks.hetzner.cloud.resources.HetznerFirewallRule
import de.solidblocks.ssh.KeyType
import de.solidblocks.ssh.SSHKeyFactory
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.bold
import de.solidblocks.utils.logWarning
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.io.StringWriter
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

class CloudProvisioner(val runtime: CloudConfigurationRuntime, val serviceRegistrations: List<ServiceRegistration<*, *>>, val providerRegistrations: List<ProviderRegistration<*, *, *>>) : Closeable {
    val registry = createRegistry()

    val context = ProvisionerContext(
        runtime.providers.sshKeyProvider().keyPair,
        runtime.providers.sshKeyProvider().privateKey.absolutePathString(),
        runtime.context.configFileDirectory,
        runtime.environment,
        registry,
        serviceRegistrations,
    )

    fun plan(log: LogContext): Result<Map<ResourceGroup, List<ResourceDiff>>> = runBlocking {
        val provisioner = createProvisioner()
        log.info(bold("planning changes for cloud configuration '${runtime.environment.cloud}'"))
        val resourceGroups = createResourceGroups()
        return@runBlocking provisioner.diff(resourceGroups, context, log)
    }

    fun info(runtime: CloudConfigurationRuntime): Result<String> = runBlocking {
        val serviceOutput =
            serviceManagers().map {
                when (val result = it.second.infoText(runtime, it.first, context)) {
                    is Error<String> -> return@runBlocking Error<String>(result.error)
                    is Success<String> -> result.data
                }
            }

        return@runBlocking Success(serviceOutput.joinToString("\n"))
    }

    fun status(runtime: CloudConfigurationRuntime): Result<String> = runBlocking {
        val serviceOutput =
            serviceManagers().map {
                when (val result = it.second.status(runtime, it.first, context)) {
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

        log.info(bold("rolling out changes for cloud configuration '${runtime.environment.cloud}'"))
        return@runBlocking provisioner.apply(diffs, context, log.indent())
    }

    private fun createResourceGroups(): List<ResourceGroup> {
        val publicKey =
            SSHKeyUtils.publicKeyToOpenSSH(runtime.providers.sshKeyProvider().keyPair.public)

        val sshKey = HetznerSSHKey(sshKeyName(runtime.environment), publicKey, cloudLabels(runtime.environment))

        val firewall = HetznerFirewall(
            firewallName(runtime.environment, "ssh"),
            listOf(
                HetznerFirewallRule(
                    direction = FirewallRuleDirection.IN,
                    protocol = FirewallRuleProtocol.TCP,
                    port = "22",
                    sourceIps = listOf("0.0.0.0/0", "::/0"),
                    description = "allow SSH",
                ),
                HetznerFirewallRule(
                    direction = FirewallRuleDirection.IN,
                    protocol = FirewallRuleProtocol.ICMP,
                    sourceIps = listOf("0.0.0.0/0", "::/0"),
                    description = "allow ICMP",
                ),
            ),
            cloudLabels(runtime.environment),
            cloudLabels(runtime.environment),
        )

        val network = HetznerNetwork(networkName(runtime.environment), defaultNetwork)
        val subnet = HetznerSubnet(defaultServiceSubnet, network.asLookup())

        val cloudResourceGroup =
            ResourceGroup(
                "cloud '${runtime.environment.cloud} base resources'",
                listOf(sshKey, firewall, network, subnet, backupSecretResource(runtime)),
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

    private fun serviceManagers() = runtime.services.map {
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

    fun createSSHConfig(): Result<String> {
        val sshConfigFile =
            sshConfigFilePath(
                runtime.context.configFileDirectory,
                runtime.environment,
            )

        val sshKnownHostsFile =
            sshKnownHosts(
                runtime.context.configFileDirectory,
                runtime.environment,
            )

        val resourceGroups = createResourceGroups()

        val sshConfig = StringWriter()
        sshConfig.appendLine("Host *")
        sshConfig.appendLine("  UserKnownHostsFile ${sshKnownHostsFile.absolutePathString()}")
        sshConfig.appendLine("  StrictHostKeyChecking yes")
        sshConfig.appendLine("  User root")
        sshConfig.appendLine("  IdentityFile ${context.sshKeyAbsolutePath}")
        sshConfig.appendLine("")

        val sshKnownHosts = StringWriter()
        val servers =
            resourceGroups.flatMap { it.hierarchicalResourceList().filterIsInstance<HetznerServer>() }

        val serversIps =
            servers.mapNotNull { context.lookup(it.asLookup()) }.map { it.name to it.publicIpv4 }

        serversIps.forEach {
            val rsaSecretPath = sshHostPrivateKeySecretPath(context.environment, it.first, KeyType.rsa)
            val rsaSecret = context.lookup(PassSecretLookup(rsaSecretPath))

            val ed25519SecretPath = sshHostPrivateKeySecretPath(context.environment, it.first, KeyType.ed25519)
            val ed25519Secret = context.lookup(PassSecretLookup(ed25519SecretPath))

            if (rsaSecret != null) {
                val keyPair = SSHKeyUtils.loadKey(rsaSecret.secret)
                sshKnownHosts.appendLine("${it.second} ${SSHKeyUtils.publicKeyToOpenSSH(keyPair.public)}")
            } else {
                logWarning("failed to lookup secret for known hosts from '$rsaSecretPath'")
            }

            if (ed25519Secret != null) {
                val keyPair = SSHKeyUtils.loadKey(ed25519Secret.secret)
                sshKnownHosts.appendLine("${it.second} ${SSHKeyUtils.publicKeyToOpenSSH(keyPair.public)}")
            } else {
                logWarning("failed to lookup secret for known hosts from '$ed25519SecretPath'")
            }

            sshConfig.appendLine("Host ${it.first}")
            sshConfig.appendLine("  HostName ${it.second}")
            sshConfig.appendLine("")
        }

        return try {
            sshConfigFile.writeText(sshConfig.toString())
            sshKnownHostsFile.writeText(sshKnownHosts.toString())
            Success(sshConfigFile.absolutePathString())
        } catch (e: Exception) {
            Error<String>(e.message ?: "unknown error")
        }
    }

    override fun close() {
        context.close()
    }
}
