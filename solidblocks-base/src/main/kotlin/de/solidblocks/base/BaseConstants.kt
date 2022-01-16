package de.solidblocks.base

import de.solidblocks.base.resources.*

object BaseConstants {

    const val SERVICE_LABEL_KEY = "service"

    fun cloudId(reference: CloudResource) = reference.cloud

    fun environmentId(reference: EnvironmentResource) = "${cloudId(reference)}-${reference.environment}"

    fun tenantId(reference: TenantResource) = "${environmentId(reference)}-${reference.tenant}"

    fun serviceId(reference: ServiceResource) = "${tenantId(reference)}-${reference.service}"

    fun serversDomain(reference: ServiceResource, rootDomain: String) =
        "${reference.service}.${reference.tenant}.${reference.environment}.$rootDomain"

    fun serversDomain(reference: EnvironmentResource, rootDomain: String) =
        "${reference.environment}.$rootDomain"

    fun serversDomain(reference: EnvironmentServiceResource, rootDomain: String) =
        "${reference.service}.${reference.environment}.$rootDomain"

    fun serversDomain(reference: TenantResource, rootDomain: String) =
        "${reference.tenant}.${reference.environment}.$rootDomain"

    fun environmentHostFQDN(hostname: String, reference: EnvironmentResource, rootDomain: String) =
        "$hostname.${serversDomain(reference, rootDomain)}"

    fun tenantHostFQDN(hostname: String, reference: TenantResource, rootDomain: String) =
        "$hostname.${serversDomain(reference, rootDomain)}"

    fun vaultTokenName(name: String, reference: EnvironmentResource, location: String, index: Int) =
        serverName(name, reference, location, index)

    fun vaultTokenName(name: String, reference: ServiceResource) =
        "$name-${serviceId(reference)}"

    fun serverName(name: String, reference: EnvironmentResource, location: String, index: Int) =
        "${environmentId(reference)}-$name-$index-$location"

    fun serverName(reference: ServiceResource, location: String, index: Int) =
        "${serviceId(reference)}-$index-$location"
}
