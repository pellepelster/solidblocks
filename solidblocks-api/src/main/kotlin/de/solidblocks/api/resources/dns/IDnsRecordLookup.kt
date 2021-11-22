package de.solidblocks.api.resources.dns

import de.solidblocks.core.IResourceLookup

interface IDnsRecordLookup : IResourceLookup<DnsRecordRuntime> {
    fun dnsZone(): IDnsZoneLookup
}