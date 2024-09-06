package de.solidblocks.cli.hetzner

import de.solidblocks.cli.hetzner.resources.ActionResponseWrapper
import de.solidblocks.cli.utils.pascalCaseToWhiteSpace

interface NamedHetznerResource {
    val id: Long
    val name: String
}

interface HetznerBaseResourceApi<T : NamedHetznerResource> {
    suspend fun list(): List<T>
}

interface HetznerSimpleResourceApi<T : NamedHetznerResource> : HetznerBaseResourceApi<T> {
    suspend fun delete(id: Long): Boolean
}

interface HetznerProtectedResourceApi {
    suspend fun changeProtection(id: Long, delete: Boolean): ActionResponseWrapper
    suspend fun action(id: Long): ActionResponseWrapper
}

interface HetznerAssignedResourceApi {
    suspend fun unassign(id: Long): ActionResponseWrapper
    suspend fun action(id: Long): ActionResponseWrapper
}

interface HetznerComplexResourceApi<T : NamedHetznerResource> : HetznerBaseResourceApi<T> {
    suspend fun delete(id: Long): ActionResponseWrapper
    suspend fun action(id: Long): ActionResponseWrapper
}

fun NamedHetznerResource.logText() =
    "${this::class.pascalCaseToWhiteSpace()} '${name}' (${id})"