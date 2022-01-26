package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.api.MessageResponse

class CloudResponseWrapper(val cloud: CloudResponse? = null, val messages: List<MessageResponse> = emptyList())
