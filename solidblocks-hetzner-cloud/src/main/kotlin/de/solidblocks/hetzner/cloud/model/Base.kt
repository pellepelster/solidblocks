package de.solidblocks.hetzner.cloud.model

import kotlinx.serialization.Serializable

interface HetznerDeleteProtectedResource<ID> : HetznerNamedResource<ID> {
    val protection: HetznerDeleteProtectionResponse
}

interface HetznerChangeProtectedResource<ID> : HetznerNamedResource<ID> {
    val protection: HetznerChangeProtectionResponse
}

interface HetznerAssignedResource<ID> : HetznerNamedResource<ID> {
    val isAssigned: Boolean
}

interface HetznerNamedResource<ID> : HetznerResource<ID> {
    val name: String?
}

interface HetznerResource<ID> {
    val id: ID
}

@Serializable
data class HetznerDeleteProtectionResponse(val delete: Boolean)

@Serializable
data class HetznerChangeProtectionResponse(val change: Boolean)

sealed class LabelSelectorValue {
    data class Equals(val value: String) : LabelSelectorValue() {
        override fun query(key: String) = "$key==$value"
    }

    data class NotEquals(val value: String) : LabelSelectorValue() {
        override fun query(key: String) = "$key!=$value"
    }

    abstract fun query(key: String): String
}

public fun Map<String, String>.toLabelSelectors() = this.entries.associate { it.key to LabelSelectorValue.Equals(it.value) }

open class BaseFilter(val attribute: String, val value: String) {
    fun queryPart() = "$attribute=$value"
}

enum class Architecture { x86, arm }

enum class HetznerLocation {
    fsn1,
    nbg1,
    hel1,
    ash,
    hil,
    sin,
}

enum class HetznerServerType {
    cx23,
    cx33,
    cx43,
    cx53,
    cpx21,
    cpx31,
    cpx41,
    cpx51,
    cax11,
    cax21,
    cax31,
    cax41,
    ccx13,
    ccx23,
    ccx33,
    ccx43,
    ccx53,
    ccx63,
    cpx12,
    cpx22,
    cpx32,
    cpx42,
    cpx52,
    cpx62,
}
