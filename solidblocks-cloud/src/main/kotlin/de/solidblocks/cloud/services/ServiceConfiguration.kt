package de.solidblocks.cloud.services

import kotlin.reflect.KClass

interface ServiceConfiguration {
    val name: String
    val type: String
    val environmentVars: Map<String, String>
    val neededProviders: List<KClass<*>>
}
