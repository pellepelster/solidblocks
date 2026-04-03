package de.solidblocks.cloud.services

data class ServiceInfo(val serviceName: String, val linkedEnvironmentVariables: Map<String, String>)
