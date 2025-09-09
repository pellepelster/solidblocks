package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.api.HetznerNamedResource
import kotlinx.serialization.Serializable

@Serializable
data class FirewallsListWrapper(val firewalls: List<FirewallResponse>, override val meta: Meta) :
    ListResponse<FirewallResponse> {

  override val list: List<FirewallResponse>
    get() = firewalls
}

@Serializable
data class FirewallResponse(override val id: Long, override val name: String) :
    HetznerNamedResource

class HetznerFirewallsApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<FirewallResponse> {

  suspend fun listPaged(page: Int = 0, perPage: Int = 25): FirewallsListWrapper =
      api.get("v1/firewalls?page=$page&per_page=$perPage") ?: throw RuntimeException("failed to list firewalls")

  override suspend fun list() =
      api.handlePaginatedList { page, perPage -> listPaged(page, perPage) }

  override suspend fun delete(id: Long) = api.simpleDelete("v1/firewalls/$id")
}
