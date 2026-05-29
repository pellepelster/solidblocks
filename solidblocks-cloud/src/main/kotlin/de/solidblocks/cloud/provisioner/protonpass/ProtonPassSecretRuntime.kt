package de.solidblocks.cloud.provisioner.protonpass

import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime

class ProtonPassSecretRuntime(
    name: String,
    secret: String,
    val shareId: String,
    val itemId: String,
) : GenericSecretRuntime(name, secret)
