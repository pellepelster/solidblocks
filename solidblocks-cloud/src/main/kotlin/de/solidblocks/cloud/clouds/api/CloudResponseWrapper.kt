package de.solidblocks.cloud.clouds.api

import de.solidblocks.base.api.MessageResponse

class CloudResponseWrapper(val cloud: CloudResponse? = null, val messages: List<MessageResponse> = emptyList())
