package de.solidblocks.agent.base.api

import de.solidblocks.base.http.HttpClient
import de.solidblocks.base.http.HttpResponse

class BaseAgentApiClient(val address: String) {

    private val client: HttpClient = HttpClient(address)

    fun version() = try {
        val currentVersion: HttpResponse<VersionResponse> = client.get("/v1/agent/version")
        currentVersion.data
    } catch (e: Exception) {
        null
    }

    fun triggerUpdate(targetVersion: String): Boolean? {
        val currentVersion: HttpResponse<TriggerUpdateResponse> = client.post(
            "$AGENT_BASE_PATH/${TriggerUpdateRequest.TRIGGER_UPDATE_PATH}",
            TriggerUpdateRequest(targetVersion)
        )

        return currentVersion.data?.triggered
    }
}
