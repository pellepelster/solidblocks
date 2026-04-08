package de.solidblocks.cloud.services

import de.solidblocks.cloud.provisioner.ProvisionerContext

sealed class BaseEnvironmentVariable(val name: String, val description: String)

class EnvironmentVariableCallback(
    name: String,
    description: String,
    val value: (ProvisionerContext) -> String,
) : BaseEnvironmentVariable(name, description)

class EnvironmentVariableStatic(name: String, description: String, val value: String) :
    BaseEnvironmentVariable(name, description)
