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
import de.solidblocks.vault.VaultConstants.backupPolicyName
import de.solidblocks.vault.VaultConstants.domain
import de.solidblocks.vault.VaultConstants.environentBasePolicyName
import de.solidblocks.vault.VaultConstants.hostSshMountName
import de.solidblocks.vault.VaultConstants.ingressPolicyName
import de.solidblocks.vault.VaultConstants.kvMountName
import de.solidblocks.vault.VaultConstants.pkiMountName
import de.solidblocks.vault.VaultConstants.providersGithubPolicy
import de.solidblocks.vault.VaultConstants.tenantBaseServicePolicyName
import de.solidblocks.vault.VaultConstants.tokenSelfRenewalPolicies
import de.solidblocks.vault.VaultConstants.userSshMountName
import org.springframework.vault.support.Policy

object VaultCloudConfiguration {

    fun createEnvironmentVaultConfig(
        parentResourceGroups: Set<ResourceGroup>,
        environment: EnvironmentEntity
    ): ResourceGroup {
        val resourceGroup = ResourceGroup("vaultConfig", parentResourceGroups)

        val hostPkiMount = VaultMount(pkiMountName(environment.reference), "pki")
        val hostPkiBackendRole = VaultPkiBackendRole(
            name = pkiMountName(environment.reference),
            allowedDomains = listOf(domain(environment.reference, environment.cloud.rootDomain)),
            allowSubdomains = true,
            allowLocalhost = environment.cloud.isDevelopment,
            generateLease = true,
            maxTtl = "168h",
            ttl = "168h",
            keyBits = 521,
            keyType = "ec",
            mount = hostPkiMount
        )
        resourceGroup.addResource(hostPkiBackendRole)

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
                    .path("${pkiMountName(environment.reference)}/issue/${pkiMountName(environment.reference)}")
                    .capabilities(Policy.BuiltinCapabilities.UPDATE).build(),

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

        val baseServicePolicy = VaultPolicy(
            environentBasePolicyName(environment.reference),
            setOf(
                providersGithubPolicy(environment.reference)
            ) + tokenSelfRenewalPolicies()
        )
        resourceGroup.addResource(baseServicePolicy)

        val backupPolicy = VaultPolicy(
            backupPolicyName(environment.reference),
            setOf(
                providersGithubPolicy(environment.reference),
                Policy.Rule.builder().path(
                    "${kvMountName(environment)}/data/solidblocks/cloud/config"
                ).capabilities(Policy.BuiltinCapabilities.READ).build(),

                Policy.Rule.builder().path(
                    "${kvMountName(environment)}/data/solidblocks/cloud/config/consul"
                ).capabilities(Policy.BuiltinCapabilities.READ).build(),
                Policy.Rule.builder().path(
                    "${kvMountName(environment)}/data/solidblocks/cloud/providers/hetzner"
                ).capabilities(Policy.BuiltinCapabilities.READ).build(),

                Policy.Rule.builder()
                    .path("${pkiMountName(environment.reference)}/issue/${pkiMountName(environment.reference)}")
                    .capabilities(Policy.BuiltinCapabilities.UPDATE).build(),

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

            ) + tokenSelfRenewalPolicies()
        )
        resourceGroup.addResource(backupPolicy)

        return resourceGroup
    }

    fun createTenantVaultConfig(
        parentResourceGroups: Set<ResourceGroup>,
        tenant: TenantEntity
    ): ResourceGroup {
        val resourceGroup = ResourceGroup("vaultConfig", parentResourceGroups)

        val hostPkiMount = VaultMount(pkiMountName(tenant.reference), "pki")
        val hostPkiBackendRole = VaultPkiBackendRole(
            name = pkiMountName(tenant.reference),
            allowedDomains = listOf(domain(tenant.reference, tenant.environment.cloud.rootDomain)),
            allowSubdomains = true,
            allowLocalhost = tenant.environment.cloud.isDevelopment,
            generateLease = true,
            maxTtl = "168h",
            ttl = "168h",
            keyBits = 521,
            keyType = "ec",
            mount = hostPkiMount
        )
        resourceGroup.addResource(hostPkiBackendRole)

        val kvMount = VaultMount(kvMountName(tenant.reference), "kv-v2")
        resourceGroup.addResource(kvMount)

        val baseServicePolicy = VaultPolicy(
            tenantBaseServicePolicyName(tenant.reference),
            setOf(
                Policy.Rule.builder().path("${pkiMountName(tenant.reference)}/issue/${pkiMountName(tenant.reference)}")
                    .capabilities(Policy.BuiltinCapabilities.UPDATE).build(),
            )
        )
        resourceGroup.addResource(baseServicePolicy)

        return resourceGroup
    }
}
