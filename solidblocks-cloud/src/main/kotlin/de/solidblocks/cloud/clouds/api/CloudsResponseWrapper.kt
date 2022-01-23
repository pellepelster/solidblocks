package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.api.BaseApiResponse
import de.solidblocks.cloud.api.MessageResponse

class CloudsResponseWrapper(val clouds: List<CloudResponse>, messages: List<MessageResponse> = emptyList()) : BaseApiResponse(messages)
