package de.solidblocks.api.resources

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.getInfraParents
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.TopologicalOrderIterator

fun Map<ResourceGroup, List<ResourceDiff>>.allDiffs() = this.flatMap { it.value }

fun Map<ResourceGroup, List<ResourceDiff>>.allChangedOrMissingDiffs() = this.allDiffs().filter {
    it.needsApply()
}

fun Map<ResourceGroup, List<ResourceDiff>>.allChangedOrMissingResources() =
    this.allChangedOrMissingDiffs().map { it.resource }

fun Collection<ResourceGroup>.hierarchicalResourceGroupList(): List<ResourceGroup> {
    val graph: Graph<ResourceGroup, DefaultEdge> = DefaultDirectedGraph(DefaultEdge::class.java)

    this.forEach {
        graph.addVertex(it)
    }

    this.forEach { source ->
        source.dependsOn.forEach { target ->
            graph.addEdge(source, target)
        }
    }

    val orderIterator = TopologicalOrderIterator(graph)
    return orderIterator.asSequence().map { it }.toList()
}

data class ResourceGroup(
    val name: String,
    val dependsOn: Set<ResourceGroup> = emptySet(),
    val resources: ArrayList<IInfrastructureResource<*, *>> = ArrayList()
) {

    fun <T : IInfrastructureResource<*, *>> addResource(resource: T): T {
        this.resources.add(resource)

        return resource
    }

    fun hierarchicalResourceList(): List<IInfrastructureResource<*, *>> {
        val graph = createResourceGraph(resources)
        val orderIterator = TopologicalOrderIterator(graph)

        val result = ArrayList<IInfrastructureResource<*, *>>()
        orderIterator.forEachRemaining {
            result.add(it as IInfrastructureResource<*, *>)
        }

        result.reverse()
        return result
    }

    private fun createResourceGraph(resources: List<IInfrastructureResource<*, *>>): Graph<IInfrastructureResource<*, *>, DefaultEdge> {
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

    override fun toString(): String {
        return name
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
}
