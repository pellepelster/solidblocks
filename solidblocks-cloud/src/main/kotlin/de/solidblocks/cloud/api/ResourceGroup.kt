package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.health.HealthCheck
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.api.resources.BaseResource
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.TopologicalOrderIterator
import java.util.*

fun Map<ResourceGroup, List<ResourceDiff>>.allDiffs() = this.flatMap { it.value }

fun Map<ResourceGroup, List<ResourceDiff>>.allChangedOrMissingDiffs() =
    this.allDiffs().filter {
        when (it.status) {
            missing -> true
            has_changes -> true
            else -> false
        }
    }

fun Map<ResourceGroup, List<ResourceDiff>>.allChangedOrMissingResources() =
    this.allChangedOrMissingDiffs().map { it.resource }

fun ResourceGroup.logText() = "resource group '$name'"

data class ResourceGroup(
    val name: String = UUID.randomUUID().toString(),
    val resources: List<BaseInfrastructureResource<*>> = ArrayList(),
    val dependsOn: Set<ResourceGroup> = emptySet(),
    val readinessHealthChecks: List<HealthCheck> = emptyList(),
) {

    fun hierarchicalResourceList() = this.resources.hierarchicalResourceList()

    override fun toString(): String = name
}

fun List<BaseResource>.hierarchicalResourceList(): List<BaseResource> {

    val graph = this.createResourceGraph()
    val orderIterator = TopologicalOrderIterator(graph)

    val result = ArrayList<BaseResource>()
    orderIterator.forEachRemaining { result.add(it) }
    result.reverse()

    return result
}

private fun List<BaseResource>.createResourceGraph():
        Graph<BaseResource, DefaultEdge> {
    val graph: Graph<BaseResource, DefaultEdge> =
        DefaultDirectedGraph(DefaultEdge::class.java)

    val allResources = ArrayList<BaseResource>()
    flattenModels(allResources, this)

    val infrastructureResources = allResources.filterIsInstance<BaseInfrastructureResource<*>>().toSet()
    val infrastructureLookupResources = allResources.filterIsInstance<InfrastructureResourceLookup<*>>().toSet()

    val lookupMappings = infrastructureLookupResources.map { lookup ->
        lookup to infrastructureResources.firstOrNull() { it.lookupType == lookup::class }
    }.toMap()


    allResources.forEach { graph.addVertex(it) }

    allResources.forEach { resource ->
        resource.dependsOn.forEach { target ->
            when (target) {
                is BaseInfrastructureResource<*> -> graph.addEdge(resource, target)
                is InfrastructureResourceLookup<*> -> {
                    if (lookupMappings[target] != null) {
                        graph.addEdge(resource, lookupMappings[target])
                    }
                }
            }
        }
    }

    return graph
}

private fun flattenModels(
    allModels: ArrayList<BaseResource>,
    models: List<BaseResource>,
) {
    models.forEach { model ->
        if (!allModels.contains(model)) {
            allModels.add(model)
            flattenModels(allModels, model.dependsOn.toList())
        }
    }
}
