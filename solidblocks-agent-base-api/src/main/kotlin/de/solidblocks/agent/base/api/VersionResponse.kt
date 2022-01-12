package de.solidblocks.agent.base.api

const val AGENT_BASE_PATH = "/v1/agent"

data class VersionResponse(val version: String) {
    companion object {
        const val VERSION_PATH = "/version"
    }
}
