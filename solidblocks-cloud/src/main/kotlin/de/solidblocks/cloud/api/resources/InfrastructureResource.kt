package de.solidblocks.cloud.api.resources

import kotlin.reflect.KClass

abstract class BaseInfrastructureResource<RuntimeType>(name: String, dependsOn: Set<BaseResource>) : BaseResource(name, dependsOn) {
    var tainted: Boolean = false

    fun taint() {
        tainted = true
    }

    abstract val lookupType: KClass<*>
}

abstract class BaseLabeledInfrastructureResource<RuntimeType>(
    name: String, dependsOn: Set<BaseResource>,
    val labels: Map<String, String>
) : BaseInfrastructureResource<RuntimeType>(name, dependsOn)
