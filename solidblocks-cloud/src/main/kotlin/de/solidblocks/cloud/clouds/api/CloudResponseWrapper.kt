package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.api.BaseApiResponse
import de.solidblocks.cloud.api.MessageResponse

class CloudResponseWrapper(val cloud: CloudResponse? = null, messages: List<MessageResponse> = emptyList()) : BaseApiResponse(messages)
