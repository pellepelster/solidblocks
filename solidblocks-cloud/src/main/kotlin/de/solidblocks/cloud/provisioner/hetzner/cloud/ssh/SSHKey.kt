package de.solidblocks.cloud.provisioner.hetzner.cloud.ssh

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResource

class SSHKey(override val name: String, val publicKey: String, labels: Map<String, String>) :
    LabeledInfrastructureResource<SSHKey, SSHKeyRuntime>(labels) {

  fun asLookup() = SSHKeyLookup(name)

  override fun logText() = "SSH key '$name'"
}
