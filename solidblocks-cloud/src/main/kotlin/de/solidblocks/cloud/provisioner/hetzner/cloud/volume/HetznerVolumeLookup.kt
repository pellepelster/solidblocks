package de.solidblocks.cloud.provisioner.hetzner.cloud.volume

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class HetznerVolumeLookup(name: String) : InfrastructureResourceLookup<HetznerVolumeRuntime>(name, emptySet())
