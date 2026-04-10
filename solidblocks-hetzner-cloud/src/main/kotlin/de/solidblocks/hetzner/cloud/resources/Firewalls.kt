package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.HetznerDeleteResourceApi
import de.solidblocks.hetzner.cloud.InstantSerializer
import de.solidblocks.hetzner.cloud.listQuery
import de.solidblocks.hetzner.cloud.model.FilterValue
import de.solidblocks.hetzner.cloud.model.HetznerNamedResource
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.model.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class FirewallRuleDirection {
    @SerialName("in")
    IN,

    @SerialName("out")
    OUT,
}

enum class FirewallRuleProtocol {
    @SerialName("tcp")
    TCP,

    @SerialName("udp")
    UDP,

    @SerialName("icmp")
    ICMP,

    @SerialName("esp")
    ESP,

    @SerialName("gre")
    GRE,
}

enum class FirewallResourceType {
    @SerialName("server")
    SERVER,

    @SerialName("label_selector")
    LABEL_SELECTOR,
}

@Serializable
data class FirewallServerReference(val id: Long)

@Serializable
data class FirewallLabelSelector(val selector: String)

@Serializable
data class FirewallResource(
    val type: FirewallResourceType,
    val server: FirewallServerReference? = null,
    @SerialName("label_selector") val labelSelector: FirewallLabelSelector? = null,
)

@Serializable
data class FirewallRule(
    val direction: FirewallRuleDirection,
    @SerialName("source_ips") val sourceIps: List<String> = emptyList(),
    @SerialName("destination_ips") val destinationIps: List<String> = emptyList(),
    val protocol: FirewallRuleProtocol,
    val port: String? = null,
    val description: String? = null,
)

@Serializable
data class FirewallResponse
@OptIn(ExperimentalTime::class)
constructor(
    override val id: Long,
    override val name: String,
    val rules: List<FirewallRule> = emptyList(),
    @SerialName("applied_to") val appliedTo: List<FirewallResource> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
    @Serializable(with = InstantSerializer::class) val created: Instant,
) : HetznerNamedResource<Long>

@Serializable
data class FirewallResponseWrapper(val firewall: FirewallResponse)

@Serializable
data class FirewallsListWrapper(val firewalls: List<FirewallResponse>, override val meta: MetaResponse) : ListResponse<FirewallResponse> {
    override val list: List<FirewallResponse>
        get() = firewalls
}

@Serializable
data class FirewallCreateRequest(
    val name: String,
    val rules: List<FirewallRule> = emptyList(),
    @SerialName("apply_to") val applyTo: List<FirewallResource> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
)

@Serializable
data class FirewallCreateResponseWrapper(val firewall: FirewallResponse, val actions: List<ActionResponse>)

@Serializable
data class FirewallUpdateRequest(val name: String? = null, val labels: Map<String, String>? = null)

@Serializable
data class FirewallSetRulesRequest(val rules: List<FirewallRule>)

@Serializable
data class FirewallApplyToResourcesRequest(@SerialName("apply_to") val applyTo: List<FirewallResource>)

@Serializable
data class FirewallRemoveFromResourcesRequest(@SerialName("remove_from") val removeFrom: List<FirewallResource>)

class HetznerFirewallsApi(private val api: HetznerApi) : HetznerDeleteResourceApi<Long, FirewallResponse> {
    override suspend fun listPaged(page: Int, perPage: Int, filter: Map<String, FilterValue>, labelSelectors: Map<String, LabelSelectorValue>): FirewallsListWrapper =
        api.get("v1/firewalls?${listQuery(page, perPage, filter, labelSelectors)}")
            ?: throw RuntimeException("failed to list firewalls")

    suspend fun get(id: Long) = api.get<FirewallResponseWrapper>("v1/firewalls/$id")?.firewall

    suspend fun get(name: String) = list(mapOf("name" to FilterValue.Equals(name))).singleOrNull()

    suspend fun create(request: FirewallCreateRequest) = api.post<FirewallCreateResponseWrapper>("v1/firewalls", request)

    suspend fun update(id: Long, request: FirewallUpdateRequest) = api.put<FirewallResponseWrapper>("v1/firewalls/$id", request)

    override suspend fun delete(id: Long) = api.delete("v1/firewalls/$id")

    suspend fun setRules(id: Long, request: FirewallSetRulesRequest): ActionsListResponseWrapper = api.post("v1/firewalls/$id/actions/set_rules", request)
        ?: throw RuntimeException("failed to set rules for firewall '$id'")

    suspend fun applyToResources(id: Long, request: FirewallApplyToResourcesRequest): ActionsListResponseWrapper = api.post("v1/firewalls/$id/actions/apply_to_resources", request)
        ?: throw RuntimeException("failed to apply firewall '$id' to resources")

    suspend fun removeFromResources(id: Long, request: FirewallRemoveFromResourcesRequest): ActionsListResponseWrapper = api.post("v1/firewalls/$id/actions/remove_from_resources", request)
        ?: throw RuntimeException("failed to remove firewall '$id' from resources")

    suspend fun action(id: Long): ActionResponseWrapper = api.get("v1/firewalls/actions/$id")
        ?: throw RuntimeException("failed to get firewall action '$id'")

    fun waitForAction(action: ActionResponse, logCallback: ((String) -> Unit)? = null) = api.waitForAction(action.id, logCallback) { api.firewalls.action(it) }

    fun waitForAction(actions: ActionsListResponseWrapper, logCallback: ((String) -> Unit)? = null) = actions.actions.all { waitForAction(it, logCallback) }
}
