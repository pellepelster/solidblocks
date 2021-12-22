package de.solidblocks.cloud.model.model

fun createConfigValue(name: String, value: String): CloudConfigValue {
    return CloudConfigValue(name, value)
}

fun List<CloudConfigValue>.getConfigValue(name: String): CloudConfigValue? {
    return this.firstOrNull { it.name == name }
}

fun EnvironmentModel.getConfigValue(name: String): CloudConfigValue? {
    return this.configValues.firstOrNull { it.name == name }
}

data class CloudConfigValue(val name: String, val value: String, val version: Int = 0)
