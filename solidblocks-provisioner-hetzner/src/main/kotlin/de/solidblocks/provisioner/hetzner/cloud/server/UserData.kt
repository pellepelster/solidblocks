package de.solidblocks.provisioner.hetzner.cloud.server

import de.solidblocks.core.IResourceLookup

data class UserDataRuntime(val staticUserData: String, val ephemeralUserData: String)

class UserData(
    val resourceFile: String,
    val staticVariables: HashMap<String, IResourceLookup<String>>,
    val ephemeralVariables: HashMap<String, IResourceLookup<String>>
) :
    IResourceLookup<UserDataRuntime> {

    override val name = resourceFile

    override val parents = ephemeralVariables.values.toSet() + staticVariables.values.toSet()
}
