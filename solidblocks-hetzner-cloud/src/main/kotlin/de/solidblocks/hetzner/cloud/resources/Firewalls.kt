package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
import kotlinx.serialization.Serializable

@Serializable
data class FirewallsListWrapper(
    val firewalls: List<FirewallResponse>,
    override val meta: MetaResponse,
) : ListResponse<FirewallResponse> {

    override val list: List<FirewallResponse>
        get() = firewalls
}

@Serializable
data class FirewallResponse(override val id: Long, override val name: String) :
    HetznerNamedResource<Long>

class HetznerFirewallsApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, FirewallResponse> {

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>,
    ): FirewallsListWrapper =
        api.get("v1/firewalls?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list firewalls")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/firewalls/$id")
}
