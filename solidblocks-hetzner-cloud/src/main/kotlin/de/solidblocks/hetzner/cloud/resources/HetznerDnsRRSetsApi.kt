package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DnsRRSetsListResponseWrapper(
    val rrsets: List<DnsRrSetResponse>,
    override val meta: MetaResponse,
) : ListResponse<DnsRrSetResponse> {

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

@Serializable data class DnsRRSetRecord(val value: String, val comment: String? = null)

@Serializable
data class DnsRRSetsCreateRequest(
    val name: String,
    val type: RRType,
    val records: List<DnsRRSetRecord>,
    val ttl: Int = 3600,
    val labels: Map<String, String> = emptyMap(),
)

@Serializable
data class DnsRRSetsCreateResponseWrapper(
    @SerialName("rrset") val rrset: DnsRrSetResponse,
    @SerialName("action") val action: ActionResponse,
)

@Serializable data class DnsRRSetsResponseWrapper(@SerialName("rrset") val rrset: DnsRrSetResponse)

@Serializable
data class DnsRrSetResponse(
    override val id: String,
    override val name: String,
    val type: RRType,
    override val protection: HetznerChangeProtectionResponse,
    val records: List<DnsRRSetRecord>,
) : HetznerChangeProtectedResource<String>

class HetznerDnsRRSetsApi(private val api: HetznerApi, val dnsZoneReference: String) :
    HetznerBaseResourceApi<DnsRrSetResponse> {

  override suspend fun listPaged(
      page: Int,
      perPage: Int,
      filter: Map<String, FilterValue>,
      labelSelectors: Map<String, LabelSelectorValue>,
  ): DnsRRSetsListResponseWrapper =
      api.get(
          "v1/zones/$dnsZoneReference/rrsets?${listQuery(page, perPage, filter, labelSelectors)}",
      ) ?: throw RuntimeException("failed to list dns rr sets")

  suspend fun create(request: DnsRRSetsCreateRequest) =
      api.post<DnsRRSetsCreateResponseWrapper>("v1/zones/$dnsZoneReference/rrsets", request)

  suspend fun get(name: String, type: RRType) =
      api.get<DnsRRSetsResponseWrapper>("v1/zones/$dnsZoneReference/rrsets/$name/$type")

  suspend fun delete(rrName: String, rrType: RRType) =
      api.simpleDelete("v1/zones/$dnsZoneReference/rrsets/$rrName/$rrType")
}
