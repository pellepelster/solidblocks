package de.solidblocks.provisioner.vault.pki

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.pki.dto.PkiBackendRole
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import mu.KotlinLogging
import org.joda.time.Instant
import org.joda.time.MutablePeriod
import org.joda.time.format.PeriodFormatterBuilder
import org.springframework.stereotype.Component
import org.springframework.vault.core.VaultTemplate
import java.util.*
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

@Component
class VaultPkiBackendRoleProvisioner(
        val vaultRootClientProvider: VaultRootClientProvider,
        val provisioner: Provisioner,
) :
        IResourceLookupProvider<IVaultPkiBackendRoleLookup, VaultPkiBackendRoleRuntime>,
        IInfrastructureResourceProvisioner<VaultPkiBackendRole, VaultPkiBackendRoleRuntime> {

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

    private fun keysExist(
            client: VaultTemplate,
            resource: IVaultPkiBackendRoleLookup
    ): Boolean {
        val pem = client.doWithVault {
            val pem = it.getForEntity("${resource.mount().id()}/ca/pem", String::class.java)
            pem
        }

        return pem.hasBody()
    }

    override fun diff(resource: VaultPkiBackendRole): Result<ResourceDiff> {
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
                        VaultPkiBackendRole::maxTtl to it.backendRole::max_ttl,
                        compareTtl
                    )
                )

                changes.add(
                    createDiff(
                        resource,
                        it,
                        VaultPkiBackendRole::ttl to it.backendRole::ttl,
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

    override fun apply(resource: VaultPkiBackendRole): Result<*> {
        val vaultClient = vaultRootClientProvider.createClient()

        val role = PkiBackendRole(
            key_type = resource.keyType,
            key_bits = resource.keyBits,
            max_ttl = resource.maxTtl,
            ttl = resource.ttl,
            allow_any_name = resource.allowAnyName,
            generate_lease = resource.generateLease,
        )

        val response = vaultClient.write("${resource.mount.id()}/roles/${resource.id}", role)

        if (!keysExist(vaultClient, resource)) {
            val response = vaultClient.write(
                    "${resource.mount.id()}/root/generate/internal",
                    mapOf("common_name" to "${resource.id} root")
            )
        }

        return Result(resource, response)
    }

    override fun getResourceType(): Class<VaultPkiBackendRole> {
        return VaultPkiBackendRole::class.java
    }

    override fun lookup(lookup: IVaultPkiBackendRoleLookup): Result<VaultPkiBackendRoleRuntime> {
        val vaultClient = vaultRootClientProvider.createClient()

        val role = vaultClient.read("${lookup.mount().id()}/roles/${lookup.id()}", PkiBackendRole::class.java)
                ?: return Result(lookup, null)

        val keysExist = keysExist(vaultClient, lookup)

        return Result(lookup, VaultPkiBackendRoleRuntime(role.data!!, keysExist))
    }

    override fun getLookupType(): Class<*> {
        return IVaultPkiBackendRoleLookup::class.java
    }
}
