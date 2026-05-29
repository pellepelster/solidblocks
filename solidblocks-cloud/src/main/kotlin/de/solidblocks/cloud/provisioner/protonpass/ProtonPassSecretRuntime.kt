package de.solidblocks.cloud.provisioner.protonpass

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime

class ProtonPassSecretRuntime(
    val name: String,
    val secret: String,
    val shareId: String,
    val itemId: String,
) : GenericSecretRuntime()
