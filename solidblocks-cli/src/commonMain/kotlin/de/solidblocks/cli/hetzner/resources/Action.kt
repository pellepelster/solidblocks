package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.HetznerApi
import kotlinx.serialization.Serializable

@Serializable
data class ActionResponseWrapper(val action: Action)

@Serializable
data class ActionError(val code: String, val message: String)

@Serializable
data class ActionResource(val id: Long, val type: String)

enum class ActionStatus {
    running, success, error
}

@Serializable
data class Action(
    val command: String,
    val error: ActionError? = null,
    val finished: String? = null,
    val id: Long,
    val progress: Int,
    val resources: List<ActionResource>,
    val started: String,
    val status: ActionStatus,
)

class HetznerActionsApi(private val api: HetznerApi) {
    suspend fun create(): ServersListWrapper = api.post("v1/servers", String())

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): ServersListWrapper =
        api.get("v1/servers?page=${page}&per_page=${perPage}")

    suspend fun get(id: Long): ActionResponseWrapper = api.get("actions/${id}")
}
