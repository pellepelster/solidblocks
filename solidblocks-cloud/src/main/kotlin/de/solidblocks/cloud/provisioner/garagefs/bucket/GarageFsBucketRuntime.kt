package de.solidblocks.cloud.provisioner.garagefs.bucket

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

data class GarageFsBucketRuntime(val name: String, val id: String, val websiteAccess: Boolean, val globalAliases: List<String>) : BaseInfrastructureResourceRuntime()
