package de.solidblocks.provisioner

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IDataSourceLookup
import de.solidblocks.api.resources.infrastructure.IInfrastructureClientProvider
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.compute.Server
import de.solidblocks.api.resources.infrastructure.network.FloatingIp
import de.solidblocks.api.resources.infrastructure.network.FloatingIpAssignment
import de.solidblocks.api.resources.infrastructure.network.Network
import de.solidblocks.api.resources.infrastructure.ssh.SshKey
import de.solidblocks.core.IDataSource
import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResource
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.logName
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
    private val providers = mutableListOf<IInfrastructureClientProvider<*>>()

    fun createResourceGroup(name: String, dependsOn: Set<ResourceGroup> = emptySet()): ResourceGroup {
        return ResourceGroup(name, dependsOn).apply {
            resourceGroups.add(this)
        }
    }

    fun addProvider(provider: IInfrastructureClientProvider<*>) {
        providers.add(provider)
    }

    fun <T> provider(type: Class<T>): IInfrastructureClientProvider<T> {
        val provider = providers.firstOrNull { it.providerType() == type }

        if (provider == null) {
            throw RuntimeException("no provider found of type '${type.name}'")
        }

        return provider as IInfrastructureClientProvider<T>
    }

    fun <ResourceType : IResource, RuntimeType> lookup(resource: ResourceType): Result<RuntimeType> =
            try {
                this.provisionerRegistry.provisioner<ResourceType, RuntimeType>(resource).lookup(resource)
            } catch (e: Exception) {
                logger.error(e) { "lookup for ${resource.logName()} failed" }
                Result(resource, failed = true, message = e.message)
            }

    fun <ResourceType : IResource, RuntimeType> lookup(resource: IInfrastructureResource<ResourceType, RuntimeType>): Result<RuntimeType> =
            try {
                this.provisionerRegistry.provisioner<IInfrastructureResource<ResourceType, RuntimeType>, RuntimeType>(resource).lookup(resource)
            } catch (e: Exception) {
                logger.error(e) { "lookup for ${resource.logName()} failed" }
                Result(resource, failed = true, message = e.message)
            }

    fun <DataSourceType : IDataSource<ReturnType>, ReturnType> lookup(resource: DataSourceType): Result<ReturnType> {
        return this.provisionerRegistry.datasource(resource::class).lookup(resource)
    }

    fun <ResourceType : IResource, ReturnType> provisioner(resource: ResourceType): IInfrastructureResourceProvisioner<ResourceType, ReturnType> {
        return this.provisionerRegistry.provisioner(resource)
    }

    fun <ReturnType, DataSourceType : IDataSource<ReturnType>> datasource(resource: DataSourceType): IDataSourceLookup<IDataSource<ReturnType>, ReturnType> {
        return this.provisionerRegistry.datasource(resource::class)
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
                it.getParents().any { parent -> diffsForAllLayersWithChanges.any { it.resource == parent } }
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

    fun lookup(): List<Result<*>> {
        return resourceGroups.map {
            val resources = createResourceList(it.resources)

            return resources.map {
                val lookup = this.lookup<IResource, Any>(it)
                logger.info { "lookup of ${it.logName()} returned: ${lookup.result ?: "<none>"}" }
                lookup
            }
        }
    }

    private fun getParents(resource: IInfrastructureResource<*, *>): MutableList<IInfrastructureResource<*, *>> {
        val parents = mutableListOf<IInfrastructureResource<*, *>>()
        resource.getParents().forEach { getParents(it, parents) }
        return parents
    }

    private fun getParents(resource: IInfrastructureResource<*, *>, parents: MutableList<IInfrastructureResource<*, *>>) {
        parents.add(resource)
        resource.getParents().forEach { getParents(it, parents) }
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
                val parents = getParents(resource)

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

    fun destroyAll() {
        this.provisionerRegistry.provisioner1(FloatingIpAssignment::class).destroyAll()
        this.provisionerRegistry.provisioner1(Server::class).destroyAll()
        //this.provisionerRegistry.provisioner(Volume::class).destroyAll()
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
                flattenModels(allModels, model.getParents() as List<IInfrastructureResource<*, *>>)
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
            source.getParents().forEach { target ->
                graph.addEdge(source, target as IInfrastructureResource<*, *>?)
            }
        }

        return graph
    }

    fun clear() {
        resourceGroups.clear()
    }
}
