package de.solidblocks.cloud.clouds.api

import de.solidblocks.base.api.MessageResponse

class CloudsResponseWrapper(val clouds: List<CloudResponse>, val messages: List<MessageResponse> = emptyList())
