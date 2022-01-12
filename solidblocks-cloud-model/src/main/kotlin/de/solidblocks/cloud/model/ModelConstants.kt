package de.solidblocks.cloud.model

import de.solidblocks.base.CloudReference
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.base.TenantReference
import de.solidblocks.cloud.model.entities.Role

object ModelConstants {

    const val LABEL_PREFIX = "solidblocks"

    const val SERVICE_LABEL_KEY = "service"

    val SERVICE_LABEL: String = "$LABEL_PREFIX/service"

    val ROLE_LABEL: String = "$LABEL_PREFIX/role"

    fun serviceBucketName(reference: ServiceReference) = serviceId(reference)

    fun serviceConfigPath(reference: ServiceReference) =
        "/solidblocks/services/${reference.service}/config"

    // fun environmentId(cloud: String, environment: String) = "$cloud-$environment"
    fun cloudId(reference: CloudReference) = reference.cloud

    fun environmentId(reference: EnvironmentReference) = "${cloudId(reference)}-${reference.environment}"

    fun tenantId(reference: TenantReference) = "${environmentId(reference)}-${reference.tenant}"

    fun serviceId(reference: ServiceReference) = "${tenantId(reference)}-${reference.service}"

    fun networkName(environment: EnvironmentReference) = environmentId(environment)

    fun sshKeyName(environment: EnvironmentReference) = environmentId(environment)

    fun networkName(reference: TenantReference) = tenantId(reference)

    fun defaultRoleLabels(role: Role) = mapOf(ROLE_LABEL to role.toString())

    fun defaultCloudLabels(reference: CloudReference) =
        mapOf("$LABEL_PREFIX/cloud" to reference.cloud)

    fun defaultEnvironmentLabels(reference: EnvironmentReference) =
        mapOf("$LABEL_PREFIX/environment" to reference.environment) + defaultCloudLabels(reference)

    fun defaultCloudLabels(cloud: CloudReference, role: Role) =
        defaultRoleLabels(role) + defaultCloudLabels(cloud)

    fun defaultEnvironmentLabels(reference: EnvironmentReference, role: Role) =
        mapOf("$LABEL_PREFIX/environment" to reference.environment) + defaultCloudLabels(reference, role)

    fun defaultTenantLabels(reference: TenantReference) =
        mapOf("$LABEL_PREFIX/tenant" to reference.tenant) + defaultEnvironmentLabels(reference)

    fun defaultTenantLabels(reference: TenantReference, role: Role) =
        defaultTenantLabels(reference) + defaultRoleLabels(role)

    fun defaultServiceLabels(reference: ServiceReference, role: Role) =
        mapOf(SERVICE_LABEL to reference.service) + defaultTenantLabels(reference) + defaultRoleLabels(role)

    /*
    fun volumeName(reference: EnvironmentReference, serverName: String, location: String) =
        "${environmentId(reference)}-$serverName-$location"
    */

    fun volumeName(reference: ServiceReference, location: String, index: Int) =
        serverName(reference, location, index)

    fun serverName(reference: ServiceReference, location: String, index: Int) =
        "${serviceId(reference)}-$index-$location"

    fun volumeName(name: String, reference: EnvironmentReference, location: String, index: Int) =
        serverName(name, reference, location, index)

    fun floatingIpName(name: String, reference: EnvironmentReference, location: String, index: Int) =
        serverName(name, reference, location, index)

    fun serverName(name: String, reference: EnvironmentReference, location: String, index: Int) =
        "${environmentId(reference)}-$name-$index-$location"

    fun vaultTokenName(name: String, reference: EnvironmentReference, location: String, index: Int) =
        serverName(name, reference, location, index)

    fun vaultTokenName(name: String, reference: ServiceReference) =
        "$name-${serviceId(reference)}"

    const val GITHUB_TOKEN_RO_KEY = "github_token_ro"
    const val GITHUB_USERNAME_KEY = "github_username"

    const val CONSUL_MASTER_TOKEN_KEY = "consul_master_token"
    const val CONSUL_SECRET_KEY = "consul_secret"

    const val SSH_PUBLIC_KEY = "ssh_public_key"
    const val SSH_PRIVATE_KEY = "ssh_private_key"

    const val SSH_IDENTITY_PUBLIC_KEY = "ssh_identity_public_key"
    const val SSH_IDENTITY_PRIVATE_KEY = "ssh_identity_private_key"

    const val TENANT_NETWORK_CIDR_KEY = "network_cidr"
}
