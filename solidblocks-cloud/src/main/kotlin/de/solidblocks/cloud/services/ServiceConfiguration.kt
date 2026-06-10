package de.solidblocks.cloud.services

import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType

data class ServiceCommonConfig(val name: String, val useFloatingIp: Boolean, val environmentVars: Map<String, String>, val instance: InstanceConfig)

data class InstanceConfig(val volumeSize: Int, val hetznerLocation: HetznerLocation?, val hetznerInstanceType: HetznerServerType?)

interface ServiceConfiguration {
    val common: ServiceCommonConfig
    val type: String

    val name: String
        get() = common.name

    val instance: InstanceConfig
        get() = common.instance

    val environmentVars: Map<String, String>
        get() = common.environmentVars
}
