package de.solidblocks.cloud

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DnsServiceTest {

    @Test
    fun `get invalid a records`() {
        assertSoftly(DnsService().tryResolveARecords("invalid-domain-pelle.io.")) {
            it shouldHaveSize 8
            it.all { it.values.isEmpty() } shouldBe true
        }
    }

    @Test
    fun `get a records`() {
        assertSoftly(DnsService().tryResolveARecords("pelle.io.")) {
            it shouldHaveAtLeastSize 4
            it.none { it.values.isEmpty() } shouldBe true
        }
    }
}
