package de.solidblocks.cloud.model.entities

import de.solidblocks.base.TenantReference
import java.util.*

data class TenantEntity(
    val id: UUID,
    val name: String,
    val environment: EnvironmentEntity
) {

    val reference: TenantReference
        get() = environment.reference.toTenant(name)
}
