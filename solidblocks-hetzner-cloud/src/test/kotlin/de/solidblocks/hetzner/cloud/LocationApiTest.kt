package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.resources.LocationNameFilter
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocationApiTest : BaseTest() {

    @Test
    fun testLocationsFlow() {
        runBlocking {
            val locations = api.locations.list()
            locations shouldHaveAtLeastSize 1

            val first = locations.first()
            first.id shouldNotBe null
            first.name shouldNotBe null
            first.description shouldNotBe null
            first.country shouldNotBe null
            first.city shouldNotBe null

            val byId = api.locations.get(first.id)
            byId shouldNotBe null
            byId!!.name shouldBe first.name

            val byName = api.locations.get(first.name)
            byName shouldNotBe null
            byName!!.id shouldBe first.id

            val byFilter = api.locations.list(listOf(LocationNameFilter(first.name)))
            byFilter shouldHaveSize 1
            byFilter.first().id shouldBe first.id
        }
    }
}
