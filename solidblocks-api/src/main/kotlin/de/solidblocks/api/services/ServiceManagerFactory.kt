package de.solidblocks.api.services

interface ServiceManagerFactory<T : ServiceManager> {

    fun createServiceManager(): T

    val type: String

    val catalogItem: ServiceCatalogItemResponse
}
