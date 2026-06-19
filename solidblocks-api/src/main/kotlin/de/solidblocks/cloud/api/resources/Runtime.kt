package de.solidblocks.cloud.api.resources

import de.solidblocks.cloud.api.endpoint.Endpoint

abstract class BaseLabeledInfrastructureResourceRuntime(val labels: Map<String, String>) : BaseInfrastructureResourceRuntime()

abstract class BaseInfrastructureResourceRuntime() {
    open fun logText(): String = this.toString()
}

interface EndpointResourceRuntime {
    val endpoints: List<Endpoint>
}
