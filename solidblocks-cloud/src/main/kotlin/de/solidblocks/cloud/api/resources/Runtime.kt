package de.solidblocks.cloud.api.resources

import de.solidblocks.cloud.api.endpoint.Endpoint

abstract class BaseLabeledInfrastructureResourceRuntime(val labels: Map<String, String>, endpoints: List<Endpoint> = emptyList()) : BaseInfrastructureResourceRuntime(endpoints) {
}

abstract class BaseInfrastructureResourceRuntime(val endpoints: List<Endpoint> = emptyList()) {
}
