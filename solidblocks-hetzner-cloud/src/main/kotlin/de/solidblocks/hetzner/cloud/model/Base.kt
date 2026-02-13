package de.solidblocks.hetzner.cloud.model

import de.solidblocks.hetzner.cloud.pascalCaseToWhiteSpace
import kotlin.reflect.KClass
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
data class HetznerDeleteProtectionResponse(
    val delete: Boolean,
)

@Serializable
data class HetznerChangeProtectionResponse(
    val change: Boolean,
)

sealed class LabelSelectorValue {
  data class Equals(val value: String) : LabelSelectorValue() {
    override fun query(key: String) = "$key==$value"
  }

  data class NotEquals(val value: String) : LabelSelectorValue() {
    override fun query(key: String) = "$key!=$value"
  }

  abstract fun query(key: String): String
}

fun Map<String, String>.toLabelSelectors() =
    this.entries.associate { it.key to LabelSelectorValue.Equals(it.value) }

sealed class FilterValue {
  data class Equals(val value: String) : FilterValue() {
    override val query: String
      get() = value
  }

  abstract val query: String
}

fun KClass<out HetznerNamedResource<*>>.pascalCaseToWhiteSpace() =
    this.simpleName!!.removeSuffix("Response").pascalCaseToWhiteSpace().lowercase()

fun HetznerNamedResource<*>.logText() =
    "${this::class.pascalCaseToWhiteSpace()} '${name ?: "<no name>"}' ($id)"
