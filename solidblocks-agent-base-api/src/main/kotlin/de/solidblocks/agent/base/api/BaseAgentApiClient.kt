package de.solidblocks.agent.base.api

import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import de.solidblocks.vault.http.HttpResponse
import de.solidblocks.vault.http.MtlsHttpClient
import mu.KotlinLogging

class BaseAgentApiClient(val address: String, vaultCertificateManager: VaultCertificateManager, vaultCaCertificateManager: VaultCaCertificateManager) {

    private val logger = KotlinLogging.logger {}

    private val client = MtlsHttpClient(address, vaultCertificateManager, vaultCaCertificateManager)

    fun version() = try {
        val currentVersion: HttpResponse<VersionResponse> = client.get("$AGENT_BASE_PATH/version")
        currentVersion.data
    } catch (e: Exception) {
        logger.error(e) { "error executing request for '$address'" }
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
