package de.solidblocks.cloud.provisioner.hetzner.cloud.ssh

import de.solidblocks.cloud.api.resources.ResourceLookup

data class HetznerSSHKeyLookup(override val name: String) : ResourceLookup<HetznerSSHKeyRuntime>
