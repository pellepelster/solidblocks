package de.solidblocks.garagefs

import kotlinx.serialization.Serializable


@Serializable
data class ClusterStatusNodeResponse(val id: String)

@Serializable
data class ClusterStatusResponse(val nodes: List<ClusterStatusNodeResponse>)

class ClusterApi(val api: GarageFsApi) {
    suspend fun getClusterStatus() = api.get<ClusterStatusResponse>("v2/GetClusterStatus")
}