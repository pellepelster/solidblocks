package de.solidblocks.service.helloworld.backend

import de.solidblocks.api.services.ServiceCatalogItemResponse
import de.solidblocks.api.services.ServiceManagerFactory

class HelloWorldServiceManagerFactory : ServiceManagerFactory<HelloWorldServiceManager> {

    override val type: String
        get() = "helloworld"

    override val catalogItem: ServiceCatalogItemResponse
        get() = ServiceCatalogItemResponse(type, "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor ")

    override fun createServiceManager(): HelloWorldServiceManager {
        return HelloWorldServiceManager()
    }
}
