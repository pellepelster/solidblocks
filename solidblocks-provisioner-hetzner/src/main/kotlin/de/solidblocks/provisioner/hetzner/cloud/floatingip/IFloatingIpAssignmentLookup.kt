package de.solidblocks.provisioner.hetzner.cloud.floatingip

import de.solidblocks.core.IResourceLookup

interface IFloatingIpAssignmentLookup : IResourceLookup<FloatingIpAssignmentRuntime> {
    fun floatingIp(): IFloatingIpLookup
}
