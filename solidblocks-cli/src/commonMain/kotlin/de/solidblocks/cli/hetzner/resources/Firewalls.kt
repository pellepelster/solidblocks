package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.HetznerApi
import de.solidblocks.cli.hetzner.HetznerSimpleResourceApi
import de.solidblocks.cli.hetzner.NamedHetznerResource
import kotlinx.serialization.Serializable

@Serializable
data class FirewallsListWrapper(val firewalls: List<FirewallResponse>, override val meta: Meta) :
    ListResponse<FirewallResponse> {

    override val list: List<FirewallResponse>
        get() = firewalls
}

@Serializable
data class FirewallResponse(override val id: Long, override val name: String) : NamedHetznerResource


class HetznerFirewallsApi(private val api: HetznerApi) : HetznerSimpleResourceApi<FirewallResponse> {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): FirewallsListWrapper =
        api.get("v1/firewalls?page=${page}&per_page=${perPage}")

    override suspend fun list() = api.handlePaginatedList { page, perPage ->
        listPaged(page, perPage)
    }

    override suspend fun delete(id: Long) = api.simpleDelete("v1/firewalls/${id}")

}


