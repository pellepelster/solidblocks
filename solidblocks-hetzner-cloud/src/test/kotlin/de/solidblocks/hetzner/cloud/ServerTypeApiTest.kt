package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.resources.ServerTypeNameFilter
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTypeApiTest : BaseTest() {

    @Test
    fun testServerTypeFlow() {
        runBlocking {
            val serverTypes = api.serverTypes.list()
            serverTypes shouldHaveAtLeastSize 1

            val first = serverTypes.first()
            first.id shouldNotBe null
            first.name shouldNotBe null
            first.description shouldNotBe null
            first.cores shouldNotBe null
            first.memory shouldNotBe null
            first.disk shouldNotBe null

            val byId = api.serverTypes.get(first.id)
            byId shouldNotBe null
            byId!!.name shouldBe first.name

            val byName = api.serverTypes.get(first.name)
            byName shouldNotBe null
            byName!!.id shouldBe first.id

            val byFilter = api.serverTypes.list(listOf(ServerTypeNameFilter(first.name)))
            byFilter shouldHaveSize 1
            byFilter.first().id shouldBe first.id
        }
    }
}
