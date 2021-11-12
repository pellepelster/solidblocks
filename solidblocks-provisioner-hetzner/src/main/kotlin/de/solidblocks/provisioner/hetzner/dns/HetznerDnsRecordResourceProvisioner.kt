package de.solidblocks.provisioner.hetzner.dns

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.ResourceDiffItem
import de.solidblocks.api.resources.dns.DnsRecord
import de.solidblocks.api.resources.dns.DnsRecordRuntime
import de.solidblocks.core.Result
import de.solidblocks.core.Result.Companion.onNonNullSuccess
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
    val credentialsProvider: HetznerDnsCredentialsProvider
) :
    BaseHetznerProvisioner<DnsRecord, DnsRecordRuntime, HetznerDnsAPI>(
        { HetznerDnsAPI(credentialsProvider.defaultApiToken()) },
        DnsRecord::class.java
    ) {

    private val logger = KotlinLogging.logger {}

    override fun lookup(resource: DnsRecord): Result<DnsRecordRuntime> {
        return this.provisioner.lookup(resource.zone).mapNonNullResultNullable { zone ->
            checkedApiCall(resource, HetznerDnsAPI::getRecords) {
                it.getRecords(zone.id).firstOrNull { it.name == resource.name }
            }.mapNonNull {
                DnsRecordRuntime(it.id, it.name, it.value, it.ttl)
            }
        }
    }

    fun resolveIp(resource: DnsRecord): Result<String> {
        if (resource.floatingIp != null) {
            return provisioner.lookup(resource.floatingIp!!).mapResourceResult { it?.ipv4 }
        }

        if (resource.server != null) {
            return provisioner.lookup(resource.server!!).mapResourceResult { it?.privateIp }
        }

        return Result(resource, failed = true, message = "neither floating ip nor server was provided")
    }

    override fun diff(resource: DnsRecord): Result<ResourceDiff<DnsRecordRuntime>> {
        return onSuccess(
            resource,
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
                        resource::name,
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
            resource,
            provisioner.lookup(resource.zone),
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
                    checkedApiCall(resource, HetznerDnsAPI::updateRecord) {
                        it.updateRecord(record.id, recordRequest.build())
                    }
                },
                {
                    checkedApiCall(resource, HetznerDnsAPI::createRecord) {
                        it.createRecord(recordRequest.build())
                    }
                }
            )
        }
    }

    override fun destroy(resource: DnsRecord): Result<*> {
        TODO("Not yet implemented")
    }

    override fun destroyAll(): Result<*> {
        TODO("Not yet implemented")
    }
}
