package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.FilterValue
import de.solidblocks.hetzner.cloud.model.HetznerNamedResource
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue
import de.solidblocks.hetzner.cloud.model.ListResponse
import de.solidblocks.hetzner.cloud.resources.ActionResponseWrapper

interface HetznerBaseResourceApi<T> {

    suspend fun listPaged(
        page: Int = 0,
        perPage: Int = 25,
        filter: Map<String, FilterValue> = emptyMap(),
        labelSelectors: Map<String, LabelSelectorValue> = emptyMap(),
    ): ListResponse<T>

    suspend fun list(
        filter: Map<String, FilterValue> = emptyMap(),
        labelSelectors: Map<String, LabelSelectorValue> = emptyMap(),
    ) =
        handlePaginatedList<T>(filter, labelSelectors) { page, perPage, filter, labelSelectors ->
            listPaged(
                page,
                perPage,
                filter,
                labelSelectors,
            )
        }
}

interface HetznerDeleteResourceApi<ID, T : HetznerNamedResource<ID>> : HetznerBaseResourceApi<T> {
    suspend fun delete(id: Long): Boolean
}

interface HetznerProtectedResourceApi<ID, T : HetznerNamedResource<ID>> : HetznerBaseResourceApi<T> {
    suspend fun changeDeleteProtection(id: Long, delete: Boolean): ActionResponseWrapper

    suspend fun action(id: Long): ActionResponseWrapper
}

interface HetznerAssignedResourceApi {
    suspend fun unassign(id: Long): ActionResponseWrapper?

    suspend fun action(id: Long): ActionResponseWrapper
}

interface HetznerDeleteWithActionResourceApi<ID, T : HetznerNamedResource<ID>> : HetznerBaseResourceApi<T> {
    suspend fun delete(id: Long): ActionResponseWrapper

    suspend fun action(id: Long): ActionResponseWrapper
}
