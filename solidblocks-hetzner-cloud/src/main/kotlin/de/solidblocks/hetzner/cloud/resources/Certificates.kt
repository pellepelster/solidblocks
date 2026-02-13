package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
import kotlinx.serialization.Serializable

@Serializable
data class CertificatesListWrapper(
    val certificates: List<CertificatesResponse>,
    override val meta: MetaResponse,
) : ListResponse<CertificatesResponse> {

  override val list: List<CertificatesResponse>
    get() = certificates
}

@Serializable
data class CertificatesResponse(override val id: Long, override val name: String) :
    HetznerNamedResource<Long>

class HetznerCertificatesApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, CertificatesResponse> {

  override suspend fun listPaged(
      page: Int,
      perPage: Int,
      filter: Map<String, FilterValue>,
      labelSelectors: Map<String, LabelSelectorValue>,
  ): CertificatesListWrapper =
      api.get("v1/certificates?${listQuery(page, perPage, filter, labelSelectors)}")
          ?: throw RuntimeException("failed to list certificates")

  override suspend fun delete(id: Long) = api.simpleDelete("v1/certificates/$id")
}
