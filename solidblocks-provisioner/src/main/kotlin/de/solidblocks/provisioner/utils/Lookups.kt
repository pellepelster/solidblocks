package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.Provisioner

class Lookups {

    companion object {
        fun registerLookups(
            provisionerRegistry: ProvisionerRegistry,
            provisioner: Provisioner
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

            provisionerRegistry.addLookupProvider(
                UserDataResourceLookupProvider(provisioner) as IResourceLookupProvider<IResourceLookup<Any>, Any>
            )
        }
    }
}
