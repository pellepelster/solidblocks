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
import de.solidblocks.core.Result
import de.solidblocks.core.logName
import mu.KotlinLogging
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.TopologicalOrderIterator
import org.springframework.stereotype.Component

@Component
class Provisioner(private val provisionerRegistry: ProvisionerRegistry) {

    private val logger = KotlinLogging.logger {}

    private val layers = mutableListOf<ResourceLayer>()
    private val providers = mutableListOf<IInfrastructureClientProvider<*>>()

    fun createLayer(name: String): ResourceLayer {
        val layer = ResourceLayer(name)
        layers.add(layer)
        return layer
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

    fun <ResourceType : IInfrastructureResource<RuntimeType>, RuntimeType> lookup(resource: ResourceType): Result<RuntimeType> =
            try {
                this.provisionerRegistry.provisioner(resource::class).lookup(resource)
            } catch (e: Exception) {
                logger.error(e) { "lookup for ${resource.logName()} failed" }
                Result(resource, failed = true, message = e.message)
            }

    fun <ResourceType : IInfrastructureResource<RuntimeType>, RuntimeType> diffForLayer(resource: ResourceType): Result<ResourceDiff<RuntimeType>> =
            try {
                this.provisionerRegistry.provisioner(resource::class).diff(resource)
            } catch (e: Exception) {
                logger.error(e) { "diff for ${resource.logName()} failed" }
                Result(resource, failed = true, message = e.message)
            }

    fun <DataSourceType : IDataSource<ReturnType>, ReturnType> lookup(resource: DataSourceType): Result<ReturnType> {
        return this.provisionerRegistry.datasource(resource::class).lookup(resource)
    }

    fun <ResourceType : IInfrastructureResource<ReturnType>, ReturnType> provisioner(resource: ResourceType): IInfrastructureResourceProvisioner<IInfrastructureResource<ReturnType>, ReturnType> {
        return this.provisionerRegistry.provisioner(resource::class)
    }

    fun <ReturnType, DataSourceType : IDataSource<ReturnType>> datasource(resource: DataSourceType): IDataSourceLookup<IDataSource<ReturnType>, ReturnType> {
        return this.provisionerRegistry.datasource(resource::class)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun apply(): Boolean {
        logger.info {
            "applying layers : ${layers.joinToString(", ") { it.name }}"
        }

        val allLayerDiffs = HashMap<ResourceLayer, MutableList<Result<ResourceDiff<Any>>>>()

        return layers.map { layer ->

            diffForLayer(layer, allLayerDiffs)

            val allFailedDiffs = allLayerDiffs.flatMap { it.value }.filter { it.isEmptyOrFailed() }
            if (allFailedDiffs.isNotEmpty()) {
                logger.error {
                    "diffing the following resources failed: ${
                        allFailedDiffs.map { it.resource.logName() }
                    }"
                }

                return@map false
            }

            val diffsForAllLayersWithChanges =
                    allLayerDiffs.flatMap { it.value }.map { it.result!! }.filter { it.hasChanges() }
            val resourcesThatDependOnResourcesWithChanges = layer.resources.filter {
                it.getParents().any { parent -> diffsForAllLayersWithChanges.any { it.resource == parent } }
            } as List<IInfrastructureResource<Any>>

            val o = resourcesThatDependOnResourcesWithChanges.map {
                ResourceDiff(
                        it,
                        changes = listOf(ResourceDiffItem("parent", changed = true))
                )
            }

            return@map apply(allLayerDiffs[layer]!!.map { it.result!! } + o)
        }.all { it }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun apply(diffs: List<ResourceDiff<Any>>): Boolean {

        val destroyResults = diffs.filter { it.needsRecreate() }.map {
            val resource = it.resource
            logger.info { "destroying ${resource.logName()}" }
            this.provisionerRegistry.provisioner<Any, Any>(resource).destroy(resource)
        }

        destroyResults.filter { it.isEmptyOrFailed() }.forEach {
            logger.info { "destroy failed for ${it.resource.logName()}, error was: '${it.errorMessage()}'" }
            return false
        }

        val results = diffs.filter { it.hasChanges() }.map {
            logger.info { "applying ${it.resource.logName()}" }

            try {
                val result = this.provisionerRegistry.provisioner(it.resource::class).apply(it.resource)
                if (result.failed) {
                    logger.error { "applying ${it.resource.logName()} failed, result was: '${result.errorMessage()}'" }
                }

                true
            } catch (e: Exception) {
                logger.error(e) { "apply failed for resource ${it.resource.logName()}" }
                false
            }
        }

        return results.all { it }
    }

    fun lookup(): List<Result<Any>> {
        return layers.map {
            val resources = createResourceList(it.resources)

            return resources.map {
                val lookup = this.lookup(it)
                logger.info { "lookup of ${it.logName()} returned: ${lookup.result ?: "<none>"}" }
                lookup
            }
        }
    }

    private fun getParents(resource: IInfrastructureResource<*>): MutableList<IInfrastructureResource<*>> {
        val parents = mutableListOf<IInfrastructureResource<*>>()
        resource.getParents().forEach { getParents(it, parents) }
        return parents
    }

    private fun getParents(resource: IInfrastructureResource<*>, parents: MutableList<IInfrastructureResource<*>>) {
        parents.add(resource)
        resource.getParents().forEach { getParents(it, parents) }
    }

    private fun diffForLayer(
            layer: ResourceLayer,
            allLayerDiffs: HashMap<ResourceLayer, MutableList<Result<ResourceDiff<Any>>>>
    ) {
        logger.info { "creating diff for layer '${layer.name}'" }

        val resources = createResourceList(layer.resources)

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

                val diff = this.provisionerRegistry.provisioner<Any, Any>(resource).diff(resource)

                logger.info { diff.result?.toString() ?: "no result for resource ${resource.logName()}" }

                allLayerDiffs.computeIfAbsent(layer) { mutableListOf() }.add(diff)
            } catch (e: Exception) {
                logger.error(e) { "diff failed for resource ${resource.logName()}" }
                allLayerDiffs.computeIfAbsent(layer) { mutableListOf() }.add(Result(resource, failed = true))
            }
        }
    }

    private fun createResourceList(resources: ArrayList<IInfrastructureResource<*>>): ArrayList<IInfrastructureResource<Any>> {
        val graph = createGraph(resources)
        val orderIterator = TopologicalOrderIterator(graph)

        val result = ArrayList<IInfrastructureResource<Any>>()
        orderIterator.forEachRemaining {
            result.add(it as IInfrastructureResource<Any>)
        }

        result.reverse()
        return result
    }

    fun destroyAll() {
        this.provisionerRegistry.provisioner(FloatingIpAssignment::class).destroyAll()
        this.provisionerRegistry.provisioner(Server::class).destroyAll()
        //this.provisionerRegistry.provisioner(Volume::class).destroyAll()
        this.provisionerRegistry.provisioner(Network::class).destroyAll()
        this.provisionerRegistry.provisioner(SshKey::class).destroyAll()
        this.provisionerRegistry.provisioner(FloatingIp::class).destroyAll()
    }

    private fun flattenModels(
            allModels: ArrayList<IInfrastructureResource<*>>,
            models: List<IInfrastructureResource<*>>
    ) {
        models.forEach { model ->
            if (!allModels.contains(model)) {
                allModels.add(model)
                flattenModels(allModels, model.getParents() as List<IInfrastructureResource<Any>>)
            }
        }
    }

    private fun createGraph(resources: List<IInfrastructureResource<*>>): Graph<IInfrastructureResource<*>, DefaultEdge> {
        val graph: Graph<IInfrastructureResource<*>, DefaultEdge> = DefaultDirectedGraph(DefaultEdge::class.java)

        val allResources = ArrayList<IInfrastructureResource<*>>()
        flattenModels(allResources, resources)

        allResources.forEach {
            graph.addVertex(it)
        }

        allResources.forEach { source ->
            source.getParents().forEach { target ->
                graph.addEdge(source, target as IInfrastructureResource<Any>?)
            }
        }

        return graph
    }

    fun clear() {
        layers.clear()
    }
}
