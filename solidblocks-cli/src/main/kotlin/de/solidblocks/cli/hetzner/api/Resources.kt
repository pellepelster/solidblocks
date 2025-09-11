package de.solidblocks.cli.hetzner.api

import de.solidblocks.cli.hetzner.api.resources.ActionResponseWrapper
import de.solidblocks.cli.utils.pascalCaseToWhiteSpace
import kotlinx.serialization.Serializable

interface HetznerNamedResource : HetznerResource {
    val name: String?
}

interface HetznerResource {
    val id: Long
}

@Serializable
data class HetznerProtectionResponse(
    val delete: Boolean,
)

sealed class LabelSelectorValue() {
    data class Equals(val value: String) : LabelSelectorValue() {
        override fun query(key: String) = "${key}==${value}"
    }

    data class NotEquals(val value: String) : LabelSelectorValue() {
        override fun query(key: String) = "${key}!=${value}"
    }

    abstract fun query(key: String): String
}

sealed class FilterValue() {
    data class Equals(val value: String) : FilterValue() {
        override val query: String
            get() = value

    }

    abstract val query: String
}

interface HetznerProtectedResource : HetznerNamedResource {
    val protection: HetznerProtectionResponse
}

interface HetznerAssignedResource : HetznerNamedResource {
    val isAssigned: Boolean
}

interface HetznerBaseResourceApi<T> {
    suspend fun list(
        filter: Map<String, FilterValue> = emptyMap(),
        labelSelectors: Map<String, LabelSelectorValue> = emptyMap()
    ): List<T>
}

interface HetznerDeleteResourceApi<T : HetznerNamedResource> : HetznerBaseResourceApi<T> {
    suspend fun delete(id: Long): Boolean
}

interface HetznerProtectedResourceApi<T : HetznerNamedResource> : HetznerBaseResourceApi<T> {
    suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper
    suspend fun action(id: Long): ActionResponseWrapper
}

interface HetznerAssignedResourceApi {
    suspend fun unassign(id: Long): ActionResponseWrapper?
    suspend fun action(id: Long): ActionResponseWrapper
}

interface HetznerDeleteWithActionResourceApi<T : HetznerNamedResource> : HetznerBaseResourceApi<T> {
    suspend fun delete(id: Long): ActionResponseWrapper
    suspend fun action(id: Long): ActionResponseWrapper
}

fun HetznerNamedResource.logText() =
    "${this::class.pascalCaseToWhiteSpace()} '${name ?: "<no name>"}' ($id)"
