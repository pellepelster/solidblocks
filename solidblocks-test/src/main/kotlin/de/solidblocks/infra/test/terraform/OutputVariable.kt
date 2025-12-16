package de.solidblocks.infra.test.terraform

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OutputVariable(
    var type: JsonElement,
    val value: JsonElement? = null,
    var sensitive: Boolean,
)
