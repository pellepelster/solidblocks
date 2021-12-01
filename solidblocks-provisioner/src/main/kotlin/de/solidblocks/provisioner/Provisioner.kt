package de.solidblocks.provisioner

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.compute.Volume
import de.solidblocks.api.resources.infrastructure.network.FloatingIp
import de.solidblocks.api.resources.infrastructure.network.FloatingIpAssignment
import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.core.*
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import mu.KotlinLogging
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.TopologicalOrderIterator
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.function.Supplier


@Component
class Provisioner(private val provisionerRegistry: ProvisionerRegistry) {

    private val logger = KotlinLogging.logger {}

    val retryConfig = RetryConfig.custom<Boolean>()
            .maxAttempts(15)
            .waitDuration(Duration.ofMillis(5000))
            .retryOnResult { !it }
            .build()

    val retryRegistry: RetryRegistry = RetryRegistry.of(retryConfig)

    private val resourceGroups = mutableListOf<ResourceGroup>()

    fun createResourceGroup(name: String, dependsOn: Set<ResourceGroup> = emptySet()): ResourceGroup {
        return ResourceGroup(name, dependsOn).apply {
            resourceGroups.add(this)
        }
    }

    fun <LookupType : IResourceLookup<RuntimeType>, RuntimeType> lookup(resource: LookupType): Result<RuntimeType> =
            try {
                this.provisionerRegistry.datasource(resource).lookup(resource)
            } catch (e: Exception) {
                logger.error(e) { "lookup for ${resource.logName()} failed" }
                Result(resource, failed = true, message = e.message)
            }

    fun <ResourceType : IResource, ReturnType> provisioner(resource: ResourceType): IInfrastructureResourceProvisioner<ResourceType, ReturnType> {
        return this.provisionerRegistry.provisioner(resource)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun apply(): Boolean {
        logger.info {
            "applying resource groups ${resourceGroups.joinToString(", ") { it.name }}"
        }

        val allLayerDiffs = HashMap<ResourceGroup, MutableList<Result<ResourceDiff>>>()

        for (resourceGroup in resourceGroups) {
            if (diffForResourceGroup(resourceGroup, allLayerDiffs).failed) {
                return false
            }

            val allFailedDiffs = allLayerDiffs.flatMap { it.value }.filter { it.isEmptyOrFailed() }
            if (allFailedDiffs.isNotEmpty()) {
                logger.error {
                    "diffing the following resources failed: ${
                        allFailedDiffs.map { it.resource.logName() }
                    }"
                }

                return false
            }

            val diffsForAllLayersWithChanges =
                    allLayerDiffs.flatMap { it.value }.map { it.result!! }.filter { it.hasChanges() }

            val resourcesThatDependOnResourcesWithChanges = resourceGroup.resources.filter {
                it.getInfraParents().any { parent -> diffsForAllLayersWithChanges.any { it.resource == parent } }
            }

            val o = resourcesThatDependOnResourcesWithChanges.map {
                ResourceDiff(
                        it,
                        changes = listOf(ResourceDiffItem("parent", changed = true))
                )
            }

            val result = apply(allLayerDiffs[resourceGroup]!!.map { it.result!! } + o)
            if (!result) {
                return false
            }

        }

        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun apply(diffs: List<ResourceDiff>): Boolean {

        diffs.filter { it.needsRecreate() }.forEach {
            val resource = it.resource as IInfrastructureResource<IResource, Any>
            logger.info { "destroying ${resource.logName()}" }
            val result = this.provisionerRegistry.provisioner<IResource, Any>(resource).destroy(resource)

            if (result.failed) {
                logger.error { "destroying ${resource.logName()} failed" }
                return false
            }
        }

        val results = diffs.filter { it.hasChanges() }.map {

            val resource = it.resource as IInfrastructureResource<Any, Any>

            logger.info { "applying ${resource.logName()}" }

            try {
                val result = this.provisionerRegistry.provisioner<IResource, Any>(resource).apply(resource)

                if (result.failed) {
                    logger.error { "applying ${it.resource.logName()} failed, result was: '${result.errorMessage()}'" }
                    return false
                }

                return@map true
            } catch (e: Exception) {
                logger.error(e) { "apply failed for resource ${it.resource.logName()}" }
                return false
            }
        }

        return results.all { it }
    }

    private fun getInfraParents(resource: IResource): List<IInfrastructureResource<*, *>> = getParentsInternal(resource).filterIsInstance<IInfrastructureResource<*, *>>()

    private fun getParentsInternal(resource: IResource): List<IResource> {
        val parents = mutableListOf<IResource>()
        resource.getParents().forEach { getParentsInternal(it, parents) }

        return parents.toList()
    }

    private fun getParentsInternal(resource: IResource, parents: MutableList<IResource>) {
        parents.add(resource)
        resource.getParents().forEach { getParentsInternal(it, parents) }
    }

    private fun diffForResourceGroup(
            resourceGroup: ResourceGroup,
            allLayerDiffs: HashMap<ResourceGroup, MutableList<Result<ResourceDiff>>>
    ): Result<List<String>> {
        logger.info { "creating diff for resource group '${resourceGroup.name}'" }

        resourceGroup.dependsOn.forEach { parentResourceGroup ->

            val resourcesWithHealthChecks = parentResourceGroup.resources.filter { it.getHealthCheck() != null }

            if (resourcesWithHealthChecks.isNotEmpty()) {
                logger.info { "resource group '${resourceGroup.name}' depends on resource group '${parentResourceGroup.name}', running healthchecks" }

                resourcesWithHealthChecks.forEach {
                    val decorateFunction: Supplier<Boolean> = Retry.decorateSupplier(retryRegistry.retry("healthcheck")) {
                        it.getHealthCheck()!!.invoke()
                    }

                    val result = decorateFunction.get()
                    if (!result) {
                        logger.error { "healthcheck for ${it.logName()} failed" }
                        return Result(resource = it, failed = true)
                    }
                }
            }
        }

        val resources = createResourceList(resourceGroup.resources)

        for (resource in resources) {
            try {
                val parents = getInfraParents(resource)

                val nonFailedMissingDiffs =
                        allLayerDiffs.flatMap { it.value }.filter { !it.isEmptyOrFailed() }.map { it.result!! }
                                .filter { it.missing }

                val missingParents =
                        parents.filter { parent -> nonFailedMissingDiffs.any { it.resource == parent } }

                if (missingParents.isNotEmpty()) {
                    logger.info {
                        "skipping diff for ${resource.logName()} the following dependencies were missing: ${
                            missingParents.joinToString(
                                    ", "
                            ) { it.logName() }
                        } "
                    }
                    continue
                }

                val diff = this.provisionerRegistry.provisioner<IResource, Any>(resource).diff(resource)

                if (diff.failed) {
                    logger.info { "diff failed for ${resource.logName()}" }
                    return Result(resource = resource, failed = true)

                }
                logger.info { diff.result?.toString() ?: "no result for resource ${resource.logName()}" }

                allLayerDiffs.computeIfAbsent(resourceGroup) { mutableListOf() }.add(diff)
            } catch (e: Exception) {
                logger.error(e) { "diff failed for resource ${resource.logName()}" }
                allLayerDiffs.computeIfAbsent(resourceGroup) { mutableListOf() }.add(Result(resource, failed = true))
            }
        }

        return Result(resource = NullResource, failed = false)
    }

    private fun createResourceList(resources: ArrayList<IInfrastructureResource<*, *>>): ArrayList<IInfrastructureResource<*, *>> {
        val graph = createGraph(resources)
        val orderIterator = TopologicalOrderIterator(graph)

        val result = ArrayList<IInfrastructureResource<*, *>>()
        orderIterator.forEachRemaining {
            result.add(it as IInfrastructureResource<*, *>)
        }

        result.reverse()
        return result
    }

    fun destroyAll(destroyVolumes: Boolean) {
        this.provisionerRegistry.provisioner1(FloatingIpAssignment::class).destroyAll()
        this.provisionerRegistry.provisioner1(Server::class).destroyAll()

        if (destroyVolumes) {
            this.provisionerRegistry.provisioner1(Volume::class).destroyAll()
        }

        this.provisionerRegistry.provisioner1(Network::class).destroyAll()
        this.provisionerRegistry.provisioner1(SshKey::class).destroyAll()
        this.provisionerRegistry.provisioner1(FloatingIp::class).destroyAll()
    }

    private fun flattenModels(
            allModels: ArrayList<IInfrastructureResource<*, *>>,
            models: List<IInfrastructureResource<*, *>>
    ) {
        models.forEach { model ->
            if (!allModels.contains(model)) {
                allModels.add(model)
                flattenModels(allModels, model.getInfraParents())
            }
        }
    }

    private fun createGraph(resources: List<IInfrastructureResource<*, *>>): Graph<IInfrastructureResource<*, *>, DefaultEdge> {
        val graph: Graph<IInfrastructureResource<*, *>, DefaultEdge> = DefaultDirectedGraph(DefaultEdge::class.java)

        val allResources = ArrayList<IInfrastructureResource<*, *>>()
        flattenModels(allResources, resources)

        allResources.forEach {
            graph.addVertex(it)
        }

        allResources.forEach { source ->
            source.getInfraParents().forEach { target ->
                graph.addEdge(source, target as IInfrastructureResource<*, *>?)
            }
        }

        return graph
    }

    fun clear() {
        resourceGroups.clear()
    }
}
