package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.health.HealthCheck
import de.solidblocks.cloud.api.resources.InfrastructureResource
import java.util.*
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.TopologicalOrderIterator

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
    val resources: List<InfrastructureResource<*, *>> = ArrayList(),
    val dependsOn: Set<ResourceGroup> = emptySet(),
    val readinessHealthChecks: List<HealthCheck> = emptyList(),
) {

  fun hierarchicalResourceList() = this.resources.hierarchicalResourceList()

  override fun toString(): String = name
}

fun List<InfrastructureResource<*, *>>.hierarchicalResourceList():
    List<InfrastructureResource<*, *>> {
  val graph = this.createResourceGraph()
  val orderIterator = TopologicalOrderIterator(graph)

  val result = ArrayList<InfrastructureResource<*, *>>()
  orderIterator.forEachRemaining { result.add(it as InfrastructureResource<*, *>) }

  result.reverse()
  return result
}

private fun List<InfrastructureResource<*, *>>.createResourceGraph():
    Graph<InfrastructureResource<*, *>, DefaultEdge> {
  val graph: Graph<InfrastructureResource<*, *>, DefaultEdge> =
      DefaultDirectedGraph(DefaultEdge::class.java)

  val allResources = ArrayList<InfrastructureResource<*, *>>()
  flattenModels(allResources, this)

  allResources.forEach { graph.addVertex(it) }

  allResources.forEach { source ->
    source.getInfrastructureDependsOn().forEach { target ->
      graph.addEdge(source, target as InfrastructureResource<*, *>?)
    }
  }

  return graph
}

private fun flattenModels(
    allModels: ArrayList<InfrastructureResource<*, *>>,
    models: List<InfrastructureResource<*, *>>,
) {
  models.forEach { model ->
    if (!allModels.contains(model)) {
      allModels.add(model)
      flattenModels(allModels, model.getInfrastructureDependsOn())
    }
  }
}
