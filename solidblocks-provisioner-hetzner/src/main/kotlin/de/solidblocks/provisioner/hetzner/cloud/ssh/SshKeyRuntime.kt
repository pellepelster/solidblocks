package de.solidblocks.provisioner.hetzner.cloud.ssh

data class SshKeyRuntime(val id: String) {

    fun id(): String {
        return id
    }
}
