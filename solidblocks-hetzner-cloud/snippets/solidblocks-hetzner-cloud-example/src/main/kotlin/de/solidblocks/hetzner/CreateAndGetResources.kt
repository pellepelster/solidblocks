package de.solidblocks.hetzner

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.resources.ServerCreateRequest
import kotlinx.coroutines.runBlocking

fun CreateAndGetResources() {
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

        val serverByName = api.servers.get("server1") ?: throw RuntimeException("could not find server1")
        val serverById = api.servers.get(server.server.id) ?: throw RuntimeException("could not find server with id '${server.server.id}'")
    }
}