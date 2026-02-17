package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerBaseResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
import kotlinx.serialization.Serializable

@Serializable
data class DnsZoneListResponseWrapper(val zones: List<DnsZone>, override val meta: MetaResponse) :
    ListResponse<DnsZone> {

  override val list: List<DnsZone>
    get() = zones
}

@Serializable data class DnsZoneResponseWrapper(val zone: DnsZone)

@Serializable
data class DnsZone(
    override val id: Long,
    override val name: String,
    override val protection: HetznerDeleteProtectionResponse,
) : HetznerDeleteProtectedResource<Long>

class HetznerDnsZonesApi(private val api: HetznerApi) : HetznerBaseResourceApi<DnsZone> {

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
