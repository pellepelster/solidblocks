package de.solidblocks.provisioner.vault.ssh

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.base.Utils
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import de.solidblocks.provisioner.vault.ssh.dto.SshBackendRole
import de.solidblocks.provisioner.vault.ssh.dto.SshBackendRoleDefaultExtensions
import mu.KotlinLogging
import org.joda.time.Instant
import org.joda.time.MutablePeriod
import org.joda.time.format.PeriodFormatterBuilder
import org.springframework.stereotype.Component
import org.springframework.vault.VaultException
import org.springframework.vault.core.VaultTemplate
import java.util.*
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

@Component
class VaultSshBackendRoleProvisioner(val vaultRootClientProvider: VaultRootClientProvider, val provisioner: Provisioner) :
    IResourceLookupProvider<IVaultSshBackendRoleLookup, VaultSshBackendRoleRuntime>,
    IInfrastructureResourceProvisioner<VaultSshBackendRole, VaultSshBackendRoleRuntime> {

    private val compareTtl: (String, String) -> Boolean = { expectedValue, actualValue ->
        val period = MutablePeriod()

        val parser = PeriodFormatterBuilder()
            .appendHours().appendSuffix("h")
            .appendMinutes().appendSuffix("m")
            .toParser()

        parser.parseInto(period, expectedValue, 0, Locale.getDefault())
        val expectedSeconds = period.toDurationFrom(Instant.now()).standardSeconds

        actualValue.toLong() == expectedSeconds
    }

    private val compare: (String, String) -> Boolean = { expectedValue, actualValue ->
        expectedValue == actualValue
    }

    private val logger = KotlinLogging.logger {}

    private fun keysExist(
        client: VaultTemplate,
        resource: IVaultSshBackendRoleLookup
    ): Boolean {
        var keysExist = true
        try {
            client.read("${resource.mount().id()}/config/ca", Object::class.java)
        } catch (e: VaultException) {
            if (e.message?.contains("keys haven't been configured yet") == true) {
                keysExist = false
            }
        }

        return keysExist
    }

    override fun diff(resource: VaultSshBackendRole): Result<ResourceDiff> {
        return lookup(resource).mapResourceResult {

            val changes = ArrayList<ResourceDiffItem>()
            val missing = it == null

            if (it != null) {

                if (!it.keysExist) {
                    changes.add(
                        ResourceDiffItem(
                            name = "keysExists",
                            changed = true,
                            expectedValue = "true",
                            actualValue = "false"
                        )
                    )
                }

                changes.add(
                    createDiff(
                        resource,
                        it,
                        VaultSshBackendRole::maxTtl to it.backendRole::max_ttl,
                        compareTtl
                    )
                )

                changes.add(
                    createDiff(
                        resource,
                        it,
                        VaultSshBackendRole::ttl to it.backendRole::ttl,
                        compareTtl
                    )
                )
                /*
                changes.add(
                    createDiff(
                        resource,
                        it,
                        VaultSshBackendRole::allowedUsers to it.backendRole::allowed_users,
                        compare
                    )
                )

                changes.add(
                    createDiff(
                        resource,
                        it,
                        VaultSshBackendRole::allowedUsers to it.backendRole::allowed_users,
                        compare
                    )
                )*/
            }

            ResourceDiff(resource, missing = missing, changes = changes)
        }
    }

    private fun <EXPECTED, ACTUAL> createDiff(
        expected: EXPECTED,
        actual: ACTUAL,
        pair: Pair<KProperty1<EXPECTED, String>, KProperty0<String?>>,
        equals: (String, String) -> Boolean
    ): ResourceDiffItem {
        val expectedValue = pair.first.getValue(expected, pair.first)
        val actualValue = pair.second.getValue(actual, pair.second)

        if (actualValue == null) {
            return ResourceDiffItem(
                pair.first.name,
                missing = true,
                expectedValue = expectedValue,
                actualValue = actualValue
            )
        }

        val hasDiff = !equals.invoke(expectedValue, actualValue)

        return ResourceDiffItem(
            pair.first.name,
            changed = hasDiff,
            expectedValue = expectedValue,
            actualValue = actualValue
        )
    }

    override fun apply(resource: VaultSshBackendRole): Result<*> {
        val vaultClient = vaultRootClientProvider.createClient()

        val role = SshBackendRole(
            key_type = resource.keyType,
            max_ttl = resource.maxTtl,
            ttl = resource.ttl,
            allow_host_certificates = resource.allowHostCertificates,
            allow_user_certificates = resource.allowUserCertificates,
            allowed_users = resource.allowedUsers.joinToString(separator = ","),
            default_user = resource.defaultUser,
            default_extensions = resource.defaultExtensions?.let {
                SshBackendRoleDefaultExtensions(
                    it.permitPty,
                    it.permitPortForwarding
                )
            }
        )

        val response = vaultClient.write("${resource.mount.id()}/roles/${resource.id}", role)

        if (!keysExist(vaultClient, resource)) {
            val key = Utils.generateSshKey(resource.mount.id())
            vaultClient.write(
                "${resource.mount.id()}/config/ca",
                mapOf("private_key" to key.first, "public_key" to key.second)
            )
        }

        return Result(response)
    }

    override fun getResourceType(): Class<VaultSshBackendRole> {
        return VaultSshBackendRole::class.java
    }

    override fun lookup(lookup: IVaultSshBackendRoleLookup): Result<VaultSshBackendRoleRuntime> {
        val vaultClient = vaultRootClientProvider.createClient()

        val role = vaultClient.read("${lookup.mount().id()}/roles/${lookup.id()}", SshBackendRole::class.java)
                ?: return Result(null)

        val keysExist = keysExist(vaultClient, lookup)

        return Result(VaultSshBackendRoleRuntime(role.data!!, keysExist))
    }

    override fun getLookupType(): Class<*> {
        return IVaultSshBackendRoleLookup::class.java
    }
}
