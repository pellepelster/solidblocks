package de.solidblocks.hetzner

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.model.HetznerApiErrorType
import de.solidblocks.hetzner.cloud.model.HetznerApiException
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.resources.VolumeCreateRequest
import de.solidblocks.hetzner.cloud.resources.VolumeFormat
import kotlinx.coroutines.runBlocking

fun Exceptions() {
    runBlocking {
        val api = HetznerApi(System.getenv("HCLOUD_TOKEN"))

        try {
            api.volumes.create(VolumeCreateRequest("volume1", 16, HetznerLocation.nbg1, VolumeFormat.ext4))
        } catch (e: HetznerApiException) {
            if (e.error.code == HetznerApiErrorType.RESOURCE_LIMIT_EXCEEDED) {
                print("volume quota reached")
            }
        }
    }
}