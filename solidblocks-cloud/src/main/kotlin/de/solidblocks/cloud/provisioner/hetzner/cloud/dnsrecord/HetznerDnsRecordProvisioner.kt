package de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord

import de.solidblocks.cloud.api.*
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import de.solidblocks.hetzner.cloud.resources.DnsRRSetRecord
import de.solidblocks.hetzner.cloud.resources.DnsRRSetsCreateRequest
import de.solidblocks.hetzner.cloud.resources.RRType
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.logError
import de.solidblocks.utils.logWarning
import io.github.oshai.kotlinlogging.KotlinLogging

class HetznerDnsRecordProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    ResourceLookupProvider<HetznerDnsRecordLookup, HetznerDnsRecordRuntime>,
    InfrastructureResourceProvisioner<HetznerDnsRecord, HetznerDnsRecordRuntime> {

  private val logger = KotlinLogging.logger {}

  override suspend fun lookup(
      lookup: HetznerDnsRecordLookup,
      context: ProvisionerContext,
  ): HetznerDnsRecordRuntime? {
    val zone = context.lookup(lookup.zone) ?: return null

    val record = api.dnsRrSets(zone.name).get(lookup.name, RRType.A) ?: return null

    return HetznerDnsRecordRuntime(
        record.rrset.id,
        record.rrset.name,
        zone.name,
        record.rrset.records.map { it.value },
    )
  }

  override suspend fun diff(
      resource: HetznerDnsRecord,
      context: ProvisionerContext,
  ) =
      lookup(resource.asLookup(), context)?.let { runtime ->
        val changes = mutableListOf<ResourceDiffItem>()

        val expectedValues = resource.values.mapNotNull { context.lookup(it)?.publicIpv4 }

        if (expectedValues != runtime.values) {
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

        if (changes.isNotEmpty()) {
          ResourceDiff(resource, has_changes, changes = changes)
        } else {
          ResourceDiff(resource, up_to_date)
        }
      } ?: ResourceDiff(resource, missing)

  override suspend fun apply(
      resource: HetznerDnsRecord,
      context: ProvisionerContext,
      log: LogContext,
  ): ApplyResult<HetznerDnsRecordRuntime> {
    val current = lookup(resource.asLookup(), context)
    if (current != null) {
      return ApplyResult(current)
    }

    val zone = context.lookup(resource.asLookup().zone) ?: return ApplyResult(null)

    val serverRuntimes =
        resource.values.map {
          val runtime = context.lookup(it)

          if (runtime == null) {
            logError("could not resolve server ${it.logText()}")
          }

          if (runtime?.publicIpv4 == null) {
            logWarning("server ${it.logText()} has no public ip address", context = log)
          }

          runtime
        }

    val result =
        api.dnsRrSets(zone.name)
            .create(
                DnsRRSetsCreateRequest(
                    resource.name,
                    RRType.A,
                    serverRuntimes.mapNotNull { it?.publicIpv4 }.map { DnsRRSetRecord(it) },
                ),
            ) ?: return ApplyResult(null)

    return ApplyResult(lookup(resource.asLookup(), context))
  }

  override val supportedLookupType = HetznerDnsRecordLookup::class

  override val supportedResourceType = HetznerDnsRecord::class
}
