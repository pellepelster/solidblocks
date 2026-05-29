package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime

class PassSecretRuntime(val name: String, val secret: String) : GenericSecretRuntime()
