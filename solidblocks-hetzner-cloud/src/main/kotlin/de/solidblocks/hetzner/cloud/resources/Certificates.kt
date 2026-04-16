package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.BaseFilter
import de.solidblocks.hetzner.cloud.model.HetznerNamedResource
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class CertificateType {
    uploaded,
    managed,
}

@Serializable
data class CertificatesListWrapper(val certificates: List<CertificateResponse>, override val meta: MetaResponse) : ListResponse<CertificateResponse> {
    override val list: List<CertificateResponse>
        get() = certificates
}

@Serializable
data class CertificateResponse(
    override val id: Long,
    override val name: String,
    val type: CertificateType = CertificateType.uploaded,
    val labels: Map<String, String> = emptyMap(),
    @SerialName("domain_names") val domainNames: List<String> = emptyList(),
) : HetznerNamedResource<Long>

@Serializable
data class CertificateResponseWrapper(val certificate: CertificateResponse)

@Serializable
data class CertificateCreateRequest(
    val name: String,
    val certificate: String? = null,
    @SerialName("private_key") val privateKey: String? = null,
    val type: CertificateType,
    @SerialName("domain_names") val domainNames: List<String>? = null,
    val labels: Map<String, String>? = null,
)

open class CertificateFilter(attribute: String, value: String) : BaseFilter(attribute, value)

class CertificateNameFilter(name: String) : CertificateFilter("name", name)

class CertificateTypeFilter(type: CertificateType) : CertificateFilter("type", type.toString())

@Serializable
data class CertificateUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null)

class HetznerCertificatesApi(private val api: HetznerApi) : HetznerDeleteResourceApi<Long, CertificateResponse, CertificateFilter> {

    override suspend fun listPaged(page: Int, perPage: Int, filter: List<CertificateFilter>, labelSelectors: Map<String, LabelSelectorValue>): CertificatesListWrapper =
        api.get("v1/certificates?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list certificates")

    override suspend fun delete(id: Long) = api.delete("v1/certificates/$id")

    suspend fun get(id: Long) = api.get<CertificateResponseWrapper>("v1/certificates/$id")?.certificate

    suspend fun get(name: String) = list(listOf(CertificateNameFilter(name))).singleOrNull()

    suspend fun create(request: CertificateCreateRequest) = api.post<CertificateResponseWrapper>("v1/certificates", request)

    suspend fun update(id: Long, request: CertificateUpdateRequest) = api.put<CertificateResponseWrapper>("v1/certificates/$id", request)

    suspend fun retryIssuance(id: Long): ActionResponseWrapper = api.post("v1/certificates/$id/actions/retry")
        ?: throw RuntimeException("failed to retry issuance for certificate '$id'")

    suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/certificates/actions/$id")
        ?: throw RuntimeException("failed to get certificate action")

    suspend fun waitForAction(id: Long, logCallback: ((String) -> Unit)? = null) = api.waitForAction(id, logCallback, { api.certificates.action(it) })

    suspend fun waitForAction(action: ActionResponseWrapper, logCallback: ((String) -> Unit)? = null) = waitForAction(action.action, logCallback)

    suspend fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) = waitForAction(action.id, logCallback)
}
