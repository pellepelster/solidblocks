package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
import kotlinx.serialization.Serializable

@Serializable
data class DnsRRSetsListResponseWrapper(
    val rrsets: List<DnsRRSet>,
    override val meta: MetaResponse,
) : ListResponse<DnsRRSet> {

    override val list: List<DnsRRSet>
        get() = rrsets
}


@Serializable
data class DnsRRSet(
    override val id: String,
    override val name: String,
    override val protection: HetznerProtectionResponse,
) : HetznerProtectedResource

class HetznerDnsRRSetsApi(private val api: HetznerApi, val dnsZoneReference: String) :
    HetznerBaseResourceApi<DnsRRSet> {

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>,
    ): DnsRRSetsListResponseWrapper =
        api.get("v1/zones/${dnsZoneReference}/rrsets?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list dns rr sets")

}
