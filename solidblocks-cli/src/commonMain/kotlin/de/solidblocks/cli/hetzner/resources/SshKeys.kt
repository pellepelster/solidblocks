package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.HetznerApi
import de.solidblocks.cli.hetzner.HetznerSimpleResourceApi
import de.solidblocks.cli.hetzner.HetznerNamedResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SSHKeysListWrapper(@SerialName("ssh_keys") val sshKeys: List<SshKeyResponse>, override val meta: Meta) :
    ListResponse<SshKeyResponse> {

    override val list: List<SshKeyResponse>
        get() = sshKeys
}

@Serializable
data class SshKeyResponse(override val id: Long, override val name: String) : HetznerNamedResource

class HetznerSSHKeysApi(private val api: HetznerApi) : HetznerSimpleResourceApi<SshKeyResponse> {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): SSHKeysListWrapper =
        api.get("v1/ssh_keys?page=${page}&per_page=${perPage}")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/ssh_keys/${id}")

    override suspend fun list() = api.handlePaginatedList { page, perPage ->
        listPaged(page, perPage)
    }
}


