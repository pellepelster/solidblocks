package de.solidblocks.provisioner.consul.acl

data class Policy(val type: String, val path: String, val privilege: Privileges)
