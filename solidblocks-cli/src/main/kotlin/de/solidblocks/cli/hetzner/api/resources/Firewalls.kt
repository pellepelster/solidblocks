package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import de.solidblocks.cli.hetzner.api.model.FilterValue
import de.solidblocks.cli.hetzner.api.model.HetznerNamedResource
import de.solidblocks.cli.hetzner.api.model.LabelSelectorValue
import de.solidblocks.cli.hetzner.api.model.ListResponse
import de.solidblocks.cli.hetzner.api.model.Meta
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

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): FirewallsListWrapper =
        api.get("v1/firewalls?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list firewalls")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/firewalls/$id")
}
