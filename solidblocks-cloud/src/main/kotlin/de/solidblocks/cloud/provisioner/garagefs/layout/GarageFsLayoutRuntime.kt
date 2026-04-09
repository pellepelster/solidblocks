package de.solidblocks.cloud.provisioner.garagefs.layout

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

class GarageFsLayoutRuntime(val name: String, val nodes: List<String>) : BaseInfrastructureResourceRuntime()
