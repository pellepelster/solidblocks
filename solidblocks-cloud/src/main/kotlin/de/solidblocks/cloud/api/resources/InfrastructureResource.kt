package de.solidblocks.cloud.api.resources

abstract class InfrastructureResource<ResourceType, RuntimeType> : Resource {
  var tainted = false

  fun taint() {
    tainted = true
  }
}

abstract class LabeledInfrastructureResource<ResourceType, RuntimeType>(
    open val labels: Map<String, String>
) : InfrastructureResource<ResourceType, RuntimeType>()
