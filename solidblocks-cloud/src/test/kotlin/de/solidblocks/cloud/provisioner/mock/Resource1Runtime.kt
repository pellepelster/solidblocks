package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

class Resource1Runtime(val name: String, endpoints: List<Endpoint>) :
    BaseInfrastructureResourceRuntime(endpoints)
