package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.ResourceLookup

data class SecretLookup(override val name: String) : ResourceLookup<SecretRuntime>
