package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.api.HetznerNamedResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SSHKeysListWrapper(
    @SerialName("ssh_keys") val sshKeys: List<SshKeyResponse>,
    override val meta: Meta,
) : ListResponse<SshKeyResponse> {

    override val list: List<SshKeyResponse>
        get() = sshKeys
}

@Serializable
data class SshKeyResponse(override val id: Long, override val name: String) : HetznerNamedResource

class HetznerSSHKeysApi(private val api: HetznerApi) : HetznerDeleteResourceApi<SshKeyResponse> {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): SSHKeysListWrapper =
        api.get("v1/ssh_keys?page=$page&per_page=$perPage") ?: throw RuntimeException("failed to list ssh keys")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/ssh_keys/$id")

    override suspend fun list() =
        api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }
}
