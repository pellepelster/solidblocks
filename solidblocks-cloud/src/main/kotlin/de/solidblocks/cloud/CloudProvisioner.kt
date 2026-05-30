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
import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.tainted
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.api.resources.SecretInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.providers.types.backup.backupSecretResource
import de.solidblocks.cloud.providers.types.ssh.sshKeyProvider
import de.solidblocks.cloud.provisioner.Provisioner
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.ProvisionersRegistry.Companion.createLookups
import de.solidblocks.cloud.provisioner.ProvisionersRegistry.Companion.createProvisioners
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContextImpl
import de.solidblocks.cloud.provisioner.context.ProvisionerContextImpl
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyProvisioner
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.garagefs.layout.GarageFsLayoutProvisioner
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.firewall.HetznerFirewall
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetwork
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnet
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKey
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabaseProvisioner
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUserProvisioner
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup
import de.solidblocks.cloud.provisioner.userdata.UserDataLookupProvider
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.aggregate
import de.solidblocks.cloud.utils.aggregateErrorMessage
import de.solidblocks.cloud.utils.getOrElse
import de.solidblocks.cloud.utils.hasError
import de.solidblocks.cloud.utils.map
import de.solidblocks.cloud.utils.mapSuccess
import de.solidblocks.hetzner.cloud.resources.FirewallRuleDirection
import de.solidblocks.hetzner.cloud.resources.FirewallRuleProtocol
import de.solidblocks.hetzner.cloud.resources.HetznerFirewallRule
import de.solidblocks.ssh.KeyType
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.bold
import de.solidblocks.utils.logWarning
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.io.StringWriter
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

class CloudProvisioner(val runtime: CloudConfigurationRuntime, val serviceRegistrations: List<ServiceRegistration<*, *>>, val providerRegistrations: List<ProviderRegistration<*, *, *>>) : Closeable {

    val registry = createRegistry()

    val context = ProvisionerContextImpl(
        runtime.providers.sshKeyProvider().keyPair,
        runtime.providers.sshKeyProvider().privateKey.absolutePathString(),
        runtime.environmentContext,
        registry,
        serviceRegistrations,
    )

    fun plan(taintCallback: (BaseInfrastructureResource<*>) -> Boolean, log: LogContext): Result<Map<ResourceGroup, List<ResourceDiff>>> = runBlocking {
        val provisioner = createProvisioner()
        log.info(bold("planning changes for cloud configuration '${runtime.environmentContext.cloud}'"))

        val resourceGroups = when (val result = createResourceGroups()) {
            is Error<List<ResourceGroup>> -> return@runBlocking Error(result.error)
            is Success -> result.data
        }

        return@runBlocking provisioner.diff(resourceGroups, taintCallback, context, log)
    }

    fun info(runtime: CloudConfigurationRuntime): Result<String> = runBlocking {
        val serviceOutput = serviceManagers().map {
            it.second.infoText(runtime, it.first, context).getOrElse { e -> return@runBlocking Error(e.error, e.cause) }
        }

        return@runBlocking Success(serviceOutput.joinToString("\n"))
    }

    fun status(runtime: CloudConfigurationRuntime): Result<String> = runBlocking {
        val serviceOutput = serviceManagers().map {
            it.second.status(runtime, it.first, context).getOrElse { e -> return@runBlocking Error(e.error, e.cause) }
        }

        return@runBlocking Success(serviceOutput.joinToString("\n"))
    }

    fun maintenance(runtime: CloudConfigurationRuntime, log: LogContext): Result<Unit> = runBlocking {
        val maintenanceOutputs = serviceManagers().map {
            it.second.maintenance(runtime, it.first, context, log.indent())
        }

        maintenanceOutputs.aggregate { }
    }

    fun infoJson(runtime: CloudConfigurationRuntime): Result<CloudInfo> = runBlocking {
        val services = serviceManagers().map {
            it.second.infoJson(runtime, it.first, context).getOrElse { e -> return@runBlocking Error(e.error, e.cause) }
        }

        Success(CloudInfo(services))
    }

    fun apply(taintSecrets: Boolean, log: LogContext): Result<Unit> = runBlocking {
        val provisioner = createProvisioner()

        val diffs = plan({
            if (taintSecrets) {
                it is SecretInfrastructureResource
            } else {
                false
            }
        }, log).getOrElse { return@runBlocking Error(it.error, it.cause) }

        log.info(bold("rolling out changes for cloud configuration '${runtime.environmentContext.cloud}'"))

        val taintedResources = diffs.values.flatten().filter { it.status == tainted }.map { it.resource }.toSet()

        val diffResult = if (diffs.entries.flatMap { it.value }.any { it.status != up_to_date }) {
            provisioner.apply(
                diffs,
                ProvisionerApplyContextImpl(context.sshKeyPair, context.sshKeyAbsolutePath, context.environment, context.registry, context.serviceRegistrations, taintedResources),
                log.indent(),
            )
        } else {
            log.indent().info("no pending changes")
            Success(Unit)
        }

        when (diffResult) {
            is Error<Unit> -> diffResult
            is Success<Unit> -> {
                val cleanupResults = serviceManagers().map {
                    log.debug("running cleanup for service '${it.first.name}'")
                    it.second.cleanupResources(runtime, it.first, context, log.indent())
                }

                cleanupResults.aggregate { Unit }
            }
        }
    }

    // desired state is static for the lifetime of the provisioner, so the (relatively expensive)
    // resource group construction is computed once and reused across plan/apply/ssh-config.
    private val resourceGroups: Result<List<ResourceGroup>> by lazy { buildResourceGroups() }

    private fun createResourceGroups(): Result<List<ResourceGroup>> = resourceGroups

    fun validateWiring(): Result<Unit> = when (val result = createResourceGroups()) {
        is Error<List<ResourceGroup>> -> Error(result.error)
        is Success<List<ResourceGroup>> -> registry.validateWiring(result.data)
    }

    private fun buildResourceGroups(): Result<List<ResourceGroup>> {
        val publicKey = SSHKeyUtils.publicKeyToOpenSSH(runtime.providers.sshKeyProvider().keyPair.public)

        val sshKey = HetznerSSHKey(sshKeyName(runtime.environmentContext), publicKey, cloudLabels(runtime.environmentContext))

        val firewall = HetznerFirewall(
            firewallName(runtime.environmentContext, "ssh"),
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
            cloudLabels(runtime.environmentContext),
            cloudLabels(runtime.environmentContext),
        )

        val network = HetznerNetwork(networkName(runtime.environmentContext), defaultNetwork)
        val subnet = HetznerSubnet(defaultServiceSubnet, network.asLookup())

        val cloudResourceGroup = ResourceGroup(
            "cloud '${runtime.environmentContext.cloud} base resources'",
            listOf(sshKey, firewall, network, subnet, backupSecretResource(runtime)),
        )

        val serviceResourceGroups = serviceManagers().map { (service, manager) ->
            manager.createResources(runtime, service, context).map { resources ->
                ResourceGroup(
                    "service '${service.name}'",
                    resources,
                    setOf(cloudResourceGroup),
                )
            }
        }

        return if (serviceResourceGroups.hasError()) {
            Error(serviceResourceGroups.aggregateErrorMessage())
        } else {
            Success(listOf(cloudResourceGroup) + serviceResourceGroups.mapSuccess())
        }
    }

    private fun serviceManagers() = runtime.services.map {
        val manager: ServiceManager<ServiceConfiguration, ServiceConfigurationRuntime> = serviceRegistrations.managerForService(it)
        it to manager
    }

    private fun createProvisioner() = Provisioner(registry, serviceRegistrations)

    private fun createRegistry(): ProvisionersRegistry {
        val providerProvisioners = providerRegistrations.createProvisioners(runtime.providers)
        val providerLookups = providerRegistrations.createLookups(runtime.providers)

        val serviceProvisioners = runtime.services.flatMap {
            val manager: ServiceManager<ServiceConfiguration, ServiceConfigurationRuntime> = serviceRegistrations.managerForService(it)
            manager.createProvisioners(it)
        }

        val defaultProvisioners = listOf(
            GarageFsBucketProvisioner(),
            GarageFsAccessKeyProvisioner(),
            GarageFsPermissionProvisioner(),
            GarageFsLayoutProvisioner(),
            PostgresUserProvisioner(),
            PostgresDatabaseProvisioner(),
        )

        val lookups = providerLookups + listOf(UserDataLookupProvider()) + (providerProvisioners + serviceProvisioners + defaultProvisioners).filterIsInstance<
            InfrastructureResourceLookupProvider<InfrastructureResourceLookup<BaseInfrastructureResourceRuntime>, BaseInfrastructureResourceRuntime>,
            >()

        return ProvisionersRegistry(
            lookups,
            providerProvisioners + defaultProvisioners + serviceProvisioners,
        )
    }

    fun createSSHConfig(): Result<Path> {
        val sshConfigFile = sshConfigFilePath(
            runtime.context.configFileDirectory,
            runtime.environmentContext,
        )

        val sshKnownHostsFile = sshKnownHosts(
            runtime.context.configFileDirectory,
            runtime.environmentContext,
        )

        val resourceGroups = when (val result = createResourceGroups()) {
            is Error<List<ResourceGroup>> -> return Error(result.error)
            is Success<List<ResourceGroup>> -> result.data
        }

        val sshConfig = StringWriter()
        sshConfig.appendLine("Host *")
        sshConfig.appendLine("  User root")
        sshConfig.appendLine("  IdentityFile ${context.sshKeyAbsolutePath}")
        sshConfig.appendLine("  UserKnownHostsFile ${sshKnownHostsFile.absolutePathString()}")
        sshConfig.appendLine("  StrictHostKeyChecking yes")
        sshConfig.appendLine("  IdentitiesOnly yes")
        sshConfig.appendLine("")

        val sshKnownHosts = StringWriter()
        val servers = resourceGroups.flatMap { it.hierarchicalResourceList().filterIsInstance<HetznerServer>() }

        val serversIps = servers.mapNotNull { context.lookup(it.asLookup()) }.map { it.name to it.publicIpv4 }

        serversIps.forEach {
            val rsaSecretPath = sshHostPrivateKeySecretPath(context.environment, it.first, KeyType.rsa)
            val rsaSecret = context.lookup(GenericSecretLookup(rsaSecretPath))

            val ed25519SecretPath = sshHostPrivateKeySecretPath(context.environment, it.first, KeyType.ed25519)
            val ed25519Secret = context.lookup(GenericSecretLookup(ed25519SecretPath))

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
            Success<Path>(sshConfigFile)
        } catch (e: Exception) {
            Error<Path>(e.message ?: "unknown error")
        }
    }

    override fun close() {
        context.close()
    }
}
