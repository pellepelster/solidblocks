package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
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

    suspend fun listPaged(
        page: Int = 0,
        perPage: Int = 25,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): FirewallsListWrapper =
        api.get("v1/firewalls?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list firewalls")

    override suspend fun list(filter: Map<String, FilterValue>, labelSelectors: Map<String, LabelSelectorValue>) =
        api.handlePaginatedList(filter, labelSelectors) { page, perPage, filter, labelSelectors ->
            listPaged(
                page,
                perPage,
                filter,
                labelSelectors
            )
        }

    override suspend fun delete(id: Long) = api.simpleDelete("v1/firewalls/$id")
}
