package de.solidblocks.provisioner.consul.policy

data class Policy(val type: String, val path: String, val privilege: Privileges)
