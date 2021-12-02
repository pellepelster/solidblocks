package de.solidblocks.cloud.config

import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.cloud.config.model.getConfigValue

class CloudConfigurationContext(val environment: CloudEnvironmentConfiguration) {

    val cloudName: String
        get() = environment.cloud.name

    val environmentName: String
        get() = environment.name

    fun configurationValue(key: String): String {
        return environment.configValues.getConfigValue(key)!!.value
    }
}
