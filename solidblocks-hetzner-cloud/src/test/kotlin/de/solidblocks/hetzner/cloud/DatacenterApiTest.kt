package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.resources.DatacenterNameFilter
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatacenterApiTest : BaseTest() {

    @Test
    fun testDatacentersFlow() {
        runBlocking {
            val datacenters = api.datacenters.list()
            datacenters shouldHaveAtLeastSize 1

            val first = datacenters.first()
            first.id shouldNotBe null
            first.name shouldNotBe null
            first.description shouldNotBe null
            first.location shouldNotBe null

            val byId = api.datacenters.get(first.id)
            byId shouldNotBe null
            byId!!.name shouldBe first.name

            val byName = api.datacenters.get(first.name)
            byName shouldNotBe null
            byName!!.id shouldBe first.id

            val byFilter = api.datacenters.list(listOf(DatacenterNameFilter(first.name)))
            byFilter shouldHaveSize 1
            byFilter.first().id shouldBe first.id
        }
    }
}
