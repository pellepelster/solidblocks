package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.resources.InfrastructureResourceRuntime

data class GarageFsBucketRuntime(val name: String, val id: String, val websiteAccess: Boolean, val globalAliases: List<String>) : InfrastructureResourceRuntime
