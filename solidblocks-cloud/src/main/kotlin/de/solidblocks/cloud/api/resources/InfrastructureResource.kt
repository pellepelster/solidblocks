package de.solidblocks.cloud.api.resources

abstract class InfrastructureResource<RuntimeType> : ResourceLookup<RuntimeType> {
  var tainted = false

  fun taint() {
    tainted = true
  }
}

abstract class LabeledInfrastructureResource<RuntimeType>(
    open val labels: Map<String, String>
) : InfrastructureResource<RuntimeType>()
