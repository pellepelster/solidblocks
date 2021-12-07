package de.solidblocks.provisioner.hetzner.dns

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.dns.DnsRecord
import de.solidblocks.api.resources.dns.DnsRecordRuntime
import de.solidblocks.api.resources.dns.IDnsRecordLookup
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.core.Result.Companion.onSuccess
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import io.pelle.hetzner.HetznerDnsAPI
import io.pelle.hetzner.model.RecordRequest
import io.pelle.hetzner.model.RecordType
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerDnsRecordResourceProvisioner(
        val provisioner: Provisioner,
        val hetznerDnsAPI: HetznerDnsAPI
) :
        IResourceLookupProvider<IDnsRecordLookup, DnsRecordRuntime>,
        IInfrastructureResourceProvisioner<DnsRecord, DnsRecordRuntime>,
        BaseHetznerProvisioner<DnsRecord, DnsRecordRuntime, HetznerDnsAPI>(hetznerDnsAPI) {

    private val logger = KotlinLogging.logger {}

    fun resolveIp(resource: DnsRecord): Result<String> {
        if (resource.floatingIp != null) {
            return provisioner.lookup(resource.floatingIp!!).mapResourceResult { it?.ipv4 }
        }

        if (resource.server != null) {
            return provisioner.lookup(resource.server!!).mapResourceResult { it?.privateIp }
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
            if (first.name != resource.id) {
                changes.add(
                    ResourceDiffItem(
                        name = resource.id(),
                        actualValue = first.name,
                        expectedValue = resource.id
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
            recordRequest.name(resource.id)
            recordRequest.type(RecordType.A)
            recordRequest.value(second)
            recordRequest.ttl(resource.ttl)
            recordRequest.zoneId(first?.id)

            lookup(resource).mapResourceResultOrElse(
                { record ->
                    checkedApiCall(HetznerDnsAPI::updateRecord) {
                        it.updateRecord(record.id, recordRequest.build())
                    }
                },
                {
                    checkedApiCall(HetznerDnsAPI::createRecord) {
                        it.createRecord(recordRequest.build())
                    }
                }
            )
        }
    }

    override fun getResourceType(): Class<*> {
        return DnsRecord::class.java
    }

    override fun lookup(lookup: IDnsRecordLookup): Result<DnsRecordRuntime> {
        val result = this.provisioner.lookup(lookup.dnsZone())

        if (result.isEmptyOrFailed()) {
            return Result(failed = true)
        }

        return checkedApiCall(HetznerDnsAPI::getRecords) {
            it.getRecords(result.result!!.id).firstOrNull { it.name == lookup.id() }
        }.mapNonNullResult {
            DnsRecordRuntime(it.id, it.name, it.value, it.ttl)
        }
    }

    override fun getLookupType(): Class<*> {
        return IDnsRecordLookup::class.java
    }
}
