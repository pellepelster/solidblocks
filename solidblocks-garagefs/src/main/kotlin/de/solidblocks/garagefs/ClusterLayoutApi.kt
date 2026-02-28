package de.solidblocks.garagefs

import kotlinx.serialization.Serializable

@Serializable
data class ClusterLayoutResponse(val version: Int? = null, val nodes: List<ClusterLayoutNodeResponse>? = null, val stagedRoleChanges: List<ClusterLayoutNodeResponse>? = null)

@Serializable
data class ClusterLayoutHistoryVersionResponse(val status: String, val version: Int)

@Serializable
data class ClusterLayoutHistoryResponse(val currentVersion: Int, val versions: List<ClusterLayoutHistoryVersionResponse>)

@Serializable
data class UpdateClusterLayoutRequest(val parameters: String? = null, val roles: List<ClusterLayoutNodeRequest> = emptyList())

@Serializable
data class ApplyClusterLayoutRequest(val version: Int)

class ClusterLayoutApi(val api: GarageFsApi) {

    suspend fun getClusterLayoutHistory() = api.get<ClusterLayoutHistoryResponse>("v2/GetClusterLayoutHistory")

    suspend fun revertClusterLayout() = api.post<ClusterLayoutResponse>("v2/RevertClusterLayout")

    suspend fun updateClusterLayout(request: UpdateClusterLayoutRequest) = api.post<ClusterLayoutResponse>("v2/UpdateClusterLayout", request)

    suspend fun applyClusterLayout(request: ApplyClusterLayoutRequest) = api.post<ClusterLayoutResponse>("v2/ApplyClusterLayout", request)

    suspend fun getClusterLayout() = api.get<ClusterLayoutResponse>("v2/GetClusterLayout")
}