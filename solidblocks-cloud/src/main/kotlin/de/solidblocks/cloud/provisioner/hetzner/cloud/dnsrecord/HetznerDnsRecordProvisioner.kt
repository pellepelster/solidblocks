package de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.equalsIgnoreOrder
import de.solidblocks.hetzner.cloud.resources.*
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logError
import io.github.oshai.kotlinlogging.KotlinLogging

class HetznerDnsRecordProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<HetznerDnsRecordLookup, HetznerDnsRecordRuntime>,
    InfrastructureResourceProvisioner<HetznerDnsRecord, HetznerDnsRecordRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun lookup(lookup: HetznerDnsRecordLookup, context: CloudProvisionerContext): HetznerDnsRecordRuntime? {
        val zone = context.lookup(lookup.zone) ?: return null

        val record = api.dnsRrSets(zone.name).get(lookup.name, RRType.A) ?: return null

        return HetznerDnsRecordRuntime(
            record.rrset.id,
            record.rrset.name,
            record.rrset.ttl,
            zone.name,
            record.rrset.records.map { it.value },
            record.rrset.labels,
        )
    }

    override suspend fun diff(resource: HetznerDnsRecord, context: CloudProvisionerContext) = lookup(resource.asLookup(), context)?.let { runtime ->
        val changes = mutableListOf<ResourceDiffItem>()

        if (runtime.ttl != resource.ttl) {
            changes.add(
                ResourceDiffItem(
                    "value",
                    false,
                    false,
                    true,
                    expectedValue = resource.ttl,
                    actualValue = runtime.ttl,
                ),
            )
        }

        val expectedValues = resource.values.mapNotNull { context.lookup(it)?.publicIpv4 }
        if (!(expectedValues equalsIgnoreOrder runtime.values)) {
            changes.add(
                ResourceDiffItem(
                    "value",
                    false,
                    false,
                    true,
                    expectedValue = expectedValues.joinToString(","),
                    actualValue = runtime.values.joinToString(","),
                ),
            )
        }

        changes.addAll(createLabelDiff(resource, runtime))

        if (changes.isNotEmpty()) {
            ResourceDiff(resource, has_changes, changes = changes)
        } else {
            ResourceDiff(resource, up_to_date)
        }
    } ?: ResourceDiff(resource, missing)

    override suspend fun apply(resource: HetznerDnsRecord, context: CloudProvisionerContext, log: LogContext): Result<HetznerDnsRecordRuntime> {
        val current = lookup(resource.asLookup(), context)
        val zone =
            context.lookup(resource.asLookup().zone)
                ?: return Error("zone '${resource.asLookup().zone.name}' not found'")

        val serverIps =
            resource.values.mapNotNull {
                val runtime = context.lookup(it)

                if (runtime == null) {
                    logError("could not resolve server ${it.logText()}")
                }

                if (runtime?.publicIpv4 == null) {
                    log.warning("server ${it.logText()} has no public ip address")
                }

                runtime?.publicIpv4
            }

        if (current != null) {
            if (current.ttl != resource.ttl) {
                logger.info { "updating ${resource.name}/${resource.type} TTL to ${resource.ttl}" }
                val ttlUpdateResult =
                    api.dnsRrSets(zone.name)
                        .updateTTL(resource.name, resource.type, DnsRRSetsTTLUpdateRequest(resource.ttl))
                api.dnsRrSets(zone.name).waitForAction(ttlUpdateResult.action) {
                    log.info("waiting for TTL update on ${resource.logText()}")
                }
            }

            api.dnsRrSets(zone.name).update(
                resource.name,
                resource.type,
                DnsRRSetsUpdateRequest(resource.labels),
            )

            if (!(current.values equalsIgnoreOrder serverIps)) {
                logger.info {
                    "updating ${resource.name}/${resource.type} values to ${serverIps.joinToString(",")}"
                }
                val ttlUpdateResult =
                    api.dnsRrSets(zone.name)
                        .updateRecords(
                            resource.name,
                            resource.type,
                            DnsRRSetsRecordsUpdateRequest(
                                serverIps.map { DnsRRSetRecord(it) },
                            ),
                        )
                api.dnsRrSets(zone.name).waitForAction(ttlUpdateResult.action) {
                    log.info("waiting for record update on ${resource.logText()}")
                }
            }
        } else {
            createRRSet(zone, resource, serverIps)
        }

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<HetznerDnsRecordRuntime>("error creating ${resource.logText()}")
    }

    private suspend fun createRRSet(zone: HetznerDnsZoneRuntime, resource: HetznerDnsRecord, serverIps: List<String>) = api.dnsRrSets(zone.name)
        .create(
            DnsRRSetsCreateRequest(
                resource.name,
                resource.type,
                ttl = resource.ttl,
                serverIps.map { DnsRRSetRecord(it) },
                labels = resource.labels,
            ),
        )

    override val supportedLookupType = HetznerDnsRecordLookup::class

    override val supportedResourceType = HetznerDnsRecord::class
}
