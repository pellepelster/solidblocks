package de.solidblocks.hetzner.cloud.model

import de.solidblocks.hetzner.cloud.pascalCaseToWhiteSpace
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

interface HetznerProtectedResource : HetznerNamedResource {
    val protection: HetznerProtectionResponse
}

interface HetznerAssignedResource : HetznerNamedResource {
    val isAssigned: Boolean
}

interface HetznerNamedResource : HetznerResource {
    val name: String?
}

interface HetznerResource {
    val id: Long
}

@Serializable
data class HetznerProtectionResponse(
    val delete: Boolean,
)

sealed class LabelSelectorValue() {
    data class Equals(val value: String) : LabelSelectorValue() {
        override fun query(key: String) = "${key}==${value}"
    }

    data class NotEquals(val value: String) : LabelSelectorValue() {
        override fun query(key: String) = "${key}!=${value}"
    }

    abstract fun query(key: String): String
}

sealed class FilterValue() {
    data class Equals(val value: String) : FilterValue() {
        override val query: String
            get() = value

    }

    abstract val query: String
}

fun KClass<out HetznerNamedResource>.pascalCaseToWhiteSpace() =
    this.simpleName!!.removeSuffix("Response").pascalCaseToWhiteSpace().lowercase()

fun HetznerNamedResource.logText() =
    "${this::class.pascalCaseToWhiteSpace()} '${name ?: "<no name>"}' ($id)"
