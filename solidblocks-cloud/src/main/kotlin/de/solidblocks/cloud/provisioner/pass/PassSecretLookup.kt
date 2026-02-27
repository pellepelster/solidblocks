package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class PassSecretLookup(name: String) : InfrastructureResourceLookup<PassSecretRuntime>(name, emptySet())
