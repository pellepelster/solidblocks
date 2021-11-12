package de.solidblocks.api.resources

import de.solidblocks.core.IInfrastructureResource

class ResourceStatus<ReturnType>(
    val resource: IInfrastructureResource<ReturnType>,
)
