package de.solidblocks.cloud

import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.cloud.config.ConfigConstants
import de.solidblocks.cloud.config.ConfigConstants.CONSUL_MASTER_TOKEN_KEY
import de.solidblocks.cloud.config.ConfigConstants.CONSUL_SECRET_KEY
import de.solidblocks.cloud.config.ConfigConstants.GITHUB_TOKEN_RO_KEY
import de.solidblocks.cloud.config.ConfigConstants.GITHUB_USERNAME_KEY
import de.solidblocks.cloud.config.ConfigConstants.HETZNER_CLOUD_API_TOKEN_RO_KEY
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.cloud.config.model.getConfigValue
import de.solidblocks.provisioner.vault.kv.VaultKV
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRole
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRole
import de.solidblocks.vault.VaultConstants
import de.solidblocks.vault.VaultConstants.hostSshMountName
import de.solidblocks.vault.VaultConstants.kvMountName
import de.solidblocks.vault.VaultConstants.pkiMountName
import de.solidblocks.vault.VaultConstants.userSshMountName
import org.springframework.vault.support.Policy

class VaultConfig {

    companion object {

        fun createVaultConfig(
            parentResourceGroups: Set<ResourceGroup>,
            environment: CloudEnvironmentConfiguration
        ): ResourceGroup {
            val resourceGroup = ResourceGroup("vaultConfig", parentResourceGroups)

            val hostPkiMount = VaultMount(pkiMountName(environment), "pki")
            val hostPkiBackendRole = VaultPkiBackendRole(
                id = pkiMountName(environment),
                allowAnyName = true,
                generateLease = true,
                maxTtl = "168h",
                ttl = "168h",
                keyBits = 521,
                keyType = "ec",
                mount = hostPkiMount
            )
            resourceGroup.addResource(hostPkiBackendRole)

            val hostSshMount = VaultMount(
                hostSshMountName(environment),
                "ssh"
            )
            val hostSshBackendRole = VaultSshBackendRole(
                id = hostSshMountName(environment),
                keyType = "ca",
                maxTtl = "168h",
                ttl = "168h",
                allowHostCertificates = true,
                allowUserCertificates = false,
                mount = hostSshMount
            )
            resourceGroup.addResource(hostSshBackendRole)

            val userSshMount = VaultMount(
                userSshMountName(environment),
                "ssh"
            )
            val userSshBackendRole = VaultSshBackendRole(
                id = userSshMountName(environment),
                keyType = "ca",
                maxTtl = "168h",
                ttl = "168h",
                allowedUsers = listOf(ConfigConstants.cloudId(environment)),
                defaultUser = ConfigConstants.cloudId(environment),
                allowHostCertificates = false,
                allowUserCertificates = true,
                mount = userSshMount
            )
            resourceGroup.addResource(userSshBackendRole)

            val kvMount = VaultMount(kvMountName(environment), "kv-v2")
            resourceGroup.addResource(kvMount)

            val solidblocksConfig =
                VaultKV(
                    "solidblocks/cloud/config",
                    mapOf(), kvMount
                )
            resourceGroup.addResource(solidblocksConfig)

            val hetznerConfig =
                VaultKV(
                    "solidblocks/cloud/providers/hetzner",
                    mapOf(HETZNER_CLOUD_API_TOKEN_RO_KEY to environment.configValues.getConfigValue(HETZNER_CLOUD_API_TOKEN_RO_KEY)!!.value), kvMount
                )
            resourceGroup.addResource(hetznerConfig)

            val githubConfig =
                VaultKV(
                    "solidblocks/cloud/providers/github",
                    mapOf(
                        GITHUB_TOKEN_RO_KEY to environment.configValues.getConfigValue(GITHUB_TOKEN_RO_KEY)!!.value,
                        GITHUB_USERNAME_KEY to "pellepelster"
                    ),
                    kvMount
                )
            resourceGroup.addResource(githubConfig)

            val consulConfig =
                VaultKV(
                    "solidblocks/cloud/config/consul",
                    mapOf(
                        CONSUL_SECRET_KEY to environment.configValues.getConfigValue(CONSUL_SECRET_KEY)!!.value,
                        CONSUL_MASTER_TOKEN_KEY to environment.configValues.getConfigValue(CONSUL_MASTER_TOKEN_KEY)!!.value
                    ),
                    kvMount
                )
            resourceGroup.addResource(consulConfig)

            val controllerPolicy = VaultPolicy(
                VaultConstants.CONTROLLER_POLICY_NAME,
                setOf(
                    Policy.Rule.builder().path(
                        "${kvMountName(environment)}/data/solidblocks/cloud/config"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path(
                        "${kvMountName(environment)}/data/solidblocks/cloud/config/consul"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path(
                        "/auth/token/renew-self"
                    )
                        .capabilities(Policy.BuiltinCapabilities.UPDATE)
                        .build(),

                    Policy.Rule.builder().path(
                        "${kvMountName(environment)}/data/solidblocks/cloud/providers/github"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path(
                        "${kvMountName(environment)}/data/solidblocks/cloud/providers/hetzner"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder()
                        .path("${pkiMountName(environment)}/issue/${pkiMountName(environment)}")
                        .capabilities(Policy.BuiltinCapabilities.UPDATE)
                        .build(),

                    Policy.Rule.builder()
                        .path("${userSshMountName(environment)}/sign/${userSshMountName(environment)}")
                        .capabilities(
                            Policy.BuiltinCapabilities.UPDATE,
                            Policy.BuiltinCapabilities.CREATE
                        )
                        .build(),

                    Policy.Rule.builder().path("${userSshMountName(environment)}/config/ca")
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder()
                        .path("${hostSshMountName(environment)}/sign/${hostSshMountName(environment)}")
                        .capabilities(
                            Policy.BuiltinCapabilities.UPDATE,
                            Policy.BuiltinCapabilities.CREATE
                        )
                        .build(),

                    Policy.Rule.builder().path("${hostSshMountName(environment)}/config/ca")
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                )
            )
            resourceGroup.addResource(controllerPolicy)

            val backupPolicy = VaultPolicy(
                VaultConstants.BACKUP_POLICY_NAME,
                setOf(
                    Policy.Rule.builder().path(
                        "${kvMountName(environment)}/data/solidblocks/cloud/config"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path(
                        "${kvMountName(environment)}/data/solidblocks/cloud/config/consul"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path(
                        "/auth/token/renew-self"
                    )
                        .capabilities(Policy.BuiltinCapabilities.UPDATE)
                        .build(),

                    Policy.Rule.builder().path(
                        "${kvMountName(environment)}/data/solidblocks/cloud/providers/github"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path(
                        "${kvMountName(environment)}/data/solidblocks/cloud/providers/hetzner"
                    )
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path("${pkiMountName(environment)}/issue/${pkiMountName(environment)}")
                        .capabilities(Policy.BuiltinCapabilities.UPDATE)
                        .build(),

                    Policy.Rule.builder().path("${userSshMountName(environment)}/sign/${userSshMountName(environment)}")
                        .capabilities(
                            Policy.BuiltinCapabilities.UPDATE,
                            Policy.BuiltinCapabilities.CREATE
                        )
                        .build(),

                    Policy.Rule.builder().path("${userSshMountName(environment)}/config/ca")
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                    Policy.Rule.builder().path("${hostSshMountName(environment)}/sign/${hostSshMountName(environment)}")
                        .capabilities(
                            Policy.BuiltinCapabilities.UPDATE,
                            Policy.BuiltinCapabilities.CREATE
                        )
                        .build(),

                    Policy.Rule.builder().path("${hostSshMountName(environment)}/config/ca")
                        .capabilities(Policy.BuiltinCapabilities.READ)
                        .build(),

                ),
            )
            resourceGroup.addResource(backupPolicy)
            return resourceGroup
        }
    }
}
