package de.solidblocks.cli.hetzner

import de.solidblocks.cli.hetzner.resources.ActionResponseWrapper
import de.solidblocks.cli.utils.pascalCaseToWhiteSpace
import kotlinx.serialization.Serializable

interface HetznerNamedResource {
    val id: Long
    val name: String?
}

@Serializable
data class HetznerProtectionResponse(
    val delete: Boolean,
)

interface HetznerProtectedResource : HetznerNamedResource {
    val protection: HetznerProtectionResponse
}

interface HetznerAssignedResource : HetznerNamedResource {
    val isAssigned: Boolean
}

interface HetznerBaseResourceApi<T : HetznerNamedResource> {
    suspend fun list(): List<T>
}

interface HetznerSimpleResourceApi<T : HetznerNamedResource> : HetznerBaseResourceApi<T> {
    suspend fun delete(id: Long): Boolean
}

interface HetznerProtectedResourceApi {
    suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper
    suspend fun action(id: Long): ActionResponseWrapper
    suspend fun list(): List<HetznerProtectedResource>
}

interface HetznerAssignedResourceApi {
    suspend fun unassign(id: Long): ActionResponseWrapper
    suspend fun action(id: Long): ActionResponseWrapper
}

interface HetznerDeleteWithActionResourceApi<T : HetznerNamedResource>: HetznerBaseResourceApi<T> {
    suspend fun delete(id: Long): ActionResponseWrapper
    suspend fun action(id: Long): ActionResponseWrapper
}

interface HetznerDeleteResourceApi<T : HetznerNamedResource> : HetznerBaseResourceApi<T> {
    suspend fun delete(id: Long): Boolean
    suspend fun action(id: Long): ActionResponseWrapper
}

fun HetznerNamedResource.logText() =
    "${this::class.pascalCaseToWhiteSpace()} '${name ?: "<no name>"}' (${id})"