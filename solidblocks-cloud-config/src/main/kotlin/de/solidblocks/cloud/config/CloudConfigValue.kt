package de.solidblocks.cloud.config


fun createConfigValue(name: String, value: String): CloudConfigValue {
    return CloudConfigValue(name, value)
}

fun List<CloudConfigValue>.getConfigValue(name: String): CloudConfigValue? {
    return this.firstOrNull { it.name == name }
}


data class CloudConfigValue(val name: String, val value: String, val version: Int = 0)
