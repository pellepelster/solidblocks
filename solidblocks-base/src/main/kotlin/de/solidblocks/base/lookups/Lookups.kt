package de.solidblocks.base.lookups

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.InfrastructureProvisioner
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.core.IResourceLookup

class Lookups {

    companion object {
        fun registerLookups(
            provisionerRegistry: ProvisionerRegistry,
            provisioner: InfrastructureProvisioner
        ) {
            provisionerRegistry.addLookupProvider(
                Base64EncodeResourceLookupProvider(provisioner) as IResourceLookupProvider<IResourceLookup<Any>, Any>
            )

            provisionerRegistry.addLookupProvider(
                ConstantResourceLookupProvider() as IResourceLookupProvider<IResourceLookup<Any>, Any>
            )

            provisionerRegistry.addLookupProvider(
                CustomResourceLookupProvider() as IResourceLookupProvider<IResourceLookup<Any>, Any>
            )

            provisionerRegistry.addLookupProvider(
                ResourceLookupProvider<Any>(provisioner) as IResourceLookupProvider<IResourceLookup<Any>, Any>
            )
        }
    }
}
