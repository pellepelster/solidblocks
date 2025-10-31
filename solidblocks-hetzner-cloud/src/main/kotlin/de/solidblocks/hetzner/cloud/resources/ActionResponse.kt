package de.solidblocks.hetzner.cloud.resources

import kotlinx.serialization.Serializable

@Serializable data class ActionResponseWrapper(val action: ActionResponse)

@Serializable data class ActionError(val code: String, val message: String)

@Serializable data class ActionResource(val id: Long, val type: String)

enum class ActionStatus {
  RUNNING,
  SUCCESS,
  ERROR,
}

@Serializable
data class ActionResponse(
    val command: String,
    val error: ActionError? = null,
    val finished: String? = null,
    val id: Long,
    val progress: Int,
    val resources: List<ActionResource>,
    val started: String,
    val status: ActionStatus,
)

@Serializable
data class ActionsListResponseWrapper(
    val actions: List<ActionResponse>,
)
