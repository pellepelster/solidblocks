package de.solidblocks.cloud.services

import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import kotlin.reflect.KClass

data class ServiceCommonConfig(val name: String, val environmentVars: Map<String, String>, val instance: InstanceConfig)

data class InstanceConfig(val volumeSize: Int, val hetznerLocation: HetznerLocation?, val hetznerInstanceType: HetznerServerType?)

interface ServiceConfiguration {
    val name: String
    val type: String
    val environmentVars: Map<String, String>
    val neededProviders: List<KClass<*>>
}
