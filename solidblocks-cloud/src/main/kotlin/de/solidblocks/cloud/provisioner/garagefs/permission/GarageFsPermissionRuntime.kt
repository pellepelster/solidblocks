package de.solidblocks.cloud.provisioner.garagefs.permission

import de.solidblocks.cloud.api.resources.InfrastructureResourceRuntime
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyRuntime
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketRuntime

data class GarageFsPermissionRuntime(
    val bucket: GarageFsBucketRuntime,
    val accessKey: GarageFsAccessKeyRuntime,
    val owner: Boolean,
    val read: Boolean,
    val write: Boolean,
) : InfrastructureResourceRuntime {
  val name: String
    get() = "${bucket.name}.${accessKey.name}"
}
