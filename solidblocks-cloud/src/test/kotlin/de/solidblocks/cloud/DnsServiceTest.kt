package de.solidblocks.cloud

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DnsServiceTest() {

    @Test
    fun testGetInvalidARecords() {
        assertSoftly(DnsService().tryResolveARecords("invalid-domain-pelle.io.")) {
            it shouldHaveSize 8
            it.all { it.values.isEmpty() } shouldBe true
        }
    }

    @Test
    fun testGetARecords() {
        assertSoftly(DnsService().tryResolveARecords("pelle.io.")) {
            it shouldHaveAtLeastSize 4
            it.none { it.values.isEmpty() } shouldBe true
        }
    }
}
