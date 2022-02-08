package de.solidblocks.cloud.model.entities

import de.solidblocks.base.reference.TenantReference
import java.util.*

fun TenantEntity.toReference() = TenantReference(this.environment.cloud.name, this.environment.name, this.name)

data class TenantEntity(
    val id: UUID,
    val name: String,
    val environment: EnvironmentEntity,
    val configValues: List<CloudConfigValue>,
) {
    fun getConfigValue(key: String) = configValues.getConfigValue(key)!!.value

    val reference: TenantReference
        get() = environment.reference.toTenant(name)



}
