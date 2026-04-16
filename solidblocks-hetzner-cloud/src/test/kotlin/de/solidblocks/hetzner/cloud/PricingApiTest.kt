package de.solidblocks.hetzner.cloud

import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PricingApiTest : BaseTest() {

    @Test
    fun testPricingFlow() {
        runBlocking {
            val pricing = api.pricing.get()
            pricing shouldNotBe null
            pricing.currency shouldNotBe null
            pricing.vatRate shouldNotBe null
            pricing.image shouldNotBe null
            pricing.image.pricePerGbMonth shouldNotBe null
            pricing.serverBackup shouldNotBe null
            pricing.serverTypes shouldHaveAtLeastSize 1
            pricing.loadBalancerTypes shouldHaveAtLeastSize 1
            pricing.volume shouldNotBe null
        }
    }
}
