package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.FilterValue
import de.solidblocks.hetzner.cloud.model.HetznerChangeProtectedResource
import de.solidblocks.hetzner.cloud.model.HetznerChangeProtectionResponse
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DnsRRSetsListResponseWrapper(val rrsets: List<DnsRrSetResponse>, override val meta: MetaResponse) : ListResponse<DnsRrSetResponse> {
    override val list: List<DnsRrSetResponse>
        get() = rrsets
}

enum class RRType {
    A,
    AAAA,
    CAA,
    CNAME,
    DS,
    HINFO,
    HTTPS,
    MX,
    NS,
    PTR,
    RP,
    SOA,
    SRV,
    SVCB,
    TLSA,
    TXT,
}

@Serializable
data class DnsRRSetRecord(val value: String, val comment: String? = null)

@Serializable
data class DnsRRSetsCreateRequest(val name: String, val type: RRType, val ttl: Int, val records: List<DnsRRSetRecord>, val labels: Map<String, String> = emptyMap())

@Serializable
data class DnsRRSetsRecordsUpdateRequest(val records: List<DnsRRSetRecord>)

@Serializable
data class DnsRRSetsUpdateRequest(val labels: Map<String, String> = emptyMap())

@Serializable
data class DnsRRSetsTTLUpdateRequest(val ttl: Int)

@Serializable
data class DnsRRSetsCreateResponseWrapper(@SerialName("rrset") val rrset: DnsRrSetResponse, @SerialName("action") val action: ActionResponse)

@Serializable
data class DnsRRSetsResponseWrapper(@SerialName("rrset") val rrset: DnsRrSetResponse)

@Serializable
data class DnsRrSetResponse(
    override val id: String,
    override val name: String,
    val type: RRType,
    val ttl: Int? = null,
    override val protection: HetznerChangeProtectionResponse,
    val records: List<DnsRRSetRecord>,
) : HetznerChangeProtectedResource<String>

class HetznerDnsRRSetsApi(private val api: HetznerApi, val dnsZoneReference: String) : HetznerBaseResourceApi<DnsRrSetResponse> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: Map<String, FilterValue>, labelSelectors: Map<String, LabelSelectorValue>): DnsRRSetsListResponseWrapper = api.get(
        "v1/zones/$dnsZoneReference/rrsets?${listQuery(page, perPage, filter, labelSelectors)}",
    ) ?: throw RuntimeException("failed to list dns rr sets")

    suspend fun create(request: DnsRRSetsCreateRequest): DnsRRSetsCreateResponseWrapper = api.post<DnsRRSetsCreateResponseWrapper>("v1/zones/$dnsZoneReference/rrsets", request)

    suspend fun updateTTL(name: String, type: RRType, request: DnsRRSetsTTLUpdateRequest) = api.post<ActionResponseWrapper>(
        "v1/zones/$dnsZoneReference/rrsets/$name/$type/actions/change_ttl",
        request,
    )

    suspend fun updateRecords(name: String, type: RRType, request: DnsRRSetsRecordsUpdateRequest) = api.post<ActionResponseWrapper>(
        "v1/zones/$dnsZoneReference/rrsets/$name/$type/actions/set_records",
        request,
    )

    suspend fun get(name: String, type: RRType) = api.get<DnsRRSetsResponseWrapper>("v1/zones/$dnsZoneReference/rrsets/$name/$type")

    suspend fun delete(rrName: String, rrType: RRType) = api.delete("v1/zones/$dnsZoneReference/rrsets/$rrName/$rrType")

    suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/zones/$dnsZoneReference/actions/$id")
        ?: throw RuntimeException("failed to get zone action")

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) = api.waitForAction(id, logCallback, { action(it) })

    suspend fun waitForAction(action: ActionResponseWrapper, logCallback: ((String) -> Unit)? = null) = waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) = waitForAction(action.id, logCallback)
}
