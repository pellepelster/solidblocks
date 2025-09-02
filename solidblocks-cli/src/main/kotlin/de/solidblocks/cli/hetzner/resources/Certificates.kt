package de.solidblocks.cli.hetzner.resources

import de.solidblocks.cli.hetzner.HetznerApi
import de.solidblocks.cli.hetzner.HetznerDeleteResourceApi
import de.solidblocks.cli.hetzner.HetznerNamedResource
import kotlinx.serialization.Serializable

@Serializable
data class CertificatesListWrapper(val certificates: List<CertificatesResponse>, override val meta: Meta) :
    ListResponse<CertificatesResponse> {

    override val list: List<CertificatesResponse>
        get() = certificates
}

@Serializable
data class CertificatesResponse(override val id: Long, override val name: String) : HetznerNamedResource


class HetznerCertificatesApi(private val api: HetznerApi) : HetznerDeleteResourceApi<CertificatesResponse> {

    suspend fun listPaged(page: Int = 0, perPage: Int = 25): CertificatesListWrapper =
        api.get("v1/certificates?page=${page}&per_page=${perPage}")

    override suspend fun list() = api.handlePaginatedList { page, perPage ->
        listPaged(page, perPage)
    }

    override suspend fun delete(id: Long) = api.simpleDelete("v1/certificates/${id}")

}


