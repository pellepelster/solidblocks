package de.solidblocks.cloud.model.entities

import de.solidblocks.base.resources.TenantResource
import java.util.*

data class TenantEntity(
    val id: UUID,
    val name: String,
    val environment: EnvironmentEntity,
    val configValues: List<CloudConfigValue>,
) {
    fun getConfigValue(key: String) = configValues.getConfigValue(key)!!.value

    val reference: TenantResource
        get() = environment.reference.toTenant(name)
}
