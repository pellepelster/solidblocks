package de.solidblocks.cloud.tenants.api

import de.solidblocks.base.api.MessageResponse

data class TenantCreateResponse(val tenant: TenantResponse? = null, val messages: List<MessageResponse> = emptyList())
