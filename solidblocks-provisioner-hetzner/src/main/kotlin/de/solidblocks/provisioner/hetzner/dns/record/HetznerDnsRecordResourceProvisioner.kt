package de.solidblocks.provisioner.hetzner.dns.record

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.InfrastructureProvisioner
import de.solidblocks.core.Result
import de.solidblocks.core.Result.Companion.onSuccess
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import io.pelle.hetzner.HetznerDnsAPI
import io.pelle.hetzner.model.RecordRequest
import io.pelle.hetzner.model.RecordType
import mu.KotlinLogging

class HetznerDnsRecordResourceProvisioner(
    hetznerDnsAPI: HetznerDnsAPI,
    val provisioner: InfrastructureProvisioner,
) :
    IResourceLookupProvider<IDnsRecordLookup, DnsRecordRuntime>,
    IInfrastructureResourceProvisioner<DnsRecord, DnsRecordRuntime>,
    BaseHetznerProvisioner<DnsRecord, DnsRecordRuntime, HetznerDnsAPI>(hetznerDnsAPI) {

    private val logger = KotlinLogging.logger {}

    private fun resolveIp(resource: DnsRecord): Result<String> {
        if (resource.floatingIp != null) {
            return provisioner.lookup(resource.floatingIp).mapResult { it?.ipv4 }
        }

        if (resource.floatingIpAssignment != null) {
            return provisioner.lookup(resource.floatingIpAssignment.floatingIp).mapResult { it?.ipv4 }
        }

        if (resource.server != null) {
            return provisioner.lookup(resource.server).mapResult { it?.privateIp }
        }

        return Result(failed = true, message = "neither floating ip nor server was provided")
    }

    override fun diff(resource: DnsRecord): Result<ResourceDiff> {
        return onSuccess(
            lookup(resource),
            resolveIp(resource)
        ) { first, second ->
            if (first == null || second == null) {
                return@onSuccess ResourceDiff(resource, missing = true)
            }

            val changes = ArrayList<ResourceDiffItem>()

            if (first.value != second) {
                changes.add(
                    ResourceDiffItem(
                        "ipaddress",
                        changed = true,
                        actualValue = first.value,
                        expectedValue = second
                    )
                )
            }

            if (first.ttl != resource.ttl) {
                changes.add(
                    ResourceDiffItem(
                        resource::ttl,
                        actualValue = first.ttl.toString(),
                        expectedValue = resource.ttl.toString()
                    )
                )
            }
            if (first.name != resource.name) {
                changes.add(
                    ResourceDiffItem(
                        name = resource.name,
                        actualValue = first.name,
                        expectedValue = resource.name
                    )
                )
            }

            ResourceDiff(resource, changes = changes)
        }
    }

    override fun apply(resource: DnsRecord): Result<*> {
        return onSuccess(
            provisioner.lookup(resource.dnsZone),
            resolveIp(resource)
        ) { first, second ->

            val recordRequest = RecordRequest.builder()
            recordRequest.name(resource.name)
            recordRequest.type(RecordType.A)
            recordRequest.value(second)
            recordRequest.ttl(resource.ttl)
            recordRequest.zoneId(first?.id)

            lookup(resource).mapResourceResultOrElse(
                { record ->
                    checkedApiCall {
                        it.updateRecord(record.id, recordRequest.build())
                    }
                },
                {
                    checkedApiCall {
                        it.createRecord(recordRequest.build())
                    }
                }
            )
        }
    }

    override fun lookup(lookup: IDnsRecordLookup): Result<DnsRecordRuntime> {
        val result = this.provisioner.lookup(lookup.dnsZone)

        if (result.isEmptyOrFailed()) {
            return Result(failed = true)
        }

        return checkedApiCall {
            it.getRecords(result.result!!.id).firstOrNull { it.name == lookup.name }
        }.mapNonNullResult {
            DnsRecordRuntime(it.id, it.name, it.value, it.ttl)
        }
    }

    override val resourceType = DnsRecord::class.java

    override val lookupType = IDnsRecordLookup::class.java
}
