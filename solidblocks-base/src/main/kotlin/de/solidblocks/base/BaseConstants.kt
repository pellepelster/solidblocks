package de.solidblocks.base

object BaseConstants {

    const val SERVICE_LABEL_KEY = "service"

    fun cloudId(reference: CloudReference) = reference.cloud

    fun environmentId(reference: EnvironmentReference) = "${cloudId(reference)}-${reference.environment}"

    fun tenantId(reference: TenantReference) = "${environmentId(reference)}-${reference.tenant}"

    fun serviceId(reference: ServiceReference) = "${tenantId(reference)}-${reference.service}"

    fun serversDomain(reference: ServiceReference, rootDomain: String) =
        "${reference.service}.${reference.tenant}.${reference.environment}.$rootDomain"

    fun serversDomain(reference: EnvironmentReference, rootDomain: String) =
        "${reference.environment}.$rootDomain"

    fun serversDomain(reference: EnvironmentServiceReference, rootDomain: String) =
        "${reference.service}.${reference.environment}.$rootDomain"

    fun serversDomain(reference: TenantReference, rootDomain: String) =
        "${reference.tenant}.${reference.environment}.$rootDomain"

    fun environmentHostFQDN(hostname: String, reference: EnvironmentReference, rootDomain: String) =
        "$hostname.${serversDomain(reference, rootDomain)}"

    fun tenantHostFQDN(hostname: String, reference: TenantReference, rootDomain: String) =
        "$hostname.${serversDomain(reference, rootDomain)}"

    fun vaultTokenName(name: String, reference: EnvironmentReference, location: String, index: Int) =
        serverName(name, reference, location, index)

    fun vaultTokenName(name: String, reference: ServiceReference) =
        "$name-${serviceId(reference)}"

    fun serverName(name: String, reference: EnvironmentReference, location: String, index: Int) =
        "${environmentId(reference)}-$name-$index-$location"

    fun serverName(reference: ServiceReference, location: String, index: Int) =
        "${serviceId(reference)}-$index-$location"
}
