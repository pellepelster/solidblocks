package de.solidblocks.api.resources.infrastructure.network

import de.solidblocks.core.IResourceLookup

interface IFloatingIpAssignmentLookup : IResourceLookup<FloatingIpAssignmentRuntime> {
    fun floatingIp(): IFloatingIpLookup
}
