package de.solidblocks.provisioner.vault.ssh

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.base.Utils
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.ssh.dto.SshBackendRoleDefaultExtensions
import de.solidblocks.provisioner.vault.ssh.dto.SshBackendRole
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
class VaultSshBackendRoleProvisioner(
    val provisioner: Provisioner,
) :
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

    private val logger = KotlinLogging.logger {}

    override fun lookup(resource: VaultSshBackendRole): Result<VaultSshBackendRoleRuntime> {
        val vaultClient = provisioner.provider(VaultTemplate::class.java).createClient()

        val role = vaultClient.read("${resource.mount.name}/roles/${resource.name}", SshBackendRole::class.java)
            ?: return Result(resource, null)

        val keysExist = keysExist(vaultClient, resource)

        return Result(resource, VaultSshBackendRoleRuntime(role.data!!, keysExist))
    }

    private fun keysExist(
        client: VaultTemplate,
        resource: VaultSshBackendRole
    ): Boolean {
        var keysExist = true
        try {
            client.read("${resource.mount.name}/config/ca", Object::class.java)
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
        val vaultClient = provisioner.provider(VaultTemplate::class.java).createClient()

        val role = SshBackendRole(
            key_type = resource.keyType,
            max_ttl = resource.maxTtl,
            ttl = resource.ttl,
            allow_host_certificates = resource.allowHostCertificates,
            allow_user_certificates = resource.allowUserCertificates,
            allowed_users = resource.allowedUsers,
            default_extensions = resource.defaultExtensions?.let {
                SshBackendRoleDefaultExtensions(
                    it.permitPty,
                    it.permitPortForwarding
                )
            }
        )

        val response = vaultClient.write("${resource.mount.name}/roles/${resource.name}", role)

        if (!keysExist(vaultClient, resource)) {
            val key = Utils.generateSshKey(resource.mount.name)
            vaultClient.write(
                "${resource.mount.name}/config/ca",
                mapOf("private_key" to key.first, "public_key" to key.second)
            )
        }

        return Result(resource, response)
    }

    override fun getResourceType(): Class<VaultSshBackendRole> {
        return VaultSshBackendRole::class.java
    }
}
