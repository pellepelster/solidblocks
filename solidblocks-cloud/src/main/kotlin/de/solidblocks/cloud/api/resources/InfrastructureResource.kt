package de.solidblocks.cloud.api.resources

import kotlin.reflect.KClass

abstract class BaseInfrastructureResource<RuntimeType>(name: String, dependsOn: Set<BaseResource>, val taintable: Boolean = true, val taintRequiresRecreate: Boolean = false) : BaseResource(name, dependsOn) {
    private var tainted: Boolean = false

    fun isTainted() = tainted

    fun taint() = if (taintable) {
        tainted = true
        true
    } else {
        false
    }

    abstract val lookupType: KClass<*>

    abstract fun asLookup(): InfrastructureResourceLookup<*>
}

abstract class BaseLabeledInfrastructureResource<RuntimeType>(
    name: String,
    dependsOn: Set<BaseResource>,
    val labels: Map<String, String>,
    taintRequiresRecreate: Boolean = false,
) : BaseInfrastructureResource<RuntimeType>(name, dependsOn, true, taintRequiresRecreate)
