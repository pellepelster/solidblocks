package de.solidblocks.hetzner.dns.model

import kotlinx.serialization.Serializable

@Serializable
data class RecordsResponseWrapper(val records: List<RecordResponse>)
