package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.InfrastructureResourceRuntime

data class GarageFsAccessKeyRuntime(val name: String, val id: String, val secretAccessKey: String) :
    InfrastructureResourceRuntime
