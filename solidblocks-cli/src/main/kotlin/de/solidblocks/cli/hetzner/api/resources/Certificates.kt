package de.solidblocks.cli.hetzner.api.resources

import de.solidblocks.cli.hetzner.api.*
import de.solidblocks.cli.hetzner.api.model.FilterValue
import de.solidblocks.cli.hetzner.api.model.HetznerNamedResource
import de.solidblocks.cli.hetzner.api.model.LabelSelectorValue
import de.solidblocks.cli.hetzner.api.model.ListResponse
import de.solidblocks.cli.hetzner.api.model.Meta
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

    override suspend fun listPaged(
        page: Int,
        perPage: Int,
        filter: Map<String, FilterValue>,
        labelSelectors: Map<String, LabelSelectorValue>
    ): CertificatesListWrapper =
        api.get("v1/certificates?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list certificates")

    override suspend fun delete(id: Long) = api.simpleDelete("v1/certificates/$id")
}
