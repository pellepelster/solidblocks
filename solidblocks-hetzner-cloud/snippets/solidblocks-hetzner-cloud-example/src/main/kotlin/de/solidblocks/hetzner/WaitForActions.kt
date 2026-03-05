package de.solidblocks.hetzner

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import kotlinx.coroutines.runBlocking

fun WaitForActions() {
    runBlocking {
        val api = HetznerApi(System.getenv("HCLOUD_TOKEN"))

        val server = api.servers.create(
            ServerCreateRequest(
                "server1",
                HetznerLocation.nbg1,
                HetznerServerType.cx23,
                image = "debian-12",
            )
        )

        api.servers.waitForAction(server.action)
    }
}