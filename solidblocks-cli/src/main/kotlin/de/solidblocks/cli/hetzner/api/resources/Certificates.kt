package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import kotlinx.serialization.Serializable

@Serializable
data class CertificatesListWrapper(
    val certificates: List<CertificatesResponse>,
    override val meta: Meta,
) : ListResponse<CertificatesResponse> {

    override val list: List<CertificatesResponse>
        get() = certificates
}

@Serializable
data class CertificatesResponse(override val id: Long, override val name: String) :
    HetznerNamedResource

class HetznerCertificatesApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<CertificatesResponse> {

    suspend fun listPaged(
        page: Int = 0,
        perPage: Int = 25,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): CertificatesListWrapper =
        api.get("v1/certificates?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list certificates")

    override suspend fun list(filter: Map<String, FilterValue>, labelSelectors: Map<String, LabelSelectorValue>) =
        api.handlePaginatedList(filter, labelSelectors) { page, perPage, filter, labelSelectors ->
            listPaged(
                page,
                perPage,
                filter,
                labelSelectors
            )
        }

    override suspend fun delete(id: Long) = api.simpleDelete("v1/certificates/$id")
}
