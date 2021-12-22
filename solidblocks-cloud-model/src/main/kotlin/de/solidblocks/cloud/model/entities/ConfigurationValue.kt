package de.solidblocks.cloud.model.entities

fun createConfigValue(name: String, value: String): CloudConfigValue {
    return CloudConfigValue(name, value)
}

fun List<CloudConfigValue>.getConfigValue(name: String): CloudConfigValue? {
    return this.firstOrNull { it.name == name }
}

fun List<CloudConfigValue>.getRawConfigValue(name: String): String {
    return this.first { it.name == name }.value
}

data class CloudConfigValue(val name: String, val value: String, val version: Int = 0)
