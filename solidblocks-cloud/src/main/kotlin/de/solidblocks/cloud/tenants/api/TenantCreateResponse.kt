package de.solidblocks.cloud.tenants.api

import de.solidblocks.cloud.api.BaseApiResponse
import de.solidblocks.cloud.api.MessageResponse

class TenantCreateResponse(val tenant: TenantResponse, errors: List<MessageResponse>) : BaseApiResponse(errors)
