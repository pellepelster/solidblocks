package de.solidblocks.cloud.environments.api

data class EnvironmentCreateRequest(
    val environment: String?,
    val email: String?,
    val password: String?,
    val githubReadOnlyToken: String?,
    val hetznerCloudApiTokenReadOnly: String?,
    val hetznerCloudApiTokenReadWrite: String?,
    val hetznerDnsApiToken: String?
)
