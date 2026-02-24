package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.ResourceLookup

data class PassSecretLookup(override val name: String) : ResourceLookup<PassSecretRuntime>
