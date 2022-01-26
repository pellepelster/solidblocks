package de.solidblocks.cloud.clouds.api

import de.solidblocks.cloud.api.MessageResponse

class CloudsResponseWrapper(val clouds: List<CloudResponse>, val messages: List<MessageResponse> = emptyList())
