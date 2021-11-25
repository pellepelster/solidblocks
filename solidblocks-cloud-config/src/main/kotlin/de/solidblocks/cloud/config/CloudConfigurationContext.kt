package de.solidblocks.cloud.config

import de.solidblocks.cloud.config.model.CloudConfiguration
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.cloud.config.model.getConfigValue

class CloudConfigurationContext(val cloud: CloudConfiguration, val environment: CloudEnvironmentConfiguration) {

    val cloudName: String
        get() = cloud.name

    val environmentName: String
        get() = environment.name


    fun configurationValue(key: String): String {
        return environment.configValues.getConfigValue(key)!!.value
    }
}