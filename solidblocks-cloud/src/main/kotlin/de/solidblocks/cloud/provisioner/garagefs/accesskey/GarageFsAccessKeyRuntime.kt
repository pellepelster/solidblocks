package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

class GarageFsAccessKeyRuntime(val name: String, val id: String, val secretAccessKey: String) : BaseInfrastructureResourceRuntime()
