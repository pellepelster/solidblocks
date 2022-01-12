package de.solidblocks.provisioner

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.api.resources.allChangedOrMissingResources
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.InfrastructureProvisioner
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.core.*
import de.solidblocks.provisioner.hetzner.cloud.floatingip.FloatingIp
import de.solidblocks.provisioner.hetzner.cloud.floatingip.FloatingIpAssignment
import de.solidblocks.provisioner.hetzner.cloud.network.Network
import de.solidblocks.provisioner.hetzner.cloud.server.Server
import de.solidblocks.provisioner.hetzner.cloud.ssh.SshKey
import de.solidblocks.provisioner.hetzner.cloud.volume.Volume
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import mu.KotlinLogging
import java.time.Duration
import java.util.function.Supplier

class Provisioner(
    private val provisionerRegistry: ProvisionerRegistry,
    healthCheckWait: Duration = Duration.ofSeconds(10)
) : InfrastructureProvisioner {

    private val logger = KotlinLogging.logger {}

    private val appliedResources: MutableList<IResource> = mutableListOf()

    private val healthCheckRetryConfig = RetryConfig.custom<Boolean>()
        .maxAttempts(15)
        .waitDuration(healthCheckWait)
        .retryOnResult { !it }
        .build()

    private val healthCheckRetryRegistry: RetryRegistry = RetryRegistry.of(healthCheckRetryConfig)

    private val resourceGroups = mutableListOf<ResourceGroup>()

    fun createResourceGroup(name: String, dependsOn: Set<ResourceGroup> = emptySet()): ResourceGroup {
        return ResourceGroup(name, dependsOn).apply {
            resourceGroups.add(this)
        }
    }

    override fun <LookupType : IResourceLookup<RuntimeType>, RuntimeType> lookup(resource: LookupType): Result<RuntimeType> =
        try {
            this.provisionerRegistry.datasource(resource).lookup(resource)
        } catch (e: Exception) {
            logger.error(e) { "lookup for ${resource.logName()} failed" }
            Result(failed = true, message = e.message)
        }

    fun <ResourceType : IResource, ReturnType> provisioner(resource: ResourceType): IInfrastructureResourceProvisioner<ResourceType, ReturnType> {
        return this.provisionerRegistry.provisioner(resource)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun apply(): Boolean {
        appliedResources.clear()

        logger.info {
            "applying resource groups ${resourceGroups.joinToString(", ") { it.name }}"
        }

        val allDiffs = HashMap<ResourceGroup, List<ResourceDiff>>()

        for (resourceGroup in resourceGroups) {

            val diffs = diffForResourceGroup(resourceGroup, allDiffs) ?: return false
            logger.info {
                "resource group '${resourceGroup.name}', changed resources: ${
                diffs.filter { it.hasChanges() }.joinToString(", ") { it.toString() }.ifEmpty { "<none>" }
                }, missing resources: ${
                diffs.filter { it.isMissing() }.joinToString(", ") { it.resource.name }.ifEmpty { "<none>" }
                }"
            }

            allDiffs[resourceGroup] = diffs

            val diffsForAllLayersWithChanges =
                allDiffs.flatMap { it.value }.filter { it.hasChangesOrMissing() }

            val resourcesThatDependOnResourcesWithChanges = resourceGroup.resources.filter {
                it.getInfraParents().any { parent -> diffsForAllLayersWithChanges.any { it.resource == parent } }
            }

            val o = resourcesThatDependOnResourcesWithChanges.map {
                ResourceDiff(
                    it,
                    changes = listOf(ResourceDiffItem("parent", changed = true))
                )
            }

            val resourcesToApply = allDiffs[resourceGroup]!! + o

            val result = apply(resourcesToApply)

            if (!result) {
                return false
            }
        }

        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun apply(diffs: List<ResourceDiff>): Boolean {

        for (diffToDestroy in diffs.filter { it.needsRecreate() }) {
            val resource = diffToDestroy.resource as IInfrastructureResource<IResource, Any>

            logger.info { "destroying ${resource.logName()}" }
            val result = this.provisionerRegistry.provisioner<IResource, Any>(resource).destroy(resource)
            if (!result) {
                logger.error { "destroying ${resource.logName()} failed" }
                return false
            }
        }

        for (diffWithChange in diffs.filter { it.hasChangesOrMissing() }) {

            val resource = diffWithChange.resource as IInfrastructureResource<Any, Any>

            if (appliedResources.contains(resource)) {
                logger.info { "${resource.logName()} already applied" }
                continue
            }

            logger.info { "applying ${resource.logName()}" }

            try {
                val result = this.provisionerRegistry.provisioner<IResource, Any>(resource).apply(resource)

                if (result.failed) {
                    logger.error { "applying ${diffWithChange.resource.logName()} failed, result was: '${result.logText()}'" }
                    return false
                }

                if (!runHealthCheckIfNeeded(resource)) {
                    return false
                }

                appliedResources.add(resource)
            } catch (e: Exception) {
                logger.error(e) { "apply failed for resource ${diffWithChange.resource.logName()}" }
                return false
            }
        }

        return true
    }

    private fun runHealthCheckIfNeeded(resource: IInfrastructureResource<Any, Any>): Boolean {
        val healthCheck = resource.healthCheck ?: return true

        logger.info { "running healthcheck for ${resource.logName()}" }

        val decorateFunction: Supplier<Boolean> =
            Retry.decorateSupplier(healthCheckRetryRegistry.retry("healthcheck-${resource.name}")) { healthCheck.invoke() }

        val result = decorateFunction.get()
        if (!result) {
            logger.error { "healthcheck for ${resource.logName()} failed" }
            return false
        }

        return true
    }

    private fun diffForResourceGroup(
        resourceGroup: ResourceGroup,
        allDiffs: Map<ResourceGroup, List<ResourceDiff>>
    ): List<ResourceDiff>? {
        logger.info { "creating diff for resource group '${resourceGroup.name}'" }

        val changedOrMissingResources = allDiffs.allChangedOrMissingResources()

        for (parentResourceGroup in resourceGroup.dependsOn) {
            val healthCheckResults =
                parentResourceGroup.resources.map { runHealthCheckIfNeeded(it as IInfrastructureResource<Any, Any>) }

            if (healthCheckResults.any { !it }) {
                logger.error { "at least one healthcheck for resource group '${parentResourceGroup.name}' failed" }
                return null
            }
        }

        val resources = resourceGroup.hierarchicalResourceList().toSet()
        val result = mutableListOf<ResourceDiff>()

        for (resource in resources) {
            val allInfraParents = resource.getAllInfraParents()
            val missingOrChangedParents =
                allInfraParents.filter { changedOrMissingResources.contains(it) } + result.filter { it.hasChangesOrMissing() }
                    .map { it.resource }

            if (missingOrChangedParents.isNotEmpty()) {
                logger.info {
                    "skipping diff for ${resource.name} the following dependencies were missing or changed: ${
                    missingOrChangedParents.joinToString(
                        ", "
                    ) { it.name }
                    } "
                }

                result.add(ResourceDiff(resource, missing = true))

                continue
            }

            try {
                logger.info { "creating diff for ${resource.logName()}" }
                val diff = this.provisionerRegistry.provisioner<IResource, Any>(resource).diff(resource)

                if (diff.failed) {
                    logger.error { "diff failed for ${resource.logName()} ${diff.logText()}" }
                    return null
                }

                result.add(diff.result!!)
            } catch (e: Exception) {
                logger.error(e) { "diff failed for resource ${resource.logName()}" }
                return null
            }
        }

        return result
    }

    fun destroy(destroyVolumes: Boolean): Boolean {
        this.resourceGroups.forEach {
            it.resources.forEach {
                logger.info { "destroying ${it.logName()}" }
                // this.provisionerRegistry.provisioner(it)
            }
        }

        return true
    }

    fun destroyAll(destroyVolumes: Boolean): Boolean {
        if (!this.provisionerRegistry.provisioner(FloatingIpAssignment::class).destroyAll()) {
            return false
        }

        if (!this.provisionerRegistry.provisioner(Server::class).destroyAll()) {
            return false
        }

        if (destroyVolumes) {
            if (!this.provisionerRegistry.provisioner(Volume::class).destroyAll()) {
                return false
            }
        }

        if (!this.provisionerRegistry.provisioner(Network::class).destroyAll()) {
            return false
        }
        if (!this.provisionerRegistry.provisioner(SshKey::class).destroyAll()) {
            return false
        }
        if (!this.provisionerRegistry.provisioner(FloatingIp::class).destroyAll()) {
            return false
        }

        return true
    }

    fun addResourceGroup(resourceGroup: ResourceGroup) {
        resourceGroups.add(resourceGroup)
    }
}
