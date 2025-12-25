package de.solidblocks.infra.test.cloudinit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudInitResultWrapper(
    @SerialName("v1") val v1: CloudInitResult,
) {
  val allErrors: List<String>
    get() =
        listOfNotNull(
                v1.errors,
                v1.init?.errors,
                v1.initLocal?.errors,
                v1.modulesInit?.errors,
                v1.modulesConfig?.errors,
                v1.modulesFinal?.errors,
            )
            .flatten()

  val hasErrors: Boolean
    get() = allErrors.any { it.isNotEmpty() }

  val isFinished: Boolean
    get() = v1.modulesFinal?.finished != null
}

@Serializable
data class CloudInitResult(
    @SerialName("errors") val errors: List<String>? = null,
    @SerialName("init") val init: CloudInitResultItem? = null,
    @SerialName("init-local") val initLocal: CloudInitResultItem? = null,
    @SerialName("modules-config") val modulesConfig: CloudInitResultItem? = null,
    @SerialName("modules-final") val modulesFinal: CloudInitResultItem? = null,
    @SerialName("modules-init") val modulesInit: CloudInitResultItem? = null,
)

@Serializable
data class CloudInitResultItem(
    @SerialName("errors") val errors: List<String>,
    @SerialName("finished") val finished: Float? = null,
    @SerialName("start") val start: Float? = null,
)
