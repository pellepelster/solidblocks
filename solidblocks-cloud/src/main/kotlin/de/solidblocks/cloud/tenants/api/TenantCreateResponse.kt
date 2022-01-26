package de.solidblocks.cloud.tenants.api

import de.solidblocks.cloud.api.MessageResponse

data class TenantCreateResponse(val tenant: TenantResponse? = null, val messages: List<MessageResponse> = emptyList())
