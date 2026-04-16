package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectedResource
import de.solidblocks.hetzner.cloud.model.HetznerDeleteProtectionResponse
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.Serializable

open class DnsZoneFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class DnsZoneNameFilter(name: String) : DnsZoneFilter("name", name)

@Serializable
data class DnsZoneListResponseWrapper(val zones: List<HetznerDnsZone>, override val meta: MetaResponse) : ListResponse<HetznerDnsZone> {
    override val list: List<HetznerDnsZone>
        get() = zones
}

@Serializable
data class DnsZoneResponseWrapper(val zone: HetznerDnsZone)

@Serializable
data class HetznerDnsZone(override val id: Long, override val name: String, override val protection: HetznerDeleteProtectionResponse, val labels: Map<String, String>) : HetznerDeleteProtectedResource<Long>

class HetznerDnsZonesApi(private val api: HetznerApi) : HetznerBaseResourceApi<HetznerDnsZone, DnsZoneFilter> {
    suspend fun get(name: String) = api.get<DnsZoneResponseWrapper>("v1/zones/$name")

    suspend fun get(id: Long) = api.get<DnsZoneResponseWrapper>("v1/zones/$id")

    override suspend fun listPaged(page: Int, perPage: Int, filter: List<DnsZoneFilter>, labelSelectors: Map<String, LabelSelectorValue>): DnsZoneListResponseWrapper =
        api.get("v1/zones?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list dns zones")
}
