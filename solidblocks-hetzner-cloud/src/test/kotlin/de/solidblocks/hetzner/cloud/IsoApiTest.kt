package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.Architecture
import de.solidblocks.hetzner.cloud.resources.IsoArchitectureFilter
import de.solidblocks.hetzner.cloud.resources.IsoIncludeArchitectureWildcardFilter
import de.solidblocks.hetzner.cloud.resources.IsoNameFilter
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsoApiTest : BaseTest() {

    @Test
    fun testIsosFlow() {
        runBlocking {
            val isos = api.isos.list()
            isos shouldHaveAtLeastSize 1

            val first = isos.first()
            first.id shouldNotBe null
            first.description shouldNotBe null

            val byId = api.isos.get(first.id)
            byId shouldNotBe null
            byId!!.id shouldBe first.id

            val named = isos.firstOrNull { it.name != null }
            if (named != null) {
                val byNameFilter = api.isos.list(listOf(IsoNameFilter(named.name!!)))
                byNameFilter shouldHaveSize 1
                byNameFilter.first().id shouldBe named.id
            }

            val x86Isos = api.isos.list(listOf(IsoArchitectureFilter(Architecture.x86)))
            x86Isos shouldHaveAtLeastSize 1
            x86Isos.all { it.architecture == "x86" } shouldBe true

            api.isos.list(listOf(IsoArchitectureFilter(Architecture.x86), IsoIncludeArchitectureWildcardFilter(true))) shouldHaveAtLeastSize x86Isos.size
            api.isos.list(listOf(IsoArchitectureFilter(Architecture.x86), IsoIncludeArchitectureWildcardFilter(false))) shouldHaveAtLeastSize x86Isos.size
        }
    }
}
