package de.solidblocks.cli.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageVersion(
    val id: Long,
    val name: String,
    val metadata: Metadata? = null,
)

@Serializable
data class Metadata(
    @SerialName("package_type") val packageType: String,
    @SerialName("container") val container: Container? = null,
)

@Serializable
data class Container(
    val tags: List<String> = emptyList(),
)
