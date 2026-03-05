package de.solidblocks.hetzner

import de.solidblocks.hetzner.cloud.HetznerApi
import kotlinx.coroutines.runBlocking

fun BasicUsage() {
    runBlocking {
        val api = HetznerApi(System.getenv("HCLOUD_TOKEN"))

        //api.servers.shutdown(123)
        //api.firewalls.delete(12)
        //api.volumes.create(...)
    }
}