package de.solidblocks.cloud.services

import de.solidblocks.cloud.provisioner.CloudProvisionerContext

sealed class BaseEnvironmentVariable(val name: String, val description: String)

class EnvironmentVariableCallback(name: String, description: String, val value: (CloudProvisionerContext) -> String) : BaseEnvironmentVariable(name, description)

class EnvironmentVariableStatic(name: String, description: String, val value: String) : BaseEnvironmentVariable(name, description)
