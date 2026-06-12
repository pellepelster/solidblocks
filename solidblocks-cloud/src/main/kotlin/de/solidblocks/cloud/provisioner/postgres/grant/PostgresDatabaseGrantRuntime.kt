package de.solidblocks.cloud.provisioner.postgres.grant

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

class PostgresDatabaseGrantRuntime(val user: String, val database: String, val admin: Boolean, val read: Boolean, val write: Boolean) : BaseInfrastructureResourceRuntime(emptyList())
