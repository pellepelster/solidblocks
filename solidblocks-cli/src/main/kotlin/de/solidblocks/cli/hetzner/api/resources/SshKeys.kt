package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import de.solidblocks.cli.hetzner.api.model.FilterValue
import de.solidblocks.cli.hetzner.api.model.HetznerNamedResource
import de.solidblocks.cli.hetzner.api.model.LabelSelectorValue
import de.solidblocks.cli.hetzner.api.model.ListResponse
import de.solidblocks.cli.hetzner.api.model.Meta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SSHKeysListResponseWrapper(
    @SerialName("ssh_keys") val sshKeys: List<SshKeyResponse>,
    override val meta: Meta,
) : ListResponse<SshKeyResponse> {

    override val list: List<SshKeyResponse>
        get() = sshKeys
}

@Serializable
data class SSHKeyResponseWrapper(@SerialName("ssh_key") val sshKey: SshKeyResponse)

@Serializable
data class SshKeyResponse(override val id: Long, override val name: String) : HetznerNamedResource

class HetznerSSHKeysApi(private val api: HetznerApi) : HetznerDeleteResourceApi<SshKeyResponse> {

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): SSHKeysListResponseWrapper =
        api.get("v1/ssh_keys?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list ssh keys")

    suspend fun get(id: Long) = api.get<SSHKeyResponseWrapper>("v1/ssh_keys/$id")?.sshKey

    suspend fun get(name: String) =
        list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

    override suspend fun delete(id: Long) = api.simpleDelete("v1/ssh_keys/$id")

}
