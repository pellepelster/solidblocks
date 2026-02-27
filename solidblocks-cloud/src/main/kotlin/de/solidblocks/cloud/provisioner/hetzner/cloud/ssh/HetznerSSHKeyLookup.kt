package de.solidblocks.cloud.provisioner.hetzner.cloud.ssh

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class HetznerSSHKeyLookup(name: String) : InfrastructureResourceLookup<HetznerSSHKeyRuntime>(name, emptySet())
