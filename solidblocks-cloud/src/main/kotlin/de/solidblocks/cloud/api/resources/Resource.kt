package de.solidblocks.cloud.api.resources

import de.solidblocks.cloud.api.endpoint.Endpoint

interface Resource {

  val dependsOn: Set<Resource>
    get() = emptySet()

  val name: String

  fun logText(): String {
    var simpleName = this.javaClass.simpleName.removeSuffix("Lookup").lowercase()

    if (simpleName.isEmpty()) {
      simpleName = this.javaClass.superclass.simpleName.lowercase()
    }

    return "$simpleName '${this.name}'"
  }

  fun getInfrastructureDependsOn(): List<InfrastructureResource<*>> =
      this.dependsOn.filterIsInstance<InfrastructureResource<*>>()
}

interface LabeledInfrastructureResourceRuntime : InfrastructureResourceRuntime {
  val labels: Map<String, String>
}

interface InfrastructureResourceRuntime {
  fun endpoints(): List<Endpoint> = emptyList()
}
