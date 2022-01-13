package de.solidblocks.cloud

import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.cloud.model.ModelConstants.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.model.ModelConstants.CONSUL_SECRET_KEY
import de.solidblocks.cloud.model.ModelConstants.GITHUB_TOKEN_RO_KEY
import de.solidblocks.cloud.model.ModelConstants.GITHUB_USERNAME_KEY
import de.solidblocks.cloud.model.ModelConstants.environmentId
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.TenantEntity
import de.solidblocks.cloud.model.entities.getConfigValue
import de.solidblocks.provisioner.hetzner.Hetzner.HETZNER_CLOUD_API_TOKEN_RO_KEY
import de.solidblocks.provisioner.vault.kv.VaultKV
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRole
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRole
import de.solidblocks.vault.VaultConstants
import de.solidblocks.vault.VaultConstants.backupPolicyName
import de.solidblocks.vault.VaultConstants.clientsDomain
import de.solidblocks.vault.VaultConstants.environmentClientPkiMountName
import de.solidblocks.vault.VaultConstants.environmentServerPkiMountName
import de.solidblocks.vault.VaultConstants.hostSshMountName
import de.solidblocks.vault.VaultConstants.ingressPolicyName
import de.solidblocks.vault.VaultConstants.kvMountName
import de.solidblocks.vault.VaultConstants.providersGithubPolicy
import de.solidblocks.vault.VaultConstants.serversDomain
import de.solidblocks.vault.VaultConstants.tenantClientPkiMountName
import de.solidblocks.vault.VaultConstants.tenantServerPkiMountName
import de.solidblocks.vault.VaultConstants.userSshMountName
import org.springframework.vault.support.Policy

object VaultCloudConfiguration {

    fun createEnvironmentVaultConfig(
        parentResourceGroups: Set<ResourceGroup>,
        environment: EnvironmentEntity
    ): ResourceGroup {
        val resourceGroup = ResourceGroup("vaultConfig", parentResourceGroups)

        val serverPkiMount = VaultMount(environmentServerPkiMountName(environment.reference), "pki")
        val serverPkiBackendRole = VaultPkiBackendRole(
            name = environmentServerPkiMountName(environment.reference),
            allowedDomains = listOf(serversDomain(environment.reference, environment.cloud.rootDomain)),
            allowSubdomains = true,
            allowLocalhost = environment.cloud.isDevelopment,
            generateLease = true,
            serverFlag = true,
            clientFlag = false,
            maxTtl = "168h",
            ttl = "168h",
            keyBits = 521,
            keyType = "ec",
            mount = serverPkiMount
        )
        resourceGroup.addResource(serverPkiBackendRole)

        val clientPkiMount = VaultMount(environmentClientPkiMountName(environment.reference), "pki")
        val clientPkiBackendRole = VaultPkiBackendRole(
            name = environmentClientPkiMountName(environment.reference),
            allowedDomains = listOf(clientsDomain()),
            allowSubdomains = true,
            generateLease = true,
            serverFlag = false,
            clientFlag = true,
            maxTtl = "168h",
            ttl = "168h",
            keyBits = 521,
            keyType = "ec",
            mount = clientPkiMount
        )
        resourceGroup.addResource(clientPkiBackendRole)

        val hostSshMount = VaultMount(
            hostSshMountName(environment.reference), "ssh"
        )
        val hostSshBackendRole = VaultSshBackendRole(
            name = hostSshMountName(environment.reference),
            keyType = "ca",
            maxTtl = "168h",
            ttl = "168h",
            allowHostCertificates = true,
            allowUserCertificates = false,
            mount = hostSshMount
        )
        resourceGroup.addResource(hostSshBackendRole)

        val userSshMount = VaultMount(
            userSshMountName(environment.reference), "ssh"
        )
        val userSshBackendRole = VaultSshBackendRole(
            name = userSshMountName(environment.reference),
            keyType = "ca",
            maxTtl = "168h",
            ttl = "168h",
            allowedUsers = listOf(environmentId(environment.reference)),
            defaultUser = environmentId(environment.reference),
            allowHostCertificates = false,
            allowUserCertificates = true,
            mount = userSshMount
        )
        resourceGroup.addResource(userSshBackendRole)

        val kvMount = VaultMount(kvMountName(environment), "kv-v2")
        resourceGroup.addResource(kvMount)

        val solidblocksConfig = VaultKV(
            "solidblocks/cloud/config", mapOf(), kvMount
        )
        resourceGroup.addResource(solidblocksConfig)

        val hetznerConfig = VaultKV(
            "solidblocks/cloud/providers/hetzner",
            mapOf(
                HETZNER_CLOUD_API_TOKEN_RO_KEY to environment.configValues.getConfigValue(
                    HETZNER_CLOUD_API_TOKEN_RO_KEY
                )!!.value
            ),
            kvMount
        )
        resourceGroup.addResource(hetznerConfig)

        val githubConfig = VaultKV(
            "solidblocks/cloud/providers/github",
            mapOf(
                GITHUB_TOKEN_RO_KEY to environment.configValues.getConfigValue(GITHUB_TOKEN_RO_KEY)!!.value,
                GITHUB_USERNAME_KEY to "pellepelster"
            ),
            kvMount
        )
        resourceGroup.addResource(githubConfig)

        val consulConfig = VaultKV(
            "solidblocks/cloud/config/consul",
            mapOf(
                CONSUL_SECRET_KEY to environment.configValues.getConfigValue(CONSUL_SECRET_KEY)!!.value,
                CONSUL_MASTER_TOKEN_KEY to environment.configValues.getConfigValue(CONSUL_MASTER_TOKEN_KEY)!!.value
            ),
            kvMount
        )
        resourceGroup.addResource(consulConfig)

        val ingressPolicy = VaultPolicy(
            ingressPolicyName(environment.reference),
            setOf(
                providersGithubPolicy(environment.reference),
                Policy.Rule.builder().path(
                    "${kvMountName(environment)}/data/solidblocks/cloud/config"
                ).capabilities(Policy.BuiltinCapabilities.READ).build(),

                Policy.Rule.builder().path(
                    "${kvMountName(environment)}/data/solidblocks/cloud/providers/hetzner"
                ).capabilities(Policy.BuiltinCapabilities.READ).build(),

                Policy.Rule.builder()
                    .path("${userSshMountName(environment.reference)}/sign/${userSshMountName(environment.reference)}")
                    .capabilities(
                        Policy.BuiltinCapabilities.UPDATE, Policy.BuiltinCapabilities.CREATE
                    ).build(),

                Policy.Rule.builder().path("${userSshMountName(environment.reference)}/config/ca")
                    .capabilities(Policy.BuiltinCapabilities.READ).build(),

                Policy.Rule.builder()
                    .path("${hostSshMountName(environment.reference)}/sign/${hostSshMountName(environment.reference)}")
                    .capabilities(
                        Policy.BuiltinCapabilities.UPDATE, Policy.BuiltinCapabilities.CREATE
                    ).build(),
                Policy.Rule.builder().path("${hostSshMountName(environment.reference)}/config/ca")
                    .capabilities(Policy.BuiltinCapabilities.READ).build(),

            )
        )
        resourceGroup.addResource(ingressPolicy)

        val backupPolicy = VaultPolicy(
            backupPolicyName(environment.reference),
            setOf(
                providersGithubPolicy(environment.reference),
                VaultConstants.tokenSelfRenewalPolicy(),
                VaultConstants.tokenSelfLookupPolicy(),
                Policy.Rule.builder().path(
                    "${kvMountName(environment)}/data/solidblocks/cloud/config"
                ).capabilities(Policy.BuiltinCapabilities.READ).build(),

                Policy.Rule.builder().path(
                    "${kvMountName(environment)}/data/solidblocks/cloud/providers/hetzner"
                ).capabilities(Policy.BuiltinCapabilities.READ).build(),

                Policy.Rule.builder()
                    .path("${userSshMountName(environment.reference)}/sign/${userSshMountName(environment.reference)}")
                    .capabilities(
                        Policy.BuiltinCapabilities.UPDATE, Policy.BuiltinCapabilities.CREATE
                    ).build(),

                Policy.Rule.builder().path("${userSshMountName(environment.reference)}/config/ca")
                    .capabilities(Policy.BuiltinCapabilities.READ).build(),

                Policy.Rule.builder()
                    .path("${hostSshMountName(environment.reference)}/sign/${hostSshMountName(environment.reference)}")
                    .capabilities(
                        Policy.BuiltinCapabilities.UPDATE, Policy.BuiltinCapabilities.CREATE
                    ).build(),

                Policy.Rule.builder().path("${hostSshMountName(environment.reference)}/config/ca")
                    .capabilities(Policy.BuiltinCapabilities.READ).build(),

            )
        )
        resourceGroup.addResource(backupPolicy)

        return resourceGroup
    }

    fun createTenantVaultConfig(
        parentResourceGroups: Set<ResourceGroup>,
        tenant: TenantEntity
    ): ResourceGroup {
        val resourceGroup = ResourceGroup("vaultConfig", parentResourceGroups)

        val serverPkiMount = VaultMount(tenantServerPkiMountName(tenant.reference), "pki")
        val serverPkiBackendRole = VaultPkiBackendRole(
            name = tenantServerPkiMountName(tenant.reference),
            allowedDomains = listOf(serversDomain(tenant.reference, tenant.environment.cloud.rootDomain)),
            allowSubdomains = true,
            allowLocalhost = tenant.environment.cloud.isDevelopment,
            generateLease = true,
            serverFlag = true,
            clientFlag = false,
            maxTtl = "168h",
            ttl = "168h",
            keyBits = 521,
            keyType = "ec",
            mount = serverPkiMount
        )
        resourceGroup.addResource(serverPkiBackendRole)

        val clientPkiMount = VaultMount(tenantClientPkiMountName(tenant.reference), "pki")
        val clientPkiBackendRole = VaultPkiBackendRole(
            name = tenantClientPkiMountName(tenant.reference),
            allowedDomains = listOf(clientsDomain()),
            allowSubdomains = true,
            generateLease = true,
            serverFlag = false,
            clientFlag = true,
            maxTtl = "168h",
            ttl = "168h",
            keyBits = 521,
            keyType = "ec",
            mount = clientPkiMount
        )
        resourceGroup.addResource(clientPkiBackendRole)

        val kvMount = VaultMount(kvMountName(tenant.reference), "kv-v2")
        resourceGroup.addResource(kvMount)

        return resourceGroup
    }
}
