package de.solidblocks.hetzner.dns.model

import kotlinx.serialization.Serializable

@Serializable data class ListRecordsResponse(val records: List<RecordResponse>)
