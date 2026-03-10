package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
import kotlinx.serialization.Serializable

@Serializable
data class DnsZoneListResponseWrapper(
    val zones: List<HetznerDnsZone>,
    override val meta: MetaResponse,
) : ListResponse<HetznerDnsZone> {

  override val list: List<HetznerDnsZone>
    get() = zones
}

@Serializable data class DnsZoneResponseWrapper(val zone: HetznerDnsZone)

@Serializable
data class HetznerDnsZone(
    override val id: Long,
    override val name: String,
    override val protection: HetznerDeleteProtectionResponse,
    val labels: Map<String, String>,
) : HetznerDeleteProtectedResource<Long>

class HetznerDnsZonesApi(private val api: HetznerApi) : HetznerBaseResourceApi<HetznerDnsZone> {

  suspend fun get(name: String) = api.get<DnsZoneResponseWrapper>("v1/zones/$name")

  suspend fun get(id: Long) = api.get<DnsZoneResponseWrapper>("v1/zones/$id")

  override suspend fun listPaged(
      page: Int,
      perPage: Int,
      filter: Map<String, FilterValue>,
      labelSelectors: Map<String, LabelSelectorValue>,
  ): DnsZoneListResponseWrapper =
      api.get("v1/zones?${listQuery(page, perPage, filter, labelSelectors)}")
          ?: throw RuntimeException("failed to list dns zones")
}
