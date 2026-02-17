package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.InstantSerializer
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SSHKeysCreateRequest(
    val name: String,
    @SerialName("public_key") val publicKey: String,
    val labels: Map<String, String> = emptyMap(),
)

@Serializable
data class SSHKeysUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null)

@Serializable
data class SSHKeysListResponseWrapper(
    @SerialName("ssh_keys") val sshKeys: List<SshKeyResponse>,
    override val meta: MetaResponse,
) : ListResponse<SshKeyResponse> {

  override val list: List<SshKeyResponse>
    get() = sshKeys
}

@Serializable data class SSHKeyResponseWrapper(@SerialName("ssh_key") val sshKey: SshKeyResponse)

@Serializable
data class SshKeyResponse
@OptIn(ExperimentalTime::class)
constructor(
    override val id: Long,
    override val name: String,
    val fingerprint: String,
    @SerialName("public_key") val publicKey: String,
    val labels: Map<String, String>,
    @Serializable(with = InstantSerializer::class) val created: Instant,
) : HetznerNamedResource<Long>

class HetznerSSHKeysApi(private val api: HetznerApi) :
    HetznerDeleteResourceApi<Long, SshKeyResponse> {

  override suspend fun listPaged(
      page: Int,
      perPage: Int,
      filter: Map<String, FilterValue>,
      labelSelectors: Map<String, LabelSelectorValue>,
  ): SSHKeysListResponseWrapper =
      api.get("v1/ssh_keys?${listQuery(page, perPage, filter, labelSelectors)}")
          ?: throw RuntimeException("failed to list ssh keys")

  suspend fun get(id: Long) = api.get<SSHKeyResponseWrapper>("v1/ssh_keys/$id")?.sshKey

  suspend fun get(name: String) = list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

  override suspend fun delete(id: Long) = api.simpleDelete("v1/ssh_keys/$id")

  suspend fun create(request: SSHKeysCreateRequest) =
      api.post<SSHKeyResponseWrapper>("v1/ssh_keys", request)

  suspend fun update(id: Long, request: SSHKeysUpdateRequest) =
      api.put<SSHKeyResponseWrapper>("v1/ssh_keys/$id", request)
}
