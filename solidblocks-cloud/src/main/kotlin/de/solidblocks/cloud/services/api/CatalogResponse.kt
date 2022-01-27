package de.solidblocks.cloud.services.api

import de.solidblocks.cloud.api.MessageResponse

data class CatalogResponse(val items: List<CatalogItemResponse>, val messages: List<MessageResponse> = emptyList())
